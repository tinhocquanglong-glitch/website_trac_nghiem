package com.tttn.webthitracnghiem.model;

import java.util.ArrayList;
import java.util.List;

public class LessonPlanChapterRequest {
    private Integer id;
    private String name;
    private List<LessonPlanLessonRequest> lessons = new ArrayList<>();
    private Integer curriculumChapterId;

    public Integer getCurriculumChapterId() { return curriculumChapterId; }
    public void setCurriculumChapterId(Integer curriculumChapterId) { this.curriculumChapterId = curriculumChapterId; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<LessonPlanLessonRequest> getLessons() { return lessons; }
    public void setLessons(List<LessonPlanLessonRequest> lessons) {
        this.lessons = lessons == null ? new ArrayList<>() : lessons;
    }
}
