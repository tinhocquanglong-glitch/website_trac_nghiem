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

    @PostMapping(value = ("/{chapId}"),consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@PathVariable Integer chapId,@RequestBody List<LessonForm> lessonForms) {
        Set<Lesson> lessons = new LinkedHashSet<>();
        for (int i = 0; i < lessonForms.size(); i++) {
            Lesson lesson = new Lesson();
            lesson.setId(lessonForms.get(i).getId());
            lesson.setNameLesson(lessonForms.get(i).getLessonName());
            lesson.setChapter(chapterService.findById(lessonForms.get(i).getChapter()));
            lesson.setSerial(i + 1);
            Lesson lessonSave = lessonService.save(lesson);
            lessons.add(lessonSave);
        }
        List<Lesson> lessonAll = lessonService.findByChapId(chapId);
        for(Lesson lesson : lessonAll){
            if(!lessons.contains(lesson)){
                lessonService.remove(lesson);
            }
        }
        return ResponseEntity.ok(lessonForms);
    }
}
