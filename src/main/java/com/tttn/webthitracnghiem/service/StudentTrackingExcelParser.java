package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StudentTrackingExcelParser {
    private static final int MAX_SHEETS = 50;
    private static final int MAX_STUDENTS_PER_SHEET = 200;
    private static final int MAX_DATA_ROW_SPAN = 1000;
    private static final Pattern SCHOOL_YEAR_PATTERN = Pattern.compile("(\\d{4}\\s*-\\s*\\d{4})");

    public List<ImportedClass> parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() > MAX_SHEETS) {
                throw new IllegalArgumentException("File Excel vuot qua 50 sheet");
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<ImportedClass> result = new ArrayList<>();
            for (Sheet sheet : workbook) {
                int headerRowIndex = findHeaderRow(sheet, formatter);
                if (headerRowIndex < 0) {
                    continue;
                }
                String schoolYear = findSchoolYear(sheet, headerRowIndex, formatter);
                if (schoolYear == null) {
                    throw new IllegalArgumentException("Khong tim thay nam hoc trong sheet " + sheet.getSheetName());
                }
                result.add(parseSheet(sheet, headerRowIndex, schoolYear, formatter));
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("File Excel khong co sheet dung mau so theo doi");
            }
            return result;
        }
    }

    private ImportedClass parseSheet(Sheet sheet, int headerRowIndex, String schoolYear,
                                     DataFormatter formatter) {
        String className = sheet.getSheetName().trim();
        if (className.isEmpty() || className.length() > 50) {
            throw new IllegalArgumentException("Ten lop trong ten sheet khong hop le");
        }
        if (sheet.getLastRowNum() - headerRowIndex > MAX_DATA_ROW_SPAN) {
            throw new IllegalArgumentException("Sheet " + className + " co pham vi dong qua lon");
        }

        List<ImportedStudent> students = new ArrayList<>();
        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String fullName = cellText(row.getCell(1), formatter).trim();
            if (fullName.isEmpty()) {
                continue;
            }
            if (fullName.length() > 150) {
                throw new IllegalArgumentException("Ten hoc sinh qua dai tai sheet " + className);
            }
            if (students.size() >= MAX_STUDENTS_PER_SHEET) {
                throw new IllegalArgumentException("Sheet " + className + " vuot qua 200 hoc sinh");
            }

            ImportedStudent student = new ImportedStudent();
            student.fullName = fullName;
            student.regularScore1 = score(row.getCell(2), formatter, className, rowIndex + 1);
            student.regularScore2 = score(row.getCell(3), formatter, className, rowIndex + 1);
            student.midtermScore = score(row.getCell(4), formatter, className, rowIndex + 1);
            student.finalScore = score(row.getCell(5), formatter, className, rowIndex + 1);
            student.averageScore = score(row.getCell(6), formatter, className, rowIndex + 1);
            student.progressComment = cellText(row.getCell(7), formatter).trim();
            if (student.progressComment.length() > 2000) {
                throw new IllegalArgumentException("Nhan xet qua dai tai sheet " + className + ", dong " + (rowIndex + 1));
            }
            student.attendanceStatus = AttendanceStatus.fromText(cellText(row.getCell(8), formatter));
            students.add(student);
        }
        return new ImportedClass(className, schoolYear, students);
    }

    private int findHeaderRow(Sheet sheet, DataFormatter formatter) {
        int lastCandidate = Math.min(sheet.getLastRowNum(), 20);
        for (int i = 0; i <= lastCandidate; i++) {
            Row row = sheet.getRow(i);
            if (row != null && "ho va ten".equals(normalize(cellText(row.getCell(1), formatter)))) {
                return i;
            }
        }
        return -1;
    }

    private String findSchoolYear(Sheet sheet, int headerRowIndex, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex < headerRowIndex; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                Matcher matcher = SCHOOL_YEAR_PATTERN.matcher(cellText(cell, formatter));
                if (matcher.find()) {
                    return matcher.group(1).replaceAll("\\s", "");
                }
            }
        }
        return null;
    }

    private BigDecimal score(Cell cell, DataFormatter formatter, String sheetName, int rowNumber) {
        String raw = cellText(cell, formatter).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.replace(',', '.'));
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
                throw new IllegalArgumentException("Diem phai tu 0 den 10 tai sheet " + sheetName + ", dong " + rowNumber);
            }
            return value.stripTrailingZeros();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Diem khong hop le tai sheet " + sheetName + ", dong " + rowNumber);
        }
    }

    private String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            throw new IllegalArgumentException("File Excel khong duoc chua cong thuc");
        }
        return formatter.formatCellValue(cell);
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    public static class ImportedClass {
        private final String className;
        private final String schoolYear;
        private final List<ImportedStudent> students;

        public ImportedClass(String className, String schoolYear, List<ImportedStudent> students) {
            this.className = className;
            this.schoolYear = schoolYear;
            this.students = new ArrayList<>(students);
        }

        public String getClassName() {
            return className;
        }

        public String getSchoolYear() {
            return schoolYear;
        }

        public List<ImportedStudent> getStudents() {
            return Collections.unmodifiableList(students);
        }
    }

    public static class ImportedStudent {
        private String fullName;
        private BigDecimal regularScore1;
        private BigDecimal regularScore2;
        private BigDecimal midtermScore;
        private BigDecimal finalScore;
        private BigDecimal averageScore;
        private String progressComment;
        private AttendanceStatus attendanceStatus;

        public String getFullName() { return fullName; }
        public BigDecimal getRegularScore1() { return regularScore1; }
        public BigDecimal getRegularScore2() { return regularScore2; }
        public BigDecimal getMidtermScore() { return midtermScore; }
        public BigDecimal getFinalScore() { return finalScore; }
        public BigDecimal getAverageScore() { return averageScore; }
        public String getProgressComment() { return progressComment; }
        public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    }
}
