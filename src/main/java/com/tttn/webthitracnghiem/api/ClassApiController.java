package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/class")
public class ClassApiController {
    @Autowired
    private IClassesService classesService;

    @GetMapping("/{id}")
    public ResponseEntity<List<ClassOption>> getClass(@PathVariable Integer id) {
        List<ClassOption> options = new ArrayList<>();
        for (Classes classes : classesService.findAllBySubject(id)) {
            options.add(new ClassOption(classes.getId(), classes.getNameClass(), classes.getGrade(),
                    classes.getDisplayName()));
        }
        return ResponseEntity.ok(options);
    }

    /** Lightweight response for dependent selects; do not serialize the curriculum graph here. */
    public static class ClassOption {
        public final Integer id;
        public final String nameClass;
        public final Integer grade;
        public final String displayName;

        public ClassOption(Integer id, String nameClass, Integer grade, String displayName) {
            this.id = id;
            this.nameClass = nameClass;
            this.grade = grade;
            this.displayName = displayName;
        }
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
