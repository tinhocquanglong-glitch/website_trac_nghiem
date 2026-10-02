package com.tttn.webthitracnghiem.api;


import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.SubjectClassesRequest;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjectClasses")
public class SubjectClassApiController {

    @Autowired
    ISubjectClassService subjectClassService;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@ModelAttribute SubjectClassesRequest subjectClassesRequest){
        for(int i = 0; i<subjectClassesRequest.getSubjects().size();i++){
            SubjectClasses subjectClasses = new SubjectClasses();
            subjectClasses.setClasses(subjectClassesRequest.getClasses());
            subjectClasses.setSubject(subjectClassesRequest.getSubjects().get(i));
            subjectClassService.save(subjectClasses);
        }
        return ResponseEntity.ok(subjectClassesRequest);
    }
    @GetMapping("/{classId}/{subjectId}")
    public ResponseEntity<SubjectClasses> getAll(@PathVariable("classId") Integer classId,
                                                       @PathVariable("subjectId") Integer subjectId){
        return ResponseEntity.ok(subjectClassService.findByClassAndSubject(classId,subjectId));
    }
}
