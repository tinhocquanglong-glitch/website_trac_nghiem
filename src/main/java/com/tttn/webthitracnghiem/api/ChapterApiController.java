package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.ChapterForm;
import com.tttn.webthitracnghiem.service.CurriculumService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapter")
public class ChapterApiController {

    @Autowired
    private CurriculumService curriculumService;
    @Autowired
    private IUserService userService;

    /** Chapters of the caller's school for a subject and grade-catalog id. */
    @GetMapping("/{subId}/{classId}")
    public ResponseEntity<List<Chapter>> getAll(@PathVariable Integer subId, @PathVariable Integer classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer schoolId = curriculumService.schoolIdOf(auth == null ? null : userService.findById(auth.getName()));
        return ResponseEntity.ok(curriculumService.findSubjectClass(subId, classId)
                .map(subjectClass -> curriculumService.schoolChapters(schoolId, subjectClass))
                .orElse(List.of()));
    }

    /** The old bulk editor replaced (and deleted) every chapter; editing now happens per school in /curriculum. */
    @PostMapping(value = ("/{subClassId}"), consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@PathVariable Integer subClassId, @RequestBody List<ChapterForm> chapterForms) {
        return ResponseEntity.status(HttpStatus.GONE).body("Hãy sửa chủ đề ở trang Chương trình học (/curriculum).");
    }
}
