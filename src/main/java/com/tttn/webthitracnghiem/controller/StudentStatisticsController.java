package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.service.IResultService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
public class StudentStatisticsController {
    @Autowired private IResultService resultService;
    @Autowired private IUserService userService;

    @GetMapping("/student/statistics")
    public String statistics(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null ? null : auth.getName();
        List<Result> results = username == null ? List.of() : resultService.findByHistory(username);
        Comparator<Result> newestFirst = Comparator.comparing(Result::getEndTime,
                Comparator.nullsLast(Comparator.reverseOrder()));
        List<Result> realResults = results.stream()
                .filter(item -> item.getExam() != null && !item.getExam().isPractice())
                .sorted(newestFirst).collect(Collectors.toList());
        List<Result> practiceResults = results.stream()
                .filter(item -> item.getExam() != null && item.getExam().isPractice())
                .sorted(newestFirst).collect(Collectors.toList());
        model.addAttribute("realResults", realResults);
        model.addAttribute("practiceResults", practiceResults);
        model.addAttribute("realAverage", average(realResults));
        model.addAttribute("practiceAverage", average(practiceResults));
        model.addAttribute("student", username == null ? null : userService.findById(username));
        return "student/statistics";
    }

    private double average(List<Result> results) {
        return results.stream().mapToDouble(Result::getMark).average().orElse(0D);
    }
}
