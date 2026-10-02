package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.service.*;
import com.tttn.webthitracnghiem.service.impl.ExamServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class ExamController {
    @Autowired
    IUserService userService;

    @Autowired
    IResultService rService;

    @Autowired
    private IExamService examService;

    @Autowired
    private ISubjectService subjectService;

    @Autowired
    private IClassesService classesService;

    @Autowired
    private IQuestionService questionService;
    @Autowired
    private INewsService newsService;
    @Autowired
    private IDocumentService documentService;
    @Autowired
    private ILessonService lessonService;
    @Autowired
    private IChapterService chapterService;

    @ModelAttribute("subjects")
    public Iterable<Subject> subjects() {
        return subjectService.findAll();
    }

    @ModelAttribute("classes")
    public Iterable<Classes> classes() {
        return classesService.findAll();
    }

    @ModelAttribute("questions")
    public Iterable<Question> questions() {
        return questionService.findAll();
    }


    @GetMapping("/exam/list")
    public String showList(@RequestParam("subjectId") Optional<Integer> subjectId,
                           @RequestParam("keyword") Optional<String> name,
                           @RequestParam("create") Optional<Boolean> create,
                           @RequestParam("update") Optional<Boolean> update,
                           Model model, @PageableDefault(value = 5) Pageable pageable) {
        Page<Exam> exams;
        model.addAttribute("subjects", subjectService.findAll());
        if (create.isPresent()) {
            model.addAttribute("message", "Thêm đề thi thành công!");
        }
        if (update.isPresent()) {
            model.addAttribute("message", "Chỉnh sửa đề thi thành công!");
        }
        if (!name.isPresent()) {
            if (subjectId.isPresent()) {
                exams = examService.findAllBySubject(subjectId.get(), pageable);
                model.addAttribute("exams", exams);
                model.addAttribute("subjectId", subjectId.get());
                return "exam/listExam";
            }
            exams = examService.findAll(pageable);
        } else {
            if (subjectId.isPresent()) {
                exams = examService.findAllByNameExamContainingAndBySubject(subjectId.get(), name.get(), pageable);
                model.addAttribute("subjectId", subjectId.get());
                model.addAttribute("keyword", name.get());
            } else {
                exams = examService.findAllByNameExamContaining(name.get(), pageable);
                model.addAttribute("keyword", name.get());
            }
        }
        model.addAttribute("exams", exams);
        return "exam/listExam";
    }

    @GetMapping("/exam/create")
    public String showCreateForm(Model model) {
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("exam", new Exam());
        model.addAttribute("questions", questionService.findAll());
        return "exam/createExam";
    }

//    @PostMapping("/exam/create")
//    public String saveExam(@Validated @ModelAttribute("exam") Exam exam, BindingResult bindingResult, RedirectAttributes re) {
//        if (bindingResult.hasFieldErrors()) {
//            return "exam/createExam";
//        } else {
//            re.addFlashAttribute("message", "Tạo đề thi thành công!");
//            examService.save(exam);
//            return "redirect:/exam/list";
//        }
//    }

    @GetMapping("/exam/editExam/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Exam exam = examService.findById(id);
        Iterable<Subject> subjects = subjectService.findAll();
        List<Chapter> chapters = new ArrayList<>(exam.getLesson().getChapter().getSubjectClasses().getChapters());
        Collections.sort(chapters);
        Set<Chapter> chapters1 = new LinkedHashSet<>(chapters);
        exam.getLesson().getChapter().getSubjectClasses().setChapters(chapters1);
        List<Lesson> lessons = new ArrayList<>(exam.getLesson().getChapter().getLessons());
        Collections.sort(lessons);
        Set<Lesson> lessons1 = new LinkedHashSet<>(lessons);
        exam.getLesson().getChapter().setLessons(lessons1);
        model.addAttribute("exam", exam);
        model.addAttribute("subjects", subjects);
        model.addAttribute("classes",classesService.
                findAllBySubject(exam.getLesson().getChapter().getSubjectClasses().getClasses().getId()));
        model.addAttribute("chapters",chapterService.findBySubjectAndClass(exam.getLesson().getChapter().getSubjectClasses().getSubject().getId(),
                exam.getLesson().getChapter().getSubjectClasses().getClasses().getId()));
        model.addAttribute("lessons",lessonService.findByChapId(exam.getLesson().getChapter().getId()));
        return "exam/editExam";
    }

    @GetMapping("/exam/delete/{id}")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes re) {
        Exam exam = examService.findById(id);
        examService.delete(exam);
        re.addFlashAttribute("message", "Xóa đề thi thành công!");
        return "redirect:/exam/list";
    }

    @GetMapping("/listExamSubject/{id}")
    public String listExamSubject(@RequestParam Optional<String> keyword,
                                  @PathVariable int id, Model model,
                                  @PageableDefault(value = 10) Pageable pageable) {
        Page<Exam> exams;
        if(keyword.isPresent()){
            exams = examService.findAllByNameExamContainingAndByLesson(id,keyword.get(),pageable);
            model.addAttribute("keyword",keyword.get());
        } else {
            exams = examService.findAllByLesson(id,pageable);
        }
        // Danh sách tài liệu
        List<Document> documents = documentService.findAll();
        List<Document> sDocument = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            if (i >= 5) {
                break;
            }
            sDocument.add(documents.get(i));
        }
        List<News> newses = newsService.getNewsInWeek();
        String userName = "null";
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            userName = ((UserDetails) principal).getUsername();
            model.addAttribute("userName", userName);
        }
        List<Result> results = null;
        int normal = 0;
        if (userName.equals("null")) {
            normal++;
        } else {
            results = rService.findByHistory(userName);
        }
        List<Integer> arrayNumber = new ArrayList<>();
        int dem = 0;
        if (results != null) {
            for (Exam exam : exams) {
                for (Result result : results) {
                    if (result.getExam() == null) {
                        continue;
                    }
                    if (exam.getId() == result.getExam().getId()) {
                        dem++;
                    }
                }
                arrayNumber.add(dem);
                dem = 0;

            }
        }
        List<Result> sList = rService.getTopTen();
        model.addAttribute("sList", sList);
        int total = userService.getTotalMember();
        List<User> newUsers = userService.getNewMember();
        model.addAttribute("totalMember", total);
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("newses", newses);
        model.addAttribute("documents", documents);
        model.addAttribute("exams", exams);
        model.addAttribute("normal", normal);
        model.addAttribute("arrayNumber", arrayNumber);
        model.addAttribute("subjectId", id);
        model.addAttribute("listIdExam", results);
        model.addAttribute("lesson",lessonService.findById(id));
        return "listExamSubject";
    }

    @GetMapping("/listExamSubject/{id}/{examId}")
    public String listExamSubject(@PathVariable int id, @PathVariable int examId, Model model) {
        Exam exam = examService.findById(examId);
        exam.setView(exam.getView() + 1);
        examService.save(exam);
        model.addAttribute("exam", exam);
        model.addAttribute("subjectId",id);
        return "exam/detail";
    }
}
