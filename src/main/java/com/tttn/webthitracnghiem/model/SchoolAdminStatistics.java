package com.tttn.webthitracnghiem.model;

import java.util.ArrayList;
import java.util.List;

public class SchoolAdminStatistics {
    private int studentCount;
    private int teacherCount;
    private int classCount;
    private int subjectCount;
    private int assignmentCount;
    private int expectedAssignmentCount;
    private int classesWithoutHomeroom;
    private int classesWithoutSubjectTeacher;
    private int realExamCount;
    private int practiceExamCount;
    private int totalAttempts;
    private int realAttempts;
    private int practiceAttempts;
    private double realAverageScore;
    private double practiceAverageScore;
    private double assignmentCoverage;
    private List<GradeScore> gradeScores = new ArrayList<>();
    private List<ScoreBand> scoreBands = new ArrayList<>();
    private List<ClassCoverage> classCoverage = new ArrayList<>();
    private List<TeacherWorkload> teacherWorkloads = new ArrayList<>();
    private List<ExamActivity> examActivities = new ArrayList<>();
    private List<StudentAlert> atRiskStudents = new ArrayList<>();
    private List<String> alerts = new ArrayList<>();

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    public int getTeacherCount() { return teacherCount; }
    public void setTeacherCount(int teacherCount) { this.teacherCount = teacherCount; }
    public int getClassCount() { return classCount; }
    public void setClassCount(int classCount) { this.classCount = classCount; }
    public int getSubjectCount() { return subjectCount; }
    public void setSubjectCount(int subjectCount) { this.subjectCount = subjectCount; }
    public int getAssignmentCount() { return assignmentCount; }
    public void setAssignmentCount(int assignmentCount) { this.assignmentCount = assignmentCount; }
    public int getExpectedAssignmentCount() { return expectedAssignmentCount; }
    public void setExpectedAssignmentCount(int expectedAssignmentCount) { this.expectedAssignmentCount = expectedAssignmentCount; }
    public int getClassesWithoutHomeroom() { return classesWithoutHomeroom; }
    public void setClassesWithoutHomeroom(int classesWithoutHomeroom) { this.classesWithoutHomeroom = classesWithoutHomeroom; }
    public int getClassesWithoutSubjectTeacher() { return classesWithoutSubjectTeacher; }
    public void setClassesWithoutSubjectTeacher(int classesWithoutSubjectTeacher) { this.classesWithoutSubjectTeacher = classesWithoutSubjectTeacher; }
    public int getRealExamCount() { return realExamCount; }
    public void setRealExamCount(int realExamCount) { this.realExamCount = realExamCount; }
    public int getPracticeExamCount() { return practiceExamCount; }
    public void setPracticeExamCount(int practiceExamCount) { this.practiceExamCount = practiceExamCount; }
    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
    public int getRealAttempts() { return realAttempts; }
    public void setRealAttempts(int realAttempts) { this.realAttempts = realAttempts; }
    public int getPracticeAttempts() { return practiceAttempts; }
    public void setPracticeAttempts(int practiceAttempts) { this.practiceAttempts = practiceAttempts; }
    public double getRealAverageScore() { return realAverageScore; }
    public void setRealAverageScore(double realAverageScore) { this.realAverageScore = realAverageScore; }
    public double getPracticeAverageScore() { return practiceAverageScore; }
    public void setPracticeAverageScore(double practiceAverageScore) { this.practiceAverageScore = practiceAverageScore; }
    public double getAssignmentCoverage() { return assignmentCoverage; }
    public void setAssignmentCoverage(double assignmentCoverage) { this.assignmentCoverage = assignmentCoverage; }
    public List<GradeScore> getGradeScores() { return gradeScores; }
    public void setGradeScores(List<GradeScore> gradeScores) { this.gradeScores = gradeScores; }
    public List<ScoreBand> getScoreBands() { return scoreBands; }
    public void setScoreBands(List<ScoreBand> scoreBands) { this.scoreBands = scoreBands; }
    public List<ClassCoverage> getClassCoverage() { return classCoverage; }
    public void setClassCoverage(List<ClassCoverage> classCoverage) { this.classCoverage = classCoverage; }
    public List<TeacherWorkload> getTeacherWorkloads() { return teacherWorkloads; }
    public void setTeacherWorkloads(List<TeacherWorkload> teacherWorkloads) { this.teacherWorkloads = teacherWorkloads; }
    public List<ExamActivity> getExamActivities() { return examActivities; }
    public void setExamActivities(List<ExamActivity> examActivities) { this.examActivities = examActivities; }
    public List<StudentAlert> getAtRiskStudents() { return atRiskStudents; }
    public void setAtRiskStudents(List<StudentAlert> atRiskStudents) { this.atRiskStudents = atRiskStudents; }
    public List<String> getAlerts() { return alerts; }
    public void setAlerts(List<String> alerts) { this.alerts = alerts; }

    public static class GradeScore {
        private final Integer grade;
        private final double average;
        private final int attempts;
        public GradeScore(Integer grade, double average, int attempts) { this.grade = grade; this.average = average; this.attempts = attempts; }
        public Integer getGrade() { return grade; }
        public double getAverage() { return average; }
        public int getAttempts() { return attempts; }
    }

    public static class ScoreBand {
        private final String label;
        private final int count;
        public ScoreBand(String label, int count) { this.label = label; this.count = count; }
        public String getLabel() { return label; }
        public int getCount() { return count; }
    }

    public static class ClassCoverage {
        private final StudentTrackingClass trackingClass;
        private final int studentCount;
        private final int assignmentCount;
        private final boolean hasHomeroom;
        public ClassCoverage(StudentTrackingClass trackingClass, int studentCount, int assignmentCount, boolean hasHomeroom) {
            this.trackingClass = trackingClass; this.studentCount = studentCount; this.assignmentCount = assignmentCount; this.hasHomeroom = hasHomeroom;
        }
        public StudentTrackingClass getTrackingClass() { return trackingClass; }
        public int getStudentCount() { return studentCount; }
        public int getAssignmentCount() { return assignmentCount; }
        public boolean isHasHomeroom() { return hasHomeroom; }
    }

    public static class TeacherWorkload {
        private final User teacher;
        private final int classCount;
        private final int subjectCount;
        public TeacherWorkload(User teacher, int classCount, int subjectCount) { this.teacher = teacher; this.classCount = classCount; this.subjectCount = subjectCount; }
        public User getTeacher() { return teacher; }
        public int getClassCount() { return classCount; }
        public int getSubjectCount() { return subjectCount; }
    }

    public static class ExamActivity {
        private final Exam exam;
        private final int eligibleCount;
        private final int takenCount;
        private final double average;
        public ExamActivity(Exam exam, int eligibleCount, int takenCount, double average) { this.exam = exam; this.eligibleCount = eligibleCount; this.takenCount = takenCount; this.average = average; }
        public Exam getExam() { return exam; }
        public int getEligibleCount() { return eligibleCount; }
        public int getTakenCount() { return takenCount; }
        public double getAverage() { return average; }
        public int getPendingCount() { return Math.max(0, eligibleCount - takenCount); }
        public int getParticipationPercent() { return eligibleCount == 0 ? 0 : (int) Math.round(takenCount * 100.0 / eligibleCount); }
    }

    public static class StudentAlert {
        private final User student;
        private final double average;
        private final int attempts;
        public StudentAlert(User student, double average, int attempts) { this.student = student; this.average = average; this.attempts = attempts; }
        public User getStudent() { return student; }
        public double getAverage() { return average; }
        public int getAttempts() { return attempts; }
    }
}
