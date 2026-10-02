package com.tttn.webthitracnghiem.model;

public class LessonForm {
    private int id;
    private String lessonName;
    private int chapter;

    public LessonForm(int id, String lessonName, int chapter) {
        this.id = id;
        this.lessonName = lessonName;
        this.chapter = chapter;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }

    public String getLessonName() {
        return lessonName;
    }

    public void setLessonName(String lessonName) {
        this.lessonName = lessonName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
