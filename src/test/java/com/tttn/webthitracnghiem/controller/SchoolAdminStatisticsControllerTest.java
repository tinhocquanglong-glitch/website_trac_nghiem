package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolAdminStatistics;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.SchoolAdminAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class SchoolAdminStatisticsControllerTest {
    @Test
    void schoolAdminSeesStatisticsForTheirOwnSchool() throws Exception {
        IUserService userService = mock(IUserService.class);
        SchoolAdminAnalyticsService analyticsService = mock(SchoolAdminAnalyticsService.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        School school = new School();
        school.setId(3);
        school.setName("THCS Nguyen Du");
        com.tttn.webthitracnghiem.model.User admin = new com.tttn.webthitracnghiem.model.User();
        admin.setId("principal");
        admin.setSchool(school);
        SchoolAdminStatistics statistics = new SchoolAdminStatistics();
        when(userService.findById("principal")).thenReturn(admin);
        when(analyticsService.build(eq(school), eq(6), eq("2026-2027"))).thenReturn(statistics);
        when(classRepository.findBySchoolId(3)).thenReturn(List.of());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SchoolAdminStatisticsController(
                analyticsService, userService, schoolRepository, classRepository)).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "principal", "n/a", List.of(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))));
        try {
            mvc.perform(get("/school-admin/statistics").param("grade", "6").param("schoolYear", "2026-2027"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("school-admin/statistics"))
                    .andExpect(model().attribute("targetSchool", school))
                    .andExpect(model().attribute("statistics", statistics));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
