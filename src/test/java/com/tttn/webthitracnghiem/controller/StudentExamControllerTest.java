package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IResultService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentExamControllerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsOnlyNonPracticeExamsAssignedToTheStudentsClass() {
        StudentTrackingClass studentClass = new StudentTrackingClass();
        studentClass.setId(61);
        studentClass.setClassName("6A1");
        User student = new User();
        student.setId("student");
        student.setTrackingClass(studentClass);

        Exam assigned = new Exam();
        assigned.setId(2);
        assigned.setTargetType("CLASS");
        assigned.setTargetClass(studentClass);
        Exam anotherClass = new Exam();
        anotherClass.setId(3);
        anotherClass.setTargetType("CLASS");
        StudentTrackingClass otherClass = new StudentTrackingClass();
        otherClass.setId(62);
        anotherClass.setTargetClass(otherClass);
        Exam practice = new Exam();
        practice.setId(4);
        practice.setPractice(true);
        practice.setTargetType("CLASS");
        practice.setTargetClass(studentClass);

        IExamService examService = mock(IExamService.class);
        IResultService resultService = mock(IResultService.class);
        IUserService userService = mock(IUserService.class);
        when(userService.findById("student")).thenReturn(student);
        when(examService.getAll()).thenReturn(List.of(assigned, anotherClass, practice));
        when(resultService.findByHistory("student")).thenReturn(List.of());

        StudentExamController controller = new StudentExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "resultService", resultService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.list(model);

        assertThat(view).isEqualTo("student/exams");
        assertThat(model.get("assignedExams")).isEqualTo(List.of(assigned));
        assertThat(model.get("completedExamIds")).isEqualTo(java.util.Set.of());
    }

    @Test
    void exposesCompletedExamIdsForTheStudent() {
        User student = new User();
        student.setId("student");
        Exam assigned = new Exam();
        assigned.setId(12);
        assigned.setTargetType("PUBLIC");
        Result result = new Result();
        result.setExam(assigned);

        IExamService examService = mock(IExamService.class);
        IResultService resultService = mock(IResultService.class);
        IUserService userService = mock(IUserService.class);
        when(userService.findById("student")).thenReturn(student);
        when(examService.getAll()).thenReturn(List.of(assigned));
        when(resultService.findByHistory("student")).thenReturn(List.of(result));

        StudentExamController controller = new StudentExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "resultService", resultService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        ExtendedModelMap model = new ExtendedModelMap();
        controller.list(model);

        assertThat(model.get("completedExamIds")).isEqualTo(java.util.Set.of(12));
        assertThat(((java.util.Map<Integer, ?>) model.get("completedResults"))).containsKey(12);
    }

    @Test
    void studentExamTemplateLinksToTheProtectedQuizRoute() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/student/exams.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("/quiz1/{id}")
                .contains("assignedExams")
                .contains("/css/navbar.css")
                .contains("completedExamIds")
                .contains("exam-card--completed")
                .contains("Bài thi được giao");
    }
}
