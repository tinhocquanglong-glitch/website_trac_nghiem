package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.ILocationService;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.SchoolManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolControllerTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private SchoolManagementService schoolManagementService;
    private ILocationService locationService;
    private SchoolController controller;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId("admin");
        admin.setPassWord(encoder.encode("dung-mat-khau"));
        IUserService userService = mock(IUserService.class);
        when(userService.findById("admin")).thenReturn(admin);
        schoolManagementService = mock(SchoolManagementService.class);
        locationService = mock(ILocationService.class);

        controller = new SchoolController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "locationService", locationService);
        ReflectionTestUtils.setField(controller, "schoolManagementService", schoolManagementService);
        ReflectionTestUtils.setField(controller, "passwordEncoder", encoder);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteRequiresTheAdminPassword() {
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller.deleteSchool(4, "sai-mat-khau", null, attributes);

        assertThat(view).isEqualTo("redirect:/school/list");
        assertThat(attributes.getFlashAttributes().get("error")).asString().contains("Mật khẩu admin không đúng");
        verify(schoolManagementService, never()).deleteSchool(anyInt());
        controller.deleteSchool(4, "", null, new RedirectAttributesModelMap());
        verify(schoolManagementService, never()).deleteSchool(anyInt());
    }

    @Test
    void deleteWithCorrectPasswordReturnsToTheUserList() {
        School school = new School();
        school.setId(4);
        school.setName("THCS A");
        when(schoolManagementService.deleteSchool(4)).thenReturn(school);
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller.deleteSchool(4, "dung-mat-khau", "/user/list?viewMode=schools", attributes);

        assertThat(view).isEqualTo("redirect:/user/list?viewMode=schools");
        assertThat(attributes.getFlashAttributes().get("message")).isEqualTo("Đã xóa trường THCS A.");
        verify(schoolManagementService).deleteSchool(4);
    }

    @Test
    void refusedDeleteShowsTheReasonAndNeverRedirectsOffSite() {
        when(schoolManagementService.deleteSchool(4))
                .thenThrow(new IllegalStateException("Không thể xóa trường vì vẫn còn 3 tài khoản"));
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller.deleteSchool(4, "dung-mat-khau", "https://evil.example/user/list", attributes);

        assertThat(view).isEqualTo("redirect:/school/list");
        assertThat(attributes.getFlashAttributes().get("error")).asString().contains("vẫn còn 3 tài khoản");
    }

    @Test
    void toggleLocksAndUnlocksSchool() {
        School school = new School();
        school.setId(4);
        school.setName("THCS A");
        school.setActive(true);
        when(locationService.findSchoolById(4)).thenReturn(school);

        controller.toggleStatus(4, "/user/list?viewMode=schools", new RedirectAttributesModelMap());
        assertThat(school.isActive()).isFalse();
        controller.toggleStatus(4, null, new RedirectAttributesModelMap());
        assertThat(school.isActive()).isTrue();
    }
}
