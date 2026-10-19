package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import com.tttn.webthitracnghiem.service.IQuestionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamControllerTest {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void customLessonPlanExamUsesCustomEditViewInsteadOfLegacyLessonGraph() {
        LessonPlanLesson customLesson = new LessonPlanLesson();
        customLesson.setId(7);
        LessonPlanChapter topic = new LessonPlanChapter();
        topic.setName("So nguyen");
        customLesson.setChapter(topic);

        Exam exam = new Exam();
        exam.setId(1);
        exam.setLessonPlanLesson(customLesson);
        exam.setLesson(null);

        IExamService examService = mock(IExamService.class);
        IQuestionService questionService = mock(IQuestionService.class);
        when(examService.findById(1)).thenReturn(exam);
        Question selectedQuestion = new Question();
        when(questionService.findByExam(1)).thenReturn(List.of(selectedQuestion));
        when(questionService.findAllWithContext()).thenReturn(List.of(selectedQuestion));

        ExamController controller = new ExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "questionService", questionService);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.edit(1, model, null);

        assertThat(view).isEqualTo("exam/editCustomExam");
        assertThat(model.get("exam")).isSameAs(exam);
        assertThat(model.get("selectedQuestions")).isEqualTo(List.of(selectedQuestion));
        verify(questionService).findAllWithContext();
    }

    @Test
    void listsExamsLinkedToCustomLessonPlanLesson() {
        LessonPlanLesson customLesson = new LessonPlanLesson();
        customLesson.setId(7);
        LessonPlanChapter topic = new LessonPlanChapter();
        topic.setName("Số nguyên");
        customLesson.setChapter(topic);

        Exam exam = new Exam();
        exam.setId(12);
        exam.setLessonPlanLesson(customLesson);

        PageRequest pageable = PageRequest.of(0, 10);
        IExamService examService = mock(IExamService.class);
        IQuestionService questionService = mock(IQuestionService.class);
        ILessonPlanService lessonPlanService = mock(ILessonPlanService.class);
        when(lessonPlanService.findLesson(7)).thenReturn(customLesson);
        when(examService.findAllByLessonPlanLesson(7, pageable))
                .thenReturn(new PageImpl<>(List.of(exam), pageable, 1));
        when(questionService.findAll()).thenReturn(List.of());

        ExamController controller = new ExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "questionService", questionService);
        ReflectionTestUtils.setField(controller, "lessonPlanService", lessonPlanService);
        ReflectionTestUtils.setField(controller, "lessonService", mock(com.tttn.webthitracnghiem.service.ILessonService.class));
        ReflectionTestUtils.setField(controller, "documentService", mock(com.tttn.webthitracnghiem.service.IDocumentService.class));
        ReflectionTestUtils.setField(controller, "newsService", mock(com.tttn.webthitracnghiem.service.INewsService.class));
        ReflectionTestUtils.setField(controller, "rService", mock(com.tttn.webthitracnghiem.service.IResultService.class));
        ReflectionTestUtils.setField(controller, "userService", mock(com.tttn.webthitracnghiem.service.IUserService.class));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymousUser", null));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.listLessonPlanExam(Optional.empty(), 7, model, pageable);

        assertThat(view).isEqualTo("listExamSubject");
        Page<Exam> resultPage = (Page<Exam>) model.get("exams");
        assertThat(resultPage.getContent())
                .containsExactly(exam);
        assertThat(model.get("lessonPlanLesson")).isSameAs(customLesson);
        assertThat(model.get("examListPath")).isEqualTo("/listExamSubject/lesson-plan/7");
    }

    @Test
    void legacyLessonRouteDoesNotMergeCustomLessonWithTheSameNumericId() {
        Subject subject = new Subject();
        subject.setId(2);
        Classes grade = new Classes();
        grade.setId(6);
        grade.setGrade(6);
        SubjectClasses curriculum = new SubjectClasses();
        curriculum.setSubject(subject);
        curriculum.setClasses(grade);
        Chapter legacyChapter = new Chapter();
        legacyChapter.setSubjectClasses(curriculum);
        Lesson legacyLesson = new Lesson();
        legacyLesson.setId(3);
        legacyLesson.setChapter(legacyChapter);

        LessonPlan plan = new LessonPlan();
        plan.setSubject(subject);
        plan.setGrade(grade);
        LessonPlanChapter customChapter = new LessonPlanChapter();
        customChapter.setLessonPlan(plan);
        LessonPlanLesson customLesson = new LessonPlanLesson();
        customLesson.setId(3);
        customLesson.setChapter(customChapter);

        Exam legacyExam = new Exam();
        legacyExam.setId(11);
        Exam customExam = new Exam();
        customExam.setId(12);
        customExam.setLessonPlanLesson(customLesson);

        PageRequest pageable = PageRequest.of(0, 10);
        IExamService examService = mock(IExamService.class);
        IQuestionService questionService = mock(IQuestionService.class);
        ILessonPlanService lessonPlanService = mock(ILessonPlanService.class);
        com.tttn.webthitracnghiem.service.ILessonService lessonService = mock(com.tttn.webthitracnghiem.service.ILessonService.class);
        when(lessonService.findById(3)).thenReturn(legacyLesson);
        when(lessonPlanService.findLesson(3)).thenReturn(customLesson);
        when(examService.findAllByLesson(3, pageable))
                .thenReturn(new PageImpl<>(List.of(legacyExam), pageable, 1));
        when(examService.findAllByLessonPlanLesson(3, pageable))
                .thenReturn(new PageImpl<>(List.of(customExam), pageable, 1));

        ExamController controller = new ExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "questionService", questionService);
        ReflectionTestUtils.setField(controller, "lessonPlanService", lessonPlanService);
        ReflectionTestUtils.setField(controller, "lessonService", lessonService);
        ReflectionTestUtils.setField(controller, "documentService", mock(com.tttn.webthitracnghiem.service.IDocumentService.class));
        ReflectionTestUtils.setField(controller, "newsService", mock(com.tttn.webthitracnghiem.service.INewsService.class));
        ReflectionTestUtils.setField(controller, "rService", mock(com.tttn.webthitracnghiem.service.IResultService.class));
        ReflectionTestUtils.setField(controller, "userService", mock(com.tttn.webthitracnghiem.service.IUserService.class));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymousUser", null));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.listExamSubject(Optional.empty(), 3, model, pageable);

        assertThat(view).isEqualTo("listExamSubject");
        Page<Exam> resultPage = (Page<Exam>) model.get("exams");
        assertThat(resultPage.getContent()).containsExactly(legacyExam);
        verify(examService).findAllByLesson(3, pageable);
    }

    @Test
    void legacyLessonRouteExposesSameIdLessonPlanAsASeparateNavigationOption() {
        Lesson legacyLesson = new Lesson();
        legacyLesson.setId(2);

        LessonPlanLesson customLesson = new LessonPlanLesson();
        customLesson.setId(2);
        customLesson.setName("Bài 2. Cách ghi số tự nhiên");

        PageRequest pageable = PageRequest.of(0, 10);
        IExamService examService = mock(IExamService.class);
        IQuestionService questionService = mock(IQuestionService.class);
        ILessonPlanService lessonPlanService = mock(ILessonPlanService.class);
        com.tttn.webthitracnghiem.service.ILessonService lessonService = mock(com.tttn.webthitracnghiem.service.ILessonService.class);
        when(lessonService.findById(2)).thenReturn(legacyLesson);
        when(lessonPlanService.findLesson(2)).thenReturn(customLesson);
        when(examService.findAllByLesson(2, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        ExamController controller = new ExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "questionService", questionService);
        ReflectionTestUtils.setField(controller, "lessonPlanService", lessonPlanService);
        ReflectionTestUtils.setField(controller, "lessonService", lessonService);
        ReflectionTestUtils.setField(controller, "documentService", mock(com.tttn.webthitracnghiem.service.IDocumentService.class));
        ReflectionTestUtils.setField(controller, "newsService", mock(com.tttn.webthitracnghiem.service.INewsService.class));
        ReflectionTestUtils.setField(controller, "rService", mock(com.tttn.webthitracnghiem.service.IResultService.class));
        ReflectionTestUtils.setField(controller, "userService", mock(com.tttn.webthitracnghiem.service.IUserService.class));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymousUser", null));

        ExtendedModelMap model = new ExtendedModelMap();
        controller.listExamSubject(Optional.empty(), 2, model, pageable);

        assertThat(model.get("lessonPlanCandidate")).isSameAs(customLesson);
        assertThat(model.get("lessonPlanCandidatePath")).isEqualTo("/listExamSubject/lesson-plan/2");
    }
}
