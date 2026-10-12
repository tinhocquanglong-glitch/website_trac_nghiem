package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IQuestionService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired
    private IUserService userService;

    @ModelAttribute("exams")
    public Iterable<Exam> exams() {
        return examService.findAll();
    }

    /**
     * Kiểm tra người dùng hiện tại có phải ROLE_TEACHER không (không phải ADMIN)
     */
    private boolean isTeacherOnly(Authentication auth) {
        if (auth == null) return false;
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER"));
        return isTeacher && !isAdmin;
    }

    /**
     * Lấy school của user hiện tại (nếu là teacher)
     */
    private School getCurrentUserSchool(Authentication auth) {
        if (auth == null) return null;
        User user = userService.findById(auth.getName());
        return (user != null) ? user.getSchool() : null;
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean teacherOnly = isTeacherOnly(auth);

        if (teacherOnly) {
            // Giáo viên: chỉ thấy câu hỏi public HOẶC của trường mình
            School school = getCurrentUserSchool(auth);
            int schoolId = (school != null) ? school.getId() : 1;

            if (!name.isPresent()) {
                if (subjectId.isPresent()) {
                    questions = questionService.findAllBySubjectAndSchool(subjectId, schoolId, pageable);
                    model.addAttribute("subjectId", subjectId.get());
                } else {
                    questions = questionService.findAllBySchool(schoolId, pageable);
                }
            } else {
                if (subjectId.isPresent()) {
                    questions = questionService.findBySubjectAndTitleAndSchool(subjectId.get(), name.get(), schoolId, pageable);
                    model.addAttribute("subjectId", subjectId.get());
                } else {
                    questions = questionService.findAllByTitleContainingAndSchool(name.get(), schoolId, pageable);
                }
                model.addAttribute("keyword", name.get());
            }
        } else {
            // Admin: thấy tất cả
            if (!name.isPresent()) {
                if (subjectId.isPresent()) {
                    questions = questionService.findAllBySubject(subjectId, pageable);
                    model.addAttribute("subjectId", subjectId.get());
                    model.addAttribute("questions", questions);
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
        }

        model.addAttribute("questions", questions);
        return "question/listQuestion";
    }

    @GetMapping("/question/editQuest/{id}")
    public String editMember(@PathVariable("id") Integer id, Model model) {
        Question question = questionService.findById(id);

        // Kiểm tra quyền: giáo viên chỉ edit câu hỏi của trường mình hoặc do mình tạo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacherOnly(auth)) {
            School school = getCurrentUserSchool(auth);
            int schoolId = (school != null) ? school.getId() : -1;
            boolean isPublic = Boolean.TRUE.equals(question.getIsPublic());
            boolean sameSchool = (question.getSchool() != null && question.getSchool().getId() == schoolId);
            if (isPublic && !sameSchool) {
                // Câu hỏi public của trường khác => chỉ xem, không edit
                return "redirect:/question/list";
            }
        }

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
        // Giữ nguyên school của câu hỏi nếu giáo viên update
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacherOnly(auth)) {
            School school = getCurrentUserSchool(auth);
            if (question.getSchool() == null && school != null) {
                question.setSchool(school);
            }
        }
        questionService.save(question);
        ra.addFlashAttribute("message", "Chỉnh sửa câu hỏi thành công!");
        return "redirect:/question/list";
    }

    @GetMapping("/question/delete/{id}")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes ra) {
        Question question = questionService.findById(id);

        // Giáo viên chỉ xóa câu hỏi của trường mình (private)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacherOnly(auth)) {
            School school = getCurrentUserSchool(auth);
            int schoolId = (school != null) ? school.getId() : -1;
            boolean sameSchool = (question.getSchool() != null && question.getSchool().getId() == schoolId);
            if (!sameSchool) {
                ra.addFlashAttribute("error", "Bạn không có quyền xóa câu hỏi này!");
                return "redirect:/question/list";
            }
        }

        questionService.delete(question);
        ra.addFlashAttribute("message","Xóa câu hỏi thành công!");
        return "redirect:/question/list";
    }

    @GetMapping("/question/create")
    public String create(Model model) {
        model.addAttribute("question", new Question());
        model.addAttribute("subjects", subjectService.findAll());
        return "question/createQuestion";
    }

    @PostMapping("/question/create")
    public String save(@Valid @ModelAttribute Question question, BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasFieldErrors()) {
            return "question/createQuestion";
        }

        // Tự động gán school và isPublic cho giáo viên
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacherOnly(auth)) {
            School school = getCurrentUserSchool(auth);
            if (school != null) {
                question.setSchool(school);
            }
            // Nếu chưa set isPublic thì mặc định false (private của trường)
            if (question.getIsPublic() == null) {
                question.setIsPublic(false);
            }
        } else {
            // Admin: mặc định public nếu chưa chọn
            if (question.getIsPublic() == null) {
                question.setIsPublic(true);
            }
        }

        ra.addFlashAttribute("message", "Thêm câu hỏi thành công");
        questionService.save(question);
        return "redirect:/question/list";
    }
}
