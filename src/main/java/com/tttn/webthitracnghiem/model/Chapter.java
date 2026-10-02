package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "chapter")
public class Chapter implements Comparable<Chapter>  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String chapterName;
    private int serial;
    @OneToMany(mappedBy = "chapter")
    @JsonManagedReference
    private Set<Lesson> lessons;
    @ManyToOne
    @JoinColumn(name = "subject_class_id",referencedColumnName = "id")
    @JsonBackReference
    private SubjectClasses subjectClasses;

    public Chapter() {
    }

    public Chapter(int id, String chapterName, int serial, Set<Lesson> lessons, SubjectClasses subjectClasses) {
        this.id = id;
        this.chapterName = chapterName;
        this.serial = serial;
        this.lessons = lessons;
        this.subjectClasses = subjectClasses;
    }

    public int getId() {
        return id;
    }

    public SubjectClasses getSubjectClasses() {
        return subjectClasses;
    }

    public void setSubjectClasses(SubjectClasses subjectClasses) {
        this.subjectClasses = subjectClasses;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public Set<Lesson> getLessons() {
        return lessons;
    }

    public void setLessons(Set<Lesson> lessons) {
        this.lessons = lessons;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    @Override
    public int compareTo(Chapter o) {
        return this.getSerial() - o.getSerial();
    }
}
