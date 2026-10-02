package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "lesson")
public class Lesson implements Comparable<Lesson> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nameLesson;
    private int serial;
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private Set<Question> questions;
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private Set<Exam> exams;
    @ManyToOne
    @JoinColumn(name = "chapter_id",referencedColumnName = "id")
    @JsonBackReference
    private Chapter chapter;

    public Lesson() {
    }

    public Lesson(int id, String nameLesson, int serial, Set<Question> questions, Set<Exam> exams, Chapter chapter) {
        this.id = id;
        this.nameLesson = nameLesson;
        this.serial = serial;
        this.questions = questions;
        this.exams = exams;
        this.chapter = chapter;
    }

    public int getId() {
        return id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNameLesson() {
        return nameLesson;
    }

    public void setNameLesson(String nameLesson) {
        this.nameLesson = nameLesson;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }

    public Set<Exam> getExams() {
        return exams;
    }

    public void setExams(Set<Exam> exams) {
        this.exams = exams;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    @Override
    public int compareTo(Lesson o) {
        return this.getSerial() - o.getSerial();
    }
}
