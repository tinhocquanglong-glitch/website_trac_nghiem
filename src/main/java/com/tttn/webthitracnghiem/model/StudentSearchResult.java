package com.tttn.webthitracnghiem.model;

public class StudentSearchResult {
    private final Integer studentId;
    private final Integer classId;
    private final String fullName;
    private final String className;
    private final String schoolYear;

    public StudentSearchResult(Integer studentId, Integer classId, String fullName,
                               String className, String schoolYear) {
        this.studentId = studentId;
        this.classId = classId;
        this.fullName = fullName;
        this.className = className;
        this.schoolYear = schoolYear;
    }

    public Integer getStudentId() { return studentId; }
    public Integer getClassId() { return classId; }
    public String getFullName() { return fullName; }
    public String getClassName() { return className; }
    public String getSchoolYear() { return schoolYear; }
}
