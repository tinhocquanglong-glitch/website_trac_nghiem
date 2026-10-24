package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectRequest;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subject")
public class SubjectApiController {
    @Autowired
    private ISubjectService subjectService;
    @Autowired
    private IUserService userService;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? userService.findById(auth.getName()) : null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@ModelAttribute SubjectRequest subjectRequest) {
        User user = currentUser();
        boolean admin = isAdmin();

        if (subjectRequest.getId() != null) {
            Subject existing = subjectService.findById(subjectRequest.getId());
            if (existing != null && !subjectService.canManage(existing, user, admin)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền chỉnh sửa môn học này!");
            }
        }

        School school = (user != null && !admin) ? user.getSchool() : null;
        return ResponseEntity.ok(subjectService.save(subjectRequest, school));
    }
}
