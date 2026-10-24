package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolCurriculum;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ChapterRepository;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.ExamRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.repository.SchoolCurriculumRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurriculumServiceTest {
    private final SubjectClassRepository subjectClassRepository = mock(SubjectClassRepository.class);
    private final ChapterRepository chapterRepository = mock(ChapterRepository.class);
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final SchoolCurriculumRepository markerRepository = mock(SchoolCurriculumRepository.class);
    private final ClassSubjectAssignmentRepository assignmentRepository = mock(ClassSubjectAssignmentRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final ExamRepository examRepository = mock(ExamRepository.class);
    private final LessonPlanLessonRepository planLessonRepository = mock(LessonPlanLessonRepository.class);
    private final LessonPlanRepository planRepository = mock(LessonPlanRepository.class);
    private final CurriculumService service = new CurriculumService(subjectClassRepository, chapterRepository,
            lessonRepository, schoolRepository, markerRepository, assignmentRepository, questionRepository,
            examRepository, planLessonRepository, planRepository);

    private final School schoolA = school(1);
    private final School schoolB = school(2);
    private final SubjectClasses math6 = subjectClass(10, 3, "Toán", 6);

    @BeforeEach
    void setUp() {
        when(chapterRepository.save(any())).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            if (chapter.getId() == 0) chapter.setId(900);
            return chapter;
        });
        when(lessonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRepository.findById(1)).thenReturn(Optional.of(schoolA));
    }

    @Test
    void schoolReadsSharedCurriculumUntilItHasItsOwnCopyWithoutCopying() {
        Chapter shared = chapter(5, "Chủ đề 1. Số tự nhiên", null);
        when(markerRepository.existsBySchool_IdAndSubjectClasses_Id(1, 10)).thenReturn(false);
        when(chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(10, null)).thenReturn(List.of(shared));

        assertThat(service.readChapters(1, math6)).containsExactly(shared);
        verify(chapterRepository, never()).save(any());
        verify(markerRepository, never()).save(any());
    }

    @Test
    void firstSchoolUseCopiesSharedChaptersAndLessonsOnceRememberingTheirOrigin() {
        Chapter shared = chapter(5, "Chủ đề 1. Số tự nhiên", null);
        Lesson sharedLesson = lesson(50, "Bài 1. Tập hợp", shared);
        when(markerRepository.existsBySchool_IdAndSubjectClasses_Id(1, 10)).thenReturn(false);
        when(chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(10, null)).thenReturn(List.of(shared));
        when(lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(5)).thenReturn(List.of(sharedLesson));

        service.schoolChapters(1, math6);

        ArgumentCaptor<Chapter> chapterCopy = ArgumentCaptor.forClass(Chapter.class);
        verify(chapterRepository).save(chapterCopy.capture());
        assertThat(chapterCopy.getValue().getSchool()).isSameAs(schoolA);
        assertThat(chapterCopy.getValue().getTemplateChapter()).isSameAs(shared);
        ArgumentCaptor<Lesson> lessonCopy = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(lessonCopy.capture());
        assertThat(lessonCopy.getValue().getTemplateLesson()).isSameAs(sharedLesson);
        assertThat(lessonCopy.getValue().getNameLesson()).isEqualTo("Bài 1. Tập hợp");
        verify(markerRepository).save(any(SchoolCurriculum.class));

        when(markerRepository.existsBySchool_IdAndSubjectClasses_Id(1, 10)).thenReturn(true);
        service.schoolChapters(1, math6);
        verify(chapterRepository, times(1)).save(any());
    }

    @Test
    void permissionsFollowRoleSchoolAndTeachingAssignment() {
        User admin = user("admin", "ROLE_ADMIN", null);
        User principal = user("ht", "ROLE_SCHOOL_ADMIN", schoolA);
        User mathTeacher = user("gv", "ROLE_TEACHER", schoolA);
        User otherTeacher = user("gv2", "ROLE_TEACHER", schoolA);
        when(assignmentRepository.findByTeacherId("gv")).thenReturn(List.of(assignment(3, 6, schoolA)));
        when(assignmentRepository.findByTeacherId("gv2")).thenReturn(List.of(assignment(3, 7, schoolA), assignment(4, 6, schoolA)));

        assertThat(service.canEdit(admin, null, math6)).isTrue();
        assertThat(service.canEdit(admin, 2, math6)).isTrue();
        assertThat(service.canEdit(principal, 1, math6)).isTrue();
        assertThat(service.canEdit(principal, 2, math6)).isFalse();
        assertThat(service.canEdit(principal, null, math6)).isFalse();
        assertThat(service.canEdit(mathTeacher, 1, math6)).isTrue();
        assertThat(service.canEdit(mathTeacher, 2, math6)).isFalse();
        // Maths in grade 7 and another subject in grade 6 do not allow editing maths grade 6.
        assertThat(service.canEdit(otherTeacher, 1, math6)).isFalse();
    }

    @Test
    void unassignedTeacherCannotAddChapters() {
        User teacher = user("gv2", "ROLE_TEACHER", schoolA);
        when(assignmentRepository.findByTeacherId("gv2")).thenReturn(List.of());

        assertThatThrownBy(() -> service.addChapter(teacher, 1, math6, "Chủ đề mới"))
                .isInstanceOf(SecurityException.class);
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void lessonInUseCannotBeDeletedButAnUnusedOneCan() {
        User principal = user("ht", "ROLE_SCHOOL_ADMIN", schoolA);
        Chapter chapter = chapter(7, "Chủ đề 1", schoolA);
        Lesson used = lesson(70, "Bài 1", chapter);
        Lesson unused = lesson(71, "Bài 2", chapter);
        when(lessonRepository.findById(70)).thenReturn(Optional.of(used));
        when(lessonRepository.findById(71)).thenReturn(Optional.of(unused));
        when(questionRepository.countByLesson_Id(70)).thenReturn(3L);
        when(examRepository.countCoveringLesson(70)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteLesson(principal, 70))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 câu hỏi")
                .hasMessageContaining("1 đề thi");
        verify(lessonRepository, never()).delete(used);

        service.deleteLesson(principal, 71);
        verify(lessonRepository).delete(unused);
    }

    private static School school(int id) {
        School school = new School();
        school.setId(id);
        school.setName("Trường " + id);
        return school;
    }

    private static SubjectClasses subjectClass(int id, int subjectId, String subjectName, int grade) {
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName(subjectName);
        Classes classes = new Classes();
        classes.setId(grade);
        classes.setGrade(grade);
        SubjectClasses subjectClass = new SubjectClasses();
        subjectClass.setId(id);
        subjectClass.setSubject(subject);
        subjectClass.setClasses(classes);
        return subjectClass;
    }

    private Chapter chapter(int id, String name, School school) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setChapterName(name);
        chapter.setSubjectClasses(math6);
        chapter.setSchool(school);
        return chapter;
    }

    private static Lesson lesson(int id, String name, Chapter chapter) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setNameLesson(name);
        lesson.setChapter(chapter);
        return lesson;
    }

    private static User user(String id, String roleName, School school) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setId(id);
        user.setRoles(Set.of(role));
        user.setSchool(school);
        return user;
    }

    private static ClassSubjectAssignment assignment(int subjectId, int grade, School school) {
        Subject subject = new Subject();
        subject.setId(subjectId);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setGrade(grade);
        trackingClass.setSchool(school);
        ClassSubjectAssignment assignment = new ClassSubjectAssignment();
        assignment.setSubject(subject);
        assignment.setTrackingClass(trackingClass);
        return assignment;
    }
}
