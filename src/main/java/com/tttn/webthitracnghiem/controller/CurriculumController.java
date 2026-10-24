package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ExamRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.service.CurriculumService;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Curriculum editor: one subject-grade at a time, for the viewer's school (or the shared curriculum for admin). */
@Controller
@RequestMapping("/curriculum")
public class CurriculumController {
    private final CurriculumService curriculumService;
    private final SubjectClassRepository subjectClassRepository;
    private final SchoolRepository schoolRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final ILessonPlanService lessonPlanService;
    private final IUserService userService;

    public CurriculumController(CurriculumService curriculumService,
                                SubjectClassRepository subjectClassRepository,
                                SchoolRepository schoolRepository,
                                QuestionRepository questionRepository,
                                ExamRepository examRepository,
                                ILessonPlanService lessonPlanService,
                                IUserService userService) {
        this.curriculumService = curriculumService;
        this.subjectClassRepository = subjectClassRepository;
        this.schoolRepository = schoolRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.lessonPlanService = lessonPlanService;
        this.userService = userService;
    }

    @GetMapping({"", "/"})
    public String index(@RequestParam(required = false) Integer gradeId,
                        @RequestParam(required = false) Integer subjectId,
                        @RequestParam(required = false) Integer schoolId,
                        Model model) {
        User user = currentUser();
        boolean admin = isAdmin();
        List<Classes> grades = lessonPlanService.findGrades(user, admin);
        Classes grade = grades.stream().filter(item -> item.getId().equals(gradeId)).findFirst().orElse(null);
        List<SubjectClasses> subjectClasses = grade == null ? List.of()
                : subjectClassRepository.findByClassId(grade.getId()).stream()
                .sorted(Comparator.comparing(item -> item.getSubject().getName(),
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
        SubjectClasses selected = subjectClasses.stream()
                .filter(item -> subjectId != null && item.getSubject().getId() == subjectId)
                .findFirst().orElse(null);
        Integer targetSchoolId = admin ? schoolId : curriculumService.schoolIdOf(user);

        model.addAttribute("grades", grades);
        model.addAttribute("gradeId", grade == null ? null : grade.getId());
        model.addAttribute("subjectClasses", subjectClasses);
        model.addAttribute("subjectId", selected == null ? null : selected.getSubject().getId());
        model.addAttribute("isAdmin", admin);
        model.addAttribute("schools", admin ? schoolRepository.findAllByActiveTrueOrderByNameAsc() : List.of());
        model.addAttribute("schoolId", targetSchoolId);
        School targetSchool = targetSchoolId == null ? null : schoolRepository.findById(targetSchoolId).orElse(null);
        model.addAttribute("schoolName", targetSchool == null ? "Chương trình chung" : targetSchool.getName());

        if (selected != null) {
            boolean canEdit = curriculumService.canEdit(user, targetSchoolId, selected);
            boolean ownCopy = targetSchoolId == null || curriculumService.hasOwnCopy(targetSchoolId, selected);
            // Readers never trigger a copy; the first edit (or attaching content) does.
            List<Chapter> chapters = canEdit && targetSchoolId != null
                    ? curriculumService.schoolChapters(targetSchoolId, selected)
                    : curriculumService.readChapters(targetSchoolId, selected);
            List<ChapterNode> tree = new ArrayList<>();
            for (Chapter chapter : chapters) {
                List<LessonNode> lessons = curriculumService.lessonsOf(chapter).stream()
                        .map(lesson -> new LessonNode(lesson, questionRepository.countByLesson_Id(lesson.getId()),
                                examRepository.countCoveringLesson(lesson.getId())))
                        .collect(Collectors.toList());
                tree.add(new ChapterNode(chapter, lessons));
            }
            model.addAttribute("selected", selected);
            model.addAttribute("tree", tree);
            model.addAttribute("canEdit", canEdit);
            model.addAttribute("usingShared", targetSchoolId != null && !ownCopy && !canEdit);
        }
        return "curriculum/index";
    }

    @PostMapping("/chapters")
    public String addChapter(@RequestParam int subjectClassId, @RequestParam(required = false) Integer schoolId,
                             @RequestParam String name, RedirectAttributes ra) {
        SubjectClasses subjectClass = subjectClassRepository.findById(subjectClassId).orElse(null);
        if (subjectClass == null) {
            ra.addFlashAttribute("error", "Không tìm thấy môn học của khối này.");
            return "redirect:/curriculum";
        }
        Integer targetSchoolId = isAdmin() ? schoolId : curriculumService.schoolIdOf(currentUser());
        return run(ra, subjectClass, targetSchoolId, "Đã thêm chủ đề.",
                () -> curriculumService.addChapter(currentUser(), targetSchoolId, subjectClass, name));
    }

    @PostMapping("/chapters/{id}/rename")
    public String renameChapter(@PathVariable int id, @RequestParam String name, @RequestParam int subjectClassId,
                                @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, "Đã đổi tên chủ đề.",
                () -> curriculumService.renameChapter(currentUser(), id, name));
    }

    @PostMapping("/chapters/{id}/move")
    public String moveChapter(@PathVariable int id, @RequestParam int direction, @RequestParam int subjectClassId,
                              @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, null,
                () -> curriculumService.moveChapter(currentUser(), id, direction));
    }

    @PostMapping("/chapters/{id}/delete")
    public String deleteChapter(@PathVariable int id, @RequestParam int subjectClassId,
                                @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, "Đã xóa chủ đề.",
                () -> curriculumService.deleteChapter(currentUser(), id));
    }

    @PostMapping("/chapters/{id}/lessons")
    public String addLesson(@PathVariable int id, @RequestParam String name, @RequestParam int subjectClassId,
                            @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, "Đã thêm bài.",
                () -> curriculumService.addLesson(currentUser(), id, name));
    }

    @PostMapping("/lessons/{id}/rename")
    public String renameLesson(@PathVariable int id, @RequestParam String name, @RequestParam int subjectClassId,
                               @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, "Đã đổi tên bài.",
                () -> curriculumService.renameLesson(currentUser(), id, name));
    }

    @PostMapping("/lessons/{id}/move")
    public String moveLesson(@PathVariable int id, @RequestParam int direction, @RequestParam int subjectClassId,
                             @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, null,
                () -> curriculumService.moveLesson(currentUser(), id, direction));
    }

    @PostMapping("/lessons/{id}/delete")
    public String deleteLesson(@PathVariable int id, @RequestParam int subjectClassId,
                               @RequestParam(required = false) Integer schoolId, RedirectAttributes ra) {
        return run(ra, subjectClassId, schoolId, "Đã xóa bài.",
                () -> curriculumService.deleteLesson(currentUser(), id));
    }

    private String run(RedirectAttributes ra, int subjectClassId, Integer schoolId, String message, Runnable action) {
        SubjectClasses subjectClass = subjectClassRepository.findById(subjectClassId).orElse(null);
        return run(ra, subjectClass, isAdmin() ? schoolId : null, message, action);
    }

    private String run(RedirectAttributes ra, SubjectClasses subjectClass, Integer schoolId, String message, Runnable action) {
        try {
            action.run();
            if (message != null) {
                ra.addFlashAttribute("message", message);
            }
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        if (subjectClass == null) {
            return "redirect:/curriculum";
        }
        UriComponentsBuilder target = UriComponentsBuilder.fromPath("/curriculum")
                .queryParam("gradeId", subjectClass.getClasses().getId())
                .queryParam("subjectId", subjectClass.getSubject().getId());
        if (schoolId != null && isAdmin()) {
            target.queryParam("schoolId", schoolId);
        }
        return "redirect:" + target.toUriString();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : userService.findById(auth.getName());
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Plain getters (not records) so Thymeleaf's SpEL can read them as properties.
    public static class ChapterNode {
        private final Chapter chapter;
        private final List<LessonNode> lessons;

        public ChapterNode(Chapter chapter, List<LessonNode> lessons) {
            this.chapter = chapter;
            this.lessons = lessons;
        }

        public Chapter getChapter() { return chapter; }
        public List<LessonNode> getLessons() { return lessons; }
    }

    public static class LessonNode {
        private final Lesson lesson;
        private final long questionCount;
        private final long examCount;

        public LessonNode(Lesson lesson, long questionCount, long examCount) {
            this.lesson = lesson;
            this.questionCount = questionCount;
            this.examCount = examCount;
        }

        public Lesson getLesson() { return lesson; }
        public long getQuestionCount() { return questionCount; }
        public long getExamCount() { return examCount; }
    }
}
