package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExamScopeServiceTest {
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final CurriculumService curriculumService = mock(CurriculumService.class);
    private final ExamScopeService service = new ExamScopeService(lessonRepository, questionRepository, curriculumService);

    private final School schoolA = school(1);
    private final School schoolB = school(2);
    private final SubjectClasses math6 = subjectClass(10, 6);
    private final SubjectClasses math7 = subjectClass(11, 7);
    private final User teacher = teacher(schoolA);

    @Test
    void scopeSetsSubjectGradeAndAllLessonsWithTheFirstAsPrimary() {
        Lesson first = lesson(100, math6, schoolA, null);
        Lesson second = lesson(101, math6, schoolA, null);
        stubOwnSchool(first, second);

        List<Lesson> scope = service.resolveScope(List.of(101, 100), teacher);
        Exam exam = new Exam();
        service.applyScope(exam, scope);

        assertThat(exam.getLesson()).isSameAs(second);
        assertThat(exam.getScopeLessons()).containsExactly(second, first);
        assertThat(exam.getGradeLevel()).isEqualTo(6);
        assertThat(exam.getSubject()).isSameAs(math6.getSubject());
    }

    @Test
    void scopeRejectsMixedSubjectGradesAndOtherSchoolsLessons() {
        Lesson grade6 = lesson(100, math6, schoolA, null);
        Lesson grade7 = lesson(200, math7, schoolA, null);
        Lesson otherSchool = lesson(300, math6, schoolB, null);
        when(lessonRepository.findAllById(any())).thenReturn(List.of(grade6, grade7));
        when(curriculumService.schoolIdOf(teacher)).thenReturn(1);
        when(curriculumService.belongsTo(grade6, 1, math6)).thenReturn(true);

        assertThatThrownBy(() -> service.resolveScope(List.of(100, 200), teacher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cùng môn và cùng khối");

        when(lessonRepository.findAllById(any())).thenReturn(List.of(otherSchool));
        when(curriculumService.belongsTo(otherSchool, 1, math6)).thenReturn(false);
        assertThatThrownBy(() -> service.resolveScope(List.of(300), teacher))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void poolHasApprovedQuestionsOfScopeAndPublicOnesOfOtherSchoolsForTheSameSharedLesson() {
        Lesson shared = lesson(50, math6, null, null);
        Lesson own = lesson(100, math6, schoolA, shared);
        Lesson sibling = lesson(200, math6, schoolB, shared);
        stubOwnSchool(own);
        when(lessonRepository.findSiblingsOfTemplate(50)).thenReturn(List.of(shared, own, sibling));

        Question approved = question(1, own, schoolA, false, null);
        Question pending = question(2, own, schoolA, false, Question.REVIEW_PENDING);
        Question siblingPublic = question(3, sibling, schoolB, true, Question.REVIEW_APPROVED);
        Question siblingPrivate = question(4, sibling, schoolB, false, Question.REVIEW_APPROVED);
        when(questionRepository.findByLesson_IdIn(Set.of(100))).thenReturn(List.of(approved, pending));
        when(questionRepository.findByLesson_IdIn(Set.of(50, 200))).thenReturn(List.of(siblingPublic, siblingPrivate));

        assertThat(service.questionPool(List.of(100), teacher)).containsExactly(approved, siblingPublic);
    }

    @Test
    void poolIncludesApprovedSubjectOnlyQuestionsForTheSelectedGrade() {
        Lesson own = lesson(100, math6, schoolA, null);
        stubOwnSchool(own);

        Question subjectOnly = new Question();
        subjectOnly.setQuesId(5);
        subjectOnly.setSubject(math6.getSubject());
        subjectOnly.setClasses(null);
        subjectOnly.setSchool(schoolA);
        subjectOnly.setIsPublic(false);
        subjectOnly.setReviewStatus(Question.REVIEW_APPROVED);
        when(questionRepository.findBySubjectAndGradeWithoutLesson(3, 6))
                .thenReturn(List.of(subjectOnly));

        assertThat(service.questionPool(List.of(100), teacher)).containsExactly(subjectOnly);
    }

    private void stubOwnSchool(Lesson... lessons) {
        when(lessonRepository.findAllById(any())).thenReturn(List.of(lessons));
        when(curriculumService.schoolIdOf(teacher)).thenReturn(1);
        for (Lesson lesson : lessons) {
            when(curriculumService.belongsTo(lesson, 1, lesson.getChapter().getSubjectClasses())).thenReturn(true);
        }
    }

    private static School school(int id) {
        School school = new School();
        school.setId(id);
        return school;
    }

    private static SubjectClasses subjectClass(int id, int grade) {
        Subject subject = new Subject();
        subject.setId(3);
        subject.setName("Toán");
        Classes classes = new Classes();
        classes.setId(grade);
        classes.setGrade(grade);
        SubjectClasses subjectClass = new SubjectClasses();
        subjectClass.setId(id);
        subjectClass.setSubject(subject);
        subjectClass.setClasses(classes);
        return subjectClass;
    }

    private static Lesson lesson(int id, SubjectClasses subjectClass, School school, Lesson template) {
        Chapter chapter = new Chapter();
        chapter.setId(id * 10);
        chapter.setSubjectClasses(subjectClass);
        chapter.setSchool(school);
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setNameLesson("Bài " + id);
        lesson.setChapter(chapter);
        lesson.setTemplateLesson(template);
        return lesson;
    }

    private static Question question(int id, Lesson lesson, School school, boolean isPublic, String reviewStatus) {
        Question question = new Question();
        question.setQuesId(id);
        question.setLesson(lesson);
        question.setSchool(school);
        question.setIsPublic(isPublic);
        question.setReviewStatus(reviewStatus);
        return question;
    }

    private static User teacher(School school) {
        Role role = new Role();
        role.setRoleName("ROLE_TEACHER");
        User user = new User();
        user.setId("gv");
        user.setRoles(Set.of(role));
        user.setSchool(school);
        return user;
    }
}
