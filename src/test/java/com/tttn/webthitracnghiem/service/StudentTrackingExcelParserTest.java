package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentTrackingExcelParserTest {

    private final StudentTrackingExcelParser parser = new StudentTrackingExcelParser();

    @Test
    void parsesTrackingSheetsAndIgnoresHelperSheets() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "Vang", "V.08/10, V.15/10");

        var classes = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(classes).hasSize(1);
        assertThat(classes.get(0).getClassName()).isEqualTo("6A");
        assertThat(classes.get(0).getSchoolYear()).isEqualTo("2026-2027");
        assertThat(classes.get(0).getStudents()).hasSize(1);
        var student = classes.get(0).getStudents().get(0);
        assertThat(student.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(student.getRegularScore1()).isEqualByComparingTo(new BigDecimal("8"));
        assertThat(student.getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(student.getAttendanceDates()).isEqualTo("V.08/10, V.15/10");
    }

    @Test
    void parsesFullAttendanceStatus() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "Đi học", "");

        var classes = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(classes.get(0).getStudents().get(0).getAttendanceStatus())
                .isEqualTo(AttendanceStatus.FULL);
    }

    @Test
    void keepsSupportingTheOldFullAttendanceLabel() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "Đầy đủ", "");

        var classes = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(classes.get(0).getStudents().get(0).getAttendanceStatus())
                .isEqualTo(AttendanceStatus.FULL);
    }

    @Test
    void parsesNoAttendanceStatus() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "Không có", "");

        var classes = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(classes.get(0).getStudents().get(0).getAttendanceStatus())
                .isEqualTo(AttendanceStatus.NONE);
    }

    @Test
    void rejectsScoresOutsideZeroToTen() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "", "");
        try (var editable = new XSSFWorkbook(new ByteArrayInputStream(workbook));
             var output = new ByteArrayOutputStream()) {
            editable.getSheet("6A").getRow(5).getCell(2).setCellValue(11);
            editable.write(output);
            workbook = output.toByteArray();
        }

        byte[] invalidWorkbook = workbook;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(invalidWorkbook)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 den 10");
    }

    @Test
    void rejectsFormulaCellsInsteadOfEvaluatingUntrustedWorkbooks() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "", "");
        try (var editable = new XSSFWorkbook(new ByteArrayInputStream(workbook));
             var output = new ByteArrayOutputStream()) {
            editable.getSheet("6A").getRow(5).getCell(2).setCellFormula("SUM(4,4)");
            editable.write(output);
            workbook = output.toByteArray();
        }

        byte[] formulaWorkbook = workbook;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(formulaWorkbook)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cong thuc");
    }

    @Test
    void rejectsSheetsWithAnExcessiveSparseRowRange() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "", "");
        try (var editable = new XSSFWorkbook(new ByteArrayInputStream(workbook));
             var output = new ByteArrayOutputStream()) {
            editable.getSheet("6A").createRow(1205).createCell(0).setCellValue("unexpected tail");
            editable.write(output);
            workbook = output.toByteArray();
        }

        byte[] sparseWorkbook = workbook;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(sparseWorkbook)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pham vi dong qua lon");
    }

    @Test
    void stopsAtTheNextMonthlyTableHeader() throws Exception {
        byte[] workbook = workbookWithTrackingSheet("6A", "2026-2027", "Nguyen Van A", "Vang", "V.08/10");
        try (var editable = new XSSFWorkbook(new ByteArrayInputStream(workbook));
             var output = new ByteArrayOutputStream()) {
            Sheet sheet = editable.getSheet("6A");
            sheet.createRow(7).createCell(1).setCellValue("HO VA TEN");
            Row secondMonthStudent = sheet.createRow(8);
            secondMonthStudent.createCell(1).setCellValue("Tran Thi B");
            secondMonthStudent.createCell(2).setCellValue(9);
            editable.write(output);
            workbook = output.toByteArray();
        }

        var classes = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(classes.get(0).getStudents())
                .extracting(StudentTrackingExcelParser.ImportedStudent::getFullName)
                .containsExactly("Nguyen Van A");
    }

    private byte[] workbookWithTrackingSheet(String className, String schoolYear,
                                             String studentName, String attendance,
                                             String attendanceDates) throws Exception {
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(className);
            sheet.createRow(2).createCell(0)
                    .setCellValue("DANH SACH HOC SINH LOP " + className + " NAM HOC " + schoolYear);
            Row header = sheet.createRow(4);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("HO VA TEN");
            header.createCell(2).setCellValue("DGTX");
            header.createCell(4).setCellValue("DGK I");
            header.createCell(5).setCellValue("DCK I");
            header.createCell(6).setCellValue("DTBM HKI");
            header.createCell(7).setCellValue("Nhan xet");
            header.createCell(8).setCellValue("Diem danh");
            header.createCell(9).setCellValue("So ngay");

            Row student = sheet.createRow(5);
            student.createCell(0).setCellValue(1);
            student.createCell(1).setCellValue(studentName);
            student.createCell(2).setCellValue(8);
            student.createCell(3).setCellValue(9);
            student.createCell(4).setCellValue(7.5);
            student.createCell(5).setCellValue(8.5);
            student.createCell(6).setCellValue(8.2);
            student.createCell(7).setCellValue("Co tien bo");
            student.createCell(8).setCellValue(attendance);
            student.createCell(9).setCellValue(attendanceDates);

            Sheet helper = workbook.createSheet("Trang_tinh11");
            helper.createRow(0).createCell(0).setCellValue("HO VA TEN");
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
