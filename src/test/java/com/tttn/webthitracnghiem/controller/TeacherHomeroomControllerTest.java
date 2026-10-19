package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IRoleService;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherHomeroomControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminSeesOnlySchoolsMatchingAddressSearch() throws Exception {
        School matchingSchool = school(1, "School A", "Xa Tan Phu", "Quan 1", "Thanh pho Ho Chi Minh");
        School otherSchool = school(2, "School B", "Xa Tan Son", "Quan 3", "Thanh pho Ho Chi Minh");

        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        IUserService userService = mock(IUserService.class);
        when(schoolRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(matchingSchool, otherSchool));
        User admin = new User();
        admin.setId("admin");
        when(userService.findById("admin")).thenReturn(admin);

        TeacherHomeroomController controller = new TeacherHomeroomController();
        ReflectionTestUtils.setField(controller, "schoolRepository", schoolRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "trackingClassRepository", mock(StudentTrackingClassRepository.class));
        ReflectionTestUtils.setField(controller, "roleService", mock(IRoleService.class));
        ReflectionTestUtils.setField(controller, "trackingService", mock(StudentTrackingService.class));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mvc.perform(get("/teacher/homeroom").param("address", "quan 1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("allSchools", List.of(matchingSchool)))
                .andExpect(model().attribute("addressKeyword", "quan 1"))
                .andExpect(model().attribute("targetSchool", (Object) null));
    }

    @Test
    void teacherCanCreateStudentWithUppercaseUsernameAndExistingRole() {
        TeacherHomeroomController controller = new TeacherHomeroomController();
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        IUserService userService = mock(IUserService.class);
        IRoleService roleService = mock(IRoleService.class);
        StudentTrackingService trackingService = mock(StudentTrackingService.class);

        User teacher = new User();
        teacher.setId("teacher");
        User homeroomTeacher = teacher;
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setClassName("10A1");
        trackingClass.setHomeroomTeacher(homeroomTeacher);
        Role studentRole = new Role("ROLE_USER");
        studentRole.setId(1);

        when(userService.findById("teacher")).thenReturn(teacher);
        when(userService.userExists("HS001")).thenReturn(false);
        when(userService.userExistss("hs001@student.school.vn")).thenReturn(false);
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classRepository.findById(7)).thenReturn(Optional.of(trackingClass));
        when(roleService.findByName("ROLE_USER")).thenReturn(studentRole);

        ReflectionTestUtils.setField(controller, "schoolRepository", schoolRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "trackingClassRepository", classRepository);
        ReflectionTestUtils.setField(controller, "roleService", roleService);
        ReflectionTestUtils.setField(controller, "trackingService", trackingService);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "teacher", "n/a", List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));

        String result = controller.createStudent(7, "HS001", "Hoc Sinh", null,
                "secret1", null, new RedirectAttributesModelMap());

        assertThat(result).isEqualTo("redirect:/teacher/homeroom?classId=7");
        verify(userService).save(any(User.class));
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
