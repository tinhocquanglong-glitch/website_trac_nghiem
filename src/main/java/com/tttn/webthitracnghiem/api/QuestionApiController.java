package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/question")
public class QuestionApiController {
    @Autowired
    private IQuestionService questionService;
    @Autowired
    private IExamService examService;

    @GetMapping
    public ResponseEntity<List<Question>> getAll(){
        return ResponseEntity.ok(questionService.getAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Question> save(@ModelAttribute Question question){
        questionService.save(question);
        return ResponseEntity.ok(question);
    }
    @GetMapping("/{id}")
    public ResponseEntity<List<Question>> getByLesson(@PathVariable Integer id){
        return ResponseEntity.ok(questionService.findByLessonId(id));
    }
    @GetMapping("/list")
    public ResponseEntity<List<Question>> getByExam(@RequestParam("exId") Integer id){
        return ResponseEntity.ok(questionService.findByExam(id));
    }
    @GetMapping("/getBySubAndClass/{sub}/{classes}")
    public ResponseEntity<?> getBy(@PathVariable Optional<Integer> sub, @PathVariable Optional<Integer> classes){
        if(classes.isPresent()){
            return ResponseEntity.ok(questionService.findBySubAndClass(sub.get(),classes.get()));
        } else {
            return ResponseEntity.ok(questionService.findBySub(sub.get()));
        }
    }
    @DeleteMapping(value = "/{id}",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> remove(@PathVariable Integer id){
        Question question = questionService.findById(id);
        for(Exam exam : question.getExams()){
            if(exam.getQuestions().size() == 1){
                examService.delete(exam);
                continue;
            }
            List<Question> questions = exam.getQuestions();
            questions.remove(question);
            exam.setQuestions(questions);
        }
        questionService.delete(question);
        return ResponseEntity.ok(id);
    }
}
