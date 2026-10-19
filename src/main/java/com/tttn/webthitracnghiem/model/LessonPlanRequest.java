package com.tttn.webthitracnghiem.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LessonPlanRequest {
    private Integer id;

    @NotBlank(message = "Tên giáo án không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Vui lòng chọn môn học")
    private Integer subjectId;

    @NotNull(message = "Vui lòng chọn khối")
    private Integer gradeId;

    private Integer classId;

    // Legacy field retained so old records and old service callers can be read safely.
    private Integer subjectClassId;

    private Set<Integer> chapterIds = new LinkedHashSet<>();
    private Set<Integer> lessonIds = new LinkedHashSet<>();
    private Boolean isPublic = false;
    private List<LessonPlanChapterRequest> chapters = new ArrayList<>();

    private MultipartFile documentFile;
    private String documentImportMode = "LESSON_CONTENT";
    private Integer documentChapterIndex;
    private Integer documentLessonIndex;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSubjectClassId() { return subjectClassId; }
    public void setSubjectClassId(Integer subjectClassId) { this.subjectClassId = subjectClassId; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public Integer getGradeId() { return gradeId; }
    public void setGradeId(Integer gradeId) { this.gradeId = gradeId; }
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    public Set<Integer> getChapterIds() { return chapterIds; }
    public void setChapterIds(Set<Integer> chapterIds) { this.chapterIds = chapterIds == null ? new LinkedHashSet<>() : chapterIds; }
    public Set<Integer> getLessonIds() { return lessonIds; }
    public void setLessonIds(Set<Integer> lessonIds) { this.lessonIds = lessonIds == null ? new LinkedHashSet<>() : lessonIds; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public List<LessonPlanChapterRequest> getChapters() { return chapters; }
    public void setChapters(List<LessonPlanChapterRequest> chapters) {
        this.chapters = chapters == null ? new ArrayList<>() : chapters;
    }

    public MultipartFile getDocumentFile() { return documentFile; }
    public void setDocumentFile(MultipartFile documentFile) { this.documentFile = documentFile; }
    public String getDocumentImportMode() { return documentImportMode; }
    public void setDocumentImportMode(String documentImportMode) { this.documentImportMode = documentImportMode; }
    public Integer getDocumentChapterIndex() { return documentChapterIndex; }
    public void setDocumentChapterIndex(Integer documentChapterIndex) { this.documentChapterIndex = documentChapterIndex; }
    public Integer getDocumentLessonIndex() { return documentLessonIndex; }
    public void setDocumentLessonIndex(Integer documentLessonIndex) { this.documentLessonIndex = documentLessonIndex; }
}
