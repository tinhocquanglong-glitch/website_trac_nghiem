package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/subjectClasses")
public class SubjectClassApiController {

    @Autowired
    private ISubjectClassService subjectClassService;
    @Autowired
    private IClassesService classesService;
    @Autowired
    private ISubjectService subjectService;
    @Autowired
    private IUserService userService;
    @Autowired
    private SubjectClassRepository subjectClassRepository;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? userService.findById(auth.getName()) : null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@RequestParam("classId") Integer classId,
                                  @RequestParam("subjectIds") List<Integer> subjectIds) {
        Classes classes = classesService.findById(classId);
        if (classes == null || subjectIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Lớp học hoặc danh sách môn học không hợp lệ.");
        }

        User user = currentUser();
        boolean admin = isAdmin();
        School userSchool = (user != null && !admin) ? user.getSchool() : null;

        List<Subject> subjects = new ArrayList<>();
        for (Integer subjectId : subjectIds) {
            if (subjectId == null) {
                return ResponseEntity.badRequest().body("Mã môn học không hợp lệ.");
            }
            Subject subject = subjectService.findById(subjectId);
            if (subject == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy môn học.");
            }
            subjects.add(subject);
        }

        for (Subject subject : subjects) {
            School targetSchool = subject.getSchool() != null ? subject.getSchool() : userSchool;
            Integer targetSchoolId = targetSchool != null ? targetSchool.getId() : null;

            boolean exists;
            if (targetSchoolId != null) {
                exists = subjectClassRepository.findByClassAndSubjectAndSchool(classes.getId(), subject.getId(), targetSchoolId).isPresent();
            } else {
                exists = subjectClassRepository.findByClassAndSubjectAndSchoolIsNull(classes.getId(), subject.getId()).isPresent();
            }
            if (!exists) {
                subjectClassService.save(new SubjectClasses(classes, subject, targetSchool));
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{classId}/{subjectId}")
    public ResponseEntity<SubjectClasses> getAll(@PathVariable("classId") Integer classId,
                                                 @PathVariable("subjectId") Integer subjectId) {
        User user = currentUser();
        Integer schoolId = (user != null && user.getSchool() != null) ? user.getSchool().getId() : null;
        SubjectClasses sc = subjectClassService.findByClassAndSubjectAndSchool(classId, subjectId, schoolId);
        if (sc == null) {
            sc = subjectClassService.findByClassAndSubject(classId, subjectId);
        }
        return ResponseEntity.ok(sc);
    }
}
