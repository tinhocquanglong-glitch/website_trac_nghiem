package com.tttn.webthitracnghiem.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_tracking_class", uniqueConstraints =
        @UniqueConstraint(name = "uk_tracking_class_year", columnNames = {"class_name", "school_year"}))
public class StudentTrackingClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @Column(name = "school_year", nullable = false, length = 9)
    private String schoolYear;

    @OneToMany(mappedBy = "trackingClass", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<TrackedStudent> students = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }
    public List<TrackedStudent> getStudents() { return students; }
    public void setStudents(List<TrackedStudent> students) { this.students = students; }
}
