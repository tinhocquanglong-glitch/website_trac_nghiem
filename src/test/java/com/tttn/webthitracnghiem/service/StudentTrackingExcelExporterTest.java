package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import org.apache.poi.ss.util.CellRangeAddress;
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
        when(service.monthsForSemester(1)).thenReturn(List.of(9, 10, 11, 12, 1));
        when(service.monthsForSemester(2)).thenReturn(List.of(2, 3, 4, 5));
        stubMonths(service, 7, 1, List.of(9, 10, 11, 12, 1), new BigDecimal("8.5"), AttendanceStatus.FULL, null);
        stubMonths(service, 7, 2, List.of(2, 3, 4, 5), new BigDecimal("9"), AttendanceStatus.ABSENT, "V.08/10, V.15/10");

        byte[] bytes = new StudentTrackingExcelExporter(service).export(7);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("6A-2026-2027-HK1");
            assertThat(workbook.getSheetName(1)).isEqualTo("6A-2026-2027-HK2");
            assertThat(workbook.getSheetAt(0).getRow(7).getCell(1).getStringCellValue())
                    .isEqualTo("Nguyễn Văn An");
            assertThat(workbook.getSheetAt(0).getRow(7).getCell(2).getNumericCellValue())
                    .isEqualTo(8.5);
            assertThat(workbook.getSheetAt(0).getRow(7).getCell(8).getStringCellValue())
                    .isEqualTo("Đi học");
            assertThat(workbook.getSheetAt(1).getRow(7).getCell(9).getStringCellValue())
                    .isEqualTo("V.08/10, V.15/10");
            assertThat(workbook.getSheetAt(0).getRow(4).getCell(0).getStringCellValue()).isEqualTo("THÁNG 9");
            assertThat(workbook.getSheetAt(0).getRow(5).getCell(2).getStringCellValue()).isEqualTo("ĐGTX");
            assertThat(workbook.getSheetAt(0).getRow(6).getCell(2).getStringCellValue()).isEqualTo("ĐGTX 1");
            assertThat(workbook.getSheetAt(0).getRow(10).getCell(0).getStringCellValue()).isEqualTo("THÁNG 10");
            assertThat(workbook.getSheetAt(0).getMergedRegions())
                    .extracting(CellRangeAddress::formatAsString)
                    .contains("C6:D6", "A5:J5");
            var attendanceValidations = workbook.getSheetAt(0).getDataValidations();
            assertThat(attendanceValidations).hasSize(5);
            assertThat(attendanceValidations.get(0).getSuppressDropDownArrow()).isTrue();
            assertThat(attendanceValidations.get(0).getValidationConstraint().getExplicitListValues())
                    .containsExactly("Đi học", "Vắng", "Bỏ học", "Chuyển trường", "Không có");
            assertThat(attendanceValidations.get(0).getRegions().getCellRangeAddresses())
                    .extracting(CellRangeAddress::formatAsString)
                    .containsExactly("I8");
        }
    }

    @Test
    void exportsBothSemestersForAllClasses() throws Exception {
        StudentTrackingService service = mock(StudentTrackingService.class);
        StudentTrackingClass class6A = trackingClass(7, "6A", "2026-2027");
        StudentTrackingClass class7B = trackingClass(8, "7B", "2026-2027");
        when(service.listClasses()).thenReturn(List.of(class6A, class7B));
        when(service.monthsForSemester(1)).thenReturn(List.of(9, 10, 11, 12, 1));
        when(service.monthsForSemester(2)).thenReturn(List.of(2, 3, 4, 5));
        stubMonths(service, 7, 1, List.of(9, 10, 11, 12, 1), null, null, null);
        stubMonths(service, 7, 2, List.of(2, 3, 4, 5), null, null, null);
        stubMonths(service, 8, 1, List.of(9, 10, 11, 12, 1), null, null, null);
        for (int month : List.of(2, 3, 4, 5)) {
            when(service.getSheet(8, 2, month)).thenReturn(sheet(2, "Trần Thị Bình", null, null));
        }

        byte[] bytes = new StudentTrackingExcelExporter(service).export(null);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            assertThat(workbook.getSheetName(0)).isEqualTo("6A-2026-2027-HK1");
            assertThat(workbook.getSheetName(3)).isEqualTo("7B-2026-2027-HK2");
        }
    }

    private void stubMonths(StudentTrackingService service, int classId, int semester, List<Integer> months,
                            BigDecimal score, AttendanceStatus status, String dates) {
        for (int month : months) {
            when(service.getSheet(classId, semester, month))
                    .thenReturn(sheet(semester, "Nguyễn Văn An", score, status, dates));
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

    private TrackingSheetForm sheet(int semester, String fullName, BigDecimal score,
                                    AttendanceStatus attendanceStatus, String attendanceDates) {
        TrackingSheetForm form = sheet(semester, fullName, score, attendanceStatus);
        form.getRows().get(0).setAttendanceDates(attendanceDates);
        return form;
    }
}
