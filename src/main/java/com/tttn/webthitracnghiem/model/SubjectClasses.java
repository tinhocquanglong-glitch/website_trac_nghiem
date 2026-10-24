package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "subject_classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_subject_classes_school",
                columnNames = {"subject_id", "class_id", "school_id"}))
public class SubjectClasses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "class_id", referencedColumnName = "id")
    @JsonBackReference
    private Classes classes;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    @JsonBackReference
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    @JsonIgnore
    private School school;

    @OneToMany(mappedBy = "subjectClasses")
    @JsonManagedReference
    private Set<Chapter> chapters;

    public SubjectClasses() {
    }

    public SubjectClasses(int id, Classes classes, Subject subject, Set<Chapter> chapters) {
        this.id = id;
        this.classes = classes;
        this.subject = subject;
        this.chapters = chapters;
    }

    public SubjectClasses(Classes classes, Subject subject) {
        this.classes = classes;
        this.subject = subject;
    }

    public SubjectClasses(Classes classes, Subject subject, School school) {
        this.classes = classes;
        this.subject = subject;
        this.school = school;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Set<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(Set<Chapter> chapters) {
        this.chapters = chapters;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Classes getClasses() {
        return classes;
    }

    public void setClasses(Classes classes) {
        this.classes = classes;
    }
}
