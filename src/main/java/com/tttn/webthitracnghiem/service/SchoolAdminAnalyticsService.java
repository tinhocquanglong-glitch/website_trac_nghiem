package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolAdminStatistics;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.ExamRepository;
import com.tttn.webthitracnghiem.repository.ResultRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SchoolAdminAnalyticsService {
    private final UserRepository userRepository;
    private final StudentTrackingClassRepository classRepository;
    private final ClassSubjectAssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final ResultRepository resultRepository;

    @Autowired
    public SchoolAdminAnalyticsService(UserRepository userRepository,
                                       StudentTrackingClassRepository classRepository,
                                       ClassSubjectAssignmentRepository assignmentRepository,
                                       SubjectRepository subjectRepository,
                                       ExamRepository examRepository,
                                       ResultRepository resultRepository) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.examRepository = examRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public SchoolAdminStatistics build(School school, Integer grade, String schoolYear) {
        SchoolAdminStatistics statistics = new SchoolAdminStatistics();
        if (school == null || school.getId() == null) {
            return statistics;
        }
        List<StudentTrackingClass> classes = classRepository.findBySchoolId(school.getId()).stream()
                .filter(item -> grade == null || grade.equals(item.getGrade()))
                .filter(item -> schoolYear == null || schoolYear.isBlank() || schoolYear.equals(item.getSchoolYear()))
                .collect(Collectors.toList());
        Set<Integer> classIds = classes.stream().map(StudentTrackingClass::getId).collect(Collectors.toSet());
        List<User> teachers = userRepository.findTeachersBySchool(school.getId());
        List<User> students = userRepository.findStudentsBySchool(school.getId()).stream()
                .filter(student -> grade == null && (schoolYear == null || schoolYear.isBlank())
                        || student.getTrackingClass() != null
                        && classIds.contains(student.getTrackingClass().getId()))
                .collect(Collectors.toList());
        List<Subject> subjects = subjectRepository.findAll();
        List<ClassSubjectAssignment> assignments = assignmentRepository.findBySchoolId(school.getId()).stream()
                .filter(item -> item.getTrackingClass() != null && classIds.contains(item.getTrackingClass().getId()))
                .collect(Collectors.toList());

        statistics.setStudentCount(students.size());
        statistics.setTeacherCount(teachers.size());
        statistics.setClassCount(classes.size());
        statistics.setSubjectCount(subjects.size());
        statistics.setAssignmentCount(assignments.size());
        statistics.setExpectedAssignmentCount(classes.size() * subjects.size());
        statistics.setAssignmentCoverage(coverage(assignments.size(), statistics.getExpectedAssignmentCount()));

        Map<Integer, List<ClassSubjectAssignment>> assignmentsByClass = assignments.stream()
                .filter(item -> item.getTrackingClass() != null)
                .collect(Collectors.groupingBy(item -> item.getTrackingClass().getId()));
        List<SchoolAdminStatistics.ClassCoverage> classCoverage = classes.stream()
                .map(item -> new SchoolAdminStatistics.ClassCoverage(item,
                        (int) students.stream().filter(student -> sameClass(student, item)).count(),
                        assignmentsByClass.getOrDefault(item.getId(), List.of()).size(),
                        item.getHomeroomTeacher() != null))
                .sorted(Comparator.comparing(SchoolAdminStatistics.ClassCoverage::getAssignmentCount)
                        .thenComparing(item -> item.getTrackingClass().getClassName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        statistics.setClassCoverage(classCoverage);
        statistics.setClassesWithoutHomeroom((int) classCoverage.stream().filter(item -> !item.isHasHomeroom()).count());
        statistics.setClassesWithoutSubjectTeacher((int) classCoverage.stream().filter(item -> item.getAssignmentCount() == 0).count());

        Map<String, List<ClassSubjectAssignment>> assignmentsByTeacher = assignments.stream()
                .filter(item -> item.getTeacher() != null)
                .collect(Collectors.groupingBy(item -> item.getTeacher().getId()));
        List<SchoolAdminStatistics.TeacherWorkload> workloads = teachers.stream()
                .map(teacher -> {
                    List<ClassSubjectAssignment> teacherAssignments = assignmentsByTeacher.getOrDefault(teacher.getId(), List.of());
                    int classesTaught = (int) teacherAssignments.stream().map(item -> item.getTrackingClass().getId()).distinct().count();
                    int subjectsTaught = (int) teacherAssignments.stream().map(item -> item.getSubject().getId()).distinct().count();
                    return new SchoolAdminStatistics.TeacherWorkload(teacher, classesTaught, subjectsTaught);
                })
                .sorted(Comparator.comparing(SchoolAdminStatistics.TeacherWorkload::getClassCount).reversed()
                        .thenComparing(item -> item.getTeacher().getFullName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        statistics.setTeacherWorkloads(workloads);

        Set<String> studentIds = students.stream().map(User::getId).collect(Collectors.toSet());
        List<Result> results = bestResults(resultRepository.findAll()).stream()
                .filter(item -> item.getUsers() != null && studentIds.contains(item.getUsers().getId()))
                .collect(Collectors.toList());
        List<Result> realResults = results.stream().filter(item -> item.getExam() != null && !item.getExam().isPractice()).collect(Collectors.toList());
        List<Result> practiceResults = results.stream().filter(item -> item.getExam() != null && item.getExam().isPractice()).collect(Collectors.toList());
        statistics.setRealAttempts(realResults.size());
        statistics.setPracticeAttempts(practiceResults.size());
        statistics.setTotalAttempts(results.size());
        statistics.setRealAverageScore(average(realResults));
        statistics.setPracticeAverageScore(average(practiceResults));

        List<Exam> exams = examRepository.findAll().stream()
                .filter(exam -> isOwnedBySchool(exam, school))
                .filter(exam -> grade == null || exam.getTargetGrade() == null || grade.equals(exam.getTargetGrade()))
                .collect(Collectors.toList());
        statistics.setRealExamCount((int) exams.stream().filter(exam -> !exam.isPractice()).count());
        statistics.setPracticeExamCount((int) exams.stream().filter(Exam::isPractice).count());
        Map<Integer, Result> resultByExam = new HashMap<>();
        List<SchoolAdminStatistics.ExamActivity> activities = new ArrayList<>();
        for (Exam exam : exams) {
            List<User> eligible = students.stream().filter(student -> eligibleFor(exam, student, school)).collect(Collectors.toList());
            List<Result> examResults = results.stream().filter(item -> item.getExam() != null && item.getExam().getId() == exam.getId()).collect(Collectors.toList());
            activities.add(new SchoolAdminStatistics.ExamActivity(exam, eligible.size(), examResults.size(), average(examResults)));
        }
        activities.sort(Comparator.comparing(SchoolAdminStatistics.ExamActivity::getParticipationPercent)
                .thenComparing(item -> item.getExam().getNameExam(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        statistics.setExamActivities(activities);

        statistics.setGradeScores(buildGradeScores(realResults));
        statistics.setScoreBands(buildScoreBands(realResults));
        statistics.setAtRiskStudents(buildAtRiskStudents(realResults));
        statistics.setAlerts(buildAlerts(statistics, activities));
        return statistics;
    }

    private List<Result> bestResults(List<Result> results) {
        Map<String, Result> best = new LinkedHashMap<>();
        for (Result result : results) {
            if (result.getUsers() == null || result.getExam() == null) continue;
            String key = result.getUsers().getId() + "_" + result.getExam().getId();
            Result current = best.get(key);
            if (current == null || result.getMark() > current.getMark()) best.put(key, result);
        }
        return new ArrayList<>(best.values());
    }

    private List<SchoolAdminStatistics.GradeScore> buildGradeScores(List<Result> results) {
        Map<Integer, List<Result>> byGrade = results.stream()
                .filter(item -> item.getUsers().getTrackingClass() != null && item.getUsers().getTrackingClass().getGrade() != null)
                .collect(Collectors.groupingBy(item -> item.getUsers().getTrackingClass().getGrade()));
        return byGrade.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new SchoolAdminStatistics.GradeScore(entry.getKey(), average(entry.getValue()), entry.getValue().size()))
                .collect(Collectors.toList());
    }

    private List<SchoolAdminStatistics.ScoreBand> buildScoreBands(List<Result> results) {
        int underFive = (int) results.stream().filter(item -> item.getMark() < 5).count();
        int fiveToEight = (int) results.stream().filter(item -> item.getMark() >= 5 && item.getMark() < 8).count();
        int eightUp = (int) results.stream().filter(item -> item.getMark() >= 8).count();
        return List.of(new SchoolAdminStatistics.ScoreBand("Dưới 5", underFive),
                new SchoolAdminStatistics.ScoreBand("5 đến dưới 8", fiveToEight),
                new SchoolAdminStatistics.ScoreBand("Từ 8 trở lên", eightUp));
    }

    private List<SchoolAdminStatistics.StudentAlert> buildAtRiskStudents(List<Result> results) {
        Map<User, List<Result>> byStudent = results.stream().collect(Collectors.groupingBy(Result::getUsers));
        return byStudent.entrySet().stream()
                .map(entry -> new SchoolAdminStatistics.StudentAlert(entry.getKey(), average(entry.getValue()), entry.getValue().size()))
                .filter(item -> item.getAverage() < 5)
                .sorted(Comparator.comparing(SchoolAdminStatistics.StudentAlert::getAverage))
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<String> buildAlerts(SchoolAdminStatistics statistics, List<SchoolAdminStatistics.ExamActivity> activities) {
        List<String> alerts = new ArrayList<>();
        if (statistics.getClassesWithoutHomeroom() > 0) alerts.add(statistics.getClassesWithoutHomeroom() + " lớp chưa có giáo viên chủ nhiệm.");
        if (statistics.getClassesWithoutSubjectTeacher() > 0) alerts.add(statistics.getClassesWithoutSubjectTeacher() + " lớp chưa được phân công giáo viên bộ môn.");
        if (statistics.getAssignmentCoverage() < 100 && statistics.getExpectedAssignmentCount() > 0) alerts.add("Ma trận giảng dạy mới hoàn tất " + Math.round(statistics.getAssignmentCoverage()) + "%.");
        long lowParticipation = activities.stream().filter(item -> item.getEligibleCount() > 0 && item.getParticipationPercent() < 50).count();
        if (lowParticipation > 0) alerts.add(lowParticipation + " bài kiểm tra có tỷ lệ tham gia dưới 50%.");
        if (!statistics.getAtRiskStudents().isEmpty()) alerts.add(statistics.getAtRiskStudents().size() + " học sinh có điểm thi thật trung bình dưới 5.");
        return alerts;
    }

    private boolean isOwnedBySchool(Exam exam, School school) {
        return sameSchool(exam.getSchool(), school)
                || (exam.getUsers() != null && sameSchool(exam.getUsers().getSchool(), school));
    }

    private boolean eligibleFor(Exam exam, User student, School school) {
        if (!sameSchool(student.getSchool(), school)) return false;
        String targetType = exam.getTargetType();
        if (targetType == null || targetType.isBlank() || "PUBLIC".equalsIgnoreCase(targetType)) return true;
        if ("CLASS".equalsIgnoreCase(targetType)) return exam.getTargetClass() != null && sameClass(student, exam.getTargetClass());
        if ("GRADE".equalsIgnoreCase(targetType)) return exam.getTargetGrade() == null || (student.getTrackingClass() != null && exam.getTargetGrade().equals(student.getTrackingClass().getGrade()));
        return true;
    }

    private boolean sameClass(User student, StudentTrackingClass trackingClass) {
        return student != null && student.getTrackingClass() != null && trackingClass != null
                && Objects.equals(student.getTrackingClass().getId(), trackingClass.getId());
    }

    private boolean sameSchool(School first, School second) {
        return first != null && second != null && Objects.equals(first.getId(), second.getId());
    }

    private double average(List<Result> results) {
        return Math.round(results.stream().mapToDouble(Result::getMark).average().orElse(0D) * 100.0) / 100.0;
    }

    private double coverage(int numerator, int denominator) {
        return denominator == 0 ? 0D : Math.round(numerator * 10000.0 / denominator) / 100.0;
    }
}
