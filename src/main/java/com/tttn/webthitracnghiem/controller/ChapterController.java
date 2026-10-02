package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.ILessonService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.Sides;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.*;
import java.util.*;

@Controller
@RequestMapping("/chapter")
public class ChapterController {
    @Autowired
    private IChapterService chapterService;
    @Autowired
    private ISubjectClassService subjectClassService;
    @Autowired
    private ILessonService lessonService;

    @GetMapping("/list/{id}")
    public String index(@RequestParam Optional<Boolean> create,
                        @PathVariable("id") Integer subjectClassId, Model model) {
        if (create.isPresent()) {
            model.addAttribute("message", "Cập nhập chương thành công!");
        }
        List<Chapter> chapters = chapterService.findBySubjectClass(subjectClassId);
        model.addAttribute("chapters", chapters);
        model.addAttribute("subjectClassId", subjectClassService.findById(subjectClassId));
        model.addAttribute("classId", subjectClassId);
        return "class/chapterList";
    }

    @GetMapping("/create/{id}")
    public String create(@PathVariable("id") Integer subjectClassId, Model model) {
        SubjectClasses subjectClasses = subjectClassService.findById(subjectClassId);
        List<Chapter> chapters = chapterService.findBySubjectClass(subjectClasses.getId());
        model.addAttribute("serial", chapterService.getSerial(subjectClasses.getId()));
        model.addAttribute("subjectClasses", subjectClasses);
        model.addAttribute("chapters", chapters);
        return "/class/addChapterToSubject";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer chapId, Model model, RedirectAttributes ra) {
        Chapter chapter = chapterService.findById(chapId);
        List<Lesson> lessons = new ArrayList<>(chapter.getLessons());
        for (int i = 0; i < lessons.size(); i++) {
            lessonService.remove(lessons.get(i));
        }
        chapterService.remove(chapter);
        ra.addFlashAttribute("message", "Xóa chương thành công!");
        return "redirect:/chapter/list/" + chapter.getSubjectClasses().getId();
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Chapter chapter = chapterService.findById(id);
        for (int i = 0; i < chapter.getLessons().size(); i++) {
            Set<Lesson> lessons = chapter.getLessons();
            List<Lesson> lessons1 = new ArrayList<>(lessons);
            Collections.sort(lessons1);
            Set<Lesson> lessons2 = new LinkedHashSet<>(lessons1);
            chapter.setLessons(lessons2);
        }
        model.addAttribute("subjectClasses", chapter.getSubjectClasses());
        model.addAttribute("chapter", chapter);
        return "/class/editChapter";
    }

    @PostMapping("/edit")
    public String save(@ModelAttribute Chapter chapter, RedirectAttributes ra) {
        chapterService.save(chapter);
        ra.addFlashAttribute("message", "Chỉnh sửa chương thành công!");
        return "redirect:/chapter/list/" + chapter.getSubjectClasses().getId();
    }
}
