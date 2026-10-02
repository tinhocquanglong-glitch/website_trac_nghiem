package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Component
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int quesId;
    @Column(columnDefinition = "text")
    private String title;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private int ans;
    private int chose;
    private int level;
    @ManyToOne
    @JoinColumn(name = "lesson_id",referencedColumnName = "id")
    @JsonBackReference
    private Lesson lesson;
    @ManyToMany(mappedBy = "questions")
    @JsonManagedReference
    private Set<Exam> exams;

    public Question() {
        super();
    }

    public Question(int quesId, String title, String optionA, String optionB, String optionC, String optionD, int ans, int chose, int level, Lesson lesson, Set<Exam> exams) {
        this.quesId = quesId;
        this.title = title;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.ans = ans;
        this.chose = chose;
        this.level = level;
        this.lesson = lesson;
        this.exams = exams;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public int getQuesId() {
        return quesId;
    }

    public void setQuesId(int quesId) {
        this.quesId = quesId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public int getAns() {
        return ans;
    }

    public void setAns(int ans) {
        this.ans = ans;
    }

    public int getChose() {
        return chose;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public void setChose(int chose) {
        this.chose = chose;
    }

    public Set<Exam> getExams() {
        return exams;
    }

    public void setExams(Set<Exam> exams) {
        this.exams = exams;
    }

}

