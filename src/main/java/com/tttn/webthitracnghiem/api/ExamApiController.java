package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.ExamRequest;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import com.tttn.webthitracnghiem.repository.NotificationRepository;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exam")
public class ExamApiController {
    @Autowired
    private IExamService examService;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;

    @Autowired
    private LessonPlanLessonRepository lessonPlanLessonRepository;
    @Autowired
    private ILessonPlanService lessonPlanService;

    @Autowired
    private com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository assignmentRepository;

    @Autowired
    private com.tttn.webthitracnghiem.service.IUserService userService;

    @Autowired
    private com.tttn.webthitracnghiem.repository.UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Older edit forms submit empty optional fields. Convert those values to
     * null before Spring tries to bind them to typed properties such as dates.
     */
    @InitBinder("exam")
    public void trimEmptyExamFields(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.trim().isEmpty() ? null : Date.valueOf(text.trim()));
            }
        });
    }

    @GetMapping
    public ResponseEntity<List<ExamSummary>> get(){
        List<ExamSummary> exams = examService.getAll().stream()
                .map(ExamSummary::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(exams);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@ModelAttribute Exam exam,
                                  @RequestParam(required = false) Integer school,
                                  @RequestParam(required = false) Boolean isPublic,
                                  @RequestParam(required = false) String targetType,
                                  @RequestParam(required = false) Integer targetGrade,
                                  @RequestParam(required = false) Integer targetClassId,
                                  @RequestParam(required = false) String startTimeStr,
                                  @RequestParam(required = false) String endTimeStr,
                                  @RequestParam(required = false) String scoreType,
                                  @RequestParam(required = false) Integer gradeMonth,
                                  @RequestParam(required = false) Integer lessonPlanLessonId,
                                  @RequestParam(required = false, defaultValue = "false") boolean practice) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = (auth != null && userService != null) ? userService.findById(auth.getName()) : null;

        // Khi sua de, form co the khong gui lai cac quan he khong hien thi.
        // Giu lai cac lien ket hien co de khong lam mat chu de, truong hoac nguoi tao.
        if (exam.getId() > 0) {
            Exam existingExam = examService.findById(exam.getId());
            if (existingExam != null) {
                if (exam.getUsers() == null) exam.setUsers(existingExam.getUsers());
                if (exam.getSchool() == null) exam.setSchool(existingExam.getSchool());
                if (exam.getTargetClass() == null) exam.setTargetClass(existingExam.getTargetClass());
                if (exam.getQuestions() == null) exam.setQuestions(existingExam.getQuestions());
                if (exam.getGradeMonth() == null) exam.setGradeMonth(existingExam.getGradeMonth());
                if (exam.getCreateDate() == null) exam.setCreateDate(existingExam.getCreateDate());
                if (exam.getLastModify() == null) exam.setLastModify(existingExam.getLastModify());
            }
        }

        // Nếu là giáo viên (không phải Admin):
        if (!isAdmin && !isSchoolAdmin && currentUser != null) {
            // Không được tạo đề toàn quốc hoặc toàn trường nếu chưa được phép
            if ("PUBLIC".equalsIgnoreCase(targetType)) {
                targetType = "GRADE";
            }
            if ("SCHOOL".equalsIgnoreCase(targetType)) {
                targetType = "GRADE";
            }

            // Nếu giao theo lớp: BẮT BUỘC giáo viên phải là GVCN hoặc được phân công dạy môn đó ở lớp này!
            if ("CLASS".equalsIgnoreCase(targetType)) {
                if (targetClassId == null || targetClassId <= 0) {
                    return ResponseEntity.badRequest().body("Bạn phải chọn lớp cụ thể để giao bài thi!");
                }
                var optClass = trackingClassRepository.findById(targetClassId);
                if (optClass.isEmpty()) {
                    return ResponseEntity.badRequest().body("Lớp được chọn không tồn tại!");
                }
                StudentTrackingClass tc = optClass.get();
                boolean isHomeroom = tc.getHomeroomTeacher() != null && currentUser.getId().equals(tc.getHomeroomTeacher().getId());
                boolean isAssigned = assignmentRepository != null && assignmentRepository.existsByTrackingClassIdAndTeacherId(targetClassId, currentUser.getId());

                if (!isHomeroom && !isAssigned) {
                    return ResponseEntity.status(403).body("Bạn không được phân công phụ trách hay giảng dạy lớp [" + tc.getClassName() + "]! Không thể tạo bài thi cho lớp này.");
                }
            }

            // Nếu giao theo khối: kiểm tra cấp học (schoolLevel)
            if (currentUser.getSchoolLevel() != null && targetGrade != null) {
                int lvl = currentUser.getSchoolLevel();
                boolean validGrade = (lvl == 1 && targetGrade >= 1 && targetGrade <= 5)
                        || (lvl == 2 && targetGrade >= 6 && targetGrade <= 9)
                        || (lvl == 3 && targetGrade >= 10 && targetGrade <= 12);
                if (!validGrade) {
                    return ResponseEntity.status(403).body("Tài khoản của bạn thuộc " + currentUser.getSchoolLevelName() + ", không thể giao đề cho Khối " + targetGrade + "!");
                }
            }

            // Tự động gán trường của giáo viên nếu có
            if (currentUser.getSchool() != null) {
                exam.setSchool(currentUser.getSchool());
            }
        }
        if (exam.getUsers() == null) exam.setUsers(currentUser);

        if (exam.isFree()) {
            exam.setTime(0);
        }
        // Gán isPublic
        if ("PUBLIC".equalsIgnoreCase(targetType)) {
            exam.setIsPublic(true);
        } else {
            exam.setIsPublic(false);
        }
        // Gán school nếu được truyền vào
        if (school != null && school > 0) {
            schoolRepository.findById(school).ifPresent(exam::setSchool);
        }
        // Gán phạm vi giao đề
        if (targetType != null && !targetType.trim().isEmpty()) {
            exam.setTargetType(targetType);
        } else {
            exam.setTargetType("PUBLIC");
        }
        if (targetGrade != null && targetGrade > 0) {
            exam.setTargetGrade(targetGrade);
            exam.setTargetType("GRADE");
        }
        if (targetClassId != null && targetClassId > 0) {
            trackingClassRepository.findById(targetClassId).ifPresent(c -> {
                exam.setTargetClass(c);
                exam.setTargetType("CLASS");
                if (c.getGrade() != null) {
                    exam.setTargetGrade(c.getGrade());
                }
                if (exam.getSchool() == null && c.getSchool() != null) {
                    exam.setSchool(c.getSchool());
                }
            });
        }
        if (scoreType != null && !scoreType.trim().isEmpty()) {
            exam.setScoreType(scoreType);
        }
        exam.setGradeMonth(validMonth(gradeMonth) ? gradeMonth : null);
        if (practice) {
            exam.setPractice(true);
            exam.setIsPublic(true);
            exam.setTargetType("PUBLIC");
            exam.setTargetClass(null);
            exam.setSchool(null);
        }
        if (lessonPlanLessonId != null) {
            LessonPlanLesson customLesson = lessonPlanLessonRepository.findById(lessonPlanLessonId).orElse(null);
            if (customLesson == null) return ResponseEntity.notFound().build();
            if (auth == null || !lessonPlanService.canView(customLesson.getChapter().getLessonPlan(), auth.getName(), isAdmin || isSchoolAdmin)) {
                return ResponseEntity.status(403).body("Bạn không có quyền dùng bài học trong giáo án này.");
            }
            exam.setLessonPlanLesson(customLesson);
            exam.setLesson(null);
        }
        try {
            if (startTimeStr != null && !startTimeStr.trim().isEmpty()) {
                exam.setStartTime(java.sql.Timestamp.valueOf(startTimeStr.replace("T", " ") + (startTimeStr.length() == 16 ? ":00" : "")));
            }
            if (endTimeStr != null && !endTimeStr.trim().isEmpty()) {
                exam.setEndTime(java.sql.Timestamp.valueOf(endTimeStr.replace("T", " ") + (endTimeStr.length() == 16 ? ":00" : "")));
            }
        } catch (Exception ignored) {}

        examService.save(exam);
        createNotifications(exam);
        return ResponseEntity.ok(Map.of("id", exam.getId()));
    }

    // Keep the direct Java call contract used by older integrations and tests.
    public ResponseEntity<?> save(Exam exam, Integer school, Boolean isPublic, String targetType,
                                  Integer targetGrade, Integer targetClassId, String startTimeStr,
                                  String endTimeStr, String scoreType, Integer lessonPlanLessonId,
                                  boolean practice) {
        return save(exam, school, isPublic, targetType, targetGrade, targetClassId, startTimeStr,
                endTimeStr, scoreType, null, lessonPlanLessonId, practice);
    }

    @PostMapping(value = "/excel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@RequestBody ExamRequest examRequest){
        System.out.println(examRequest);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = (auth != null && userService != null) ? userService.findById(auth.getName()) : null;

        String targetType = examRequest.getTargetType();
        Integer targetClassId = examRequest.getTargetClassId();
        Integer targetGrade = examRequest.getTargetGrade();

        // Nếu là giáo viên:
        if (!isAdmin && !isSchoolAdmin && currentUser != null) {
            if ("PUBLIC".equalsIgnoreCase(targetType)) {
                targetType = "GRADE";
            }
            if ("SCHOOL".equalsIgnoreCase(targetType)) {
                targetType = "GRADE";
            }

            if ("CLASS".equalsIgnoreCase(targetType)) {
                if (targetClassId == null || targetClassId <= 0) {
                    return ResponseEntity.badRequest().body("Bạn phải chọn lớp cụ thể để giao bài thi!");
                }
                var optClass = trackingClassRepository.findById(targetClassId);
                if (optClass.isEmpty()) {
                    return ResponseEntity.badRequest().body("Lớp được chọn không tồn tại!");
                }
                StudentTrackingClass tc = optClass.get();
                boolean isHomeroom = tc.getHomeroomTeacher() != null && currentUser.getId().equals(tc.getHomeroomTeacher().getId());
                boolean isAssigned = assignmentRepository != null && assignmentRepository.existsByTrackingClassIdAndTeacherId(targetClassId, currentUser.getId());

                if (!isHomeroom && !isAssigned) {
                    return ResponseEntity.status(403).body("Bạn không được phân công phụ trách hay giảng dạy lớp [" + tc.getClassName() + "]! Không thể tạo bài thi cho lớp này.");
                }
            }

            if (currentUser.getSchoolLevel() != null && targetGrade != null) {
                int lvl = currentUser.getSchoolLevel();
                boolean validGrade = (lvl == 1 && targetGrade >= 1 && targetGrade <= 5)
                        || (lvl == 2 && targetGrade >= 6 && targetGrade <= 9)
                        || (lvl == 3 && targetGrade >= 10 && targetGrade <= 12);
                if (!validGrade) {
                    return ResponseEntity.status(403).body("Tài khoản của bạn thuộc " + currentUser.getSchoolLevelName() + ", không thể giao đề cho Khối " + targetGrade + "!");
                }
            }
        }

        LessonPlanLesson customLesson = null;
        if (examRequest.getLessonPlanLessonId() != null) {
            customLesson = lessonPlanLessonRepository.findById(examRequest.getLessonPlanLessonId()).orElse(null);
            if (customLesson == null) return ResponseEntity.notFound().build();
            if (auth == null || !lessonPlanService.canView(customLesson.getChapter().getLessonPlan(), auth.getName(), isAdmin || isSchoolAdmin)) {
                return ResponseEntity.status(403).body("Bạn không có quyền dùng bài học trong giáo án này.");
            }
        }
        Exam saved = examService.save(examRequest);
        if (customLesson != null) {
            saved.setLessonPlanLesson(customLesson);
            saved.setLesson(null);
        }
        // Gán isPublic và school từ ExamRequest
        if ("PUBLIC".equalsIgnoreCase(targetType)) {
            saved.setIsPublic(true);
        } else {
            saved.setIsPublic(false);
        }
        if (examRequest.getSchoolId() != null && examRequest.getSchoolId() > 0) {
            schoolRepository.findById(examRequest.getSchoolId()).ifPresent(saved::setSchool);
        }
        if (currentUser != null && currentUser.getSchool() != null && saved.getSchool() == null) {
            saved.setSchool(currentUser.getSchool());
        }
        if (saved.getUsers() == null) saved.setUsers(currentUser);
        // Gán phạm vi giao đề
        if (targetType != null && !targetType.trim().isEmpty()) {
            saved.setTargetType(targetType);
        } else {
            saved.setTargetType("PUBLIC");
        }
        if (examRequest.getTargetGrade() != null && examRequest.getTargetGrade() > 0) {
            saved.setTargetGrade(examRequest.getTargetGrade());
            saved.setTargetType("GRADE");
        }
        if (examRequest.getTargetClassId() != null && examRequest.getTargetClassId() > 0) {
            trackingClassRepository.findById(examRequest.getTargetClassId()).ifPresent(c -> {
                saved.setTargetClass(c);
                saved.setTargetType("CLASS");
                if (c.getGrade() != null) {
                    saved.setTargetGrade(c.getGrade());
                }
                if (saved.getSchool() == null && c.getSchool() != null) {
                    saved.setSchool(c.getSchool());
                }
            });
        }
        if (examRequest.getScoreType() != null) {
            saved.setScoreType(examRequest.getScoreType());
        }
        if (validMonth(examRequest.getGradeMonth()) && examRequest.getGradeMonth() != null) {
            saved.setGradeMonth(examRequest.getGradeMonth());
        } else if (saved.getId() > 0) {
            Exam existing = examService.findById(saved.getId());
            if (existing != null) saved.setGradeMonth(existing.getGradeMonth());
        }
        if (examRequest.getStartTime() != null) {
            saved.setStartTime(examRequest.getStartTime());
        }
        if (examRequest.getEndTime() != null) {
            saved.setEndTime(examRequest.getEndTime());
        }
        if (examRequest.isPractice()) {
            saved.setPractice(true);
            saved.setIsPublic(true);
            saved.setTargetType("PUBLIC");
            saved.setTargetClass(null);
            saved.setSchool(null);
        }

        examService.save(saved);
        createNotifications(saved);
        return ResponseEntity.ok(Map.of("id", saved.getId()));
    }

    private void createNotifications(Exam exam) {
        if (exam == null || exam.isPractice() || Boolean.TRUE.equals(exam.getIsPublic())
                || exam.getScoreType() == null || exam.getScoreType().trim().isEmpty()) return;
        List<User> recipients = new java.util.ArrayList<>();
        String targetType = exam.getTargetType();
        if ("CLASS".equalsIgnoreCase(targetType) && exam.getTargetClass() != null) {
            recipients.addAll(userRepository.findStudentsByClassId(exam.getTargetClass().getId()));
        } else if ("GRADE".equalsIgnoreCase(targetType) && exam.getSchool() != null && exam.getTargetGrade() != null) {
            recipients.addAll(userRepository.findStudentsBySchoolAndGrade(exam.getSchool().getId(), exam.getTargetGrade()));
        } else if ("SCHOOL".equalsIgnoreCase(targetType) && exam.getSchool() != null) {
            recipients.addAll(userRepository.findStudentsBySchool(exam.getSchool().getId()));
        }
        String teacherName = exam.getUsers() == null ? "Giáo viên" : exam.getUsers().getFullName();
        for (User recipient : recipients) {
            if (recipient == null || notificationRepository.existsByRecipientIdAndExamId(recipient.getId(), exam.getId())) continue;
            com.tttn.webthitracnghiem.model.Notification notification = new com.tttn.webthitracnghiem.model.Notification();
            notification.setRecipient(recipient);
            notification.setExam(exam);
            notification.setMessage(teacherName + " vừa tạo bài kiểm tra " + exam.getNameExam() + " có ghi điểm.");
            notificationRepository.save(notification);
        }
    }

    private boolean validMonth(Integer month) {
        return month == null || (month >= 1 && month <= 12);
    }

    /**
     * Keep the collection endpoint independent from the bidirectional JPA graph.
     * Serializing Exam directly walks targetClass.students.trackingClass forever.
     */
    public static class ExamSummary {
        private final int id;
        private final String idName;
        private final boolean free;
        private final int time;
        private final java.sql.Date createDate;
        private final java.sql.Date lastModify;
        private final String nameExam;
        private final Boolean isPublic;
        private final String targetType;
        private final Integer targetGrade;
        private final Integer targetClassId;
        private final String targetClassName;
        private final Integer lessonId;
        private final Integer lessonPlanLessonId;
        private final Integer schoolId;
        private final java.sql.Timestamp startTime;
        private final java.sql.Timestamp endTime;
        private final String scoreType;
        private final Integer gradeMonth;
        private final boolean practice;
        private final long view;
        private final long turn;

        public ExamSummary(Exam exam) {
            this.id = exam.getId();
            this.idName = exam.getIdName();
            this.free = exam.isFree();
            this.time = exam.getTime();
            this.createDate = exam.getCreateDate();
            this.lastModify = exam.getLastModify();
            this.nameExam = exam.getNameExam();
            this.isPublic = exam.getIsPublic();
            this.targetType = exam.getTargetType();
            this.targetGrade = exam.getTargetGrade();
            this.targetClassId = exam.getTargetClass() == null ? null : exam.getTargetClass().getId();
            this.targetClassName = exam.getTargetClass() == null ? null : exam.getTargetClass().getClassName();
            this.lessonId = exam.getLesson() == null ? null : exam.getLesson().getId();
            this.lessonPlanLessonId = exam.getLessonPlanLesson() == null ? null : exam.getLessonPlanLesson().getId();
            this.schoolId = exam.getSchool() == null ? null : exam.getSchool().getId();
            this.startTime = exam.getStartTime();
            this.endTime = exam.getEndTime();
            this.scoreType = exam.getScoreType();
            this.gradeMonth = exam.getGradeMonth();
            this.practice = exam.isPractice();
            this.view = exam.getView();
            this.turn = exam.getTurn();
        }

        public int getId() { return id; }
        public String getIdName() { return idName; }
        public boolean isFree() { return free; }
        public int getTime() { return time; }
        public java.sql.Date getCreateDate() { return createDate; }
        public java.sql.Date getLastModify() { return lastModify; }
        public String getNameExam() { return nameExam; }
        public Boolean getIsPublic() { return isPublic; }
        public String getTargetType() { return targetType; }
        public Integer getTargetGrade() { return targetGrade; }
        public Integer getTargetClassId() { return targetClassId; }
        public String getTargetClassName() { return targetClassName; }
        public Integer getLessonId() { return lessonId; }
        public Integer getLessonPlanLessonId() { return lessonPlanLessonId; }
        public Integer getSchoolId() { return schoolId; }
        public java.sql.Timestamp getStartTime() { return startTime; }
        public java.sql.Timestamp getEndTime() { return endTime; }
        public String getScoreType() { return scoreType; }
        public Integer getGradeMonth() { return gradeMonth; }
        public boolean isPractice() { return practice; }
        public long getView() { return view; }
        public long getTurn() { return turn; }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestParam Integer id){
        Exam exam = examService.findById(id);
        examService.delete(exam);
        return ResponseEntity.ok(exam);
    }
}
