package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;


@Entity
public class Classes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private  String nameClass;
    @OneToMany(mappedBy = "classes")
    @JsonManagedReference
    private Set<SubjectClasses> subjectClasses;

    public Classes() {
    }

    public Classes(Integer id, String nameClass) {
        this.id = id;
        this.nameClass = nameClass;
    }

    public Classes(Integer id, String nameClass, Set<SubjectClasses> subjectClasses) {
        this.id = id;
        this.nameClass = nameClass;
        this.subjectClasses = subjectClasses;
    }

    public Set<SubjectClasses> getSubjectClasses() {
        return subjectClasses;
    }

    public void setSubjectClasses(Set<SubjectClasses> subjectClasses) {
        this.subjectClasses = subjectClasses;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNameClass() {
        return nameClass;
    }

    public void setNameClass(String nameClass) {
        this.nameClass = nameClass;
    }

}
