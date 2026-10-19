package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ResultRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TeacherExamStatisticsController {
    @Autowired private IExamService examService;
    @Autowired private IUserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ResultRepository resultRepository;

    @GetMapping("/teacher/exams/{examId}/statistics")
    public String statistics(@PathVariable Integer examId,
                             @RequestParam(defaultValue = "score") String sort,
                             Model model) {
        Exam exam = examService.findById(examId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (exam == null || !canView(exam, auth)) return "redirect:/exam/list";
        List<User> students = studentsForExam(exam);
        Map<String, Result> bestResult = resultRepository.findByExamIdOrderByMarkDescEndTimeAsc(examId).stream()
                .filter(result -> result.getUsers() != null)
                .collect(Collectors.toMap(result -> result.getUsers().getId(), result -> result,
                        (first, duplicate) -> first));
        List<Participation> rows = students.stream().map(student -> new Participation(student, bestResult.get(student.getId())))
                .sorted(comparator(sort)).collect(Collectors.toList());
        model.addAttribute("exam", exam);
        model.addAttribute("rows", rows);
        model.addAttribute("sort", sort);
        long takenCount = rows.stream().filter(Participation::isTaken).count();
        model.addAttribute("takenCount", takenCount);
        model.addAttribute("pendingCount", rows.size() - takenCount);
        model.addAttribute("averageScore", rows.stream().map(Participation::getScore)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0D));
        model.addAttribute("highestScore", rows.stream().map(Participation::getScore)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0D));
        return "teacher/exam-statistics";
    }

    private List<User> studentsForExam(Exam exam) {
        String targetType = exam.getTargetType();
        if (targetType == null || targetType.trim().isEmpty()) {
            targetType = Boolean.FALSE.equals(exam.getIsPublic()) ? "SCHOOL" : "PUBLIC";
        }
        if ("CLASS".equalsIgnoreCase(targetType) && exam.getTargetClass() != null) {
            return userRepository.findStudentsByClassId(exam.getTargetClass().getId());
        }
        if ("GRADE".equalsIgnoreCase(targetType) && exam.getTargetGrade() != null) {
            if (exam.getSchool() != null) {
                return userRepository.findStudentsBySchoolAndGrade(exam.getSchool().getId(), exam.getTargetGrade());
            }
            return userRepository.findStudentsByGrade(exam.getTargetGrade());
        }
        if ("SCHOOL".equalsIgnoreCase(targetType) && exam.getSchool() != null) {
            return userRepository.findStudentsBySchool(exam.getSchool().getId());
        }
        return exam.getSchool() == null
                ? userRepository.findAllStudents()
                : userRepository.findStudentsBySchool(exam.getSchool().getId());
    }

    private boolean canView(Exam exam, Authentication auth) {
        if (auth == null) return false;
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                || auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))) return true;
        User current = userService.findById(auth.getName());
        return current != null && exam.getUsers() != null && current.getId().equals(exam.getUsers().getId());
    }

    private Comparator<Participation> comparator(String sort) {
        if ("time".equalsIgnoreCase(sort)) return Comparator.comparing(Participation::getEndTime,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if ("name".equalsIgnoreCase(sort)) return Comparator.comparing(Participation::getStudentName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        return Comparator.comparing(Participation::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Participation::getStudentName, String.CASE_INSENSITIVE_ORDER);
    }

    public static class Participation {
        private final User student;
        private final Result result;
        public Participation(User student, Result result) { this.student = student; this.result = result; }
        public User getStudent() { return student; }
        public Result getResult() { return result; }
        public boolean isTaken() { return result != null; }
        public Double getScore() { return result == null ? null : result.getMark(); }
        public java.sql.Timestamp getEndTime() { return result == null ? null : result.getEndTime(); }
        public String getStudentName() { return student == null ? "" : student.getFullName(); }
    }
}
