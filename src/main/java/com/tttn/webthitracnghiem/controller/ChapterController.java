package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Old per-chapter pages. Chapters are now edited per school in {@link CurriculumController};
 * these routes only forward there so bookmarks and old links keep working.
 */
@Controller
@RequestMapping("/chapter")
public class ChapterController {
    @Autowired
    private IChapterService chapterService;
    @Autowired
    private ISubjectClassService subjectClassService;

    @GetMapping({"/list/{id}", "/create/{id}"})
    public String list(@PathVariable("id") Integer subjectClassId) {
        return curriculumUrl(subjectClassService.findById(subjectClassId));
    }

    @GetMapping({"/edit/{id}", "/delete/{id}"})
    public String chapter(@PathVariable("id") Integer chapterId) {
        Chapter chapter = chapterService.findById(chapterId);
        return curriculumUrl(chapter == null ? null : chapter.getSubjectClasses());
    }

    @PostMapping("/edit")
    public String edit() {
        return "redirect:/curriculum";
    }

    static String curriculumUrl(SubjectClasses subjectClasses) {
        if (subjectClasses == null || subjectClasses.getClasses() == null || subjectClasses.getSubject() == null) {
            return "redirect:/curriculum";
        }
        return "redirect:/curriculum?gradeId=" + subjectClasses.getClasses().getId()
                + "&subjectId=" + subjectClasses.getSubject().getId();
    }
}
