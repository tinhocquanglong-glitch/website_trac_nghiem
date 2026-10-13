package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API for tracking classes (used in exam creation form and GVCN student management modal).
 */
@RestController
@RequestMapping("/api/tracking-classes")
public class TrackingClassApiController {

    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<TrackingClassDto>> listClasses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        List<StudentTrackingClass> classes;
        if (isAdmin) {
            classes = trackingClassRepository.findAll();
        } else if (isSchoolAdmin && auth != null) {
            User currentUser = userService.findById(auth.getName());
            if (currentUser != null && currentUser.getSchool() != null) {
                classes = trackingClassRepository.findBySchoolId(currentUser.getSchool().getId());
            } else {
                classes = new ArrayList<>();
            }
        } else if (auth != null) {
            User currentUser = userService.findById(auth.getName());
            if (currentUser != null) {
                classes = trackingClassRepository.findClassesByTeacherInvolved(currentUser.getId());
            } else {
                classes = new ArrayList<>();
            }
        } else {
            classes = new ArrayList<>();
        }

        List<TrackingClassDto> result = classes.stream()
                .map(c -> new TrackingClassDto(
                        c.getId(),
                        c.getClassName(),
                        c.getSchoolYear(),
                        c.getGrade(),
                        c.getHomeroomTeacher() != null ? c.getHomeroomTeacher().getFullName() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Trả về danh sách học sinh thuộc lớp – dùng cho modal quản lý HS của GVCN / Hiệu trưởng.
     * Chỉ ROLE_ADMIN, ROLE_SCHOOL_ADMIN và GVCN của chính lớp đó mới được truy cập.
     */
    @GetMapping("/{classId}/students")
    public ResponseEntity<?> getStudentsOfClass(@PathVariable("classId") Integer classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();

        boolean isAdmin      = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin= auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        Optional<StudentTrackingClass> classOpt = trackingClassRepository.findById(classId);
        if (classOpt.isEmpty()) return ResponseEntity.notFound().build();

        StudentTrackingClass cls = classOpt.get();

        // Kiểm tra quyền: Admin / SchoolAdmin của trường đó / GVCN của lớp này
        if (!isAdmin && !isSchoolAdmin) {
            User currentUser = userService.findById(auth.getName());
            boolean isHomeroom = cls.getHomeroomTeacher() != null
                    && cls.getHomeroomTeacher().getId().equals(auth.getName());
            if (!isHomeroom) {
                return ResponseEntity.status(403).body("Bạn không có quyền xem lớp này.");
            }
        }

        // Lấy học sinh từ User có class_id = classId
        List<User> students = userRepository.findStudentsBySchool(
                cls.getSchool() != null ? cls.getSchool().getId() : 0);
        // Lọc đúng lớp
        List<StudentDto> result = students.stream()
                .filter(u -> u.getTrackingClass() != null && u.getTrackingClass().getId().equals(classId))
                .map(u -> new StudentDto(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhoneNumber(),
                        u.getImg()
                ))
                .sorted((a, b) -> a.fullName != null ? a.fullName.compareToIgnoreCase(b.fullName != null ? b.fullName : "") : 0)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─── DTOs ───

    public static class TrackingClassDto {
        public Integer id;
        public String className;
        public String schoolYear;
        public Integer grade;
        public String homeroomTeacher;

        public TrackingClassDto(Integer id, String className, String schoolYear, Integer grade, String homeroomTeacher) {
            this.id = id;
            this.className = className;
            this.schoolYear = schoolYear;
            this.grade = grade;
            this.homeroomTeacher = homeroomTeacher;
        }
    }

    public static class StudentDto {
        public String id;
        public String fullName;
        public String email;
        public String phoneNumber;
        public String img;

        public StudentDto(String id, String fullName, String email, String phoneNumber, String img) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.img = img;
        }
    }
}
