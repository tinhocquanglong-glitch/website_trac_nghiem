package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for tracking classes (used in exam creation form).
 */
@RestController
@RequestMapping("/api/tracking-classes")
public class TrackingClassApiController {

    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;

    @Autowired
    private IUserService userService;

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
}
