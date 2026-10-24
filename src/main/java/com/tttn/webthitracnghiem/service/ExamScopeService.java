package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Curriculum scope of exams and the question pool they draw from.
 *
 * <p>An exam covers one or more lessons of one subject-grade in the author's school
 * curriculum. Its question pool is the approved questions of those lessons, plus public
 * approved questions other schools wrote for the same shared lesson.
 */
@Service
public class ExamScopeService {
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final CurriculumService curriculumService;

    public ExamScopeService(LessonRepository lessonRepository, QuestionRepository questionRepository,
                            CurriculumService curriculumService) {
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.curriculumService = curriculumService;
    }

    /**
     * Resolves and checks scope lessons in the given order: all must exist, share one
     * subject-grade and, except for the system admin, belong to the user's school curriculum.
     */
    @Transactional(readOnly = true)
    public List<Lesson> resolveScope(Collection<Integer> lessonIds, User user) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một chủ đề hoặc bài học cho đề thi.");
        }
        Map<Integer, Lesson> byId = new LinkedHashMap<>();
        for (Lesson lesson : lessonRepository.findAllById(new LinkedHashSet<>(lessonIds))) {
            byId.put(lesson.getId(), lesson);
        }
        List<Lesson> lessons = new ArrayList<>();
        for (Integer id : new LinkedHashSet<>(lessonIds)) {
            Lesson lesson = byId.get(id);
            if (lesson == null || lesson.getChapter() == null || lesson.getChapter().getSubjectClasses() == null) {
                throw new IllegalArgumentException("Bài học đã chọn không tồn tại trong chương trình học.");
            }
            lessons.add(lesson);
        }
        SubjectClasses subjectClass = lessons.get(0).getChapter().getSubjectClasses();
        boolean admin = hasRole(user, "ROLE_ADMIN");
        Integer schoolId = curriculumService.schoolIdOf(user);
        for (Lesson lesson : lessons) {
            if (lesson.getChapter().getSubjectClasses().getId() != subjectClass.getId()) {
                throw new IllegalArgumentException("Các bài trong một đề thi phải cùng môn và cùng khối.");
            }
            if (!admin && !curriculumService.belongsTo(lesson, schoolId, subjectClass)) {
                throw new SecurityException("Bài \"" + lesson.getNameLesson()
                        + "\" không thuộc chương trình học của trường bạn.");
            }
        }
        return lessons;
    }

    /** Sets subject, grade and scope; the first lesson stays the exam's primary lesson for older pages. */
    public void applyScope(Exam exam, List<Lesson> lessons) {
        SubjectClasses subjectClass = lessons.get(0).getChapter().getSubjectClasses();
        exam.setSubject(subjectClass.getSubject());
        exam.setGradeLevel(subjectClass.getClasses() == null ? null : subjectClass.getClasses().getGrade());
        exam.setScopeLessons(new LinkedHashSet<>(lessons));
        exam.setLesson(lessons.get(0));
        exam.setLessonPlanLesson(null);
    }

    /** Approved questions usable for an exam over these lessons, visible to the user. */
    @Transactional(readOnly = true)
    public List<Question> questionPool(Collection<Integer> lessonIds, User user) {
        List<Lesson> lessons = resolveScope(lessonIds, user);
        SubjectClasses subjectClass = lessons.get(0).getChapter().getSubjectClasses();
        boolean admin = hasRole(user, "ROLE_ADMIN");
        Integer schoolId = curriculumService.schoolIdOf(user);
        Set<Integer> ownLessonIds = new LinkedHashSet<>();
        Set<Integer> sharedLessonIds = new LinkedHashSet<>();
        for (Lesson lesson : lessons) {
            ownLessonIds.add(lesson.getId());
            int templateId = lesson.getTemplateLesson() != null ? lesson.getTemplateLesson().getId() : lesson.getId();
            for (Lesson sibling : lessonRepository.findSiblingsOfTemplate(templateId)) {
                if (sibling.getId() != lesson.getId()) {
                    sharedLessonIds.add(sibling.getId());
                }
            }
        }
        Map<Integer, Question> pool = new LinkedHashMap<>();
        for (Question question : questionRepository.findByLesson_IdIn(ownLessonIds)) {
            if (question.isApproved() && (admin || visibleTo(question, schoolId))) {
                pool.put(question.getQuesId(), question);
            }
        }

        Integer subjectId = subjectClass.getSubject() == null ? null : subjectClass.getSubject().getId();
        Integer classId = subjectClass.getClasses() == null ? null : subjectClass.getClasses().getId();
        if (subjectId != null && classId != null) {
            for (Question question : questionRepository.findBySubjectAndGradeWithoutLesson(subjectId, classId)) {
                if (question.isApproved() && (admin || visibleTo(question, schoolId))) {
                    pool.putIfAbsent(question.getQuesId(), question);
                }
            }
        }

        sharedLessonIds.removeAll(ownLessonIds);
        if (!sharedLessonIds.isEmpty()) {
            for (Question question : questionRepository.findByLesson_IdIn(sharedLessonIds)) {
                if (question.isApproved() && Boolean.TRUE.equals(question.getIsPublic())) {
                    pool.putIfAbsent(question.getQuesId(), question);
                }
            }
        }
        return new ArrayList<>(pool.values());
    }

    private static boolean visibleTo(Question question, Integer schoolId) {
        return question.getIsPublic() == null || Boolean.TRUE.equals(question.getIsPublic())
                || (question.getSchool() != null && question.getSchool().getId().equals(schoolId));
    }

    private static boolean hasRole(User user, String roleName) {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getRoleName()));
    }
}
