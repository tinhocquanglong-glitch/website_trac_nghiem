package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.service.*;
import com.tttn.webthitracnghiem.service.impl.ExamServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    @Autowired
    private ILessonPlanService lessonPlanService;
    @Autowired
    private com.tttn.webthitracnghiem.service.CurriculumService curriculumService;

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

    /**
     * Kiểm tra người dùng hiện tại có phải ROLE_TEACHER (không phải ADMIN)
     */
    private boolean isTeacherOnly(Authentication auth) {
        if (auth == null) return false;
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER"));
        return isTeacher && !isAdmin;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null) return null;
        return userService.findById(auth.getName());
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean teacherOnly = isTeacherOnly(auth);

        if (teacherOnly) {
            // Giáo viên chỉ thấy đề thi do mình tạo
            String userId = auth.getName();
            if (!name.isPresent()) {
                if (subjectId.isPresent()) {
                    exams = examService.findByTeacherAndSubject(userId, subjectId.get(), pageable);
                    model.addAttribute("subjectId", subjectId.get());
                } else {
                    exams = examService.findByTeacher(userId, pageable);
                }
            } else {
                if (subjectId.isPresent()) {
                    exams = examService.findByTeacherAndSubjectAndNameContaining(userId, subjectId.get(), name.get(), pageable);
                    model.addAttribute("subjectId", subjectId.get());
                } else {
                    exams = examService.findByTeacherAndNameContaining(userId, name.get(), pageable);
                }
                model.addAttribute("keyword", name.get());
            }
        } else {
            // Admin thấy tất cả
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
        }

        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        model.addAttribute("isAdminOrSchoolAdmin", isAdmin || isSchoolAdmin);
        model.addAttribute("exams", exams);
        return "exam/listExam";
    }

    @GetMapping("/exam/create")
    public String showCreateForm(Model model) {
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("exam", new Exam());
        // Questions are loaded per chosen scope from /api/question/pool.
        model.addAttribute("questions", List.of());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = getCurrentUser(auth);

        model.addAttribute("isAdminOrSchoolAdmin", isAdmin || isSchoolAdmin);
        model.addAttribute("currentUser", currentUser);
        return "exam/createExam";
    }

    @GetMapping("/exam/editExam/{id}")
    public String edit(@PathVariable("id") Integer id, Model model, RedirectAttributes ra) {
        Exam exam = examService.findById(id);

        if (exam == null) {
            if (ra != null) {
                ra.addFlashAttribute("error", "Khong tim thay de thi!");
            }
            return "redirect:/exam/list";
        }

        // Giáo viên chỉ edit đề của mình
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacherOnly(auth)) {
            User currentUser = getCurrentUser(auth);
            if (currentUser != null && (exam.getUsers() == null || !exam.getUsers().getId().equals(currentUser.getId()))) {
                ra = null; // workaround - redirect với flash attribute qua RedirectAttributes ko inject được qua GET
                return "redirect:/exam/list";
            }
        }

        // Đề tạo từ giáo án không có legacy Lesson. Tách sang template tương ứng
        // để không dereference exam.lesson và vẫn cho phép cập nhật đề.
        if (exam.getLessonPlanLesson() != null && exam.getLesson() == null) {
            List<Question> selectedQuestions = questionService.findByExam(id);
            model.addAttribute("exam", exam);
            model.addAttribute("questions", questionService.findAllWithContext());
            model.addAttribute("selectedQuestions", selectedQuestions);
            model.addAttribute("selectedQuestionIds", selectedQuestions.stream()
                    .map(Question::getQuesId)
                    .collect(java.util.stream.Collectors.toSet()));
            return "exam/editCustomExam";
        }

        Iterable<Subject> subjects = subjectService.findAll();
        // Only the curriculum the exam's lesson belongs to (its school's copy, or the shared one).
        Chapter examChapter = exam.getLesson().getChapter();
        Integer curriculumSchoolId = examChapter.getSchool() == null ? null : examChapter.getSchool().getId();
        List<Chapter> chapters = curriculumService.readChapters(curriculumSchoolId, examChapter.getSubjectClasses());
        exam.getLesson().getChapter().getSubjectClasses().setChapters(new LinkedHashSet<>(chapters));
        List<Lesson> lessons = new ArrayList<>(exam.getLesson().getChapter().getLessons());
        Collections.sort(lessons);
        Set<Lesson> lessons1 = new LinkedHashSet<>(lessons);
        exam.getLesson().getChapter().setLessons(lessons1);
        model.addAttribute("exam", exam);
        model.addAttribute("subjects", subjects);
        model.addAttribute("classes",classesService.
                findAllBySubject(exam.getLesson().getChapter().getSubjectClasses().getClasses().getId()));
        model.addAttribute("chapters", chapters);
        model.addAttribute("lessons",lessonService.findByChapId(exam.getLesson().getChapter().getId()));
        return "exam/editExam";
    }

    @PostMapping("/exam/delete")
    public String delete(@RequestParam("id") Integer id,
                         @RequestParam(value = "password", required = false) String password,
                         RedirectAttributes re) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        if (!isAdmin && !isSchoolAdmin) {
            re.addFlashAttribute("error", "Chỉ Ban giám hiệu (Hiệu trưởng) hoặc Quản trị viên mới có quyền xóa đề thi.");
            return "redirect:/exam/list";
        }

        User currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            re.addFlashAttribute("error", "Vui lòng đăng nhập lại!");
            return "redirect:/exam/list";
        }

        // Xác thực mật khẩu
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (password == null || password.trim().isEmpty() || !encoder.matches(password.trim(), currentUser.getPassWord())) {
            re.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa đề thi.");
            return "redirect:/exam/list";
        }

        Exam exam = examService.findById(id);
        if (exam == null) {
            re.addFlashAttribute("error", "Không tìm thấy đề thi cần xóa.");
            return "redirect:/exam/list";
        }

        // Hiệu trưởng chỉ được xóa đề thi thuộc trường mình
        if (isSchoolAdmin && !isAdmin) {
            Integer userSchoolId = currentUser.getSchool() != null ? currentUser.getSchool().getId() : null;
            Integer examSchoolId = exam.getSchool() != null ? exam.getSchool().getId() : null;
            if (userSchoolId == null || !userSchoolId.equals(examSchoolId)) {
                re.addFlashAttribute("error", "Hiệu trưởng chỉ được quyền xóa đề thi thuộc trường mình quản lý!");
                return "redirect:/exam/list";
            }
        }

        examService.delete(exam);
        re.addFlashAttribute("message", "Đã xóa (ẩn) đề thi " + exam.getNameExam() + " thành công!");
        return "redirect:/exam/list";
    }

    @GetMapping("/exam/delete/{id}")
    public String deleteGet(@PathVariable("id") Integer id, RedirectAttributes re) {
        re.addFlashAttribute("error", "Hành động xóa đề thi yêu cầu xác thực mật khẩu qua biểu mẫu!");
        return "redirect:/exam/list";
    }

    @GetMapping("/listExamSubject/{id}")
    public String listExamSubject(@RequestParam Optional<String> keyword,
                                  @PathVariable int id, Model model,
                                  @PageableDefault(value = 10) Pageable pageable) {
        Lesson legacyLesson = lessonService.findById(id);
        LessonPlanLesson lessonPlanCandidate = lessonPlanService.findLesson(id);
        LessonPlanLesson customLesson = legacyLesson == null ? lessonPlanCandidate : null;
        Page<Exam> exams = legacyLesson == null
                ? findCustomLessonExams(id, keyword, pageable)
                : findLegacyLessonExams(id, keyword, pageable);
        String view = renderExamList(keyword, model, pageable, id, legacyLesson, customLesson, exams,
                "/listExamSubject/" + id);
        if (legacyLesson != null && lessonPlanCandidate != null) {
            model.addAttribute("lessonPlanCandidate", lessonPlanCandidate);
            model.addAttribute("lessonPlanCandidatePath", "/listExamSubject/lesson-plan/" + id);
        }
        return view;
    }

    /**
     * Custom lesson-plan lessons use a separate URL because their IDs are
     * independent from legacy lessons and can collide numerically.
     */
    @GetMapping("/listExamSubject/lesson-plan/{id}")
    public String listLessonPlanExam(@RequestParam Optional<String> keyword,
                                     @PathVariable int id, Model model,
                                     @PageableDefault(value = 10) Pageable pageable) {
        LessonPlanLesson customLesson = lessonPlanService.findLesson(id);
        if (customLesson == null) {
            return "redirect:/lesson-plans";
        }
        Page<Exam> exams = findCustomLessonExams(id, keyword, pageable);
        return renderExamList(keyword, model, pageable, id, null, customLesson, exams,
                "/listExamSubject/lesson-plan/" + id);
    }

    private String renderExamList(Optional<String> keyword, Model model, Pageable pageable, int id,
                                  Lesson legacyLesson, LessonPlanLesson customLesson, Page<Exam> exams,
                                  String examListPath) {
        if (keyword.isPresent()) model.addAttribute("keyword", keyword.get());
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
        model.addAttribute("examListPath", examListPath);
        model.addAttribute("listIdExam", results);
        model.addAttribute("lesson", legacyLesson);
        model.addAttribute("lessonPlanLesson", customLesson);
        return "listExamSubject";
    }

    private Page<Exam> findLegacyLessonExams(int id, Optional<String> keyword, Pageable pageable) {
        return keyword.isPresent()
                ? examService.findAllByNameExamContainingAndByLesson(id, keyword.get(), pageable)
                : examService.findAllByLesson(id, pageable);
    }

    private Page<Exam> findCustomLessonExams(int id, Optional<String> keyword, Pageable pageable) {
        return keyword.isPresent()
                ? examService.findAllByNameExamContainingAndByLessonPlanLesson(id, keyword.get(), pageable)
                : examService.findAllByLessonPlanLesson(id, pageable);
    }

    @GetMapping("/listExamSubject/{id}/{examId}")
    public String listExamSubject(@PathVariable int id, @PathVariable int examId, Model model) {
        Exam exam = examService.findById(examId);
        exam.setView(exam.getView() + 1);
        examService.save(exam);
        model.addAttribute("exam", exam);
        model.addAttribute("subjectId",id);
        model.addAttribute("examListPath", exam.getLessonPlanLesson() != null
                ? "/listExamSubject/lesson-plan/" + id
                : "/listExamSubject/" + id);
        return "exam/detail";
    }
}
