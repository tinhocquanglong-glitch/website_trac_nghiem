package com.tttn.webthitracnghiem.model;

import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.util.Set;

public class SubjectRequest {
    private Integer id;
    private String name;
    private MultipartFile image;
    private Set<Exam> exams;
    private Set<Question> questions;

    public SubjectRequest() {
    }

    public SubjectRequest(Integer id, String name, MultipartFile image, Set<Exam> exams, Set<Question> questions) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.exams = exams;
        this.questions = questions;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public Set<Exam> getExams() {
        return exams;
    }

    public void setExams(Set<Exam> exams) {
        this.exams = exams;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }
}
