package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.repository.ChapterRepository;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.ILessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/lesson")
public class LessonController {
    @Autowired
    private ILessonService lessonService;
    @Autowired
    private IChapterService chapterService;

    @GetMapping("/list/{id}")
    public String index(@RequestParam Optional<Boolean> create,
                        @PathVariable Integer id, Model model, @PageableDefault(value = 20) Pageable pageable) {
        if(create.isPresent()){
            model.addAttribute("message","Cập nhập bài học thành công!");
        }
        Page<Lesson> lessons = lessonService.findByChapterId(id, pageable);
        model.addAttribute("lessons", lessons);
        model.addAttribute("chapter", chapterService.findById(id));
        return "class/lessonList";
    }
    @GetMapping("/create/{id}")
    public String create(@PathVariable Integer id, Model model){
        Chapter chapter = chapterService.findById(id);
        List<Lesson> lessons = new ArrayList<>(chapter.getLessons());
        Collections.sort(lessons);
        Set<Lesson> lessons1 = new LinkedHashSet<>(lessons);
        chapter.setLessons(lessons1);
        model.addAttribute("chapter",chapter);
        model.addAttribute("serial",lessonService.getSerialMax(chapter.getId()));
        return "class/addLessonToChapter";
    }
    @GetMapping("/delete/{lessonId}")
    public String delete(@PathVariable int lessonId, Model model, RedirectAttributes ra){
        Lesson lesson = lessonService.findById(lessonId);
        lessonService.remove(lesson);
        ra.addFlashAttribute("message","Xóa thành công bài học!");
        return "redirect:/lesson/list/"+lesson.getChapter().getId();
    }
    @GetMapping("/edit/{lessonId}")
    public String edit(@PathVariable int lessonId, Model model){
        Lesson lesson = lessonService.findById(lessonId);
        model.addAttribute("lesson",lesson);
        model.addAttribute("chapter",lesson.getChapter());
        return "class/editLesson";
    }
    @PostMapping("/edit")
    public String save(@ModelAttribute Lesson lesson, RedirectAttributes ra){
        lessonService.save(lesson);
        ra.addFlashAttribute("message","Chỉnh sửa bài học thành công!");
        return "redirect:/lesson/list/"+lesson.getChapter().getId();
    }
}
