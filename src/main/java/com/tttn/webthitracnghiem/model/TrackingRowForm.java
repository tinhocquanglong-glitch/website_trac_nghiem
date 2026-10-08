package com.tttn.webthitracnghiem.model;

import java.math.BigDecimal;

public class TrackingRowForm {
    private Integer studentId;
    private String fullName;
    private BigDecimal regularScore1;
    private BigDecimal regularScore2;
    private BigDecimal midtermScore;
    private BigDecimal finalScore;
    private BigDecimal averageScore;
    private String progressComment;
    private AttendanceStatus attendanceStatus;
    private String attendanceDates;

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
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
    public String getAttendanceDates() { return attendanceDates; }
    public void setAttendanceDates(String attendanceDates) { this.attendanceDates = attendanceDates; }
}
