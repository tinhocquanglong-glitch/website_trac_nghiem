package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.School;
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
        context.setVariable("isAdminOrSchoolAdmin", true);
        context.setVariable("canCreateClasses", true);

        String html = templateEngine.process("student-tracking/list", context);

        assertThat(html)
                .contains("Sổ theo dõi học sinh", "Nhập từ Excel", "Tìm học sinh", "Xuất toàn bộ")
                .contains("data-import-month", "value=\"9\"", "value=\"5\"")
                .contains("/student-tracking/export");
    }

    @Test
    void rendersAdminSchoolHierarchyWithoutClassCreationForm() {
        School school = new School();
        school.setId(3);
        school.setName("THPT Nguyễn Du");

        WebContext context = webContext();
        context.setVariable("trackingClasses", List.of());
        context.setVariable("trackingSchools", List.of(school));
        context.setVariable("classCountBySchool", Map.of(3, 2));
        context.setVariable("gradesBySchool", Map.of(3, List.of(10, 11)));
        context.setVariable("classCountByGrade", Map.of());
        context.setVariable("selectedSchool", null);
        context.setVariable("selectedSchoolId", null);
        context.setVariable("selectedGrade", null);
        context.setVariable("selectedUnassigned", false);
        context.setVariable("unassignedClassCount", 2);
        context.setVariable("addressKeyword", "nguyen");
        context.setVariable("hasAddressSearch", true);
        context.setVariable("isAdmin", true);
        context.setVariable("isSchoolAdmin", false);
        context.setVariable("isAdminHierarchy", true);
        context.setVariable("canCreateClasses", false);
        context.setVariable("isAdminOrSchoolAdmin", true);
        context.setVariable("searchResults", List.of());
        context.setVariable("query", "");
        context.setVariable("searchPerformed", false);

        String html = templateEngine.process("student-tracking/list", context);

        assertThat(html)
                .contains("Chọn trường để xem sổ theo dõi", "THPT Nguyễn Du", "Chưa gán trường", "unassigned=true")
                .doesNotContain("action=\"/student-tracking/classes\"", "Thêm lớp");
    }

    @Test
    void rendersActualClassCountOnGradeCard() {
        School school = new School();
        school.setId(3);
        school.setName("THPT Nguyễn Du");

        WebContext context = webContext();
        context.setVariable("trackingClasses", List.of());
        context.setVariable("trackingSchools", List.of(school));
        context.setVariable("classCountBySchool", Map.of(3, 1));
        context.setVariable("gradesBySchool", Map.of(3, List.of(10)));
        context.setVariable("classCountByGrade", Map.of(10, 1));
        context.setVariable("selectedSchool", school);
        context.setVariable("selectedSchoolId", 3);
        context.setVariable("selectedGrade", null);
        context.setVariable("selectedUnassigned", false);
        context.setVariable("unassignedClassCount", 0);
        context.setVariable("addressKeyword", "");
        context.setVariable("hasAddressSearch", false);
        context.setVariable("isAdmin", true);
        context.setVariable("isSchoolAdmin", false);
        context.setVariable("isAdminHierarchy", true);
        context.setVariable("canCreateClasses", false);
        context.setVariable("isAdminOrSchoolAdmin", true);
        context.setVariable("searchResults", List.of());
        context.setVariable("query", "");
        context.setVariable("searchPerformed", false);

        String html = templateEngine.process("student-tracking/list", context);

        assertThat(html).contains("Khối 10", "1 lớp").doesNotContain("0 lớp");
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
                .contains("/student-tracking/classes/7?semester=1&amp;month=9");
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
        row.setAttendanceDates("Nghỉ có phép, phụ huynh đã báo");
        sheetForm.setRows(List.of(row));

        WebContext context = webContext();
        context.setVariable("trackingClass", trackingClass);
        context.setVariable("trackingClasses", List.of(trackingClass, otherClass));
        context.setVariable("sheetForm", sheetForm);
        context.setVariable("semester", 1);
        context.setVariable("month", 10);
        context.setVariable("months", List.of(9, 10, 11, 12, 1));
        context.setVariable("attendanceStatuses", AttendanceStatus.values());

        String html = templateEngine.process("student-tracking/sheet", context);

        assertThat(html)
                .contains("Sổ theo dõi lớp 6A", "rows[0].studentId", "Nguyen Van A", "Vắng", "Đi học", "Không có")
                .contains("ĐGTX", "colspan=\"2\"", "Số ngày", "rows[0].attendanceDates",
                        "Nghỉ có phép, phụ huynh đã báo", "placeholder=\"Ghi chú tùy ý\"")
                .contains("class=\"tracking-view-controls\"")
                .contains("id=\"trackingClassSelector\"")
                .contains("id=\"classStudentSearch\"")
                .contains("value=\"/student-tracking/classes/1?semester=1&amp;month=10\"")
                .contains("selected=\"selected\">6A - 2026-2027")
                .contains("value=\"/student-tracking/classes/2?semester=1&amp;month=10\"")
                .contains("name=\"month\" value=\"10\"")
                .contains("Tháng 9", "Tháng 10", "Tháng 1")
                .contains("/student-tracking/export?classId=1");
    }

    @Test
    void rendersSchoolUsers() {
        WebContext context = webContext();
        User teacher = new User();
        teacher.setId("gv_gvcn");
        teacher.setFullName("Le Dong Ngoc");
        teacher.setSchoolLevel(3);

        StudentTrackingClass cls = new StudentTrackingClass();
        cls.setId(1);
        cls.setClassName("10A2");
        cls.setGrade(10);
        cls.setHomeroomTeacher(teacher);

        User student = new User();
        student.setId("hs01");
        student.setFullName("Dang Le Thi Anh");
        student.setSchoolLevel(3);
        student.setTrackingClass(cls);

        java.util.Map<String, List<StudentTrackingClass>> homeroomMap = new java.util.HashMap<>();
        homeroomMap.put("gv_gvcn", List.of(cls));

        java.util.Map<Integer, java.util.Map<Integer, java.util.Map<String, List<User>>>> grouped = new java.util.TreeMap<>();
        java.util.Map<Integer, java.util.Map<String, List<User>>> gradeMap = new java.util.TreeMap<>();
        java.util.Map<String, List<User>> classMap = new java.util.TreeMap<>();
        classMap.put("10A2", List.of(student));
        gradeMap.put(10, classMap);
        grouped.put(3, gradeMap);

        context.setVariable("teachers", List.of(teacher));
        context.setVariable("teacherAssignments", Map.of());
        context.setVariable("homeroomMap", homeroomMap);
        context.setVariable("teacherKeyword", "");
        context.setVariable("groupedStudents", grouped);
        context.setVariable("totalStudents", 1);
        context.setVariable("studentKeyword", "");
        context.setVariable("studentGrade", null);
        context.setVariable("schoolName", "THPT Test");

        String html = templateEngine.process("user/school-users", context);
        assertThat(html)
                .contains("Quản lý người dùng trong trường")
                .contains("Le Dong Ngoc")
                .contains("Quản lý HS lớp")
                .contains("Dang Le Thi Anh");
    }

    @Test
    void rendersTeacherHomeroom() {
        WebContext context = webContext();
        User teacher = new User();
        teacher.setId("gv_gvcn");
        teacher.setFullName("Le Dong Ngoc");

        StudentTrackingClass cls = new StudentTrackingClass();
        cls.setId(1);
        cls.setClassName("10A2");
        cls.setGrade(10);
        cls.setSchoolYear("2026-2027");
        cls.setHomeroomTeacher(teacher);

        User student = new User();
        student.setId("hs01");
        student.setFullName("Dang Le Thi Anh");

        context.setVariable("isAdmin", true);
        context.setVariable("isSchoolAdmin", false);
        context.setVariable("isTeacher", false);
        context.setVariable("currentUser", teacher);
        context.setVariable("keyword", "");
        context.setVariable("targetSchool", null);
        context.setVariable("allSchools", List.of());
        context.setVariable("classesByGrade", Map.of(10, List.of(cls)));
        context.setVariable("selectedGrade", 10);
        context.setVariable("availableClasses", List.of(cls));
        context.setVariable("selectedClass", cls);
        context.setVariable("students", List.of(student));
        context.setVariable("totalStudents", 1);
        context.setVariable("unassignedStudents", List.of());

        String html = templateEngine.process("teacher/homeroom", context);
        assertThat(html)
                .contains("Học sinh lớp chủ nhiệm")
                .contains("10A2")
                .contains("Dang Le Thi Anh");
    }

    @Test
    void rendersUserList() {
        WebContext context = webContext();
        context.setVariable("viewMode", "schools");
        context.setVariable("keyword", "");
        context.setVariable("schools", List.of());
        context.setVariable("addressGroups", Map.of());
        context.setVariable("selectedAddress", null);
        context.setVariable("addressKeyword", "");
        context.setVariable("hasSchoolSearch", false);
        context.setVariable("schoolManagersMap", Map.of());
        context.setVariable("teacherCountMap", Map.of());
        context.setVariable("studentCountMap", Map.of());
        context.setVariable("isAdmin", true);
        context.setVariable("users", new org.springframework.data.domain.PageImpl<>(List.of()));

        String html = templateEngine.process("user/list", context);
        assertThat(html).contains("Quản lý theo từng trường");
    }

    @Test
    void rendersAddressSearchResultsInsteadOfAddressGroups() {
        School school = new School();
        school.setId(3);
        school.setName("THPT Nguyễn Du");

        WebContext context = webContext();
        context.setVariable("viewMode", "schools");
        context.setVariable("keyword", "");
        context.setVariable("schools", List.of(school));
        context.setVariable("addressGroups", Map.of("Quận 1, Thành phố Hồ Chí Minh", List.of(school)));
        context.setVariable("selectedAddress", null);
        context.setVariable("addressKeyword", "quan 1");
        context.setVariable("hasSchoolSearch", true);
        context.setVariable("schoolManagersMap", Map.of());
        context.setVariable("teacherCountMap", Map.of());
        context.setVariable("studentCountMap", Map.of());
        context.setVariable("isAdmin", true);
        context.setVariable("users", new org.springframework.data.domain.PageImpl<>(List.of()));

        String html = templateEngine.process("user/list", context);

        assertThat(html)
                .contains("name=\"address\"", "THPT Nguyễn Du")
                .doesNotContain("Chọn địa chỉ để xem trường học");
    }

    private WebContext webContext() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", Map.of("img", "/img/test.png", "id", "admin"));
        return new WebContext(request, new MockHttpServletResponse(), servletContext);
    }
}
