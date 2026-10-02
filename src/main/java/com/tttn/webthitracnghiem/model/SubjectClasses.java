package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "subject_classes")
public class SubjectClasses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "class_id",referencedColumnName = "id")
    @JsonBackReference
    private Classes classes;
    @ManyToOne
    @JoinColumn(name = "subject_id",referencedColumnName = "id")
    @JsonBackReference
    private Subject subject;
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
