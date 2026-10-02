package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Excel;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.service.IFileService;
import com.tttn.webthitracnghiem.service.IUploadExcel;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class UploadExcelImpl implements IUploadExcel {
    public static final int COLUMN_INDEX_TITLE = 0;
    public static final int COLUMN_INDEX_LEVEL = 1;
    public static final int COLUMN_INDEX_OPTION_1 = 2;
    public static final int COLUMN_INDEX_OPTION_2 = 3;
    public static final int COLUMN_INDEX_OPTION_3 = 4;
    public static final int COLUMN_INDEX_OPTION_4 = 5;
    public static final int COLUMN_INDEX_ANSWER = 6;
    @Autowired
    private IFileService fileService;

    @Override
    public String upload(Excel excel) {
        String realUrl = new File(".").getAbsolutePath();
        // Thêm file mới được chọn
        String pathFile = realUrl + "\\src\\main\\resources\\static\\excel";
        // Đọc file
        String fileName = excel.getFile().getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        String newFileName = fileName.substring(0, fileName.lastIndexOf(".")) + System.currentTimeMillis() + '.' + extension;
        File folder = new File(pathFile);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File fileSaved = new File(folder.getAbsolutePath() + File.separator + newFileName);
        FileOutputStream fos;
        BufferedOutputStream bs;
        try {
            fos = new FileOutputStream(fileSaved);
            bs = new BufferedOutputStream(fos);
            bs.write(excel.getFile().getBytes());
            fos.close();
            bs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File targetAvatar = new File(realUrl + "\\target\\classes\\static\\excel\\" + fileSaved.getName());
        fileService.copyFile(fileSaved, targetAvatar);
        return fileSaved.getAbsolutePath();
    }

    @Override
    public List<Question> readExcel(String fileName) throws IOException {
        List<Question> questions = new ArrayList<>();
        InputStream inputStream = new FileInputStream(fileName);
        Workbook workbook = getWorkbook(inputStream, fileName);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = sheet.iterator();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            if (row.getRowNum() <= 1) {
                continue;
            }
            Iterator<Cell> cellIterator = row.cellIterator();
            Question question = new Question();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                Object cellValue = getCellValue(cell);
                if (cellValue == null || cellValue.toString().isEmpty()) {
                    continue;
                }
                int columnIndex = cell.getColumnIndex();
                switch (columnIndex) {
                    case COLUMN_INDEX_TITLE:
                        question.setTitle(cellValue.toString());
                        break;
                    case COLUMN_INDEX_LEVEL:
                        question.setLevel(new BigDecimal(Double.parseDouble(cellValue.toString())).intValue());
                        break;
                    case COLUMN_INDEX_OPTION_1:
                        question.setOptionA(cellValue.toString());
                        break;
                    case COLUMN_INDEX_OPTION_2:
                        question.setOptionB(cellValue.toString());
                        break;
                    case COLUMN_INDEX_OPTION_3:
                        question.setOptionC(cellValue.toString());
                        break;
                    case COLUMN_INDEX_OPTION_4:
                        question.setOptionD(cellValue.toString());
                        break;
                    case COLUMN_INDEX_ANSWER:
                        question.setAns(new BigDecimal(Double.parseDouble(cellValue.toString())).intValue());
                        break;
                    default:
                        break;
                }
            }
            questions.add(question);
        }
        workbook.close();
        inputStream.close();
        return questions;
    }

    @Override
    public boolean checkExcel(Excel excel) throws IOException {
        Workbook workbook;
        if(excel.getFile().getOriginalFilename().endsWith("xlsx")){
            workbook = new XSSFWorkbook(excel.getFile().getInputStream());
        } else {
            workbook = new HSSFWorkbook(excel.getFile().getInputStream());
        }
        Sheet sheet = workbook.getSheetAt(0);
        if(sheet.getRow(0).getLastCellNum() != COLUMN_INDEX_ANSWER + 1) {
            return false;
        }
        return checkCell(sheet);
    }

    private boolean checkCell(Sheet sheet) {
        for(int i = 2; i<sheet.getLastRowNum();i++){
            Cell cell1 = sheet.getRow(i).getCell(COLUMN_INDEX_LEVEL);
            double cellValue1 = Double.parseDouble(getCellValue(cell1).toString());
            int level = new BigDecimal(cellValue1).intValue();
            if(level < 1 || level > 3){
                return false;
            }
            Cell cell2 = sheet.getRow(i).getCell(COLUMN_INDEX_ANSWER);
            double cellValue2 = Double.parseDouble(getCellValue(cell2).toString());
            int ans = new BigDecimal((double) cellValue2).intValue();
            if(ans < 1 || ans > 4){
                return false;
            }
        }
        return true;
    }

    @Override
    public void delete(String fileName) throws FileNotFoundException {
        File file = new File(fileName);
        file.delete();
    }

    private static Object getCellValue(Cell cell) {
        CellType cellType = cell.getCellType();
        Object cellValue = null;
        switch (cellType) {
            case BOOLEAN:
                cellValue = cell.getBooleanCellValue();
                break;
            case FORMULA:
                Workbook workbook = cell.getSheet().getWorkbook();
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                cellValue = evaluator.evaluate(cell).getNumberValue();
                break;
            case NUMERIC:
                DecimalFormat format = new DecimalFormat("0.#");
                cellValue = format.format(cell.getNumericCellValue());
                break;
            case STRING:
                cellValue = cell.getStringCellValue();
                break;
            case _NONE:
            case BLANK:
            case ERROR:
                break;
            default:
                break;
        }
        return cellValue;
    }

    private static Workbook getWorkbook(InputStream inputStream, String excelFilePath) throws IOException {
        Workbook workbook = null;
        if (excelFilePath.endsWith("xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else if (excelFilePath.endsWith("xls")) {
            workbook = new HSSFWorkbook(inputStream);
        }
        return workbook;
    }
}
