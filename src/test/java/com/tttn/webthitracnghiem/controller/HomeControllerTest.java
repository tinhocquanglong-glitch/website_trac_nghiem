package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTest {
    @Test
    void includesAStudentAccessibleExamPlanEvenWhenThePlanIsPrivate() {
        User student = new User();
        student.setId("student");
        StudentTrackingClass studentClass = new StudentTrackingClass();
        studentClass.setId(41);
        studentClass.setGrade(6);
        student.setTrackingClass(studentClass);

        Classes grade = new Classes();
        grade.setId(6);
        grade.setGrade(6);
        LessonPlan plan = new LessonPlan();
        plan.setId(9);
        plan.setGrade(grade);
        Subject subject = new Subject();
        subject.setId(3);
        plan.setSubject(subject);
        plan.setIsPublic(false);
        LessonPlanChapter topic = new LessonPlanChapter();
        topic.setLessonPlan(plan);
        LessonPlanLesson lesson = new LessonPlanLesson();
        lesson.setId(99);
        lesson.setChapter(topic);
        topic.setLessons(List.of(lesson));
        plan.setPlanChapters(List.of(topic));

        Exam exam = new Exam();
        exam.setId(20);
        exam.setLessonPlanLesson(lesson);
        exam.setTargetType("CLASS");
        exam.setTargetClass(studentClass);

        ILessonPlanService lessonPlanService = mock(ILessonPlanService.class);
        when(lessonPlanService.findVisibleForStudent("student", 3, 6, 41)).thenReturn(List.of());
        com.tttn.webthitracnghiem.repository.ExamRepository examRepository =
                mock(com.tttn.webthitracnghiem.repository.ExamRepository.class);
        when(examRepository.findAll()).thenReturn(List.of(exam));

        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "lessonPlanService", lessonPlanService);
        ReflectionTestUtils.setField(controller, "examRepository", examRepository);

        List<LessonPlan> plans = controller.findStudentLessonPlans(student, 3, grade);

        assertThat(plans).extracting(LessonPlan::getId).containsExactly(9);
    }
}
