package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.ExamRequest;
import com.tttn.webthitracnghiem.service.IExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam")
public class ExamApiController {
    @Autowired
    private IExamService examService;

    @GetMapping
    public ResponseEntity<List<Exam>> get(){
        return ResponseEntity.ok(examService.getAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Exam> save(@ModelAttribute Exam exam) {
        if (exam.isFree()) {
            exam.setTime(0);
        }
        examService.save(exam);
        return ResponseEntity.ok(exam);
    }
    @PostMapping(value = "/excel",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@RequestBody ExamRequest examRequest){
        System.out.println(examRequest);
        return ResponseEntity.ok(examService.save(examRequest));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestParam Integer id){
        Exam exam = examService.findById(id);
        examService.delete(exam);
        return ResponseEntity.ok(exam);
    }
}
