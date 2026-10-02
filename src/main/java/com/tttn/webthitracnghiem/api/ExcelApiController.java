package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Excel;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.service.IUploadExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/excel")
public class ExcelApiController {
    @Autowired
    private IUploadExcel uploadExcel;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@ModelAttribute Excel excel){
        boolean checkFile;
        try {
            checkFile = uploadExcel.checkExcel(excel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Question> questions = new ArrayList<>();
        if(checkFile){
            String fileName = uploadExcel.upload(excel);
            try {
                questions = uploadExcel.readExcel(fileName);
                uploadExcel.delete(fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ResponseEntity.ok(questions);
    }
}
