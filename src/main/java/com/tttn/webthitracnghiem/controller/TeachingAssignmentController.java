package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IClassAssignmentService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/school-admin/teaching-assignments")
@SessionAttributes("admin")
public class TeachingAssignmentController {

    private final IClassAssignmentService assignmentService;
    private final ISubjectService subjectService;
    private final IUserService userService;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final StudentTrackingClassRepository classRepository;

    @Autowired
    public TeachingAssignmentController(IClassAssignmentService assignmentService,
                                        ISubjectService subjectService,
                                        IUserService userService,
                                        UserRepository userRepository,
                                        SchoolRepository schoolRepository,
                                        StudentTrackingClassRepository classRepository) {
        this.assignmentService = assignmentService;
        this.subjectService = subjectService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.classRepository = classRepository;
    }

    @org.springframework.web.bind.annotation.ModelAttribute("admin")
    public User admin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : userService.findById(authentication.getName());
    }

    @GetMapping
    public String page(@RequestParam(value = "schoolId", required = false) Integer schoolId,
                       @RequestParam(value = "grade", required = false) Integer grade,
                       @RequestParam(value = "schoolYear", defaultValue = "") String schoolYear,
                       @RequestParam(value = "keyword", defaultValue = "") String keyword,
                       Model model) {
        School school = resolveSchool(schoolId);
        if (school == null) {
            model.addAttribute("error", "Không tìm thấy trường học để phân công.");
            return "school-admin/teaching-assignments";
        }
        populate(model, school, grade, schoolYear, keyword);
        return "school-admin/teaching-assignments";
    }

    @PostMapping("/bulk")
    public String assignBulk(@RequestParam Integer subjectId,
                             @RequestParam String teacherId,
                             @RequestParam(value = "classIds", required = false) List<Integer> classIds,
                             @RequestParam(value = "schoolId", required = false) Integer schoolId,
                             @RequestParam(value = "grade", required = false) Integer grade,
                             @RequestParam(value = "schoolYear", defaultValue = "") String schoolYear,
                             @RequestParam(value = "keyword", defaultValue = "") String keyword,
                             RedirectAttributes redirectAttributes) {
        School school = resolveSchool(schoolId);
        if (school == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy trường học để phân công.");
            return redirect();
        }
        List<StudentTrackingClass> schoolClasses = classRepository.findBySchoolId(school.getId());
        Set<Integer> validClassIds = schoolClasses.stream()
                .map(StudentTrackingClass::getId)
                .collect(Collectors.toSet());
        User teacher = findTeacherInSchool(teacherId, school.getId());
        List<Integer> safeClassIds = classIds == null ? List.of() : classIds.stream()
                .filter(validClassIds::contains)
                .distinct()
                .collect(Collectors.toList());
        if (teacher == null) {
            redirectAttributes.addFlashAttribute("error", "Giáo viên không thuộc trường đang chọn.");
        } else if (safeClassIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Hãy chọn ít nhất một lớp.");
        } else {
            int assigned = assignmentService.assignTeacherToClasses(safeClassIds, subjectId, teacherId);
            redirectAttributes.addFlashAttribute("message", "Đã phân công " + assigned + " lớp.");
        }
        return redirectWithFilters(school.getId(), grade, schoolYear, keyword);
    }

    @PostMapping("/matrix")
    public String saveMatrix(@RequestParam(value = "schoolId", required = false) Integer schoolId,
                             @RequestParam(value = "grade", required = false) Integer grade,
                             @RequestParam(value = "schoolYear", defaultValue = "") String schoolYear,
                             @RequestParam(value = "keyword", defaultValue = "") String keyword,
                             @RequestParam Map<String, String> parameters,
                             RedirectAttributes redirectAttributes) {
        School school = resolveSchool(schoolId);
        if (school == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy trường học để phân công.");
            return redirect();
        }
        Set<Integer> validClassIds = classRepository.findBySchoolId(school.getId()).stream()
                .map(StudentTrackingClass::getId)
                .collect(Collectors.toSet());
        Set<Integer> validSubjectIds = subjectService.getAll().stream()
                .map(Subject::getId)
                .collect(Collectors.toSet());
        Set<String> validTeacherIds = userRepository.findTeachersBySchool(school.getId()).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        int changed = 0;
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (!parameter.getKey().startsWith("assignment_")) {
                continue;
            }
            String[] parts = parameter.getKey().substring("assignment_".length()).split("_");
            if (parts.length != 2) {
                continue;
            }
            try {
                Integer classId = Integer.valueOf(parts[0]);
                Integer subjectId = Integer.valueOf(parts[1]);
                if (!validClassIds.contains(classId) || !validSubjectIds.contains(subjectId)) {
                    continue;
                }
                String teacherId = parameter.getValue();
                if (teacherId == null || teacherId.trim().isEmpty()) {
                    assignmentService.removeAssignment(classId, subjectId);
                } else if (validTeacherIds.contains(teacherId)) {
                    assignmentService.assignTeacher(classId, subjectId, teacherId);
                } else {
                    continue;
                }
                changed++;
            } catch (NumberFormatException ignored) {
                // Ignore non-matrix parameters instead of failing the whole save.
            }
        }
        redirectAttributes.addFlashAttribute("message", "Đã lưu " + changed + " thay đổi trong ma trận giảng dạy.");
        return redirectWithFilters(school.getId(), grade, schoolYear, keyword);
    }

    private void populate(Model model, School school, Integer grade, String schoolYear, String keyword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        List<StudentTrackingClass> classes = classRepository.findBySchoolId(school.getId()).stream()
                .filter(item -> grade == null || grade.equals(item.getGrade()))
                .filter(item -> schoolYear == null || schoolYear.isBlank() || schoolYear.equals(item.getSchoolYear()))
                .filter(item -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(item.getClassName(), keyword))
                .sorted(Comparator.comparing(StudentTrackingClass::getGrade,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StudentTrackingClass::getClassName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        List<Subject> subjects = subjectService.getAll().stream()
                .sorted(Comparator.comparing(Subject::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        List<User> teachers = userRepository.findTeachersBySchool(school.getId());
        List<ClassSubjectAssignment> assignments = assignmentService.findBySchoolId(school.getId());
        Map<String, ClassSubjectAssignment> assignmentByKey = new HashMap<>();
        Map<Integer, Integer> assignmentCountByClass = new HashMap<>();
        for (ClassSubjectAssignment assignment : assignments) {
            if (assignment.getTrackingClass() == null || assignment.getSubject() == null) {
                continue;
            }
            String key = assignment.getTrackingClass().getId() + "_" + assignment.getSubject().getId();
            assignmentByKey.put(key, assignment);
            assignmentCountByClass.merge(assignment.getTrackingClass().getId(), 1, Integer::sum);
        }
        Set<Integer> availableGrades = classRepository.findBySchoolId(school.getId()).stream()
                .map(StudentTrackingClass::getGrade)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        model.addAttribute("targetSchool", school);
        model.addAttribute("allSchools", isAdmin ? schoolRepository.findAllByActiveTrueOrderByNameAsc() : List.of());
        model.addAttribute("classes", classes);
        model.addAttribute("schoolTeachers", teachers);
        model.addAttribute("allSubjects", subjects);
        model.addAttribute("assignmentByKey", assignmentByKey);
        model.addAttribute("assignmentCountByClass", assignmentCountByClass);
        model.addAttribute("availableGrades", availableGrades);
        model.addAttribute("grade", grade);
        model.addAttribute("schoolYear", schoolYear == null ? "" : schoolYear);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("isAdmin", isAdmin);
    }

    private School resolveSchool(Integer schoolId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "ROLE_ADMIN")) {
            if (schoolId != null) {
                return schoolRepository.findById(schoolId).orElse(null);
            }
            return schoolRepository.findAllByActiveTrueOrderByNameAsc().stream().findFirst().orElse(null);
        }
        User currentUser = authentication == null ? null : userService.findById(authentication.getName());
        return currentUser == null ? null : currentUser.getSchool();
    }

    private User findTeacherInSchool(String teacherId, Integer schoolId) {
        if (teacherId == null) {
            return null;
        }
        return userRepository.findTeachersBySchool(schoolId).stream()
                .filter(teacher -> teacherId.equals(teacher.getId()))
                .findFirst().orElse(null);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && keyword != null && value.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private String redirect() {
        return "redirect:/school-admin/teaching-assignments";
    }

    private String redirectWithFilters(Integer schoolId, Integer grade, String schoolYear, String keyword) {
        StringBuilder url = new StringBuilder("redirect:/school-admin/teaching-assignments?schoolId=")
                .append(schoolId);
        if (grade != null) url.append("&grade=").append(grade);
        if (schoolYear != null && !schoolYear.isBlank()) url.append("&schoolYear=").append(java.net.URLEncoder.encode(schoolYear, java.nio.charset.StandardCharsets.UTF_8));
        if (keyword != null && !keyword.isBlank()) url.append("&keyword=").append(java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8));
        return url.toString();
    }
}
