package com.tttn.webthitracnghiem.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_tracking_class", uniqueConstraints =
        @UniqueConstraint(name = "uk_tracking_class_year_school", columnNames = {"class_name", "school_year", "school_id"}))
public class StudentTrackingClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @Column(name = "school_year", nullable = false, length = 9)
    private String schoolYear;

    @Column(name = "grade")
    private Integer grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_teacher_id")
    private User homeroomTeacher;

    @OneToMany(mappedBy = "trackingClass", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<TrackedStudent> students = new ArrayList<>();

    @OneToMany(mappedBy = "trackingClass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassSubjectAssignment> assignments = new ArrayList<>();

    public StudentTrackingClass() {
    }

    public StudentTrackingClass(String className, String schoolYear) {
        this.className = className;
        this.schoolYear = schoolYear;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public User getHomeroomTeacher() { return homeroomTeacher; }
    public void setHomeroomTeacher(User homeroomTeacher) { this.homeroomTeacher = homeroomTeacher; }
    public List<TrackedStudent> getStudents() { return students; }
    public void setStudents(List<TrackedStudent> students) { this.students = students; }
    public List<ClassSubjectAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<ClassSubjectAssignment> assignments) { this.assignments = assignments; }
}
