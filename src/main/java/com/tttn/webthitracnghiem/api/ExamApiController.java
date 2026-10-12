package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.ExamRequest;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.service.IExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam")
public class ExamApiController {
    @Autowired
    private IExamService examService;

    @Autowired
    private SchoolRepository schoolRepository;

    @GetMapping
    public ResponseEntity<List<Exam>> get(){
        return ResponseEntity.ok(examService.getAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Exam> save(@ModelAttribute Exam exam,
                                     @RequestParam(required = false) Integer school,
                                     @RequestParam(required = false) Boolean isPublic) {
        if (exam.isFree()) {
            exam.setTime(0);
        }
        // Gán isPublic
        if (isPublic != null) {
            exam.setIsPublic(isPublic);
        } else if (exam.getIsPublic() == null) {
            exam.setIsPublic(true);
        }
        // Gán school nếu được truyền vào
        if (school != null && school > 0) {
            schoolRepository.findById(school).ifPresent(exam::setSchool);
        }
        // Tự động gán người tạo nếu chưa có
        if (exam.getUsers() == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // users được bind qua ModelAttribute nên thường đã có,
                // chỉ fallback để phòng trường hợp thiếu
            }
        }
        examService.save(exam);
        return ResponseEntity.ok(exam);
    }

    @PostMapping(value = "/excel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@RequestBody ExamRequest examRequest){
        System.out.println(examRequest);
        Exam saved = examService.save(examRequest);
        // Gán isPublic và school từ ExamRequest
        if (examRequest.getIsPublic() != null) {
            saved.setIsPublic(examRequest.getIsPublic());
        }
        if (examRequest.getSchoolId() != null && examRequest.getSchoolId() > 0) {
            schoolRepository.findById(examRequest.getSchoolId()).ifPresent(saved::setSchool);
        }
        examService.save(saved);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestParam Integer id){
        Exam exam = examService.findById(id);
        examService.delete(exam);
        return ResponseEntity.ok(exam);
    }
}
