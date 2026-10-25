package com.tttn.webthitracnghiem.model;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "lesson_plan")
public class LessonPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_name", nullable = false, length = 180)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_lesson_plan_owner"),
            columnDefinition = "varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "subject_class_id", nullable = true)
    private SubjectClasses subjectClasses;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "subject_id", nullable = true)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "grade_id", nullable = true)
    private Classes grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_class_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private StudentTrackingClass targetClass;

    @ManyToMany
    @JoinTable(name = "lesson_plan_chapter",
            joinColumns = @JoinColumn(name = "lesson_plan_id"),
            inverseJoinColumns = @JoinColumn(name = "chapter_id"))
    private Set<Chapter> chapters = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "lesson_plan_lesson",
            joinColumns = @JoinColumn(name = "lesson_plan_id"),
            inverseJoinColumns = @JoinColumn(name = "lesson_id"))
    private Set<Lesson> lessons = new LinkedHashSet<>();

    @OneToMany(mappedBy = "lessonPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<LessonPlanChapter> planChapters = new ArrayList<>();

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public SubjectClasses getSubjectClasses() { return subjectClasses; }
    public void setSubjectClasses(SubjectClasses subjectClasses) { this.subjectClasses = subjectClasses; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Classes getGrade() { return grade; }
    public void setGrade(Classes grade) { this.grade = grade; }
    public StudentTrackingClass getTargetClass() { return targetClass; }
    public void setTargetClass(StudentTrackingClass targetClass) { this.targetClass = targetClass; }
    public Set<Chapter> getChapters() { return chapters; }
    public void setChapters(Set<Chapter> chapters) { this.chapters = chapters; }
    public Set<Lesson> getLessons() { return lessons; }
    public void setLessons(Set<Lesson> lessons) { this.lessons = lessons; }
    public List<LessonPlanChapter> getPlanChapters() { return planChapters; }
    /** Updates the list in place: Hibernate rejects replacing an orphan-removal collection on a loaded plan. */
    public void setPlanChapters(List<LessonPlanChapter> planChapters) {
        if (planChapters == this.planChapters) return;
        List<LessonPlanChapter> copy = planChapters == null ? new ArrayList<>() : new ArrayList<>(planChapters);
        if (this.planChapters == null) this.planChapters = new ArrayList<>();
        this.planChapters.clear();
        this.planChapters.addAll(copy);
    }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
