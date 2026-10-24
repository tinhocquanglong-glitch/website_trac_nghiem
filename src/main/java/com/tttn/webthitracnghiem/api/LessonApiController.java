package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.ILessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/lesson")
public class LessonApiController {
    @Autowired
    ILessonService lessonService;
    @Autowired
    IChapterService chapterService;

    @GetMapping("/{chapId}")
    public ResponseEntity<?> get(@PathVariable Integer chapId){
        return ResponseEntity.ok(lessonService.findByChapId(chapId));
    }
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id){
        return ResponseEntity.ok(lessonService.findById(id));
    }

    /** The old bulk editor replaced (and deleted) every lesson; editing now happens per school in /curriculum. */
    @PostMapping(value = ("/{chapId}"),consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@PathVariable Integer chapId,@RequestBody List<LessonForm> lessonForms) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.GONE)
                .body("Hãy sửa bài học ở trang Chương trình học (/curriculum).");
    }
}
