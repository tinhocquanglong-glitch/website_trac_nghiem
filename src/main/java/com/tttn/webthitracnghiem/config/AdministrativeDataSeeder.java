package com.tttn.webthitracnghiem.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdministrativeDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdministrativeDataSeeder.class);

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

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdministrativeUnits();
        seedDefaultSchools();
        migrateExistingData();
        upgradeVuLongToAdmin();
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

    private void seedRoles() {
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
        if (schoolRepository.count() == 0) {
            School defaultSchool = new School("Thí sinh tự do / Khác", "FREE-001", null, null, null, "Toàn quốc");
            defaultSchool.setProvinceName("Toàn quốc");
            schoolRepository.save(defaultSchool);

            // Một số trường mẫu
            School ams = new School("THPT Chuyên Hà Nội - Amsterdam", "HN-AMS", 1, 5, 175, "Số 1 Hoàng Minh Giám");
            ams.setProvinceName("Thành phố Hà Nội");
            ams.setDistrictName("Quận Cầu Giấy");
            ams.setWardName("Phường Trung Hoà");
            schoolRepository.save(ams);

            School chuVanAn = new School("THPT Chu Văn An", "HN-CVA", 1, 3, 112, "Số 10 Thụy Khuê");
            chuVanAn.setProvinceName("Thành phố Hà Nội");
            chuVanAn.setDistrictName("Quận Tây Hồ");
            chuVanAn.setWardName("Phường Thụy Khuê");
            schoolRepository.save(chuVanAn);

            School lhp = new School("THPT Chuyên Lê Hồng Phong", "HCM-LHP", 79, 760, 26734, "235 Nguyễn Văn Cừ");
            lhp.setProvinceName("Thành phố Hồ Chí Minh");
            lhp.setDistrictName("Quận 5");
            schoolRepository.save(lhp);

            logger.info("Initialized default schools.");
        }
    }
}
