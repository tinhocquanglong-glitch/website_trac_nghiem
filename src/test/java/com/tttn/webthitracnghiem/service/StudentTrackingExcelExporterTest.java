package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentTrackingExcelExporterTest {

    @Test
    void exportsBothSemestersForSelectedClass() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(7);
        trackingClass.setClassName("6A");
        trackingClass.setSchoolYear("2026-2027");
        when(service.findClass(7)).thenReturn(trackingClass);
        when(service.getSheet(7, 1)).thenReturn(sheet(1, "Nguyễn Văn An", new BigDecimal("8.5"), AttendanceStatus.FULL));
        when(service.getSheet(7, 2)).thenReturn(sheet(2, "Nguyễn Văn An", new BigDecimal("9"), AttendanceStatus.ABSENT));

        byte[] bytes = new StudentTrackingExcelExporter(service).export(7);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("6A-2026-2027-HK1");
            assertThat(workbook.getSheetName(1)).isEqualTo("6A-2026-2027-HK2");
            assertThat(workbook.getSheetAt(0).getRow(5).getCell(1).getStringCellValue())
                    .isEqualTo("Nguyễn Văn An");
            assertThat(workbook.getSheetAt(0).getRow(5).getCell(2).getNumericCellValue())
                    .isEqualTo(8.5);
            assertThat(workbook.getSheetAt(0).getRow(5).getCell(8).getStringCellValue())
                    .isEqualTo("Đầy đủ");
        }
    }

    @Test
    void exportsBothSemestersForAllClasses() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingClass class6A = trackingClass(7, "6A", "2026-2027");
        StudentTrackingClass class7B = trackingClass(8, "7B", "2026-2027");
        when(service.listClasses()).thenReturn(List.of(class6A, class7B));
        when(service.getSheet(7, 1)).thenReturn(sheet(1, "Nguyễn Văn An", null, null));
        when(service.getSheet(7, 2)).thenReturn(sheet(2, "Nguyễn Văn An", null, null));
        when(service.getSheet(8, 1)).thenReturn(sheet(1, "Trần Thị Bình", null, null));
        when(service.getSheet(8, 2)).thenReturn(sheet(2, "Trần Thị Bình", null, null));

        byte[] bytes = new StudentTrackingExcelExporter(service).export(null);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            assertThat(workbook.getSheetName(0)).isEqualTo("6A-2026-2027-HK1");
            assertThat(workbook.getSheetName(3)).isEqualTo("7B-2026-2027-HK2");
        }
    }

    private StudentTrackingClass trackingClass(int id, String className, String schoolYear) {
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(id);
        trackingClass.setClassName(className);
        trackingClass.setSchoolYear(schoolYear);
        return trackingClass;
    }

    private TrackingSheetForm sheet(int semester, String fullName, BigDecimal score,
                                    AttendanceStatus attendanceStatus) {
        TrackingRowForm row = new TrackingRowForm();
        row.setStudentId(10);
        row.setFullName(fullName);
        row.setRegularScore1(score);
        row.setProgressComment("Có tiến bộ");
        row.setAttendanceStatus(attendanceStatus);
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(semester);
        form.setRows(java.util.List.of(row));
        return form;
    }
}
