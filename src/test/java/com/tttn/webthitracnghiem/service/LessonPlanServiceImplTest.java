package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.repository.ClassesRepository;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ChapterRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.service.impl.LessonPlanServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonPlanServiceImplTest {
    @Mock
    private LessonPlanRepository lessonPlanRepository;
    @Mock
    private SubjectClassRepository subjectClassRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private StudentTrackingClassRepository trackingClassRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ISubjectClassService subjectClassService;
    @Mock
    private CurriculumService curriculumService;
    @InjectMocks
    private LessonPlanServiceImpl service;

    @Test
    void savePersistsSelectedScopeFromTheSameSubjectClass() {
        SubjectClasses curriculum = curriculum(10);
        Chapter chapter = chapter(20, curriculum);
        Lesson lesson = lesson(30, chapter);
        User owner = user("teacher");
         owner.setSchoolLevel(2);
        LessonPlanRequest request = request(10, 20, 30);

        when(subjectClassRepository.findById(10)).thenReturn(Optional.of(curriculum));
        when(chapterRepository.findById(20)).thenReturn(Optional.of(chapter));
        when(lessonRepository.findById(any(Integer.class))).thenReturn(Optional.of(lesson));
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonPlan saved = service.save(request, owner);

        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getSubjectClasses()).isSameAs(curriculum);
        assertThat(saved.getChapters()).containsExactly(chapter);
        assertThat(saved.getLessons()).containsExactly(lesson);
        assertThat(saved.getIsPublic()).isTrue();
    }

    @Test
    void saveRejectsChapterFromAnotherSubjectClass() {
        SubjectClasses selected = curriculum(10);
        SubjectClasses other = curriculum(11);
        Chapter foreignChapter = chapter(20, other);
         User owner = user("teacher");
         owner.setSchoolLevel(2);
        when(subjectClassRepository.findById(10)).thenReturn(Optional.of(selected));
        when(chapterRepository.findById(20)).thenReturn(Optional.of(foreignChapter));

         assertThatThrownBy(() -> service.save(request(10, 20, null), owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không thuộc");
        verify(lessonPlanRepository, never()).save(any(LessonPlan.class));
    }

     @Test
     void legacySubjectClassCannotBypassTeacherGradeScope() {
         SubjectClasses curriculum = curriculum(10);
         curriculum.getClasses().setGrade(10);
         User owner = user("teacher");
         owner.setSchoolLevel(2);
         when(subjectClassRepository.findById(10)).thenReturn(Optional.of(curriculum));

         assertThatThrownBy(() -> service.save(request(10, null, null), owner))
                 .isInstanceOf(SecurityException.class)
                 .hasMessageContaining("không được phép");
         verify(lessonPlanRepository, never()).save(any(LessonPlan.class));
     }

    @Test
    void privatePlanIsVisibleOnlyToOwnerUnlessPrivileged() {
        User owner = user("owner");
        LessonPlan privatePlan = new LessonPlan();
        privatePlan.setId(1);
        privatePlan.setOwner(owner);
        privatePlan.setIsPublic(false);

        when(lessonPlanRepository.findByIsPublicTrueOrderByUpdatedAtDesc()).thenReturn(List.of());
        when(lessonPlanRepository.findByOwner_IdOrderByUpdatedAtDesc(anyString()))
                .thenAnswer(invocation -> "owner".equals(invocation.getArgument(0)) ? List.of(privatePlan) : List.of());

        assertThat(service.findVisible("other", false)).isEmpty();
        assertThat(service.findVisible("owner", false)).containsExactly(privatePlan);
        assertThat(service.canView(privatePlan, "other", true)).isTrue();
    }

    @Test
    void studentCanSeePrivatePlanTargetedToTheirClass() {
        StudentTrackingClass studentClass = new StudentTrackingClass();
        studentClass.setId(42);
        LessonPlan privatePlan = new LessonPlan();
        privatePlan.setId(9);
        privatePlan.setIsPublic(false);
        privatePlan.setTargetClass(studentClass);
        privatePlan.setSubject(subject(7, "Toan"));
        privatePlan.setGrade(new Classes(3, "Khoi 10", 10, null));

        when(lessonPlanRepository.findByIsPublicTrueOrderByUpdatedAtDesc()).thenReturn(List.of());
        when(lessonPlanRepository.findByTargetClass_IdOrderByUpdatedAtDesc(42))
                .thenReturn(List.of(privatePlan));

        assertThat(service.findVisibleForStudent("student", 7, 3, 42))
                .containsExactly(privatePlan);
    }

    @Test
    void filtersLessonPlansByScopeAndMine() {
        User owner = user("teacher-1");
        owner.setFullName("Nguyen Van Toan");
        User other = user("teacher-2");
        other.setFullName("Tran Thi Ly");
        Subject math = new Subject();
        math.setId(7);
        math.setName("Toan");
        Classes gradeTen = new Classes(3, "Khoi 10", 10, null);
        StudentTrackingClass class10 = new StudentTrackingClass();
        class10.setId(8);
        class10.setClassName("10A1");
        class10.setGrade(10);

        LessonPlan ownPlan = plan(1, "Dai so 10", owner, math, gradeTen, class10, true);
        LessonPlan otherPlan = plan(2, "Hinh hoc 10", other, math, gradeTen, null, true);
        LessonPlan foreignPlan = plan(3, "Vat ly 10", other, subject(9, "Vat ly"), gradeTen, null, true);
        when(lessonPlanRepository.findAllByOrderByUpdatedAtDesc())
                .thenReturn(List.of(ownPlan, otherPlan, foreignPlan));

        assertThat(service.findVisible("teacher-1", true, "", 7, 3, 8, null, false, "PUBLIC"))
                .containsExactly(ownPlan);
        assertThat(service.findVisible("teacher-1", true, null, null, null, null,
                "Nguyen", true, "ALL")).containsExactly(ownPlan);
    }

    @Test
    void deleteRejectsNonOwner() {
        LessonPlan plan = new LessonPlan();
        plan.setOwner(user("owner"));
        when(lessonPlanRepository.findById(1)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.delete(1, "other"))
                .isInstanceOf(SecurityException.class);
        verify(lessonPlanRepository, never()).delete(any(LessonPlan.class));
    }

    @Test
    void saveStoresSubjectGradeOptionalClassAndCustomContent() {
        Subject subject = new Subject();
        subject.setId(7);
        subject.setName("Toán");
        Classes grade = new Classes(3, "Khối 10", 10, null);
        School school = new School();
        school.setId(5);
        StudentTrackingClass targetClass = new StudentTrackingClass();
        targetClass.setId(8);
        targetClass.setGrade(10);
        targetClass.setSchool(school);
        User owner = user("teacher");
        owner.setSchool(school);
        owner.setSchoolLevel(3);

        LessonPlanChapterRequest chapterRequest = new LessonPlanChapterRequest();
        chapterRequest.setName("Mệnh đề");
        LessonPlanLessonRequest lessonRequest = new LessonPlanLessonRequest();
        lessonRequest.setName("Mệnh đề và tập hợp");
        chapterRequest.setLessons(List.of(lessonRequest));

        LessonPlanRequest request = new LessonPlanRequest();
        request.setName("Giáo án Toán 10");
        request.setSubjectId(7);
        request.setGradeId(3);
        request.setClassId(8);
        request.setChapters(List.of(chapterRequest));

        when(subjectRepository.findById(7)).thenReturn(Optional.of(subject));
        when(classesRepository.findById(3)).thenReturn(Optional.of(grade));
        when(trackingClassRepository.findById(8)).thenReturn(Optional.of(targetClass));
        when(subjectClassRepository.findByClassesAndSubject(3, 7)).thenReturn(Optional.empty());
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonPlan saved = service.save(request, owner);

        assertThat(saved.getSubject()).isSameAs(subject);
        assertThat(saved.getGrade()).isSameAs(grade);
        assertThat(saved.getTargetClass()).isSameAs(targetClass);
        assertThat(saved.getPlanChapters()).singleElement().satisfies(chapter -> {
            assertThat(chapter.getName()).isEqualTo("Mệnh đề");
            assertThat(chapter.getLessons()).singleElement()
                    .extracting(lesson -> lesson.getName())
                    .isEqualTo("Mệnh đề và tập hợp");
        });
    }

    @Test
    void saveStoresLessonContentAndWordAttachment() {
        Subject subject = new Subject();
        subject.setId(7);
        Classes grade = new Classes(3, "Khối 10", 10, null);
        User owner = user("teacher");
        owner.setSchoolLevel(3);

        LessonPlanChapterRequest topicRequest = new LessonPlanChapterRequest();
        topicRequest.setName("Hàm số");
        LessonPlanLessonRequest lessonRequest = new LessonPlanLessonRequest();
        lessonRequest.setName("Hàm số bậc nhất");
        lessonRequest.setContent("Nội dung bài học");
        lessonRequest.setAttachment(new MockMultipartFile(
                "attachment", "ham-so.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "word-data".getBytes(StandardCharsets.UTF_8)));
        topicRequest.setLessons(List.of(lessonRequest));

        LessonPlanRequest request = new LessonPlanRequest();
        request.setName("Toán 10");
        request.setSubjectId(7);
        request.setGradeId(3);
        request.setChapters(List.of(topicRequest));

        when(subjectRepository.findById(7)).thenReturn(Optional.of(subject));
        when(classesRepository.findById(3)).thenReturn(Optional.of(grade));
        when(subjectClassRepository.findByClassesAndSubject(3, 7)).thenReturn(Optional.empty());
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonPlan saved = service.save(request, owner);

        LessonPlanLessonRequest input = request.getChapters().get(0).getLessons().get(0);
        assertThat(saved.getPlanChapters()).singleElement()
                .extracting(topic -> topic.getLessons().get(0))
                .satisfies(savedLesson -> {
                    assertThat(savedLesson.getContent()).isEqualTo(input.getContent());
                    assertThat(savedLesson.getAttachmentOriginalName()).isEqualTo("ham-so.docx");
                    assertThat(savedLesson.getAttachmentData()).isEqualTo("word-data".getBytes(StandardCharsets.UTF_8));
                });
    }

    @Test
    void saveLinksLessonsToTheSchoolCurriculumAndKeepsPlainText() {
        Subject subject = new Subject();
        subject.setId(7);
        Classes grade = new Classes(3, "Khối 10", 10, null);
        SubjectClasses curriculum = new SubjectClasses();
        curriculum.setId(30);
        User owner = user("teacher");
        owner.setSchoolLevel(3);
        Lesson ownLesson = new Lesson();
        ownLesson.setId(100);
        Lesson foreignLesson = new Lesson();
        foreignLesson.setId(200);

        LessonPlanLessonRequest lessonRequest = new LessonPlanLessonRequest();
        lessonRequest.setName("Hàm số bậc nhất");
        lessonRequest.setContent("<p>Hàm số <strong>bậc nhất</strong></p><ul><li>Định nghĩa</li></ul>");
        lessonRequest.setCurriculumLessonId(100);
        lessonRequest.setPeriods(2);
        lessonRequest.setObjectives("Nhận biết hàm số bậc nhất");
        LessonPlanChapterRequest topicRequest = new LessonPlanChapterRequest();
        topicRequest.setName("Hàm số");
        topicRequest.setLessons(List.of(lessonRequest));
        LessonPlanRequest request = new LessonPlanRequest();
        request.setName("Toán 10");
        request.setSubjectId(7);
        request.setGradeId(3);
        request.setChapters(List.of(topicRequest));

        when(subjectRepository.findById(7)).thenReturn(Optional.of(subject));
        when(classesRepository.findById(3)).thenReturn(Optional.of(grade));
        when(subjectClassRepository.findByClassesAndSubject(3, 7)).thenReturn(Optional.of(curriculum));
        when(lessonRepository.findById(100)).thenReturn(Optional.of(ownLesson));
        when(curriculumService.schoolIdOf(owner)).thenReturn(5);
        when(curriculumService.belongsTo(ownLesson, 5, curriculum)).thenReturn(true);
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonPlan saved = service.save(request, owner);

        com.tttn.webthitracnghiem.model.LessonPlanLesson savedLesson = saved.getPlanChapters().get(0).getLessons().get(0);
        assertThat(savedLesson.getCurriculumLesson()).isSameAs(ownLesson);
        assertThat(savedLesson.getPeriods()).isEqualTo(2);
        assertThat(savedLesson.getObjectives()).isEqualTo("Nhận biết hàm số bậc nhất");
        assertThat(savedLesson.getContentText()).isEqualTo("Hàm số bậc nhất\n- Định nghĩa");

        // A lesson from another school's (or another subject's) curriculum is refused.
        lessonRequest.setCurriculumLessonId(200);
        when(lessonRepository.findById(200)).thenReturn(Optional.of(foreignLesson));
        when(curriculumService.belongsTo(foreignLesson, 5, curriculum)).thenReturn(false);
        assertThatThrownBy(() -> service.save(request, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không thuộc chương trình học");
    }

    @Test
    void saveRejectsTargetClassOutsideSelectedGradeOrSchool() {
        Subject subject = new Subject();
        subject.setId(7);
        Classes grade = new Classes(3, "Khối 10", 10, null);
        School teacherSchool = new School();
        teacherSchool.setId(5);
        School otherSchool = new School();
        otherSchool.setId(9);
        StudentTrackingClass foreignClass = new StudentTrackingClass();
        foreignClass.setId(8);
        foreignClass.setGrade(11);
        foreignClass.setSchool(otherSchool);
        User owner = user("teacher");
        owner.setSchool(teacherSchool);
        owner.setSchoolLevel(3);
        LessonPlanRequest request = new LessonPlanRequest();
        request.setName("Giáo án");
        request.setSubjectId(7);
        request.setGradeId(3);
        request.setClassId(8);

        when(subjectRepository.findById(7)).thenReturn(Optional.of(subject));
        when(classesRepository.findById(3)).thenReturn(Optional.of(grade));
        when(trackingClassRepository.findById(8)).thenReturn(Optional.of(foreignClass));

        assertThatThrownBy(() -> service.save(request, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kh");
        verify(lessonPlanRepository, never()).save(any(LessonPlan.class));
    }

    @Test
    void gradesFollowTeachersSchoolLevel() {
        User teacher = user("teacher");
        teacher.setSchoolLevel(3);
        Classes grade9 = new Classes(2, "Khối 9", 9, null);
        Classes grade10 = new Classes(3, "Khối 10", 10, null);
        Classes grade12 = new Classes(5, "Khối 12", 12, null);
        when(classesRepository.findAll()).thenReturn(List.of(grade9, grade12, grade10));

        assertThat(service.findGrades(teacher, false))
                .extracting(Classes::getGrade)
                .containsExactly(10, 12);
    }

    @Test
    void gradesForMultiLevelTeacherIncludesAllGrades() {
        User teacher = user("teacher_multilevel");
        teacher.setSchoolLevel(null);
        School school = new School();
        school.setName("Trường Phổ thông nội trú Tiểu học và THCS Ia Rve");
        teacher.setSchool(school);
        Classes grade3 = new Classes(1, "Khối 3", 3, null);
        Classes grade8 = new Classes(2, "Khối 8", 8, null);
        Classes grade11 = new Classes(3, "Khối 11", 11, null);
        when(classesRepository.findAll()).thenReturn(List.of(grade3, grade8, grade11));

        assertThat(service.findGrades(teacher, false))
                .extracting(Classes::getGrade)
                .containsExactly(3, 8, 11);
    }

    @Test
    void targetClassesAreLimitedToSelectedGradeAndTeacherSchool() {
        User teacher = user("teacher");
        teacher.setSchoolLevel(3);
        School school = new School();
        school.setId(5);
        teacher.setSchool(school);
        Classes grade = new Classes(3, "Khối 10", 10, null);
        StudentTrackingClass class10 = new StudentTrackingClass();
        class10.setId(8);
        class10.setClassName("10A1");
        class10.setGrade(10);
        StudentTrackingClass class11 = new StudentTrackingClass();
        class11.setId(9);
        class11.setClassName("11A1");
        class11.setGrade(11);
        when(classesRepository.findById(3)).thenReturn(Optional.of(grade));
        when(trackingClassRepository.findBySchoolIdAndGradeOrderByClassNameAsc(5, 10))
                .thenReturn(List.of(class10));

        assertThat(service.findTargetClasses(teacher, false, 3)).containsExactly(class10);
    }

    private LessonPlanRequest request(int subjectClassId, Integer chapterId, Integer lessonId) {
        LessonPlanRequest request = new LessonPlanRequest();
        request.setName("Giáo án mẫu");
        request.setSubjectClassId(subjectClassId);
        request.setIsPublic(true);
        if (chapterId != null) request.setChapterIds(new LinkedHashSet<>(List.of(chapterId)));
        if (lessonId != null) request.setLessonIds(new LinkedHashSet<>(List.of(lessonId)));
        return request;
    }

    private SubjectClasses curriculum(int id) {
        SubjectClasses curriculum = new SubjectClasses();
        curriculum.setId(id);
        curriculum.setClasses(new Classes(1, "Khối 6", 6, null));
        curriculum.setSubject(new Subject());
        return curriculum;
    }

    private Chapter chapter(int id, SubjectClasses curriculum) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setSubjectClasses(curriculum);
        return chapter;
    }

    private Lesson lesson(int id, Chapter chapter) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setChapter(chapter);
        return lesson;
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Subject subject(int id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }

    private LessonPlan plan(int id, String name, User owner, Subject subject, Classes grade,
                            StudentTrackingClass targetClass, boolean isPublic) {
        LessonPlan plan = new LessonPlan();
        plan.setId(id);
        plan.setName(name);
        plan.setOwner(owner);
        plan.setSubject(subject);
        plan.setGrade(grade);
        plan.setTargetClass(targetClass);
        plan.setIsPublic(isPublic);
        return plan;
    }
}
