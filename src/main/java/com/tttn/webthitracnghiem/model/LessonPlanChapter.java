package com.tttn.webthitracnghiem.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_plan_custom_chapter")
public class LessonPlanChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_plan_id", nullable = false)
    private LessonPlan lessonPlan;

    private String name;
    private int displayOrder;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<LessonPlanLesson> lessons = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_chapter_id")
    private Chapter curriculumChapter;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LessonPlan getLessonPlan() { return lessonPlan; }
    public void setLessonPlan(LessonPlan lessonPlan) { this.lessonPlan = lessonPlan; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<LessonPlanLesson> getLessons() { return lessons; }
    /** Updates the list in place: Hibernate rejects replacing an orphan-removal collection on a loaded chapter. */
    public void setLessons(List<LessonPlanLesson> lessons) {
        if (lessons == this.lessons) return;
        List<LessonPlanLesson> copy = lessons == null ? new ArrayList<>() : new ArrayList<>(lessons);
        if (this.lessons == null) this.lessons = new ArrayList<>();
        this.lessons.clear();
        this.lessons.addAll(copy);
    }
    public Chapter getCurriculumChapter() { return curriculumChapter; }
    public void setCurriculumChapter(Chapter curriculumChapter) { this.curriculumChapter = curriculumChapter; }
}
