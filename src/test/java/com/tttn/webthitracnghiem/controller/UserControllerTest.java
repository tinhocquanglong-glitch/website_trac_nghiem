package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void filtersSchoolsByPartialAdministrativeAddressWithoutShowingAllByDefault() {
        School firstSchool = school(1, "School A", "Phuong Ben Nghe", "Quan 1", "Thanh pho Ho Chi Minh");
        School secondSchool = school(2, "School B", "Phuong Ben Nghe", "Quan 1", "Thanh pho Ho Chi Minh");
        School otherSchool = school(3, "School C", "Phuong Vo Thi Sau", "Quan 3", "Thanh pho Ho Chi Minh");

        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        IUserService userService = mock(IUserService.class);
        when(schoolRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(firstSchool, secondSchool, otherSchool));
        when(userRepository.findAllSchoolAdmins()).thenReturn(List.of());
        when(userRepository.findTeachersBySchool(anyInt())).thenReturn(List.of());
        when(userRepository.findStudentsBySchool(anyInt())).thenReturn(List.of());
        when(userService.findAll(any())).thenReturn(new PageImpl<>(List.of()));

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "schoolRepository", schoolRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "userService", userService);

        ExtendedModelMap emptyModel = new ExtendedModelMap();
        controller.showList(Optional.empty(), "schools", null, emptyModel, PageRequest.of(0, 10));
        assertThat(emptyModel.get("schools")).isEqualTo(List.of());
        assertThat(emptyModel.get("hasSchoolSearch")).isEqualTo(false);

        ExtendedModelMap filteredModel = new ExtendedModelMap();
        controller.showList(Optional.empty(), "schools", "quan 1", filteredModel, PageRequest.of(0, 10));

        assertThat(filteredModel.get("addressKeyword")).isEqualTo("quan 1");
        assertThat(filteredModel.get("hasSchoolSearch")).isEqualTo(true);
        assertThat(filteredModel.get("schools")).isEqualTo(List.of(firstSchool, secondSchool));
    }

    @Test
    void schoolAdminCanDeleteStudentAssignedToTheirSchoolClass() {
        School school = school(5, "School A", "Ward", "District", "Province");
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setSchool(school);

        User schoolAdmin = new User();
        schoolAdmin.setId("principal");
        schoolAdmin.setSchool(school);

        Role studentRole = new Role();
        studentRole.setRoleName("ROLE_USER");
        User student = new User();
        student.setId("free-student");
        student.setRoles(java.util.Set.of(studentRole));
        student.setTrackingClass(trackingClass);
        student.setSchool(null);

        IUserService userService = mock(IUserService.class);
        when(userService.findById("free-student")).thenReturn(student);
        when(userService.findById("principal")).thenReturn(schoolAdmin);

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "principal", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))));

        controller.delete("free-student", new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap());

        verify(userService).delete(student);
    }

    private School school(Integer id, String name, String ward, String district, String province) {
        School school = new School();
        school.setId(id);
        school.setName(name);
        school.setWardName(ward);
        school.setDistrictName(district);
        school.setProvinceName(province);
        return school;
    }
}
