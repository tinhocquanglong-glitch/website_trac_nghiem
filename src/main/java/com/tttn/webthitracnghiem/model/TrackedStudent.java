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
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tracked_student")
public class TrackedStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracking_class_id", nullable = false)
    private StudentTrackingClass trackingClass;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentSemesterRecord> semesterRecords = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentMonthlyRecord> monthlyRecords = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public StudentTrackingClass getTrackingClass() { return trackingClass; }
    public void setTrackingClass(StudentTrackingClass trackingClass) { this.trackingClass = trackingClass; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public List<StudentSemesterRecord> getSemesterRecords() { return semesterRecords; }
    public void setSemesterRecords(List<StudentSemesterRecord> semesterRecords) { this.semesterRecords = semesterRecords; }
    public List<StudentMonthlyRecord> getMonthlyRecords() { return monthlyRecords; }
    public void setMonthlyRecords(List<StudentMonthlyRecord> monthlyRecords) { this.monthlyRecords = monthlyRecords; }
}
