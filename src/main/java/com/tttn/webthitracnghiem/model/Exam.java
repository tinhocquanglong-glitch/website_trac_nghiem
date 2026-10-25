package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.Date;
import java.util.List;

@Entity
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank
    @Pattern(regexp = "^MD-[\\d]{4}$",message = "Mã đề thì phải là định dạng MD-XXXX(XXXX là số)")
    @Column(unique = true)
    private String idName;
    @Column
    private boolean free = true;
    private int time;
    private Date createDate;
    private Date lastModify;
    @NotBlank(message = "Tên đề thi không được bỏ trống")
    private String nameExam;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonBackReference
    private User users;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private List<Result> results;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id",referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonBackReference
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_plan_lesson_id")
    @JsonIgnore
    private LessonPlanLesson lessonPlanLesson;
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "exam_question",
            joinColumns = @JoinColumn(name = "examId"),
            inverseJoinColumns = @JoinColumn(name = "quesId"))
    @JsonBackReference
    private List<Question> questions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private School school;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "target_type", length = 20)
    private String targetType = "PUBLIC"; // PUBLIC, SCHOOL, GRADE, CLASS

    @Column(name = "target_grade")
    private Integer targetGrade; // 1 to 12

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_class_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private StudentTrackingClass targetClass;

    @Column(name = "start_time")
    private java.sql.Timestamp startTime;

    @Column(name = "end_time")
    private java.sql.Timestamp endTime;

    @Column(name = "score_type", length = 30)
    private String scoreType; // REGULAR_1, REGULAR_2, MIDTERM, FINAL

    @Column(name = "is_practice", nullable = false)
    private boolean practice = false;

    @Column(name = "grade_month")
    private Integer gradeMonth;

    // Curriculum scope: subject, grade and every lesson the exam covers. `lesson` stays the first scope lesson.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnore
    private Subject subject;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @ManyToMany
    @JoinTable(name = "exam_scope_lesson",
            joinColumns = @JoinColumn(name = "exam_id"),
            inverseJoinColumns = @JoinColumn(name = "lesson_id"))
    @JsonIgnore
    private java.util.Set<Lesson> scopeLessons = new java.util.LinkedHashSet<>();

    private long view;
    private long turn;

    public Exam() {
    }

    public Exam(int id, String idName, boolean free, int time, Date createDate, Date lastModify, String nameExam,
                User users, List<Result> results, Lesson lesson, List<Question> questions,
                long view, long turn) {
        this.id = id;
        this.idName = idName;
        this.free = free;
        this.time = time;
        this.createDate = createDate;
        this.lastModify = lastModify;
        this.nameExam = nameExam;
        this.users = users;
        this.results = results;
        this.lesson = lesson;
        this.questions = questions;
        this.view = view;
        this.turn = turn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public LessonPlanLesson getLessonPlanLesson() {
        return lessonPlanLesson;
    }

    public void setLessonPlanLesson(LessonPlanLesson lessonPlanLesson) {
        this.lessonPlanLesson = lessonPlanLesson;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public String getNameExam() {
        return nameExam;
    }

    public void setNameExam(String nameExam) {
        this.nameExam = nameExam;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }


    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public long getView() {
        return view;
    }

    public void setView(long view) {
        this.view = view;
    }

    public long getTurn() {
        return turn;
    }

    public void setTurn(long turn) {
        this.turn = turn;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetGrade() {
        return targetGrade;
    }

    public void setTargetGrade(Integer targetGrade) {
        this.targetGrade = targetGrade;
    }

    public StudentTrackingClass getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(StudentTrackingClass targetClass) {
        this.targetClass = targetClass;
    }

    public java.sql.Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(java.sql.Timestamp startTime) {
        this.startTime = startTime;
    }

    public java.sql.Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(java.sql.Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public boolean isPractice() {
        return practice;
    }

    public void setPractice(boolean practice) {
        this.practice = practice;
    }

    public Integer getGradeMonth() { return gradeMonth; }
    public void setGradeMonth(Integer gradeMonth) { this.gradeMonth = gradeMonth; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Integer getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
    public java.util.Set<Lesson> getScopeLessons() { return scopeLessons; }
    public void setScopeLessons(java.util.Set<Lesson> scopeLessons) { this.scopeLessons = scopeLessons; }
}

