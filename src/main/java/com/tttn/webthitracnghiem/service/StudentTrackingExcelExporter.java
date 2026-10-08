package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class StudentTrackingExcelExporter {
    private static final String[] PARENT_HEADERS = {
            "STT", "HỌ VÀ TÊN", "ĐGTX", "", "ĐGK", "ĐCK", "ĐTBM",
            "Nhận xét sự tiến bộ, ưu điểm nổi bật, hạn chế chủ yếu", "Điểm danh", "Số ngày"
    };

    private final StudentTrackingService trackingService;

    public StudentTrackingExcelExporter(StudentTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    public byte[] export(Integer classId) throws IOException {
        List<StudentTrackingClass> classes = classId == null
                ? trackingService.listClasses()
                : List.of(trackingService.findClass(classId));
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            WorkbookStyles styles = new WorkbookStyles(workbook);
            Set<String> sheetNames = new HashSet<>();
            for (StudentTrackingClass trackingClass : classes) {
                createSheet(workbook, trackingClass, 1, styles, sheetNames);
                createSheet(workbook, trackingClass, 2, styles, sheetNames);
            }
            if (classes.isEmpty()) {
                workbook.createSheet("Khong co du lieu").createRow(0).createCell(0)
                        .setCellValue("Chưa có lớp theo dõi để xuất.");
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createSheet(XSSFWorkbook workbook, StudentTrackingClass trackingClass,
                             int semesterNumber, WorkbookStyles styles, Set<String> usedNames) {
        String baseName = trackingClass.getClassName() + "-" + trackingClass.getSchoolYear()
                + "-HK" + semesterNumber;
        Sheet sheet = workbook.createSheet(uniqueSheetName(baseName, usedNames));
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 7);

        Row titleRow = sheet.createRow(2);
        Cell title = titleRow.createCell(0);
        title.setCellValue("SỔ THEO DÕI HỌC SINH LỚP " + trackingClass.getClassName()
                + " NĂM HỌC " + trackingClass.getSchoolYear());
        title.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 9));

        Row semesterRow = sheet.createRow(3);
        Cell semester = semesterRow.createCell(0);
        semester.setCellValue("Học kỳ " + semesterNumber);
        semester.setCellStyle(styles.semester);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 9));

        int nextRow = 4;
        for (int month : trackingService.monthsForSemester(semesterNumber)) {
            TrackingSheetForm form = trackingService.getSheet(trackingClass.getId(), semesterNumber, month);
            nextRow = writeMonthTable(sheet, form, month, nextRow, styles);
        }
        setColumnWidths(sheet);
    }

    private int writeMonthTable(Sheet sheet, TrackingSheetForm form, int month,
                                int startRow, WorkbookStyles styles) {
        Row monthRow = sheet.createRow(startRow);
        Cell monthCell = monthRow.createCell(0);
        monthCell.setCellValue("THÁNG " + month);
        monthCell.setCellStyle(styles.semester);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 9));

        int parentHeaderIndex = startRow + 1;
        int childHeaderIndex = startRow + 2;
        int firstStudentRow = startRow + 3;
        Row parentHeader = sheet.createRow(parentHeaderIndex);
        Row childHeader = sheet.createRow(childHeaderIndex);
        parentHeader.setHeightInPoints(28);
        childHeader.setHeightInPoints(28);
        for (int column = 0; column < PARENT_HEADERS.length; column++) {
            Cell parentCell = parentHeader.createCell(column);
            parentCell.setCellValue(PARENT_HEADERS[column]);
            parentCell.setCellStyle(styles.header);
            Cell childCell = childHeader.createCell(column);
            childCell.setCellStyle(styles.header);
        }
        childHeader.getCell(2).setCellValue("ĐGTX 1");
        childHeader.getCell(3).setCellValue("ĐGTX 2");
        sheet.addMergedRegion(new CellRangeAddress(parentHeaderIndex, parentHeaderIndex, 2, 3));
        for (int column : new int[] {0, 1, 4, 5, 6, 7, 8, 9}) {
            sheet.addMergedRegion(new CellRangeAddress(parentHeaderIndex, childHeaderIndex, column, column));
        }

        List<TrackingRowForm> rows = form.getRows() == null ? List.of() : form.getRows();
        for (int index = 0; index < rows.size(); index++) {
            writeStudentRow(sheet.createRow(index + firstStudentRow), rows.get(index), index + 1, styles);
        }
        addAttendanceValidation(sheet, firstStudentRow, rows.size());
        return firstStudentRow + rows.size() + 2;
    }

    private void addAttendanceValidation(Sheet sheet, int firstRowIndex, int studentCount) {
        if (studentCount == 0) {
            return;
        }
        String[] options = Stream.of(AttendanceStatus.values())
                .map(AttendanceStatus::getDisplayName)
                .toArray(String[]::new);
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        int lastRowIndex = firstRowIndex + studentCount - 1;
        DataValidation validation = helper.createValidation(
                constraint, new CellRangeAddressList(firstRowIndex, lastRowIndex, 8, 8));
        validation.setEmptyCellAllowed(true);
        // XSSF maps true to showDropDown=false; Excel displays the arrow only with that OOXML value.
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Điểm danh không hợp lệ", "Hãy chọn một trạng thái trong danh sách.");
        sheet.addValidationData(validation);
    }

    private void writeStudentRow(Row excelRow, TrackingRowForm row, int order, WorkbookStyles styles) {
        setNumber(excelRow.createCell(0), order, styles.center);
        setText(excelRow.createCell(1), row.getFullName(), styles.text);
        setDecimal(excelRow.createCell(2), row.getRegularScore1(), styles.score);
        setDecimal(excelRow.createCell(3), row.getRegularScore2(), styles.score);
        setDecimal(excelRow.createCell(4), row.getMidtermScore(), styles.score);
        setDecimal(excelRow.createCell(5), row.getFinalScore(), styles.score);
        setDecimal(excelRow.createCell(6), row.getAverageScore(), styles.score);
        setText(excelRow.createCell(7), row.getProgressComment(), styles.comment);
        setText(excelRow.createCell(8), row.getAttendanceStatus() == null
                ? "" : row.getAttendanceStatus().getDisplayName(), styles.center);
        setText(excelRow.createCell(9), row.getAttendanceDates(), styles.center);
    }

    private void setNumber(Cell cell, int value, CellStyle style) {
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setDecimal(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void setText(Cell cell, String value, CellStyle style) {
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String uniqueSheetName(String requestedName, Set<String> usedNames) {
        String safeName = WorkbookUtil.createSafeSheetName(requestedName);
        safeName = safeName.substring(0, Math.min(31, safeName.length()));
        String candidate = safeName;
        int suffix = 2;
        while (!usedNames.add(candidate.toLowerCase())) {
            String marker = "-" + suffix++;
            candidate = safeName.substring(0, Math.min(31 - marker.length(), safeName.length())) + marker;
        }
        return candidate;
    }

    private void setColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 28 * 256);
        for (int column = 2; column <= 6; column++) {
            sheet.setColumnWidth(column, 12 * 256);
        }
        sheet.setColumnWidth(7, 55 * 256);
        sheet.setColumnWidth(8, 20 * 256);
        sheet.setColumnWidth(9, 28 * 256);
    }

    private static class WorkbookStyles {
        private final CellStyle title;
        private final CellStyle semester;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle center;
        private final CellStyle score;
        private final CellStyle comment;

        private WorkbookStyles(XSSFWorkbook workbook) {
            title = workbook.createCellStyle();
            title.setAlignment(HorizontalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title.setFont(titleFont);

            semester = workbook.createCellStyle();
            semester.setAlignment(HorizontalAlignment.CENTER);
            Font semesterFont = workbook.createFont();
            semesterFont.setBold(true);
            semester.setFont(semesterFont);

            header = bordered(workbook);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);
            header.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            header.setFont(headerFont);

            text = bordered(workbook);
            center = bordered(workbook);
            center.setAlignment(HorizontalAlignment.CENTER);
            score = bordered(workbook);
            score.setAlignment(HorizontalAlignment.CENTER);
            score.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            comment = bordered(workbook);
            comment.setWrapText(true);
            comment.setVerticalAlignment(VerticalAlignment.TOP);
        }

        private static CellStyle bordered(XSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }
    }
}
