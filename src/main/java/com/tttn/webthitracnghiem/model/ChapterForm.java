package com.tttn.webthitracnghiem.model;

public class ChapterForm {
    private int id;
    private String chapterName;

    public ChapterForm(int id, String chapterName) {
        this.id = id;
        this.chapterName = chapterName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ChapterForm(String chapterName) {
        this.chapterName = chapterName;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }
}
