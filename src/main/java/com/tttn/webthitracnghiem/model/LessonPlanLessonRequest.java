package com.tttn.webthitracnghiem.model;

import org.springframework.web.multipart.MultipartFile;

public class LessonPlanLessonRequest {
    private Integer id;
    private String name;
    private String content;
    private String attachmentOriginalName;
    private MultipartFile attachment;

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
