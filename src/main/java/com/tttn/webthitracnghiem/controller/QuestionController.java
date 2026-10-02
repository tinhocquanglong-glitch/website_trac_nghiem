package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IQuestionService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.*;

@Controller
public class QuestionController {
    @Autowired
    private ISubjectService subjectService;
    @Autowired
    private IQuestionService questionService;

    @Autowired
    private IExamService examService;
    @Autowired
    private IClassesService classesService;

    @ModelAttribute("exams")
    public Iterable<Exam> exams() {
        return examService.findAll();
    }

    @GetMapping("/question/list")
    public String showList(@RequestParam("subjectId") Optional<Integer> subjectId,
                           @RequestParam("keyword") Optional<String> name,
                           @RequestParam("create") Optional<Boolean> create,
                           @RequestParam("update") Optional<Boolean> update,
                           @RequestParam("delete") Optional<Boolean> delete,
                           Model model, @PageableDefault(value = 5) Pageable pageable) {
        Page<Question> questions;
        model.addAttribute("exams", examService.findAll());
        model.addAttribute("subjects", subjectService.findAll());
        if (create.isPresent()) {
            model.addAttribute("message", "Thêm câu hỏi thành công!");
        }
        if (update.isPresent()) {
            model.addAttribute("message","Chỉnh sửa câu hỏi thành công!");
        }
        if(delete.isPresent()){
            model.addAttribute("message","Xóa câu hỏi thành công!");
        }
        if (!name.isPresent()) {
            if (subjectId.isPresent()) {
                questions = questionService.findAllBySubject(subjectId, pageable);
                model.addAttribute("questions", questions);
                model.addAttribute("subjectId", subjectId.get());
                return "question/listQuestion";
            }
            questions = questionService.findAll(pageable);
        } else {
            if (subjectId.isPresent()) {
                questions = questionService.findBySubjectAndTitle(subjectId.get(), name.get(), pageable);
                model.addAttribute("subjectId", subjectId.get());
            } else {
                questions = questionService.findAllByTitleContaining(name.get(), pageable);
            }
            model.addAttribute("keyword", name.get());
        }
        model.addAttribute("questions", questions);
        return "question/listQuestion";
    }

    @GetMapping("/question/editQuest/{id}")
    public String editMember(@PathVariable("id") Integer id, Model model) {
        Question question = questionService.findById(id);
        List<Chapter> chapters = new ArrayList<>(question.getLesson().getChapter().getSubjectClasses().getChapters());
        Collections.sort(chapters);
        for (int i = 0; i < chapters.size(); i++) {
            Set<Lesson> lessons = chapters.get(i).getLessons();
            List<Lesson> lessons1 = new ArrayList<>(lessons);
            Collections.sort(lessons1);
            Set<Lesson> lessons2 = new LinkedHashSet<>(lessons1);
            chapters.get(i).setLessons(lessons2);
        }
        Set<Chapter> chapterSet = new LinkedHashSet<>(chapters);
        question.getLesson().getChapter().getSubjectClasses().setChapters(chapterSet);
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("classes",classesService.findAllBySubject(question.getLesson().getChapter().getSubjectClasses().getSubject().getId()));
        model.addAttribute("question", question);
        return "question/editQuestion";
    }

    @PostMapping("/question/update")
    public String update(Question question, RedirectAttributes ra) {
        questionService.save(question);
        ra.addFlashAttribute("message", "Chỉnh sửa câu hỏi thành công!");
        return "redirect:/question/list";
    }

    @GetMapping("/question/delete/{id}")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes ra) {
        Question question = questionService.findById(id);
        questionService.delete(question);
        ra.addFlashAttribute("message","Xóa câu hỏi thành công!");
        return "redirect:/question/list";
    }

    @GetMapping("/question/create")
    public String create(Model model) {
//        ModelAndView modelAndView = new ModelAndView("question/createQuestion");
        model.addAttribute("question", new Question());
        model.addAttribute("subjects", subjectService.findAll());
        return "question/createQuestion";
    }

    @PostMapping("/question/create")
    public String save(@Valid @ModelAttribute Question question, BindingResult bindingResult, Model model, RedirectAttributes ra) {
//        if (questionService.existById(id) {
//            bindingResult.addError(new FieldError("question", "id", "Cau hoi đã tồn tại"));
//        }

        if (bindingResult.hasFieldErrors()) {
            return "question/createQuestion";
        }
        ra.addFlashAttribute("message", "Thêm câu hỏi thành công");
        questionService.save(question);
        return "redirect:/question/list";
    }
}

