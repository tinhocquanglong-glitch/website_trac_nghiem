package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IResultService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class StudentExamController {
    @Autowired
    private IExamService examService;

    @Autowired
    private IResultService resultService;

    @Autowired
    private IUserService userService;

    @GetMapping("/student/exams")
    public String list(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "redirect:/login";
        }

        User student = userService.findById(authentication.getName());
        List<Exam> assignedExams = student == null ? List.of() : examService.getAll().stream()
                .filter(Objects::nonNull)
                .filter(exam -> !exam.isPractice())
                .filter(exam -> canTake(exam, student))
                .sorted(Comparator.comparing(Exam::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        Map<Integer, Result> completedResults = new LinkedHashMap<>();
        if (student != null && resultService != null) {
            List<Result> results = resultService.findByHistory(student.getId());
            if (results != null) {
                results.stream()
                        .filter(Objects::nonNull)
                        .filter(result -> result.getExam() != null)
                        .forEach(result -> completedResults.put(result.getExam().getId(), result));
            }
        }

        model.addAttribute("assignedExams", assignedExams);
        Set<Integer> completedExamIds = new LinkedHashSet<>(completedResults.keySet());
        long completedCount = assignedExams.stream().filter(e -> completedExamIds.contains(e.getId())).count();
        model.addAttribute("completedExamIds", completedExamIds);
        model.addAttribute("completedResults", completedResults);
        model.addAttribute("completedAssignedCount", completedCount);
        model.addAttribute("pendingAssignedCount", assignedExams.size() - completedCount);
        model.addAttribute("student", student);
        model.addAttribute("now", new Timestamp(System.currentTimeMillis()));
        return "student/exams";
    }

    boolean canTake(Exam exam, User student) {
        if (exam == null || student == null) {
            return false;
        }

        String targetType = exam.getTargetType();
        if (targetType == null || targetType.trim().isEmpty()) {
            targetType = Boolean.FALSE.equals(exam.getIsPublic()) ? "SCHOOL" : "PUBLIC";
        }

        if ("CLASS".equalsIgnoreCase(targetType)) {
            StudentTrackingClass targetClass = exam.getTargetClass();
            StudentTrackingClass studentClass = student.getTrackingClass();
            return targetClass != null && studentClass != null
                    && targetClass.getId() != null && targetClass.getId().equals(studentClass.getId());
        }

        if ("GRADE".equalsIgnoreCase(targetType)) {
            Integer targetGrade = exam.getTargetGrade();
            Integer studentGrade = student.getTrackingClass() == null
                    ? null : student.getTrackingClass().getGrade();
            return sameSchool(student.getSchool(), exam.getSchool())
                    && targetGrade != null && targetGrade.equals(studentGrade);
        }

        if ("SCHOOL".equalsIgnoreCase(targetType)) {
            return sameSchool(student.getSchool(), exam.getSchool());
        }

        return !Boolean.FALSE.equals(exam.getIsPublic())
                || sameSchool(student.getSchool(), exam.getSchool());
    }

    private boolean sameSchool(School studentSchool, School examSchool) {
        return studentSchool != null && examSchool != null
                && studentSchool.getId() != null && studentSchool.getId().equals(examSchool.getId());
    }
}
