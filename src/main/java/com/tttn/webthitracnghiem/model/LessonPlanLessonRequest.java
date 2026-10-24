package com.tttn.webthitracnghiem.model;

import org.springframework.web.multipart.MultipartFile;

public class LessonPlanLessonRequest {
    private Integer id;
    private String name;
    private String content;
    private String attachmentOriginalName;
    private MultipartFile attachment;
    // Curriculum lesson this plan lesson teaches (null = not linked yet).
    private Integer curriculumLessonId;
    // When no curriculum lesson matches, add this lesson to the school's curriculum on save.
    private boolean addToCurriculum;
    private Integer periods;
    private String objectives;

    public Integer getCurriculumLessonId() { return curriculumLessonId; }
    public void setCurriculumLessonId(Integer curriculumLessonId) { this.curriculumLessonId = curriculumLessonId; }
    public boolean isAddToCurriculum() { return addToCurriculum; }
    public void setAddToCurriculum(boolean addToCurriculum) { this.addToCurriculum = addToCurriculum; }
    public Integer getPeriods() { return periods; }
    public void setPeriods(Integer periods) { this.periods = periods; }
    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public void setAttachmentOriginalName(String attachmentOriginalName) {
        this.attachmentOriginalName = attachmentOriginalName;
    }
    public MultipartFile getAttachment() { return attachment; }
    public void setAttachment(MultipartFile attachment) { this.attachment = attachment; }
}
