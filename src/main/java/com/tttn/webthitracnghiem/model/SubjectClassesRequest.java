package com.tttn.webthitracnghiem.model;

import java.util.List;

public class SubjectClassesRequest {

    private Classes classes;
    private List<Subject> subjects;

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public Classes getClasses() {
        return classes;
    }

    public void setClasses(Classes classes) {
        this.classes = classes;
    }
}
