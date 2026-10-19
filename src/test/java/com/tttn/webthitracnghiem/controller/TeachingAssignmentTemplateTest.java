package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TeachingAssignmentTemplateTest {
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
    void rendersBulkAndMatrixAssignmentWorkspaces() {
        School school = new School();
        school.setId(3);
        school.setName("THCS Nguyen Du");
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(10);
        trackingClass.setClassName("6A1");
        trackingClass.setGrade(6);
        trackingClass.setSchoolYear("2026-2027");
        trackingClass.setSchool(school);
        Subject subject = new Subject();
        subject.setId(7);
        subject.setName("Toan");
        User teacher = new User();
        teacher.setId("teacher01");
        teacher.setFullName("Nguyen Van A");

        WebContext context = webContext();
        context.setVariable("targetSchool", school);
        context.setVariable("allSchools", List.of(school));
        context.setVariable("classes", List.of(trackingClass));
        context.setVariable("schoolTeachers", List.of(teacher));
        context.setVariable("allSubjects", List.of(subject));
        context.setVariable("assignmentByKey", Map.of());
        context.setVariable("assignmentCountByClass", Map.of());
        context.setVariable("availableGrades", Set.of(6));
        context.setVariable("grade", 6);
        context.setVariable("schoolYear", "2026-2027");
        context.setVariable("keyword", "");
        context.setVariable("isAdmin", true);

        String html = templateEngine.process("school-admin/teaching-assignments", context);

        assertThat(html)
                .contains("Phân công giảng dạy", "Phân công theo nhóm lớp", "Ma trận giảng dạy")
                .contains("6A1", "Toan", "Nguyen Van A")
                .contains("/school-admin/teaching-assignments/bulk")
                .contains("/school-admin/teaching-assignments/matrix")
                .contains("assignment_10_7");
    }

    private WebContext webContext() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", Map.of("img", "/img/test.png", "id", "admin"));
        return new WebContext(request, new MockHttpServletResponse(), servletContext);
    }
}
