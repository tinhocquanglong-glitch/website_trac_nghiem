package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Excel;
import com.tttn.webthitracnghiem.model.Question;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface IUploadExcel {
    String upload(Excel excel);
    List<Question> readExcel(String fileName) throws IOException;
    boolean checkExcel(Excel excel) throws IOException;
    void delete(String fileName) throws FileNotFoundException;
}
