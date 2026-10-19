package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IQuestionService;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/question")
public class QuestionApiController {
    @Autowired
    private IQuestionService questionService;
    @Autowired
    private IExamService examService;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private LessonPlanLessonRepository lessonPlanLessonRepository;
    @Autowired
    private ILessonPlanService lessonPlanService;

    @GetMapping
    public ResponseEntity<List<Question>> getAll(){
        return ResponseEntity.ok(questionService.getAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@ModelAttribute Question question,
                                  @RequestParam(value = "lesson", required = false) Integer lessonId,
                                  @RequestParam(value = "lessonPlanLessonId", required = false) Integer lessonPlanLessonId,
                                  @RequestParam(value = "subjects", required = false) Integer subjectId,
                                  @RequestParam(value = "classId", required = false) Integer classId){
        if (lessonPlanLessonId != null) {
            return lessonPlanLessonRepository.findById(lessonPlanLessonId).map(customLesson -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !lessonPlanService.canView(customLesson.getChapter().getLessonPlan(), auth.getName(), privileged(auth))) {
                    return ResponseEntity.status(403).body("Bạn không có quyền dùng bài học trong giáo án này.");
                }
                if (subjectId != null && (customLesson.getChapter() == null
                        || customLesson.getChapter().getLessonPlan() == null
                        || customLesson.getChapter().getLessonPlan().getSubject() == null
                        || !subjectId.equals(customLesson.getChapter().getLessonPlan().getSubject().getId()))) {
                    return ResponseEntity.badRequest().body("Bài học không thuộc môn học đã chọn.");
                }
                question.setLesson(null);
                question.setLessonPlanLesson(customLesson);
                questionService.save(question);
                return ResponseEntity.ok(question);
            }).orElseGet(() -> ResponseEntity.notFound().build());
        }
        if (lessonId == null) {
            return ResponseEntity.badRequest().body("Bạn phải chọn bài học/chủ đề cho câu hỏi.");
        }
        Question existing = question;
        return lessonRepository.findById(lessonId).map(lesson -> {
            if (subjectId != null && (lesson.getChapter() == null || lesson.getChapter().getSubjectClasses() == null
                    || lesson.getChapter().getSubjectClasses().getSubject() == null
                    || lesson.getChapter().getSubjectClasses().getSubject().getId() != subjectId)) {
                return ResponseEntity.badRequest().body("Bài học không thuộc môn học đã chọn.");
            }
            if (classId != null && (lesson.getChapter() == null || lesson.getChapter().getSubjectClasses() == null
                    || lesson.getChapter().getSubjectClasses().getClasses() == null
                    || lesson.getChapter().getSubjectClasses().getClasses().getId() != classId)) {
                return ResponseEntity.badRequest().body("Bài học không thuộc lớp đã chọn.");
            }
            existing.setLesson(lesson);
            questionService.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}")
    public ResponseEntity<List<Question>> getByLesson(@PathVariable Integer id){
        return ResponseEntity.ok(questionService.findByLessonId(id));
    }

    @GetMapping("/custom/{id}")
    public ResponseEntity<List<Question>> getByCustomLesson(@PathVariable Integer id){
        return ResponseEntity.ok(questionService.findByLessonPlanLessonId(id));
    }

    private boolean privileged(Authentication auth) {
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                || auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
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
