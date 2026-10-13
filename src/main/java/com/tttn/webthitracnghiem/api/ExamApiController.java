package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.ExamRequest;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.service.IExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository assignmentRepository;

    @Autowired
    private com.tttn.webthitracnghiem.service.IUserService userService;

    @GetMapping
    public ResponseEntity<List<Exam>> get(){
        return ResponseEntity.ok(examService.getAll());
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
                                  @RequestParam(required = false) String scoreType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = (auth != null && userService != null) ? userService.findById(auth.getName()) : null;

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
        try {
            if (startTimeStr != null && !startTimeStr.trim().isEmpty()) {
                exam.setStartTime(java.sql.Timestamp.valueOf(startTimeStr.replace("T", " ") + (startTimeStr.length() == 16 ? ":00" : "")));
            }
            if (endTimeStr != null && !endTimeStr.trim().isEmpty()) {
                exam.setEndTime(java.sql.Timestamp.valueOf(endTimeStr.replace("T", " ") + (endTimeStr.length() == 16 ? ":00" : "")));
            }
        } catch (Exception ignored) {}

        examService.save(exam);
        return ResponseEntity.ok(exam);
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

        Exam saved = examService.save(examRequest);
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
        if (examRequest.getStartTime() != null) {
            saved.setStartTime(examRequest.getStartTime());
        }
        if (examRequest.getEndTime() != null) {
            saved.setEndTime(examRequest.getEndTime());
        }

        examService.save(saved);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestParam Integer id){
        Exam exam = examService.findById(id);
        examService.delete(exam);
        return ResponseEntity.ok(exam);
    }
}
