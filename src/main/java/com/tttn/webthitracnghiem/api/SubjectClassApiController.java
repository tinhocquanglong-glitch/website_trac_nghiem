package com.tttn.webthitracnghiem.api;


import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/subjectClasses")
public class SubjectClassApiController {

    @Autowired
    ISubjectClassService subjectClassService;
    @Autowired
    IClassesService classesService;
    @Autowired
    ISubjectService subjectService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@RequestParam("classId") Integer classId,
                                  @RequestParam("subjectIds") List<Integer> subjectIds) {
        Classes classes = classesService.findById(classId);
        if (classes == null || subjectIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Lớp học hoặc danh sách môn học không hợp lệ.");
        }

        List<Subject> subjects = new ArrayList<>();
        for (Integer subjectId : subjectIds) {
            if (subjectId == null) {
                return ResponseEntity.badRequest().body("Mã môn học không hợp lệ.");
            }
            Subject subject = subjectService.findById(subjectId);
            if (subject == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy môn học.");
            }
            subjects.add(subject);
        }

        for (Subject subject : subjects) {
            SubjectClasses existing = subjectClassService.findByClassAndSubject(classes.getId(), subject.getId());
            if (existing.getId() == 0) {
                subjectClassService.save(new SubjectClasses(classes, subject));
            }
        }
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{classId}/{subjectId}")
    public ResponseEntity<SubjectClasses> getAll(@PathVariable("classId") Integer classId,
                                                       @PathVariable("subjectId") Integer subjectId){
        return ResponseEntity.ok(subjectClassService.findByClassAndSubject(classId,subjectId));
    }
}
