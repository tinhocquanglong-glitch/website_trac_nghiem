package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.User;
import org.springframework.web.bind.WebDataBinder;
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
    @Autowired
    private com.tttn.webthitracnghiem.service.CurriculumService curriculumService;
    @Autowired
    private com.tttn.webthitracnghiem.service.ExamScopeService examScopeService;
    @Autowired
    private com.tttn.webthitracnghiem.service.IUserService userService;
    @Autowired
    private com.tttn.webthitracnghiem.repository.SubjectRepository subjectRepository;
    @Autowired
    private com.tttn.webthitracnghiem.repository.ClassesRepository classesRepository;

    @GetMapping
    public ResponseEntity<List<Question>> getAll(){
        return ResponseEntity.ok(questionService.getAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> save(@ModelAttribute Question question,
                                  @RequestParam(value = "creationOption", required = false, defaultValue = "1") Integer creationOption,
                                  @RequestParam(value = "lesson", required = false) Integer lessonId,
                                  @RequestParam(value = "lessonPlanLessonId", required = false) Integer lessonPlanLessonId,
                                  @RequestParam(value = "subjects", required = false) Integer subjectId,
                                  @RequestParam(value = "classId", required = false) Integer classId){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User author = auth == null ? null : userService.findById(auth.getName());
        boolean admin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        Integer schoolId = curriculumService.schoolIdOf(author);

        question.setSource(Question.SOURCE_MANUAL);
        question.setReviewStatus(Question.REVIEW_APPROVED);
        question.setAuthor(author);
        if (question.getSchool() == null && author != null) {
            question.setSchool(author.getSchool());
        }
        if (question.getIsPublic() == null) {
            question.setIsPublic(true);
        }

        // Option 3: Lấy theo giáo án của mình (KHBD)
        if (lessonPlanLessonId != null) {
            return lessonPlanLessonRepository.findById(lessonPlanLessonId).map(customLesson -> {
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
                if (customLesson.getChapter() != null && customLesson.getChapter().getLessonPlan() != null) {
                    question.setSubject(customLesson.getChapter().getLessonPlan().getSubject());
                    question.setClasses(customLesson.getChapter().getLessonPlan().getGrade());
                }
                questionService.save(question);
                return ResponseEntity.ok(question);
            }).orElseGet(() -> ResponseEntity.notFound().build());
        }

        // Option 2: Lấy theo khung chương trình
        if (lessonId != null) {
            return lessonRepository.findById(lessonId).map(lesson -> {
                if (!admin && lesson.getChapter() != null && lesson.getChapter().getSubjectClasses() != null
                        && !curriculumService.belongsTo(lesson, schoolId, lesson.getChapter().getSubjectClasses())) {
                    return ResponseEntity.status(403).body("Bài học không thuộc chương trình học của trường bạn.");
                }
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
                question.setLesson(lesson);
                question.setLessonPlanLesson(null);
                if (lesson.getChapter() != null && lesson.getChapter().getSubjectClasses() != null) {
                    question.setSubject(lesson.getChapter().getSubjectClasses().getSubject());
                    question.setClasses(lesson.getChapter().getSubjectClasses().getClasses());
                }
                questionService.save(question);
                return ResponseEntity.ok(question);
            }).orElseGet(() -> ResponseEntity.notFound().build());
        }

        // Option 1: Chỉ chọn tên môn học
        if (subjectId == null) {
            return ResponseEntity.badRequest().body("Vui lòng chọn môn học cho câu hỏi.");
        }
        com.tttn.webthitracnghiem.model.Subject subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy môn học đã chọn.");
        }
        question.setSubject(subject);
        if (classId != null) {
            com.tttn.webthitracnghiem.model.Classes cls = classesRepository.findById(classId).orElse(null);
            question.setClasses(cls);
        }
        question.setLesson(null);
        question.setLessonPlanLesson(null);
        questionService.save(question);
        return ResponseEntity.ok(question);
    }

    /** Approved questions an exam over these curriculum lessons can use (see ExamScopeService). */
    @GetMapping("/pool")
    public ResponseEntity<?> pool(@RequestParam("lessonIds") List<Integer> lessonIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = auth == null ? null : userService.findById(auth.getName());
        try {
            List<PoolItem> items = new java.util.ArrayList<>();
            for (Question question : examScopeService.questionPool(lessonIds, user)) {
                items.add(new PoolItem(question));
            }
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @InitBinder("question")
    public void protectServerManagedFields(WebDataBinder binder) {
        binder.setDisallowedFields("author", "source", "reviewStatus", "sourcePlanLessonId");
    }

    /** Flat view of a pool question with its curriculum place, for the exam form table. */
    public static class PoolItem {
        public final int quesId;
        public final String title;
        public final String optionA;
        public final String optionB;
        public final String optionC;
        public final String optionD;
        public final int ans;
        public final int level;
        public final Integer lessonId;
        public final String lessonName;
        public final Integer chapterId;
        public final String chapterName;
        public final boolean shared;

        PoolItem(Question question) {
            this.quesId = question.getQuesId();
            this.title = question.getTitle();
            this.optionA = question.getOptionA();
            this.optionB = question.getOptionB();
            this.optionC = question.getOptionC();
            this.optionD = question.getOptionD();
            this.ans = question.getAns();
            this.level = question.getLevel();
            // lesson may be null for questions created with Option 1 (subject-only)
            if (question.getLesson() != null) {
                this.lessonId = question.getLesson().getId();
                this.lessonName = question.getLesson().getNameLesson();
                this.chapterId = question.getLesson().getChapter() != null ? question.getLesson().getChapter().getId() : null;
                this.chapterName = question.getLesson().getChapter() != null ? question.getLesson().getChapter().getChapterName() : null;
                this.shared = question.getLesson().getChapter() != null
                        && question.getLesson().getChapter().getSchool() != null
                        && question.getSchool() != null
                        && !question.getSchool().getId().equals(question.getLesson().getChapter().getSchool().getId());
            } else {
                this.lessonId = null;
                this.lessonName = null;
                this.chapterId = null;
                this.chapterName = null;
                this.shared = false;
            }
        }
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
