package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ClassesRepository;
import com.tttn.webthitracnghiem.service.CurriculumService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Curriculum tree for the staff forms (questions, exams, lesson plans). Content attached through
 * these forms belongs to the user's school, so the school's own copy is used (created on first use).
 */
@RestController
@RequestMapping("/api/curriculum")
public class CurriculumApiController {
    private final CurriculumService curriculumService;
    private final ClassesRepository classesRepository;
    private final IUserService userService;

    public CurriculumApiController(CurriculumService curriculumService, ClassesRepository classesRepository,
                                   IUserService userService) {
        this.curriculumService = curriculumService;
        this.classesRepository = classesRepository;
        this.userService = userService;
    }

    /** Accepts the grade either as the grade-catalog id ({@code gradeId}) or as the grade number 1-12 ({@code grade}). */
    @GetMapping
    public ResponseEntity<?> tree(@RequestParam int subjectId,
                                  @RequestParam(required = false) Integer gradeId,
                                  @RequestParam(required = false) Integer grade) {
        Optional<Classes> gradeEntry = gradeId != null ? classesRepository.findById(gradeId)
                : grade != null ? classesRepository.findFirstByGradeOrderByIdAsc(grade) : Optional.empty();
        if (gradeEntry.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn khối.");
        }
        Optional<SubjectClasses> subjectClass = curriculumService.findSubjectClass(subjectId, gradeEntry.get().getId());
        if (subjectClass.isEmpty()) {
            return ResponseEntity.ok(new Tree(null, null, false, List.of()));
        }
        User user = currentUser();
        Integer schoolId = curriculumService.schoolIdOf(user);
        List<Chapter> chapters = curriculumService.schoolChapters(schoolId, subjectClass.get());
        List<TopicItem> topics = new ArrayList<>();
        for (Chapter chapter : chapters) {
            List<LessonItem> lessons = new ArrayList<>();
            for (Lesson lesson : curriculumService.lessonsOf(chapter)) {
                lessons.add(new LessonItem(lesson.getId(), lesson.getNameLesson()));
            }
            topics.add(new TopicItem(chapter.getId(), chapter.getChapterName(), lessons));
        }
        return ResponseEntity.ok(new Tree(subjectClass.get().getId(), schoolId,
                curriculumService.canEdit(user, schoolId, subjectClass.get()), topics));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : userService.findById(auth.getName());
    }

    public static class Tree {
        public final Integer subjectClassId;
        public final Integer schoolId;
        public final boolean editable;
        public final List<TopicItem> topics;

        public Tree(Integer subjectClassId, Integer schoolId, boolean editable, List<TopicItem> topics) {
            this.subjectClassId = subjectClassId;
            this.schoolId = schoolId;
            this.editable = editable;
            this.topics = topics;
        }
    }

    public static class TopicItem {
        public final int id;
        public final String name;
        public final List<LessonItem> lessons;

        public TopicItem(int id, String name, List<LessonItem> lessons) {
            this.id = id;
            this.name = name;
            this.lessons = lessons;
        }
    }

    public static class LessonItem {
        public final int id;
        public final String name;

        public LessonItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
