package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolAdminStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolAdminStatisticsTemplateTest {
    private TemplateEngine templateEngine;
    private MockServletContext servletContext;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        templateEngine = engine;
        servletContext = new MockServletContext();
    }

    @Test
    void rendersSchoolDashboardWithoutDetailRows() {
        School school = new School();
        school.setId(3);
        school.setName("THCS Nguyen Du");
        SchoolAdminStatistics statistics = new SchoolAdminStatistics();
        statistics.setStudentCount(120);
        statistics.setTeacherCount(18);
        statistics.setClassCount(12);
        statistics.setRealExamCount(4);
        statistics.setPracticeExamCount(2);
        statistics.setAssignmentCoverage(85.5);

        WebContext context = webContext();
        context.setVariable("statistics", statistics);
        context.setVariable("targetSchool", school);
        context.setVariable("allSchools", List.of(school));
        context.setVariable("availableGrades", List.of(6, 7));
        context.setVariable("grade", null);
        context.setVariable("schoolYear", "2026-2027");
        context.setVariable("isAdmin", true);

        String html = templateEngine.process("school-admin/statistics", context);

        assertThat(html)
                .contains("Thống kê nhà trường", "KẾT QUẢ HỌC TẬP", "TÌNH HÌNH KIỂM TRA", "Phân công giảng dạy")
                .contains("120", "18", "85,5%", "Cảnh báo nhà trường")
                .contains("/school-admin/teaching-assignments");
    }

    private WebContext webContext() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", Map.of("img", "/img/test.png", "id", "admin"));
        return new WebContext(request, new MockHttpServletResponse(), servletContext);
    }
}
