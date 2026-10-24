package com.tttn.webthitracnghiem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Marks that a school has its own copy of the curriculum for one subject-grade.
 * Without the marker the school still reads the shared curriculum; once present,
 * the school's chapters are authoritative even if the school deleted them all.
 */
@Entity
@Table(name = "school_curriculum", uniqueConstraints =
        @UniqueConstraint(name = "uk_school_curriculum", columnNames = {"school_id", "subject_class_id"}))
public class SchoolCurriculum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_class_id", nullable = false)
    private SubjectClasses subjectClasses;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SchoolCurriculum() {
    }

    public SchoolCurriculum(School school, SubjectClasses subjectClasses) {
        this.school = school;
        this.subjectClasses = subjectClasses;
    }

    public Integer getId() { return id; }
    public School getSchool() { return school; }
    public SubjectClasses getSubjectClasses() { return subjectClasses; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
