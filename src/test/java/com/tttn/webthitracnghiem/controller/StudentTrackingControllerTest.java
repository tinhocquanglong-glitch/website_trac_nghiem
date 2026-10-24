package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.service.IClassAssignmentService;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import com.tttn.webthitracnghiem.service.StudentTrackingExcelExporter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class StudentTrackingControllerTest {

    @Test
    void adminSeesSchoolsBeforeTrackingClasses() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        School school = new School();
        school.setId(3);
        school.setName("THPT Nguyễn Du");
        school.setProvinceName("Thanh pho");
        when(schoolRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(school));
        when(service.listClasses()).thenReturn(List.of());
        when(service.searchStudents("", null)).thenReturn(List.of());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class),
                        null, null, schoolRepository)).build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            mvc.perform(get("/student-tracking").param("address", "thanh"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("isAdminHierarchy", true))
                    .andExpect(model().attribute("trackingSchools", List.of(school)))
                    .andExpect(model().attribute("addressKeyword", "thanh"))
                    .andExpect(model().attribute("trackingClasses", List.of()))
                    .andExpect(model().attribute("selectedSchoolId", (Object) null));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void adminUsesExactClassCountsForSchoolAndGradeCards() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        School school = new School();
        school.setId(3);
        school.setName("THPT Nguyen Du");
        school.setProvinceName("Thanh pho");
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setGrade(10);
        trackingClass.setSchool(school);

        when(schoolRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(school));
        when(service.listClasses()).thenReturn(List.of(trackingClass));
        when(service.listClassesForSchool(3)).thenReturn(List.of(trackingClass));
        // Deliberately disagree with the list data to protect against the old stale-count bug.
        when(service.countClassesForSchool(3)).thenReturn(4L);
        when(service.countClassesForSchoolAndGrade(3, 10)).thenReturn(0L);
        when(service.searchStudents("", null)).thenReturn(List.of());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class),
                        null, null, schoolRepository)).build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            mvc.perform(get("/student-tracking").param("schoolId", "3").param("address", "thanh"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("classCountBySchool", java.util.Map.of(3, 1)))
                    .andExpect(model().attribute("classCountByGrade", java.util.Map.of(10, 1)))
                    .andExpect(model().attribute("selectedSchool", school));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void adminCanOpenClassesWithoutSchoolAssignment() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        StudentTrackingClass legacyClass = new StudentTrackingClass();
        legacyClass.setId(1);
        legacyClass.setClassName("6A");
        when(schoolRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(service.listClasses()).thenReturn(List.of());
        when(service.listClassesWithoutSchool()).thenReturn(List.of(legacyClass));
        when(service.searchStudents("", null)).thenReturn(List.of());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class),
                        null, null, schoolRepository)).build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            mvc.perform(get("/student-tracking").param("unassigned", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedUnassigned", true))
                    .andExpect(model().attribute("trackingClasses", List.of(legacyClass)))
                    .andExpect(model().attribute("unassignedClassCount", 1));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void fullAdminCannotCreateTrackingClass() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class))).build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            mvc.perform(post("/student-tracking/classes")
                        .param("className", "10A1")
                        .param("schoolYear", "2026-2027")
                        .param("grade", "10"))
                    .andExpect(status().is3xxRedirection());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(service, never()).createClass(any(String.class), any(String.class), anyInt(), anyInt(), any(String.class));
    }

    @Test
    void searchesStudentsInSelectedClass() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setClassName("6A");
        List<StudentSearchResult> results = List.of(
                new StudentSearchResult(10, 7, "Nguyễn Văn An", "6A", "2026-2027"));
        when(service.listClasses()).thenReturn(List.of(trackingClass));
        when(service.searchStudents("nguyen", 7)).thenReturn(results);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class))).build();

        mvc.perform(get("/student-tracking").param("q", "nguyen").param("classId", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("student-tracking/list"))
                .andExpect(model().attribute("searchResults", results))
                .andExpect(model().attribute("query", "nguyen"))
                .andExpect(model().attribute("selectedClassId", 7));
    }

    @Test
    void downloadsExcelForSelectedClass() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingExcelExporter exporter = mock(StudentTrackingExcelExporter.class);
        when(exporter.export(7)).thenReturn(new byte[]{1, 2, 3});
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new StudentTrackingController(service, exporter)).build();

        mvc.perform(get("/student-tracking/export").param("classId", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("so-theo-doi-lop-7.xlsx")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void showsSelectedSemesterSheet() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setClassName("6A");
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(2);
        when(service.findClass(7)).thenReturn(trackingClass);
        when(service.defaultMonth(2)).thenReturn(2);
        when(service.monthsForSemester(2)).thenReturn(List.of(2, 3, 4, 5));
        when(service.getSheet(7, 2, 2, null, null)).thenReturn(form);
        when(service.listClasses()).thenReturn(List.of(trackingClass));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class))).build();

        authenticate("admin", "ROLE_ADMIN");
        try {
            mvc.perform(get("/student-tracking/classes/7").param("semester", "2"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("student-tracking/sheet"))
                    .andExpect(model().attribute("trackingClass", trackingClass))
                    .andExpect(model().attribute("trackingClasses", List.of(trackingClass)))
                    .andExpect(model().attribute("sheetForm", form))
                    .andExpect(model().attribute("semester", 2))
                    .andExpect(model().attribute("month", 2))
                    .andExpect(model().attribute("months", List.of(2, 3, 4, 5)))
                    .andExpect(model().attribute("homeroomBook", true));
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(service).getSheet(7, 2, 2, null, null);
    }

    @Test
    void showsExplicitSelectedMonth() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setMonth(10);
        when(service.findClass(7)).thenReturn(trackingClass);
        when(service.listClasses()).thenReturn(List.of(trackingClass));
        when(service.getSheet(7, 1, 10, null, null)).thenReturn(form);
        when(service.monthsForSemester(1)).thenReturn(List.of(9, 10, 11, 12, 1));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class))).build();

        authenticate("admin", "ROLE_ADMIN");
        try {
            mvc.perform(get("/student-tracking/classes/7").param("semester", "1").param("month", "10"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("month", 10));
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(service).getSheet(7, 1, 10, null, null);
    }

    @Test
    void subjectTeacherOpensTheirOwnSubjectBookNotTheHomeroomBook() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        IUserService userService = mock(IUserService.class);
        IClassAssignmentService assignmentService = mock(IClassAssignmentService.class);
        User subjectTeacher = user("gvbm");
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setHomeroomTeacher(user("gvcn"));
        TrackingSheetForm subjectBook = new TrackingSheetForm();
        when(service.findClass(7)).thenReturn(trackingClass);
        when(service.defaultMonth(1)).thenReturn(9);
        when(service.monthsForSemester(1)).thenReturn(List.of(9, 10, 11, 12, 1));
        when(service.getSheet(7, 1, 9, "gvbm", 5)).thenReturn(subjectBook);
        when(userService.findById("gvbm")).thenReturn(subjectTeacher);
        when(assignmentService.findByClassId(7)).thenReturn(List.of(assignment(trackingClass, 5, "Toán", subjectTeacher)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                trackingController(service, userService, assignmentService)).build();

        authenticate("gvbm", "ROLE_TEACHER");
        try {
            mvc.perform(get("/student-tracking/classes/7"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("sheetForm", subjectBook))
                    .andExpect(model().attribute("homeroomBook", false))
                    .andExpect(model().attribute("bookSubjectId", 5))
                    .andExpect(model().attribute("bookSubjectName", "Toán"));
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(service, never()).getSheet(7, 1, 9, null, null);
    }

    @Test
    void subjectTeacherSaveGoesToTheirSubjectBook() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        IUserService userService = mock(IUserService.class);
        IClassAssignmentService assignmentService = mock(IClassAssignmentService.class);
        User subjectTeacher = user("gvbm");
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setHomeroomTeacher(user("gvcn"));
        when(service.findClass(7)).thenReturn(trackingClass);
        when(service.defaultMonth(1)).thenReturn(9);
        when(userService.findById("gvbm")).thenReturn(subjectTeacher);
        when(assignmentService.findByClassId(7)).thenReturn(List.of(assignment(trackingClass, 5, "Toán", subjectTeacher)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                trackingController(service, userService, assignmentService)).build();

        authenticate("gvbm", "ROLE_TEACHER");
        try {
            mvc.perform(post("/student-tracking/classes/7/records")
                            .param("semester", "1").param("month", "9").param("subjectId", "5")
                            .param("rows[0].studentId", "10").param("rows[0].fullName", "Nguyen Van A")
                            .param("rows[0].regularScore1", "8"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/student-tracking/classes/7?semester=1&month=9&subjectId=5"));
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(service).saveSheet(eq(7), any(TrackingSheetForm.class), eq("gvbm"), eq(5));
        verify(service, never()).saveSheet(eq(7), any(TrackingSheetForm.class));
    }

    @Test
    void subjectTeacherCannotWriteHomeroomBookOrUnassignedSubject() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        IUserService userService = mock(IUserService.class);
        IClassAssignmentService assignmentService = mock(IClassAssignmentService.class);
        User subjectTeacher = user("gvbm");
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setHomeroomTeacher(user("gvcn"));
        when(service.findClass(7)).thenReturn(trackingClass);
        when(service.defaultMonth(1)).thenReturn(9);
        when(userService.findById("gvbm")).thenReturn(subjectTeacher);
        when(assignmentService.findByClassId(7)).thenReturn(List.of(assignment(trackingClass, 5, "Toán", subjectTeacher)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                trackingController(service, userService, assignmentService)).build();

        authenticate("gvbm", "ROLE_TEACHER");
        try {
            // No subjectId means the homeroom book, which a subject teacher may not write.
            mvc.perform(post("/student-tracking/classes/7/records")
                            .param("semester", "1").param("month", "9")
                            .param("rows[0].studentId", "10").param("rows[0].fullName", "Nguyen Van A"))
                    .andExpect(status().is3xxRedirection());
            mvc.perform(post("/student-tracking/classes/7/records")
                            .param("semester", "1").param("month", "9").param("subjectId", "6")
                            .param("rows[0].studentId", "10").param("rows[0].fullName", "Nguyen Van A"))
                    .andExpect(status().is3xxRedirection());
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(service, never()).saveSheet(any(), any(TrackingSheetForm.class));
        verify(service, never()).saveSheet(any(), any(TrackingSheetForm.class), any(), any());
    }

    @Test
    void schoolAdminImportOnlyMatchesClassesOfTheirSchool() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        IUserService userService = mock(IUserService.class);
        School school = new School();
        school.setId(3);
        User principal = user("hieutruong");
        principal.setSchool(school);
        when(userService.findById("hieutruong")).thenReturn(principal);
        when(service.defaultMonth(1)).thenReturn(9);
        when(service.importWorkbook(any(), eq(1), eq(9), eq(3)))
                .thenReturn(new StudentTrackingService.ImportSummary(1, 2));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class),
                        userService, null, null)).build();

        authenticate("hieutruong", "ROLE_SCHOOL_ADMIN");
        try {
            mvc.perform(multipart("/student-tracking/import")
                            .file(new MockMultipartFile("file", "so.xlsx", "application/octet-stream", new byte[]{1}))
                            .param("semester", "1"))
                    .andExpect(status().is3xxRedirection());
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(service).importWorkbook(any(), eq(1), eq(9), eq(3));
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(role))));
    }

    private static StudentTrackingController trackingController(StudentTrackingService service,
                                                                IUserService userService,
                                                                IClassAssignmentService assignmentService) {
        StudentTrackingController controller = new StudentTrackingController(service,
                mock(StudentTrackingExcelExporter.class), userService, null, null);
        ReflectionTestUtils.setField(controller, "assignmentService", assignmentService);
        return controller;
    }

    private static User user(String id) {
        User user = new User();
        user.setId(id);
        user.setFullName(id);
        return user;
    }

    private static ClassSubjectAssignment assignment(StudentTrackingClass trackingClass, int subjectId,
                                                     String subjectName, User teacher) {
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName(subjectName);
        ClassSubjectAssignment assignment = new ClassSubjectAssignment();
        assignment.setTrackingClass(trackingClass);
        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        return assignment;
    }
}
