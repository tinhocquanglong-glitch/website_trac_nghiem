package com.tttn.webthitracnghiem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "student_semester_record", uniqueConstraints =
        @UniqueConstraint(name = "uk_student_semester", columnNames = {"student_id", "semester"}))
public class StudentSemesterRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private TrackedStudent student;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "regular_score_1", precision = 4, scale = 2)
    private BigDecimal regularScore1;
    @Column(name = "regular_score_2", precision = 4, scale = 2)
    private BigDecimal regularScore2;
    @Column(name = "midterm_score", precision = 4, scale = 2)
    private BigDecimal midtermScore;
    @Column(name = "final_score", precision = 4, scale = 2)
    private BigDecimal finalScore;
    @Column(name = "average_score", precision = 4, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "progress_comment", length = 2000)
    private String progressComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", length = 20)
    private AttendanceStatus attendanceStatus;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public TrackedStudent getStudent() { return student; }
    public void setStudent(TrackedStudent student) { this.student = student; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public BigDecimal getRegularScore1() { return regularScore1; }
    public void setRegularScore1(BigDecimal regularScore1) { this.regularScore1 = regularScore1; }
    public BigDecimal getRegularScore2() { return regularScore2; }
    public void setRegularScore2(BigDecimal regularScore2) { this.regularScore2 = regularScore2; }
    public BigDecimal getMidtermScore() { return midtermScore; }
    public void setMidtermScore(BigDecimal midtermScore) { this.midtermScore = midtermScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public BigDecimal getAverageScore() { return averageScore; }
    public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
    public String getProgressComment() { return progressComment; }
    public void setProgressComment(String progressComment) { this.progressComment = progressComment; }
    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(AttendanceStatus attendanceStatus) { this.attendanceStatus = attendanceStatus; }
}
