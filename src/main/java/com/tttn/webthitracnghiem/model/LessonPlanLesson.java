package com.tttn.webthitracnghiem.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_plan_custom_lesson")
public class LessonPlanLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private LessonPlanChapter chapter;

    private String name;
    private int displayOrder;

    @javax.persistence.Column(columnDefinition = "text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    private String attachmentOriginalName;
    private String attachmentContentType;
    private Long attachmentSize;

    @Lob
    @javax.persistence.Column(columnDefinition = "longblob")
    private byte[] attachmentData;

    @OneToMany(mappedBy = "lessonPlanLesson", cascade = CascadeType.REMOVE)
    private List<Exam> exams = new ArrayList<>();

    // The curriculum lesson this part of the plan teaches; questions and exams attach there.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_lesson_id")
    private Lesson curriculumLesson;

    private Integer periods;

    @javax.persistence.Column(columnDefinition = "text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String objectives;

    // Plain text of content, kept for search and as input for question generation.
    @javax.persistence.Column(columnDefinition = "text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String contentText;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LessonPlanChapter getChapter() { return chapter; }
    public void setChapter(LessonPlanChapter chapter) { this.chapter = chapter; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public void setAttachmentOriginalName(String attachmentOriginalName) { this.attachmentOriginalName = attachmentOriginalName; }
    public String getAttachmentContentType() { return attachmentContentType; }
    public void setAttachmentContentType(String attachmentContentType) { this.attachmentContentType = attachmentContentType; }
    public Long getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(Long attachmentSize) { this.attachmentSize = attachmentSize; }
    public byte[] getAttachmentData() { return attachmentData; }
    public void setAttachmentData(byte[] attachmentData) { this.attachmentData = attachmentData; }
    public List<Exam> getExams() { return exams; }
    public void setExams(List<Exam> exams) { this.exams = exams; }
    public Lesson getCurriculumLesson() { return curriculumLesson; }
    public void setCurriculumLesson(Lesson curriculumLesson) { this.curriculumLesson = curriculumLesson; }
    public Integer getPeriods() { return periods; }
    public void setPeriods(Integer periods) { this.periods = periods; }
    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
}
