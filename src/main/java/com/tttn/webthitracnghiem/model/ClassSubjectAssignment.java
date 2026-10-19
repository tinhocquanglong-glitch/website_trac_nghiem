package com.tttn.webthitracnghiem.model;

import javax.persistence.*;

@Entity
@Table(name = "class_subject_assignment", uniqueConstraints =
        @UniqueConstraint(name = "uk_class_subject", columnNames = {"tracking_class_id", "subject_id"}))
public class ClassSubjectAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracking_class_id", nullable = false)
    private StudentTrackingClass trackingClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false,
            columnDefinition = "varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci")
    private User teacher;

    public ClassSubjectAssignment() {
    }

    public ClassSubjectAssignment(StudentTrackingClass trackingClass, Subject subject, User teacher) {
        this.trackingClass = trackingClass;
        this.subject = subject;
        this.teacher = teacher;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public StudentTrackingClass getTrackingClass() {
        return trackingClass;
    }

    public void setTrackingClass(StudentTrackingClass trackingClass) {
        this.trackingClass = trackingClass;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }
}
