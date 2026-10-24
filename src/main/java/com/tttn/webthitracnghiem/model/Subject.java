package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String image;
    @OneToMany(mappedBy = "subject")
    @JsonManagedReference
    private Set<Document> documents;
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<SubjectClasses> subjectClasses;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    @JsonIgnore
    private School school;

    public Subject() {
    }

    public Subject(Integer id, String name, String image, Set<Document> documents, Set<SubjectClasses> subjectClasses) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.documents = documents;
        this.subjectClasses = subjectClasses;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public boolean isGlobal() {
        return this.school == null;
    }

    public Set<SubjectClasses> getSubjectClasses() {
        return subjectClasses;
    }

    public void setSubjectClasses(Set<SubjectClasses> subjectClasses) {
        this.subjectClasses = subjectClasses;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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

    public Set<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<Document> documents) {
        this.documents = documents;
    }
}
