package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ILessonService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SubjectControllerTest {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void schoolAdminCanOpenCreateSubjectForm() {
        SubjectController controller = controller();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "principal", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))));

        assertThat(controller.create(new ExtendedModelMap())).isEqualTo("subject/add");
    }

    @Test
    void teacherCannotOpenCreateSubjectForm() {
        SubjectController controller = controller();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "teacher", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TEACHER"))));

        assertThat(controller.create(new ExtendedModelMap())).isEqualTo("redirect:/subject/list");
    }

    private SubjectController controller() {
        SubjectController controller = new SubjectController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "subjectService", mock(ISubjectService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "userService", mock(IUserService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "subjectClassService", mock(ISubjectClassService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "classesService", mock(IClassesService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "chapterService", mock(IChapterService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "lessonService", mock(ILessonService.class));
        return controller;
    }
}
