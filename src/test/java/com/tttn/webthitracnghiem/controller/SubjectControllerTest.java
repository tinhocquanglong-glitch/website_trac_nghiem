package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IChapterService;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ILessonService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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

    @Test
    void removeWithWrongPasswordFails() {
        SubjectController controller = controller();
        IUserService userService = (IUserService) org.springframework.test.util.ReflectionTestUtils.getField(controller, "userService");
        User user = new User();
        user.setId("admin");
        user.setPassWord(new BCryptPasswordEncoder().encode("correctPassword"));
        when(userService.findById("admin")).thenReturn(user);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String result = controller.remove(1, "wrongPassword", ra);
        assertThat(result).isEqualTo("redirect:/subject/list");
        assertThat(ra.getFlashAttributes()).containsKey("error");
    }

    @Test
    void removeWithCorrectPasswordSucceeds() {
        SubjectController controller = controller();
        IUserService userService = (IUserService) org.springframework.test.util.ReflectionTestUtils.getField(controller, "userService");
        ISubjectService subjectService = (ISubjectService) org.springframework.test.util.ReflectionTestUtils.getField(controller, "subjectService");

        User user = new User();
        user.setId("admin");
        user.setPassWord(new BCryptPasswordEncoder().encode("mySecretPass"));
        when(userService.findById("admin")).thenReturn(user);

        Subject subject = new Subject();
        subject.setId(10);
        subject.setName("Môn Thử Nghiệm");
        when(subjectService.findById(10)).thenReturn(subject);
        when(subjectService.canManage(eq(subject), eq(user), eq(true))).thenReturn(true);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String result = controller.remove(10, "mySecretPass", ra);
        assertThat(result).isEqualTo("redirect:/subject/list");
        assertThat(ra.getFlashAttributes()).containsKey("message");
        verify(subjectService).remove(subject);
    }

    @Test
    void schoolAdminCannotDeleteGlobalSubjectOrOtherSchoolSubject() {
        SubjectController controller = controller();
        IUserService userService = (IUserService) org.springframework.test.util.ReflectionTestUtils.getField(controller, "userService");
        ISubjectService subjectService = (ISubjectService) org.springframework.test.util.ReflectionTestUtils.getField(controller, "subjectService");

        User principalUser = new User();
        principalUser.setId("principal");
        principalUser.setPassWord(new BCryptPasswordEncoder().encode("pass123"));
        when(userService.findById("principal")).thenReturn(principalUser);

        // Global subject (Admin created)
        Subject globalSubject = new Subject();
        globalSubject.setId(1);
        globalSubject.setName("Toán Chung");
        globalSubject.setSchool(null);
        when(subjectService.findById(1)).thenReturn(globalSubject);
        when(subjectService.canManage(eq(globalSubject), eq(principalUser), eq(false))).thenReturn(false);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "principal", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))));

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String result = controller.remove(1, "pass123", ra);
        assertThat(result).isEqualTo("redirect:/subject/list");
        assertThat(ra.getFlashAttributes().get("error")).isEqualTo("Bạn không có quyền xóa môn học này!");
        verify(subjectService, never()).remove(globalSubject);
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
