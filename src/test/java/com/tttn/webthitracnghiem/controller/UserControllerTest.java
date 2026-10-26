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

    @Test
    void adminCanResetPasswordForSchoolAdminTeacherAndStudent() {
        School school = school(1, "School A", "Ward", "District", "Province");
        User admin = userWithRole("admin", "ROLE_ADMIN", null);
        User principal = userWithRole("principal", "ROLE_SCHOOL_ADMIN", school);
        principal.setFullName("Hieu Truong");
        User teacher = userWithRole("gv01", "ROLE_TEACHER", school);
        teacher.setFullName("Giao Vien");
        User student = userWithRole("hs01", "ROLE_USER", school);
        student.setFullName("Hoc Sinh");

        IUserService userService = mock(IUserService.class);
        when(userService.findById("admin")).thenReturn(admin);
        when(userService.findById("principal")).thenReturn(principal);
        when(userService.findById("gv01")).thenReturn(teacher);
        when(userService.findById("hs01")).thenReturn(student);

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("admin", "ROLE_ADMIN");

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String res1 = controller.resetPassword("principal", "123456", "123456", "/user/list", ra);
        assertThat(res1).isEqualTo("redirect:/user/list");
        assertThat(ra.getFlashAttributes().get("message").toString()).contains("Đã cấp lại mật khẩu thành công cho Hiệu trưởng");
        verify(userService).save(principal);

        ra = new RedirectAttributesModelMap();
        String res2 = controller.resetPassword("gv01", "abcdef", "abcdef", "/school-admin/users", ra);
        assertThat(res2).isEqualTo("redirect:/school-admin/users");
        assertThat(ra.getFlashAttributes().get("message").toString()).contains("Đã cấp lại mật khẩu thành công cho Giáo viên");
        verify(userService).save(teacher);

        ra = new RedirectAttributesModelMap();
        String res3 = controller.resetPassword("hs01", "pass123", "pass123", "/teacher/homeroom", ra);
        assertThat(res3).isEqualTo("redirect:/teacher/homeroom");
        assertThat(ra.getFlashAttributes().get("message").toString()).contains("Đã cấp lại mật khẩu thành công cho Học sinh");
        verify(userService).save(student);
    }

    @Test
    void adminCannotResetAnotherAdmin() {
        User admin1 = userWithRole("admin1", "ROLE_ADMIN", null);
        User admin2 = userWithRole("admin2", "ROLE_ADMIN", null);

        IUserService userService = mock(IUserService.class);
        when(userService.findById("admin1")).thenReturn(admin1);
        when(userService.findById("admin2")).thenReturn(admin2);

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("admin1", "ROLE_ADMIN");

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String res = controller.resetPassword("admin2", "123456", "123456", "/user/list", ra);
        assertThat(res).isEqualTo("redirect:/user/list");
        assertThat(ra.getFlashAttributes().get("error").toString()).contains("Không thể cấp lại mật khẩu cho tài khoản Quản trị viên");
        verify(userService, never()).save(admin2);
    }

    @Test
    void schoolAdminCanResetTeacherAndStudentOfSameSchoolOnly() {
        School school1 = school(1, "School A", "Ward", "District", "Province");
        School school2 = school(2, "School B", "Ward", "District", "Province");

        User principal = userWithRole("principal", "ROLE_SCHOOL_ADMIN", school1);
        User sameSchoolTeacher = userWithRole("gv01", "ROLE_TEACHER", school1);
        sameSchoolTeacher.setFullName("GV Cung Truong");
        User otherSchoolTeacher = userWithRole("gv02", "ROLE_TEACHER", school2);
        otherSchoolTeacher.setFullName("GV Khac Truong");
        User sameSchoolStudent = userWithRole("hs01", "ROLE_USER", school1);
        sameSchoolStudent.setFullName("HS Cung Truong");

        IUserService userService = mock(IUserService.class);
        when(userService.findById("principal")).thenReturn(principal);
        when(userService.findById("gv01")).thenReturn(sameSchoolTeacher);
        when(userService.findById("gv02")).thenReturn(otherSchoolTeacher);
        when(userService.findById("hs01")).thenReturn(sameSchoolStudent);

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("principal", "ROLE_SCHOOL_ADMIN");

        // Same school teacher: OK
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String res1 = controller.resetPassword("gv01", "newpass123", "newpass123", null, ra);
        assertThat(res1).isEqualTo("redirect:/school-admin/users");
        assertThat(ra.getFlashAttributes().get("message").toString()).contains("Đã cấp lại mật khẩu thành công");
        verify(userService).save(sameSchoolTeacher);

        // Same school student: OK
        ra = new RedirectAttributesModelMap();
        String res2 = controller.resetPassword("hs01", "newpass123", "newpass123", null, ra);
        assertThat(res2).isEqualTo("redirect:/school-admin/users");
        assertThat(ra.getFlashAttributes().get("message").toString()).contains("Đã cấp lại mật khẩu thành công");
        verify(userService).save(sameSchoolStudent);

        // Other school teacher: REJECTED
        ra = new RedirectAttributesModelMap();
        String res3 = controller.resetPassword("gv02", "newpass123", "newpass123", null, ra);
        assertThat(res3).isEqualTo("redirect:/school-admin/users");
        assertThat(ra.getFlashAttributes().get("error").toString()).contains("thuộc trường mình");
        verify(userService, never()).save(otherSchoolTeacher);
    }

    @Test
    void teacherCanResetPasswordForStudentInHomeroomClass() {
        School school = school(1, "School A", "Ward", "District", "Province");
        User teacher = userWithRole("gvcn", "ROLE_TEACHER", school);

        StudentTrackingClass myClass = new StudentTrackingClass();
        myClass.setId(10);
        myClass.setHomeroomTeacher(teacher);

        StudentTrackingClass otherClass = new StudentTrackingClass();
        otherClass.setId(20);

        User myStudent = userWithRole("hs01", "ROLE_USER", school);
        myStudent.setFullName("Hoc Sinh Lop Toi");
        myStudent.setTrackingClass(myClass);

        User otherStudent = userWithRole("hs02", "ROLE_USER", school);
        otherStudent.setFullName("Hoc Sinh Lop Khac");
        otherStudent.setTrackingClass(otherClass);

        IUserService userService = mock(IUserService.class);
        when(userService.findById("gvcn")).thenReturn(teacher);
        when(userService.findById("hs01")).thenReturn(myStudent);
        when(userService.findById("hs02")).thenReturn(otherStudent);

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("gvcn", "ROLE_TEACHER");

        // My student: OK
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String res1 = controller.resetPassword("hs01", "newpass123", "newpass123", "/teacher/homeroom?classId=10", ra);
        assertThat(res1).isEqualTo("redirect:/teacher/homeroom?classId=10");
        assertThat(ra.getFlashAttributes().get("message").toString()).contains("Đã cấp lại mật khẩu thành công");
        verify(userService).save(myStudent);

        // Other student: REJECTED
        ra = new RedirectAttributesModelMap();
        String res2 = controller.resetPassword("hs02", "newpass123", "newpass123", "/teacher/homeroom?classId=10", ra);
        assertThat(res2).isEqualTo("redirect:/teacher/homeroom?classId=10");
        assertThat(ra.getFlashAttributes().get("error").toString()).contains("thuộc lớp mình chủ nhiệm");
        verify(userService, never()).save(otherStudent);
    }

    @Test
    void resetPasswordValidatesShortPasswordAndMismatch() {
        School school = school(1, "School A", "Ward", "District", "Province");
        User admin = userWithRole("admin", "ROLE_ADMIN", null);
        User teacher = userWithRole("gv01", "ROLE_TEACHER", school);

        IUserService userService = mock(IUserService.class);
        when(userService.findById("admin")).thenReturn(admin);
        when(userService.findById("gv01")).thenReturn(teacher);

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        authenticate("admin", "ROLE_ADMIN");

        // Too short (< 6)
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        controller.resetPassword("gv01", "123", "123", "/user/list", ra);
        assertThat(ra.getFlashAttributes().get("error").toString()).contains("ít nhất 6 ký tự");

        // Mismatch
        ra = new RedirectAttributesModelMap();
        controller.resetPassword("gv01", "123456", "654321", "/user/list", ra);
        assertThat(ra.getFlashAttributes().get("error").toString()).contains("không khớp");

        verify(userService, never()).save(teacher);
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
