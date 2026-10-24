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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        when(schoolRepository.findAllByOrderByNameAsc()).thenReturn(List.of(firstSchool, secondSchool, otherSchool));
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

    @Test
    void principalEditsTeacherOfOwnSchoolDespiteMissingPasswordFields() {
        School school = school(5, "School A", "Ward", "District", "Province");
        User principal = new User();
        principal.setId("principal");
        principal.setSchool(school);
        User teacher = userWithRole("gv01", "ROLE_TEACHER", school);
        teacher.setPassWord("$2a$10$hash");

        IUserService userService = mock(IUserService.class);
        when(userService.findById("principal")).thenReturn(principal);
        when(userService.findById("gv01")).thenReturn(teacher);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("principal", "ROLE_SCHOOL_ADMIN");

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.editMember("gv01", model, new RedirectAttributesModelMap());
        assertThat(view).isEqualTo("user/editMember");
        assertThat(model.get("backUrl")).isEqualTo("/school-admin/users");

        User submitted = new User();
        submitted.setId("gv01");
        submitted.setFullName(" Nguyen Van Moi ");
        submitted.setEmail("moi@example.com");
        submitted.setAddress("Ha Noi");
        submitted.setPhoneNumber("0912345678");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(submitted, "user");
        // The edit form has no password inputs, so the entity's password constraints always fail.
        bindingResult.rejectValue("passWord", "NotBlank");
        bindingResult.rejectValue("rePassWord", "NotBlank");

        String result = controller.update(submitted, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(result).isEqualTo("redirect:/school-admin/users");
        assertThat(teacher.getFullName()).isEqualTo("Nguyen Van Moi");
        assertThat(teacher.getPassWord()).isEqualTo("$2a$10$hash");
        verify(userService).save(teacher);
    }

    @Test
    void principalCannotEditTeacherOfAnotherSchoolOrAnotherPrincipal() {
        School ownSchool = school(5, "School A", "Ward", "District", "Province");
        School otherSchool = school(6, "School B", "Ward", "District", "Province");
        User principal = new User();
        principal.setId("principal");
        principal.setSchool(ownSchool);
        User otherTeacher = userWithRole("gv99", "ROLE_TEACHER", otherSchool);
        User otherPrincipal = userWithRole("principal2", "ROLE_SCHOOL_ADMIN", ownSchool);

        IUserService userService = mock(IUserService.class);
        when(userService.findById("principal")).thenReturn(principal);
        when(userService.findById("gv99")).thenReturn(otherTeacher);
        when(userService.findById("principal2")).thenReturn(otherPrincipal);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("principal", "ROLE_SCHOOL_ADMIN");

        assertThat(controller.editMember("gv99", new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/school-admin/users");
        assertThat(controller.editMember("principal2", new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/school-admin/users");

        User submitted = userWithRole("gv99", "ROLE_TEACHER", null);
        submitted.setFullName("Bi sua");
        controller.update(submitted, new BeanPropertyBindingResult(submitted, "user"),
                new ExtendedModelMap(), new RedirectAttributesModelMap());
        verify(userService, never()).save(any(User.class));
    }

    private static void authenticate(String username, String role) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        username, "n/a", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))));
    }

    private static User userWithRole(String id, String roleName, School school) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setId(id);
        user.setRoles(java.util.Set.of(role));
        user.setSchool(school);
        return user;
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
