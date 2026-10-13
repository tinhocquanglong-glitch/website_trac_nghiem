package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
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

import java.util.List;

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
