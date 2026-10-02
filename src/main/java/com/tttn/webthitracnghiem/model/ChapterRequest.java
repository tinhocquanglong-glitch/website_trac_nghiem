package com.tttn.webthitracnghiem.model;

import java.util.List;

public class ChapterRequest {

    private List<ChapterForm> chapters;
    private SubjectClasses subjectClasses;

    public SubjectClasses getSubjectClasses() {
        return subjectClasses;
    }

    public void setSubjectClasses(SubjectClasses subjectClasses) {
        this.subjectClasses = subjectClasses;
    }
    public List<ChapterForm> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterForm> chapters) {
        this.chapters = chapters;
    }
}
