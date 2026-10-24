package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.ILessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Old per-lesson pages; lessons are now edited in {@link CurriculumController}. */
@Controller
@RequestMapping("/lesson")
public class LessonController {
    @Autowired
    private ILessonService lessonService;
    @Autowired
    private IChapterService chapterService;

    @GetMapping({"/list/{id}", "/create/{id}"})
    public String chapter(@PathVariable("id") Integer chapterId) {
        Chapter chapter = chapterService.findById(chapterId);
        return ChapterController.curriculumUrl(chapter == null ? null : chapter.getSubjectClasses());
    }

    @GetMapping({"/edit/{lessonId}", "/delete/{lessonId}"})
    public String lesson(@PathVariable("lessonId") Integer lessonId) {
        Lesson lesson = lessonService.findById(lessonId);
        Chapter chapter = lesson == null ? null : lesson.getChapter();
        return ChapterController.curriculumUrl(chapter == null ? null : chapter.getSubjectClasses());
    }

    @PostMapping("/edit")
    public String edit() {
        return "redirect:/curriculum";
    }
}
