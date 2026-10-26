package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.print.Doc;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    @Autowired
    IQuizService qService;
    @Autowired
    IUserService userService;
    @Autowired
    ISubjectService subjectService;
    @Autowired
    IResultService rService;
    @Autowired
    IDocumentService documentService;
    @Autowired
    INewsService newsService;
    @Autowired
    IClassesService classesService;
    @Autowired
    IChapterService chapterService;

    @Autowired
    ILessonService lessonService;
    @Autowired
    ISubjectClassService subjectClassService;
    @Autowired
    com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository assignmentRepository;
    @Autowired
    com.tttn.webthitracnghiem.repository.NotificationRepository notificationRepository;
    @Autowired
    com.tttn.webthitracnghiem.repository.ExamRepository examRepository;
    @Autowired
    ILessonPlanService lessonPlanService;
    @Autowired
    CurriculumService curriculumService;
    @GetMapping("/role")
    public String redirectRole() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof MyUserDetail) {
            MyUserDetail userDetail = (MyUserDetail) principal;
            for (GrantedAuthority role : userDetail.getAuthorities()) {
                if (role.getAuthority().equals("ROLE_ADMIN")) {
                    return "redirect:/user/list";
                }
                if (role.getAuthority().equals("ROLE_SCHOOL_ADMIN")) {
                    return "redirect:/school-admin/users";
                }
                if (role.getAuthority().equals("ROLE_TEACHER")) {
                    return "redirect:/exam/list";
                }
                if (role.getAuthority().equals("ROLE_USER")) {
                    return "redirect:/";
                }
            }
        }
        return "redirect:/default";
    }

    @GetMapping("/login")
    public String showLogin(@RequestParam Optional<String> error, @RequestParam Optional<String> locked, Model model) {
        if (error.isPresent()) {
            model.addAttribute("message", "Tài khoản hoặc mật khẩu không chính xác!");
        }
        if (locked.isPresent()) {
            model.addAttribute("message", "Trường của bạn đang bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        return "login";
    }

    @GetMapping("/")
    public String show(Model model, @PageableDefault(value = 10) Pageable pageable) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String userName = ((UserDetails) principal).getUsername();
            model.addAttribute("userName", userName);
        }
        User currentUser = currentUser();
        List<Subject> studentSubjects = studentSubjects(currentUser);
        Object subjects = studentSubjects == null ? subjectService.findAll(pageable) : studentSubjects;
        model.addAttribute("studentScope", studentSubjects != null);
        List<Result> sList = rService.getTopTen();
        // Danh sách tài liệu
        List<Document> documents = filterStudentDocuments(documentService.findAll(), currentUser, studentSubjects);
        List<Document> sDocument = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            if (i >= 5) {
                break;
            }
            sDocument.add(documents.get(i));
        }
        model.addAttribute("documents", sDocument);
        // Danh sách tin tức trong tuần
        List<News> newses = newsService.getNewsInWeek();
        // Danh sách thành viên mới
        List<User> newUsers = userService.getNewMember();
        model.addAttribute("sList", sList);
        int total = userService.findByTotalUser();
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("totalMember", userService.getTotalMember());
        model.addAttribute("newses", newses);
        model.addAttribute("subjects", subjects);
        addStudentPeople(model, currentUser);
        return "default-page";
    }

    @GetMapping("/tin-tuc")
    public String news(Model model, @PageableDefault(value = 6) Pageable pageable) {
        Page<News> newses = newsService.findAll(pageable);
        model.addAttribute("newses", newses);
        return "news";
    }

    @GetMapping("/tai-lieu")
    public String document(Model model, @PageableDefault(value = 6) Pageable pageable) {
        Page<Document> documents = documentService.findAll(pageable);
        model.addAttribute("documents", documents);
        return "document";
    }

    @GetMapping("/tin-tuc/{id}")
    public String newsDetail(@PathVariable int id, Model model) {
        News news = newsService.findById(id);
        model.addAttribute("news", news);
        news.setView(news.getView() + 1);
        newsService.save(news);
        return "blog-detail";
    }

    @GetMapping("/tai-lieu/{id}")
    public String documentDetail(@PathVariable int id, Model model) {
        Document document = documentService.findById(id);
        model.addAttribute("document", document);
        document.setView(document.getView() + 1);
        documentService.save(document);
        return "document-detail";
    }

    @GetMapping("/listClassSubject/{id}")
    public String listClassSubject(@PathVariable int id, Model model) {
        User currentUser = currentUser();
        List<SubjectClasses> subjectClasses = subjectClassService.findBySubject(id);
        if (isStudent(currentUser)) {
            if (!isStudentSubject(currentUser, id)) return "redirect:/?warning=Mon hoc khong thuoc lop cua ban";
            Integer grade = studentGrade(currentUser);
            subjectClasses = subjectClasses.stream()
                    .filter(item -> item.getClasses() != null && Objects.equals(item.getClasses().getGrade(), grade))
                    .collect(Collectors.toList());
        }
        model.addAttribute("subjectClasses",subjectClasses);
        // Danh sách tài liệu
        List<Document> documents = filterStudentDocuments(documentService.findAll(), currentUser,
                isStudent(currentUser) ? studentSubjects(currentUser) : null);
        List<Document> sDocument = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            if (i >= 5) {
                break;
            }
            sDocument.add(documents.get(i));
        }
        List<News> newses = newsService.getNewsInWeek();
        List<Result> sList = rService.getTopTen();
        model.addAttribute("sList", sList);
        int total = userService.getTotalMember();
        List<User> newUsers = userService.getNewMember();
        model.addAttribute("totalMember", total);
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("newses", newses);
        model.addAttribute("documents", documents);
        addStudentPeople(model, currentUser);
        return "listClassSubject";
    }

    @GetMapping("/studyProgram/{classId}/{subjectId}")
    public String getProgram(@PathVariable("classId") Integer classId, @PathVariable("subjectId") Integer subjectId,
                             Model model) {
        User currentUser = currentUser();
        Classes selectedClass = classesService.findById(classId);
        if (isStudent(currentUser)
                && (selectedClass == null || !Objects.equals(selectedClass.getGrade(), studentGrade(currentUser))
                || !isStudentSubject(currentUser, subjectId))) {
            return "redirect:/?warning=Chuong trinh khong thuoc lop cua ban";
        }
        // The viewer's school curriculum (or the shared one while the school has no copy).
        Integer schoolId = curriculumService.schoolIdOf(currentUser);
        List<Chapter> chapters = curriculumService.findSubjectClass(subjectId, classId)
                .map(subjectClass -> curriculumService.readChapters(schoolId, subjectClass))
                .orElse(new ArrayList<>());
        Map<Integer, List<Lesson>> lessonsByChapter = new HashMap<>();
        List<Integer> lessonIds = new ArrayList<>();
        for (Chapter chapter : chapters) {
            List<Lesson> lessons = curriculumService.lessonsOf(chapter);
            lessonsByChapter.put(chapter.getId(), lessons);
            lessons.forEach(lesson -> lessonIds.add(lesson.getId()));
        }
        // An exam is listed under every lesson of its scope, not only its primary lesson.
        Map<Integer, Set<Exam>> examsByLesson = new HashMap<>();
        for (Exam exam : lessonIds.isEmpty() ? List.<Exam>of() : examRepository.findCoveringLessons(lessonIds)) {
            if (isStudent(currentUser) && (exam.isPractice() || !canStudentTakeExam(exam, currentUser))) {
                continue;
            }
            Set<Integer> covered = new LinkedHashSet<>();
            if (exam.getLesson() != null) covered.add(exam.getLesson().getId());
            exam.getScopeLessons().forEach(lesson -> covered.add(lesson.getId()));
            covered.forEach(id -> examsByLesson.computeIfAbsent(id, key -> new LinkedHashSet<>()).add(exam));
        }
        // These collections are inverse (mappedBy) sides, so filling them for the page writes nothing back.
        for (Chapter chapter : chapters) {
            List<Lesson> lessons = lessonsByChapter.get(chapter.getId());
            lessons.forEach(lesson -> lesson.setExams(examsByLesson.getOrDefault(lesson.getId(), new LinkedHashSet<>())));
            chapter.setLessons(new LinkedHashSet<>(lessons));
        }
        model.addAttribute("chapters", chapters);

        // Danh sách tài liệu
        List<Document> documents = filterStudentDocuments(documentService.findAll(), currentUser,
                isStudent(currentUser) ? studentSubjects(currentUser) : null);
        List<Document> sDocument = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            if (i >= 5) {
                break;
            }
            sDocument.add(documents.get(i));
        }
        List<News> newses = newsService.getNewsInWeek();
        List<Result> sList = rService.getTopTen();
        model.addAttribute("sList", sList);
        int total = userService.getTotalMember();
        List<User> newUsers = userService.getNewMember();
        model.addAttribute("totalMember", total);
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("newses", newses);
        model.addAttribute("documents", documents);
        model.addAttribute("subject",subjectService.findById(subjectId));
        model.addAttribute("class",selectedClass);
        addStudentLessonPlans(model, currentUser, subjectId, selectedClass);
        addStudentPeople(model, currentUser);
        return "studyProgramSubject";
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) return null;
        return userService.findById(authentication.getName());
    }

    private boolean isStudent(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return user != null && authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority()));
    }

    private Integer studentGrade(User user) {
        return user != null && user.getTrackingClass() != null ? user.getTrackingClass().getGrade() : null;
    }

    private List<Subject> studentSubjects(User user) {
        if (!isStudent(user) || user.getTrackingClass() == null) return null;
        List<Subject> subjects = assignmentRepository.findByTrackingClassId(user.getTrackingClass().getId()).stream()
                .map(ClassSubjectAssignment::getSubject).filter(s -> s != null && !s.isDeleted()).distinct()
                .sorted(Comparator.comparing(Subject::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
        if (!subjects.isEmpty()) return subjects;
        Integer grade = studentGrade(user);
        return subjectClassService.findAll().stream()
                .filter(item -> item.getClasses() != null && Objects.equals(item.getClasses().getGrade(), grade))
                .map(SubjectClasses::getSubject).filter(s -> s != null && !s.isDeleted()).distinct()
                .sorted(Comparator.comparing(Subject::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    private boolean isStudentSubject(User user, Integer subjectId) {
        List<Subject> subjects = studentSubjects(user);
        return subjects != null && subjects.stream().anyMatch(subject -> Objects.equals(subject.getId(), subjectId));
    }

    private List<Document> filterStudentDocuments(List<Document> documents, User user, List<Subject> subjects) {
        if (!isStudent(user) || subjects == null) return documents;
        Set<Integer> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toSet());
        Integer grade = studentGrade(user);
        Integer schoolId = user.getSchool() == null ? null : user.getSchool().getId();
        return documents.stream().filter(document -> document.getSubject() != null
                && subjectIds.contains(document.getSubject().getId())
                && (document.getGrade() == null || Objects.equals(document.getGrade(), grade))
                && (Boolean.TRUE.equals(document.getIsPublic()) || document.getSchool() == null
                || Objects.equals(document.getSchool().getId(), schoolId))).collect(Collectors.toList());
    }

    private void addStudentLessonPlans(Model model, User student, Integer subjectId, Classes selectedClass) {
        if (!isStudent(student) || student.getTrackingClass() == null || selectedClass == null) return;
        List<LessonPlan> plans = findStudentLessonPlans(student, subjectId, selectedClass);
        Map<Integer, List<Exam>> examsByLesson = new LinkedHashMap<>();
        List<LessonPlan> detailedPlans = new ArrayList<>();
        for (LessonPlan plan : plans) {
            LessonPlan detailed = lessonPlanService.findById(plan.getId());
            if (detailed == null) continue;
            detailedPlans.add(detailed);
            if (detailed.getPlanChapters() == null) continue;
            for (LessonPlanChapter topic : detailed.getPlanChapters()) {
                if (topic.getLessons() == null) continue;
                for (LessonPlanLesson lesson : topic.getLessons()) {
                    List<Exam> exams = examRepository.findByLessonPlanLessonId(lesson.getId()).stream()
                            .filter(exam -> !exam.isPractice() && canStudentTakeExam(exam, student))
                            .collect(Collectors.toList());
                    if (!exams.isEmpty()) examsByLesson.put(lesson.getId(), exams);
                }
            }
        }
        model.addAttribute("studentLessonPlans", detailedPlans);
        model.addAttribute("studentLessonPlanExams", examsByLesson);
    }

    /**
     * A private lesson plan is normally hidden from students. Once a teacher
     * attaches a real exam to one of its lessons, however, the students who
     * are allowed to take that exam must be able to reach it from the
     * subject/topic/lesson view as well as from the assigned-exams page.
     */
    List<LessonPlan> findStudentLessonPlans(User student, Integer subjectId, Classes selectedClass) {
        if (student == null || student.getTrackingClass() == null || selectedClass == null) {
            return Collections.emptyList();
        }

        Map<Integer, LessonPlan> plansById = new LinkedHashMap<>();
        List<LessonPlan> visiblePlans = lessonPlanService.findVisibleForStudent(student.getId(),
                subjectId, selectedClass.getId(), student.getTrackingClass().getId());
        if (visiblePlans != null) {
            visiblePlans.stream()
                    .filter(plan -> matchesStudentLessonPlan(plan, student, subjectId, selectedClass))
                    .filter(plan -> plan.getId() != null)
                    .forEach(plan -> plansById.put(plan.getId(), plan));
        }

        if (examRepository != null) {
            List<Exam> exams = examRepository.findAll();
            if (exams != null) {
                exams.stream()
                        .filter(exam -> exam != null && !exam.isPractice())
                        .filter(exam -> canStudentTakeExam(exam, student))
                        .map(Exam::getLessonPlanLesson)
                        .filter(Objects::nonNull)
                        .map(LessonPlanLesson::getChapter)
                        .filter(Objects::nonNull)
                        .map(LessonPlanChapter::getLessonPlan)
                        .filter(plan -> matchesStudentLessonPlan(plan, student, subjectId, selectedClass))
                        .filter(plan -> plan.getId() != null)
                        .forEach(plan -> plansById.putIfAbsent(plan.getId(), plan));
            }
        }

        return new ArrayList<>(plansById.values());
    }

    private boolean matchesStudentLessonPlan(LessonPlan plan, User student,
                                             Integer subjectId, Classes selectedClass) {
        if (plan == null || selectedClass == null || student == null || student.getTrackingClass() == null) {
            return false;
        }
        Integer planSubjectId = plan.getSubject() != null ? plan.getSubject().getId()
                : plan.getSubjectClasses() != null && plan.getSubjectClasses().getSubject() != null
                ? plan.getSubjectClasses().getSubject().getId() : null;
        if (subjectId != null && !Objects.equals(subjectId, planSubjectId)) return false;

        Integer planGrade = plan.getGrade() != null ? plan.getGrade().getGrade()
                : plan.getSubjectClasses() != null && plan.getSubjectClasses().getClasses() != null
                ? plan.getSubjectClasses().getClasses().getGrade() : null;
        if (!Objects.equals(selectedClass.getGrade(), planGrade)) return false;

        return plan.getTargetClass() == null
                || Objects.equals(plan.getTargetClass().getId(), student.getTrackingClass().getId());
    }

    private void addStudentPeople(Model model, User user) {
        if (!isStudent(user) || user.getTrackingClass() == null) return;
        model.addAttribute("studentSubjects", studentSubjects(user));
        model.addAttribute("subjectTeachers", assignmentRepository.findByTrackingClassId(user.getTrackingClass().getId()));
        model.addAttribute("homeroomTeacher", user.getTrackingClass().getHomeroomTeacher());
        model.addAttribute("studentClass", user.getTrackingClass());
        model.addAttribute("studentNotifications", notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId()));
    }

    private boolean canStudentTakeExam(Exam exam, User student) {
        if (exam == null || student == null) return false;
        String targetType = exam.getTargetType();
        if (targetType == null || targetType.trim().isEmpty()) {
            targetType = Boolean.FALSE.equals(exam.getIsPublic()) ? "SCHOOL" : "PUBLIC";
        }
        if ("CLASS".equalsIgnoreCase(targetType)) {
            return exam.getTargetClass() != null && student.getTrackingClass() != null
                    && Objects.equals(exam.getTargetClass().getId(), student.getTrackingClass().getId());
        }
        if ("GRADE".equalsIgnoreCase(targetType)) {
            return exam.getTargetGrade() != null && Objects.equals(exam.getTargetGrade(), studentGrade(student))
                    && sameSchool(student.getSchool(), exam.getSchool());
        }
        if ("SCHOOL".equalsIgnoreCase(targetType)) return sameSchool(student.getSchool(), exam.getSchool());
        return !Boolean.FALSE.equals(exam.getIsPublic()) || sameSchool(student.getSchool(), exam.getSchool());
    }

    private boolean sameSchool(School first, School second) {
        return first != null && second != null && first.getId() != null && first.getId().equals(second.getId());
    }

    @GetMapping("/error")
    public String error() {
        return "error-page";
    }
}
