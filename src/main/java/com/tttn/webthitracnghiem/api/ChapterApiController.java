package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.ChapterForm;
import com.tttn.webthitracnghiem.model.ChapterRequest;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.ILessonService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/chapter")
public class ChapterApiController {
    @Autowired
    private IChapterService chapterService;
    @Autowired
    private ISubjectClassService subjectClassService;
    @Autowired
    private ILessonService lessonService;

    @GetMapping("/{subId}/{classId}")
    public ResponseEntity<List<Chapter>> getAll(@PathVariable Integer subId, @PathVariable Integer classId) {
        return ResponseEntity.ok(chapterService.findBySubjectAndClass(subId, classId));
    }

    @PostMapping(value = ("/{subClassId}"), consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@PathVariable Integer subClassId, @RequestBody List<ChapterForm> chapterForms) {
        Set<Chapter> chapters = new LinkedHashSet<>();
        for (int i = 0; i < chapterForms.size(); i++) {
            Chapter chapter = new Chapter();
            chapter.setId(chapterForms.get(i).getId());
            chapter.setChapterName(chapterForms.get(i).getChapterName());
            chapter.setSerial(i + 1);
            chapter.setSubjectClasses(subjectClassService.findById(subClassId));
            Chapter chapterSave = chapterService.save(chapter);
            chapters.add(chapterSave);
        }
        List<Chapter> chapterAll = chapterService.findBySubjectClass(subClassId);
        for(Chapter chapter : chapterAll){
            if(!chapters.contains(chapter)){
                List<Lesson> lessons = new ArrayList<>(chapter.getLessons());
                for(int i = 0; i<lessons.size();i++){
                    lessonService.remove(lessons.get(i));
                }
                chapterService.remove(chapter);
            }
        }
        return ResponseEntity.ok(chapterForms);
    }
}
