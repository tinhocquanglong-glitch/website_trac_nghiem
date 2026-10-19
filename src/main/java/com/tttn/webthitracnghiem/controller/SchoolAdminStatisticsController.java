package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolAdminStatistics;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.SchoolAdminAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/school-admin/statistics")
@SessionAttributes("admin")
public class SchoolAdminStatisticsController {
    private final SchoolAdminAnalyticsService analyticsService;
    private final IUserService userService;
    private final SchoolRepository schoolRepository;
    private final StudentTrackingClassRepository classRepository;

    @Autowired
    public SchoolAdminStatisticsController(SchoolAdminAnalyticsService analyticsService,
                                           IUserService userService,
                                           SchoolRepository schoolRepository,
                                           StudentTrackingClassRepository classRepository) {
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.schoolRepository = schoolRepository;
        this.classRepository = classRepository;
    }

    @org.springframework.web.bind.annotation.ModelAttribute("admin")
    public User admin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : userService.findById(authentication.getName());
    }

    @GetMapping
    public String statistics(@RequestParam(value = "schoolId", required = false) Integer schoolId,
                             @RequestParam(value = "grade", required = false) Integer grade,
                             @RequestParam(value = "schoolYear", defaultValue = "") String schoolYear,
                             Model model) {
        School school = resolveSchool(schoolId);
        if (school == null) {
            model.addAttribute("error", "Không tìm thấy trường học để xem thống kê.");
            return "school-admin/statistics";
        }
        SchoolAdminStatistics statistics = analyticsService.build(school, grade, schoolYear);
        List<Integer> availableGrades = classRepository.findBySchoolId(school.getId()).stream()
                .map(StudentTrackingClass::getGrade)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        model.addAttribute("statistics", statistics);
        model.addAttribute("targetSchool", school);
        model.addAttribute("allSchools", isAdmin() ? schoolRepository.findAllByActiveTrueOrderByNameAsc() : List.of());
        model.addAttribute("availableGrades", availableGrades);
        model.addAttribute("grade", grade);
        model.addAttribute("schoolYear", schoolYear == null ? "" : schoolYear);
        model.addAttribute("isAdmin", isAdmin());
        return "school-admin/statistics";
    }

    private School resolveSchool(Integer schoolId) {
        if (isAdmin()) {
            if (schoolId != null) return schoolRepository.findById(schoolId).orElse(null);
            return schoolRepository.findAllByActiveTrueOrderByNameAsc().stream().findFirst().orElse(null);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = authentication == null ? null : userService.findById(authentication.getName());
        return currentUser == null ? null : currentUser.getSchool();
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
