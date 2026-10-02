package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class")
public class ClassApiController {
    @Autowired
    private IClassesService classesService;

    @GetMapping("/{id}")
    public ResponseEntity<List<Classes>> getClass(@PathVariable Integer id) {
        return ResponseEntity.ok(classesService.findAllBySubject(id));
    }

    @PostMapping
    public ResponseEntity<Classes> save(@ModelAttribute Classes classes) {
        classesService.save(classes);
        return ResponseEntity.ok(classes);
    }

    @PostMapping("/list")
    public ResponseEntity<List<Classes>> save(@RequestBody List<Classes> classes) {
        for (int i = 0; i < classes.size(); i++) {
            classesService.save(classes.get(i));
        }
        return ResponseEntity.ok(classes);
    }
}
