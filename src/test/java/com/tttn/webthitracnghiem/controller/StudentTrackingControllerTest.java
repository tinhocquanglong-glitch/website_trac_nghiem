package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import com.tttn.webthitracnghiem.service.StudentTrackingExcelExporter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class StudentTrackingControllerTest {

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
        when(service.getSheet(7, 2)).thenReturn(form);
        when(service.listClasses()).thenReturn(List.of(trackingClass));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new StudentTrackingController(service, mock(StudentTrackingExcelExporter.class))).build();

        mvc.perform(get("/student-tracking/classes/7").param("semester", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("student-tracking/sheet"))
                .andExpect(model().attribute("trackingClass", trackingClass))
                .andExpect(model().attribute("trackingClasses", List.of(trackingClass)))
                .andExpect(model().attribute("sheetForm", form))
                .andExpect(model().attribute("semester", 2));
    }
}
