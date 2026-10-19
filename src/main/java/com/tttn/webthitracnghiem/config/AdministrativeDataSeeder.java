package com.tttn.webthitracnghiem.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Component
public class AdministrativeDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdministrativeDataSeeder.class);
    private static final String RESET_CONFIRMATION = "WIPE_DATABASE";
    private static final String RESET_STATE_TABLE = "deployment_seed_state";
    private static final Set<String> RESET_EXCLUDED_TABLES = Set.of(
            RESET_STATE_TABLE,
            "hibernate_sequence",
            "flyway_schema_history",
            "databasechangelog",
            "databasechangeloglock"
    );

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectClassRepository subjectClassRepository;

    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;

    @Autowired
    private ClassSubjectAssignmentRepository assignmentRepository;

    @Autowired
    private TrackedStudentRepository trackedStudentRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${demo.seed.password:Demo@123456}")
    private String demoSeedPassword;

    @Value("${demo.seed.enabled:true}")
    private boolean demoSeedEnabled;

    @Value("${demo.reset.enabled:false}")
    private boolean demoResetEnabled;

    @Value("${demo.reset.version:}")
    private String demoResetVersion;

    @Value("${demo.reset.confirm:}")
    private String demoResetConfirmation;

    @Value("${demo.seed.admin.username:admin}")
    private String demoAdminUsername;

    @Value("${demo.seed.admin.password:}")
    private String demoAdminPassword;

    private final BCryptPasswordEncoder demoPasswordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (!demoSeedEnabled) {
            logger.info("Demo data seeding is disabled for this startup.");
            return;
        }
        boolean resetPending = resetDatabaseIfRequired();
        seedRoles();
        seedAdministrativeUnits();
        seedDefaultSchools();
        migrateUserTextEncoding();
        migrateExistingData();
        seedGradeCatalog();
        seedDemoSchoolNetwork();
        seedDemoAdminAccounts();
        migrateLessonPlanSchema();
        upgradeVuLongToAdmin();
        if (resetPending) {
            markResetApplied();
        }
    }

    /**
     * Reset business data only once for a new deploy dataset version. The
     * schema and Hibernate's ID sequence are intentionally preserved.
     */
    boolean resetDatabaseIfRequired() {
        if (!demoResetEnabled) {
            return false;
        }
        if (demoResetVersion == null || demoResetVersion.isBlank()) {
            throw new IllegalStateException("demo.reset.version must be configured when demo.reset.enabled=true");
        }
        if (!RESET_CONFIRMATION.equals(demoResetConfirmation)) {
            throw new IllegalStateException(
                    "demo.reset.confirm must equal " + RESET_CONFIRMATION
                            + " when demo.reset.enabled=true");
        }

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + RESET_STATE_TABLE + " ("
                + "id TINYINT NOT NULL PRIMARY KEY, "
                + "dataset_version VARCHAR(100) NOT NULL, "
                + "applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        List<String> appliedVersions = jdbcTemplate.queryForList(
                "SELECT dataset_version FROM " + RESET_STATE_TABLE + " WHERE id = 1",
                String.class);
        if (!appliedVersions.isEmpty() && demoResetVersion.equals(appliedVersions.get(0))) {
            logger.info("Demo dataset version '{}' is already applied; keeping existing data.", demoResetVersion);
            return false;
        }

        resetBusinessTables();
        logger.warn("Database business data was reset for demo dataset version '{}'.", demoResetVersion);
        return true;
    }

    private void resetBusinessTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
                String.class);
        List<String> tablesToReset = new ArrayList<>();
        for (String table : tables) {
            if (RESET_EXCLUDED_TABLES.contains(table)) {
                continue;
            }
            if (!table.matches("[A-Za-z0-9_$]+")) {
                throw new IllegalStateException("Unexpected table name from database metadata: " + table);
            }
            tablesToReset.add(table);
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : tablesToReset) {
                    statement.execute("TRUNCATE TABLE `" + table + "`");
                }
            } finally {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
            return null;
        });
    }

    private void markResetApplied() {
        jdbcTemplate.update(
                "INSERT INTO " + RESET_STATE_TABLE
                        + " (id, dataset_version) VALUES (1, ?) "
                        + "ON DUPLICATE KEY UPDATE dataset_version = VALUES(dataset_version), "
                        + "applied_at = CURRENT_TIMESTAMP",
                demoResetVersion);
    }

    /**
     * Keep the grade catalog available for features that select a grade
     * independently from a concrete student-tracking class.
     */
    void seedGradeCatalog() {
        try {
            for (int grade = 1; grade <= 12; grade++) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM classes WHERE grade = ?",
                        Integer.class,
                        grade
                );
                if (count == null || count == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO classes (name_class, grade) VALUES (?, ?)",
                            "Grade " + grade,
                            grade
                    );
                }
            }
            logger.info("Grade catalog checked: grades 1-12 are available.");
        } catch (Exception e) {
            logger.warn("Grade catalog migration skipped: {}", e.getMessage());
        }
    }

    private void upgradeVuLongToAdmin() {
        try {
            // Tìm kiếm user có username là VuLong (không phân biệt chữ hoa/thường)
            List<String> usernames = jdbcTemplate.query(
                    "SELECT id FROM user WHERE LOWER(id) = LOWER(?)",
                    (rs, rowNum) -> rs.getString("id"),
                    "VuLong"
            );

            if (!usernames.isEmpty()) {
                for (String username : usernames) {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM user_role WHERE username = ? AND role_id = 1",
                            Integer.class,
                            username
                    );

                    if (count == null || count == 0) {
                        int updated = jdbcTemplate.update(
                                "UPDATE user_role SET role_id = 1 WHERE username = ?",
                                username
                        );
                        if (updated == 0) {
                            jdbcTemplate.update(
                                    "INSERT INTO user_role (username, role_id) VALUES (?, 1)",
                                    username
                            );
                        }
                        logger.info("Found user '{}' on database and successfully upgraded to ROLE_ADMIN.", username);
                    } else {
                        logger.info("User '{}' already has ROLE_ADMIN.", username);
                    }
                }
            } else {
                logger.info("User 'VuLong' was not found in database during startup check.");
            }
        } catch (Exception e) {
            logger.warn("Notice checking VuLong account: {}", e.getMessage());
        }
    }

    private void migrateExistingData() {
        try {
            // Đồng bộ dữ liệu cũ: đảm bảo câu hỏi và đề thi cũ là Public, học sinh cũ thuộc trường tự do
            jdbcTemplate.update("UPDATE question SET is_public = 1 WHERE is_public IS NULL");
            jdbcTemplate.update("UPDATE exam SET is_public = 1 WHERE is_public IS NULL");
            jdbcTemplate.update("UPDATE user SET school_id = 1 WHERE school_id IS NULL");
            logger.info("Migrated legacy data: initialized is_public=1 and school_id=1 for null records.");
        } catch (Exception e) {
            logger.warn("Data migration notice: {}", e.getMessage());
        }
    }

    /**
     * The legacy schema was created with latin1, which rejects Vietnamese
     * names and addresses. Keep the username key unchanged because it is
     * referenced by foreign keys, and convert only profile text columns.
     */
    private void migrateUserTextEncoding() {
        try {
            jdbcTemplate.execute("ALTER TABLE user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            for (String column : List.of(
                    "address", "email", "full_name", "img",
                    "pass_word", "phone_number", "re_pass_word")) {
                jdbcTemplate.execute("ALTER TABLE user MODIFY " + column
                        + " VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL");
            }
            logger.info("User profile text columns are configured for utf8mb4.");
        } catch (Exception e) {
            logger.warn("User text encoding migration skipped: {}", e.getMessage());
        }
    }

    private void migrateLessonPlanSchema() {
        try {
            // Old lesson plans used a mandatory subject_class_id; new plans store subject and grade separately.
            jdbcTemplate.execute("ALTER TABLE lesson_plan MODIFY subject_class_id INT NULL");
            ensureLessonPlanLessonColumn("content", "TEXT NULL");
            ensureLessonPlanLessonColumn("attachment_original_name", "VARCHAR(255) NULL");
            ensureLessonPlanLessonColumn("attachment_content_type", "VARCHAR(255) NULL");
            ensureLessonPlanLessonColumn("attachment_size", "BIGINT NULL");
            ensureLessonPlanLessonColumn("attachment_data", "LONGBLOB NULL");
            ensureColumn("question", "lesson_plan_lesson_id", "INT NULL");
            ensureColumn("exam", "lesson_plan_lesson_id", "INT NULL");
        } catch (Exception e) {
            logger.debug("Lesson plan schema migration skipped: {}", e.getMessage());
        }
    }

    private void ensureLessonPlanLessonColumn(String column, String definition) {
        ensureColumn("lesson_plan_custom_lesson", column, definition);
    }

    private void ensureColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? "
                        + "AND column_name = ?",
                Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void seedRoles() {
        if (!roleRepository.findByRoleName("ROLE_ADMIN").isPresent()) {
            roleRepository.insertRoleNative(1, "ROLE_ADMIN");
            logger.info("Initialized ROLE_ADMIN in database with id 1.");
        }
        if (!roleRepository.findByRoleName("ROLE_USER").isPresent()) {
            roleRepository.insertRoleNative(2, "ROLE_USER");
            logger.info("Initialized ROLE_USER in database with id 2.");
        }
        if (!roleRepository.findByRoleName("ROLE_TEACHER").isPresent()) {
            roleRepository.insertRoleNative(3, "ROLE_TEACHER");
            logger.info("Initialized ROLE_TEACHER in database with id 3.");
        }
        if (!roleRepository.findByRoleName("ROLE_SCHOOL_ADMIN").isPresent()) {
            roleRepository.insertRoleNative(4, "ROLE_SCHOOL_ADMIN");
            logger.info("Initialized ROLE_SCHOOL_ADMIN in database with id 4.");
        }
    }

    private void seedAdministrativeUnits() {
        if (provinceRepository.count() > 0) {
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/administrative_units.json");
            if (!resource.exists()) {
                logger.warn("administrative_units.json not found in classpath. Skipping administrative data seeding.");
                return;
            }

            logger.info("Seeding administrative units (Provinces, Districts, Wards) from JSON...");
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                if (root != null && root.isArray()) {
                    List<Province> provinces = new ArrayList<>();
                    List<District> districts = new ArrayList<>();
                    List<Ward> wards = new ArrayList<>();

                    for (JsonNode pNode : root) {
                        int pCode = pNode.get("code").asInt();
                        String pName = pNode.get("name").asText();
                        String pDivType = pNode.path("division_type").asText(null);
                        String pCodeName = pNode.path("codename").asText(null);
                        Integer pPhone = pNode.path("phone_code").isNumber() ? pNode.get("phone_code").asInt() : null;

                        provinces.add(new Province(pCode, pName, pDivType, pCodeName, pPhone));

                        JsonNode districtsNode = pNode.path("districts");
                        if (districtsNode.isArray()) {
                            for (JsonNode dNode : districtsNode) {
                                int dCode = dNode.get("code").asInt();
                                String dName = dNode.get("name").asText();
                                String dDivType = dNode.path("division_type").asText(null);
                                String dCodeName = dNode.path("codename").asText(null);

                                districts.add(new District(dCode, dName, dDivType, dCodeName, pCode));

                                JsonNode wardsNode = dNode.path("wards");
                                if (wardsNode.isArray()) {
                                    for (JsonNode wNode : wardsNode) {
                                        int wCode = wNode.get("code").asInt();
                                        String wName = wNode.get("name").asText();
                                        String wDivType = wNode.path("division_type").asText(null);
                                        String wCodeName = wNode.path("codename").asText(null);

                                        wards.add(new Ward(wCode, wName, wDivType, wCodeName, dCode));
                                    }
                                }
                            }
                        }
                    }

                    provinceRepository.saveAll(provinces);
                    districtRepository.saveAll(districts);
                    wardRepository.saveAll(wards);

                    logger.info("Administrative data seeding completed: {} provinces, {} districts, {} wards.",
                            provinces.size(), districts.size(), wards.size());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to seed administrative units: {}", e.getMessage(), e);
        }
    }

    private void seedDefaultSchools() {
        if (schoolRepository.count() != 0) {
            return;
        }

        School defaultSchool = new School("Thí sinh tự do / Khác", "FREE-001", null, null, null, "Toàn quốc");
        defaultSchool.setProvinceName("Toàn quốc");
        schoolRepository.save(defaultSchool);
        logger.info("Initialized the free-student school.");
    }

    /**
     * Creates a small but complete school network for local demonstrations.
     * All records are addressed by deterministic demo identifiers so startup is idempotent.
     */
    void seedDemoSchoolNetwork() {
        Role teacherRole = roleRepository.findByRoleName("ROLE_TEACHER")
                .orElseThrow(() -> new IllegalStateException("ROLE_TEACHER is not configured"));
        Role studentRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is not configured"));
        Map<Integer, Classes> gradeCatalog = classesRepository.findAll().stream()
                .filter(item -> item.getGrade() != null)
                .collect(Collectors.toMap(Classes::getGrade, Function.identity(), (left, right) -> left));
        List<String> teacherNames = DemoDataPlan.teacherNames();
        List<String> studentNames = DemoDataPlan.studentNames();
        String schoolYear = currentSchoolYear();
        int teacherNameIndex = 0;
        int seededClasses = 0;
        int seededAssignments = 0;

        for (DemoDataPlan.SchoolDefinition definition : DemoDataPlan.schools()) {
            School school = findOrCreateDemoSchool(definition);
            Map<String, User> subjectTeachers = new java.util.HashMap<>();

            for (int grade = definition.firstGrade(); grade <= definition.lastGrade(); grade++) {
                Classes gradeCatalogEntry = gradeCatalog.get(grade);
                if (gradeCatalogEntry == null) {
                    logger.warn("Grade {} is missing from the grade catalog; skipping school {}.", grade, school.getSchoolCode());
                    continue;
                }

                for (DemoDataPlan.SubjectDefinition subjectDefinition : DemoDataPlan.subjectsForLevel(definition.schoolLevel())) {
                    Subject subject = findOrCreateSubject(subjectDefinition.name());
                    if (subjectClassRepository.findByClassesAndSubject(gradeCatalogEntry.getId(), subject.getId()).isEmpty()) {
                        subjectClassRepository.save(new SubjectClasses(gradeCatalogEntry, subject));
                    }
                    if (!subjectTeachers.containsKey(subjectDefinition.key())) {
                        subjectTeachers.put(subjectDefinition.key(), createDemoTeacher(
                                "demo_gvbm_" + school.getSchoolCode().toLowerCase(Locale.ROOT) + "_" + subjectDefinition.key(),
                                teacherNames.get(Math.floorMod(teacherNameIndex++, teacherNames.size())) + " (" + subjectDefinition.key() + ")",
                                school, definition.schoolLevel(), teacherRole));
                    }
                }

                for (int section = 1; section <= 3; section++) {
                    String className = grade + "A" + section;
                    StudentTrackingClass trackingClass = findOrCreateDemoClass(school, className, grade, schoolYear);
                    if (trackingClass.getHomeroomTeacher() == null) {
                        String teacherId = "demo_gvcn_" + school.getSchoolCode().toLowerCase(Locale.ROOT)
                                + "_" + grade + "_" + section;
                        User homeroomTeacher = createDemoTeacher(
                                teacherId,
                                teacherNames.get(Math.floorMod(teacherNameIndex++, teacherNames.size()))
                                        + " (GVCN " + className + ")",
                                school, definition.schoolLevel(), teacherRole);
                        trackingClass.setHomeroomTeacher(homeroomTeacher);
                        trackingClass = trackingClassRepository.save(trackingClass);
                    }
                    seedDemoRoster(trackingClass, studentNames);
                    seedDemoStudentAccounts(trackingClass, studentRole);

                    for (DemoDataPlan.SubjectDefinition subjectDefinition : DemoDataPlan.subjectsForLevel(definition.schoolLevel())) {
                        Subject subject = findOrCreateSubject(subjectDefinition.name());
                        if (assignmentRepository.findByTrackingClassIdAndSubjectId(trackingClass.getId(), subject.getId()).isEmpty()) {
                            assignmentRepository.save(new ClassSubjectAssignment(
                                    trackingClass, subject, subjectTeachers.get(subjectDefinition.key())));
                            seededAssignments++;
                        }
                    }
                    seededClasses++;
                }
            }
        }

        logger.info("Demo school network checked: {} schools, {} classes and {} subject assignments.",
                DemoDataPlan.schools().size(), seededClasses, seededAssignments);
    }

    /**
     * Creates one global administrator and one school administrator per demo
     * school. Passwords are configurable and are never replaced for an
     * existing account.
     */
    void seedDemoAdminAccounts() {
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN is not configured"));
        Role schoolAdminRole = roleRepository.findByRoleName("ROLE_SCHOOL_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_SCHOOL_ADMIN is not configured"));

        String adminId = demoAdminUsername == null || demoAdminUsername.isBlank()
                ? "admin"
                : demoAdminUsername.trim();
        createDemoAdmin(adminId, "System administrator", null, null, adminRole);

        for (DemoDataPlan.SchoolDefinition definition : DemoDataPlan.schools()) {
            School school = findOrCreateDemoSchool(definition);
            String schoolKey = definition.code().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            createDemoAdmin(
                    "demo_hieu_truong_" + schoolKey,
                    "School administrator - " + school.getName(),
                    school,
                    definition.schoolLevel(),
                    schoolAdminRole);
        }
    }

    private User createDemoAdmin(String id, String fullName, School school, Integer schoolLevel, Role role) {
        Optional<User> existing = userRepository.findById(id);
        User admin = existing.orElseGet(User::new);
        admin.setId(id);
        if (existing.isEmpty()) {
            String password = demoAdminPassword == null || demoAdminPassword.isBlank()
                    ? demoSeedPassword
                    : demoAdminPassword;
            admin.setPassWord(demoPasswordEncoder.encode(password));
            admin.setRePassWord(admin.getPassWord());
            admin.setCreateDate(java.sql.Date.valueOf(LocalDate.now()));
        }
        admin.setFullName(fullName);
        admin.setEmail(id + "@demo.school.vn");
        admin.setAddress(school == null ? "Demo platform" : "Demo school " + school.getSchoolCode());
        admin.setPhoneNumber("090" + String.format("%07d", Math.floorMod(id.hashCode(), 10_000_000)));
        admin.setImg("/img/avatar/default.jpg");
        admin.setSchool(school);
        admin.setTrackingClass(null);
        admin.setSchoolLevel(schoolLevel);
        Set<Role> roles = admin.getRoles() == null ? new HashSet<>() : new HashSet<>(admin.getRoles());
        roles.add(role);
        admin.setRoles(roles);
        return userRepository.save(admin);
    }

    private School findOrCreateDemoSchool(DemoDataPlan.SchoolDefinition definition) {
        Optional<School> existing = schoolRepository.findBySchoolCode(definition.code());
        if (existing.isPresent()) {
            return existing.get();
        }

        School school = new School(definition.name(), definition.code(), 1, 5, 175, "Khu đô thị Bình Minh");
        school.setProvinceName("Thành phố Hà Nội");
        school.setDistrictName("Quận Cầu Giấy");
        school.setWardName("Phường Trung Hòa");
        return schoolRepository.save(school);
    }

    private Subject findOrCreateSubject(String name) {
        return subjectRepository.findAll().stream()
                .filter(subject -> subject.getName() != null && subject.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Subject subject = new Subject();
                    subject.setName(name);
                    subject.setImage("/img/subject/default.jpg");
                    return subjectRepository.save(subject);
                });
    }

    private StudentTrackingClass findOrCreateDemoClass(School school, String className, int grade, String schoolYear) {
        return trackingClassRepository.findBySchoolIdAndGradeOrderByClassNameAsc(school.getId(), grade).stream()
                .filter(item -> schoolYear.equals(item.getSchoolYear()) && className.equalsIgnoreCase(item.getClassName()))
                .findFirst()
                .orElseGet(() -> {
                    StudentTrackingClass trackingClass = new StudentTrackingClass(className, schoolYear);
                    trackingClass.setGrade(grade);
                    trackingClass.setSchool(school);
                    return trackingClassRepository.save(trackingClass);
                });
    }

    private User createDemoTeacher(String id, String fullName, School school, int schoolLevel, Role teacherRole) {
        Optional<User> existing = userRepository.findById(id);
        User teacher = existing.orElseGet(User::new);
        teacher.setId(id);
        if (!existing.isPresent()) {
            teacher.setPassWord(demoPasswordEncoder.encode(demoSeedPassword));
            teacher.setRePassWord(teacher.getPassWord());
        }
        teacher.setFullName(fullName);
        teacher.setEmail(id + "@demo.school.vn");
        teacher.setAddress("Demo school " + school.getSchoolCode());
        teacher.setPhoneNumber("090" + String.format("%07d", Math.floorMod(id.hashCode(), 10_000_000)));
        teacher.setImg("/img/avatar/default.jpg");
        if (teacher.getCreateDate() == null) {
            teacher.setCreateDate(java.sql.Date.valueOf(LocalDate.now()));
        }
        teacher.setSchool(school);
        teacher.setSchoolLevel(schoolLevel);
        Set<Role> roles = teacher.getRoles() == null ? new HashSet<>() : new HashSet<>(teacher.getRoles());
        roles.add(teacherRole);
        teacher.setRoles(roles);
        return userRepository.save(teacher);
    }

    private void seedDemoRoster(StudentTrackingClass trackingClass, List<String> studentNames) {
        if (!trackedStudentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(trackingClass.getId()).isEmpty()) {
            return;
        }

        List<TrackedStudent> roster = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            TrackedStudent student = new TrackedStudent();
            student.setTrackingClass(trackingClass);
            student.setFullName(studentNames.get(index % studentNames.size()));
            student.setDisplayOrder(index + 1);
            roster.add(student);
        }
        trackedStudentRepository.saveAll(roster);
    }

    /**
     * The tracking sheet stores a roster separately from login accounts. Keep
     * the local demo data useful in the account-based screens as well.
     */
    void seedDemoStudentAccounts(StudentTrackingClass trackingClass, Role studentRole) {
        if (trackingClass == null || trackingClass.getId() == null || trackingClass.getSchool() == null) {
            return;
        }

        List<TrackedStudent> roster = trackedStudentRepository
                .findByTrackingClassIdOrderByDisplayOrderAscIdAsc(trackingClass.getId());
        int fallbackOrder = 1;
        for (TrackedStudent rosterStudent : roster) {
            int displayOrder = rosterStudent.getDisplayOrder() == null
                    ? fallbackOrder
                    : rosterStudent.getDisplayOrder();
            fallbackOrder++;

            String studentId = "demo_hs_" + trackingClass.getId() + "_" + displayOrder;
            Optional<User> existing = userRepository.findById(studentId);
            User student = existing.orElseGet(User::new);
            student.setId(studentId);
            student.setFullName(rosterStudent.getFullName());
            student.setEmail(studentId + "@demo.student.vn");
            student.setAddress("Học sinh lớp " + trackingClass.getClassName());
            student.setPhoneNumber("091" + String.format("%07d",
                    Math.floorMod(studentId.hashCode(), 10_000_000)));
            student.setImg("/img/avatar/default.jpg");
            student.setSchool(trackingClass.getSchool());
            student.setTrackingClass(trackingClass);
            student.setSchoolLevel(schoolLevelForGrade(trackingClass.getGrade()));

            if (existing.isEmpty()) {
                student.setPassWord(demoPasswordEncoder.encode(demoSeedPassword));
                student.setRePassWord(student.getPassWord());
                student.setCreateDate(java.sql.Date.valueOf(LocalDate.now()));
            }

            Set<Role> roles = student.getRoles() == null
                    ? new HashSet<>()
                    : new HashSet<>(student.getRoles());
            roles.add(studentRole);
            student.setRoles(roles);
            userRepository.save(student);
        }
    }

    private Integer schoolLevelForGrade(Integer grade) {
        if (grade == null) {
            return null;
        }
        if (grade <= 5) {
            return 1;
        }
        if (grade <= 9) {
            return 2;
        }
        return 3;
    }

    private String currentSchoolYear() {
        int year = LocalDate.now().getYear();
        return year + "-" + (year + 1);
    }
}
