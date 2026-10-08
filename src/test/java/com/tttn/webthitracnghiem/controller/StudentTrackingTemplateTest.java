package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
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

class StudentTrackingTemplateTest {
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
    void rendersTrackingClassList() {
        WebContext context = webContext();
        context.setVariable("trackingClasses", List.of());
        context.setVariable("searchResults", List.of());
        context.setVariable("query", "");
        context.setVariable("searchPerformed", false);

        String html = templateEngine.process("student-tracking/list", context);

        assertThat(html)
                .contains("Sổ theo dõi học sinh", "Nhập từ Excel", "Tìm học sinh", "Xuất toàn bộ")
                .contains("/student-tracking/export");
    }

    @Test
    void rendersStudentSearchResultsWithClassLink() {
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setClassName("6A");
        trackingClass.setSchoolYear("2026-2027");
        WebContext context = webContext();
        context.setVariable("trackingClasses", List.of(trackingClass));
        context.setVariable("searchResults", List.of(
                new StudentSearchResult(10, 7, "Nguyễn Văn An", "6A", "2026-2027")));
        context.setVariable("query", "nguyen");
        context.setVariable("selectedClassId", 7);
        context.setVariable("searchPerformed", true);

        String html = templateEngine.process("student-tracking/list", context);

        assertThat(html)
                .contains("Nguyễn Văn An", "6A", "2026-2027")
                .contains("/student-tracking/classes/7?semester=1");
    }

    @Test
    void rendersSemesterSheetWithBoundRowNames() {
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(1);
        trackingClass.setClassName("6A");
        trackingClass.setSchoolYear("2026-2027");
        StudentTrackingClass otherClass = new StudentTrackingClass();
        otherClass.setId(2);
        otherClass.setClassName("6B");
        otherClass.setSchoolYear("2026-2027");
        TrackingSheetForm sheetForm = new TrackingSheetForm();
        sheetForm.setSemester(1);
        TrackingRowForm row = new TrackingRowForm();
        row.setStudentId(10);
        row.setFullName("Nguyen Van A");
        row.setAttendanceStatus(AttendanceStatus.ABSENT);
        sheetForm.setRows(List.of(row));

        WebContext context = webContext();
        context.setVariable("trackingClass", trackingClass);
        context.setVariable("trackingClasses", List.of(trackingClass, otherClass));
        context.setVariable("sheetForm", sheetForm);
        context.setVariable("semester", 1);
        context.setVariable("attendanceStatuses", AttendanceStatus.values());

        String html = templateEngine.process("student-tracking/sheet", context);

        assertThat(html)
                .contains("Sổ theo dõi lớp 6A", "rows[0].studentId", "Nguyen Van A", "Vắng", "Đầy đủ")
                .contains("class=\"tracking-view-controls\"")
                .contains("id=\"trackingClassSelector\"")
                .contains("id=\"classStudentSearch\"")
                .contains("value=\"/student-tracking/classes/1?semester=1\"")
                .contains("selected=\"selected\">6A - 2026-2027")
                .contains("value=\"/student-tracking/classes/2?semester=1\"")
                .contains("/student-tracking/export?classId=1");
    }

    private WebContext webContext() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", Map.of("img", "/img/test.png", "id", "admin"));
        return new WebContext(request, new MockHttpServletResponse(), servletContext);
    }
}
