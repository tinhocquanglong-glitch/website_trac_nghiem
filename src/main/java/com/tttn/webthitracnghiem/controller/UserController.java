package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@SessionAttributes("admin")
public class UserController {
    @Autowired
    IUserService userService;

    @Autowired
    private com.tttn.webthitracnghiem.service.IRoleService roleService;

    @Autowired
    private com.tttn.webthitracnghiem.service.ILocationService locationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassSubjectAssignmentRepository assignmentRepository;

    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;

    @Autowired
    private com.tttn.webthitracnghiem.repository.SchoolRepository schoolRepository;

    @ModelAttribute("admin")
    public User admin(){
        return userService.findById(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    /**
     * Trang quản lý người dùng trong trường - dành cho Hiệu trưởng (ROLE_SCHOOL_ADMIN)
     * hoặc Admin toàn quyền (ROLE_ADMIN) xem theo từng trường.
     */
    @GetMapping("/school-admin/users")
    public String schoolUsers(
            @RequestParam(value = "schoolId", required = false) Integer schoolId,
            @RequestParam(value = "teacherKeyword", required = false, defaultValue = "") String teacherKeyword,
            @RequestParam(value = "studentKeyword", required = false, defaultValue = "") String studentKeyword,
            @RequestParam(value = "studentGrade", required = false) Integer studentGrade,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        if (!isAdmin && !isSchoolAdmin) {
            return "redirect:/";
        }

        User currentUser = userService.findById(auth.getName());
        com.tttn.webthitracnghiem.model.School targetSchool = null;

        if (isAdmin) {
            List<com.tttn.webthitracnghiem.model.School> allSchools = schoolRepository.findAllByActiveTrueOrderByNameAsc();
            model.addAttribute("allSchools", allSchools);
            if (schoolId != null) {
                targetSchool = schoolRepository.findById(schoolId).orElse(null);
            }
            if (targetSchool == null && !allSchools.isEmpty()) {
                targetSchool = allSchools.get(0);
            }
        } else if (currentUser != null && currentUser.getSchool() != null) {
            targetSchool = currentUser.getSchool();
        }

        if (targetSchool == null) {
            model.addAttribute("error", "Không tìm thấy thông tin trường học.");
            return "user/school-users";
        }

        int targetSchoolId = targetSchool.getId();

        // --- Giáo viên ---
        List<User> teachers;
        if (teacherKeyword != null && !teacherKeyword.trim().isEmpty()) {
            teachers = userRepository.searchTeachersBySchool(targetSchoolId, teacherKeyword.trim());
        } else {
            teachers = userRepository.findTeachersBySchool(targetSchoolId);
        }

        // Lấy thông tin phân công (môn - lớp) và GVCN cho từng giáo viên
        List<ClassSubjectAssignment> allAssignments = assignmentRepository.findBySchoolId(targetSchoolId);
        Map<String, List<ClassSubjectAssignment>> teacherAssignments = new HashMap<>();
        for (ClassSubjectAssignment a : allAssignments) {
            String tid = a.getTeacher().getId();
            teacherAssignments.computeIfAbsent(tid, k -> new ArrayList<>()).add(a);
        }

        // Map teacherId -> list lớp GVCN
        List<com.tttn.webthitracnghiem.model.StudentTrackingClass> allClasses = trackingClassRepository.findBySchoolId(targetSchoolId);
        Map<String, List<com.tttn.webthitracnghiem.model.StudentTrackingClass>> homeroomMap = new HashMap<>();
        for (com.tttn.webthitracnghiem.model.StudentTrackingClass cls : allClasses) {
            if (cls.getHomeroomTeacher() != null) {
                String tid = cls.getHomeroomTeacher().getId();
                homeroomMap.computeIfAbsent(tid, k -> new ArrayList<>()).add(cls);
            }
        }

        model.addAttribute("teachers", teachers);
        model.addAttribute("teacherAssignments", teacherAssignments);
        model.addAttribute("homeroomMap", homeroomMap);
        model.addAttribute("teacherKeyword", teacherKeyword);

        // --- Học sinh ---
        List<User> students;
        if (studentKeyword != null && !studentKeyword.trim().isEmpty()) {
            students = userRepository.searchStudentsBySchool(targetSchoolId, studentKeyword.trim());
        } else if (studentGrade != null) {
            students = userRepository.findStudentsBySchoolAndLevel(targetSchoolId, studentGrade);
        } else {
            students = userRepository.findStudentsBySchool(targetSchoolId);
        }

        // Nhóm học sinh theo cấp học -> khối -> lớp
        Map<Integer, Map<Integer, Map<String, List<User>>>> groupedStudents = new TreeMap<>();
        for (User s : students) {
            Integer level = s.getSchoolLevel() != null ? s.getSchoolLevel() : 0;
            Integer grade = (s.getTrackingClass() != null && s.getTrackingClass().getGrade() != null)
                    ? s.getTrackingClass().getGrade() : 0;
            String className = (s.getTrackingClass() != null) ? s.getTrackingClass().getClassName() : "Chưa có lớp";

            groupedStudents
                .computeIfAbsent(level, k -> new TreeMap<>())
                .computeIfAbsent(grade, k -> new TreeMap<>())
                .computeIfAbsent(className, k -> new ArrayList<>())
                .add(s);
        }

        model.addAttribute("groupedStudents", groupedStudents);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("studentKeyword", studentKeyword);
        model.addAttribute("studentGrade", studentGrade);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("targetSchool", targetSchool);
        model.addAttribute("schoolName", targetSchool.getName());
        model.addAttribute("isAdmin", isAdmin);

        return "user/school-users";
    }

    /**
     * Quản lý người dùng cho Admin toàn quyền:
     * Hiển thị danh sách Người quản lý từng trường (Hiệu trưởng / Admin trường)
     * Kèm theo nút xem chi tiết Thầy cô và Học sinh của trường đó (tách riêng).
     */
    @GetMapping("/user/list")
    public String showList(@RequestParam("keyword") Optional<String> name,
                           @RequestParam(value = "viewMode", defaultValue = "schools") String viewMode,
                           @RequestParam(value = "address", required = false) String address,
                           Model model, @PageableDefault(value = 10) Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // Lấy danh sách các trường (kể cả trường đang khóa để admin mở khóa) và người quản lý trường
        List<School> schools = schoolRepository.findAllByOrderByNameAsc();
        List<User> schoolAdmins = userRepository.findAllSchoolAdmins();

        Map<Integer, List<User>> schoolManagersMap = new HashMap<>();
        for (User u : schoolAdmins) {
            if (u.getSchool() != null) {
                schoolManagersMap.computeIfAbsent(u.getSchool().getId(), k -> new ArrayList<>()).add(u);
            }
        }

        // Đếm giáo viên và học sinh mỗi trường
        Map<Integer, Integer> teacherCountMap = new HashMap<>();
        Map<Integer, Integer> studentCountMap = new HashMap<>();
        String addressKeyword = address == null ? "" : address.trim();

        for (School s : schools) {
            teacherCountMap.put(s.getId(), userRepository.findTeachersBySchool(s.getId()).size());
            studentCountMap.put(s.getId(), userRepository.findStudentsBySchool(s.getId()).size());
        }

        // Lọc trường theo keyword nếu có
        List<com.tttn.webthitracnghiem.model.School> filteredSchools = new ArrayList<>();
        String kw = name.orElse("").trim().toLowerCase();
        boolean hasSchoolSearch = !addressKeyword.isEmpty() || !kw.isEmpty();
        for (School s : schools) {
            if (!hasSchoolSearch) {
                continue;
            }
            String currentAddress = schoolAddress(s);
            if (!addressKeyword.isEmpty() && !SchoolAddressMatcher.matches(s, addressKeyword)) {
                continue;
            }
            if (kw.isEmpty() || s.getName().toLowerCase().contains(kw) ||
                    (s.getSchoolCode() != null && s.getSchoolCode().toLowerCase().contains(kw)) ||
                    currentAddress.toLowerCase().contains(kw)) {
                filteredSchools.add(s);
            } else {
                List<User> mgrs = schoolManagersMap.get(s.getId());
                if (mgrs != null && mgrs.stream().anyMatch(m -> m.getFullName().toLowerCase().contains(kw) || m.getId().toLowerCase().contains(kw))) {
                    filteredSchools.add(s);
                }
            }
        }

        model.addAttribute("schools", filteredSchools);
        model.addAttribute("schoolManagersMap", schoolManagersMap);
        model.addAttribute("teacherCountMap", teacherCountMap);
        model.addAttribute("studentCountMap", studentCountMap);
        model.addAttribute("addressGroups", new LinkedHashMap<>());
        model.addAttribute("selectedAddress", null);
        model.addAttribute("addressKeyword", addressKeyword);
        model.addAttribute("hasSchoolSearch", hasSchoolSearch);
        // School lock/delete actions return to this same filtered list.
        model.addAttribute("returnUrl", UriComponentsBuilder.fromPath("/user/list")
                .queryParam("viewMode", "schools")
                .queryParam("keyword", name.orElse(""))
                .queryParam("address", addressKeyword)
                .encode().build().toUriString());
        model.addAttribute("keyword", name.orElse(""));
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("isAdmin", isAdmin);

        // Danh sách user thô cho chế độ all
        Page<User> users;
        if (name.isPresent() && !name.get().trim().isEmpty()) {
            users = userService.search(name.get().trim(), pageable);
        } else {
            users = userService.findAll(pageable);
        }
        model.addAttribute("users", users);

        return "user/list";
    }

    private String schoolAddress(School school) {
        return SchoolAddressMatcher.format(school);
    }


    @GetMapping("/user/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "user/create";
    }

    @PostMapping("/user/create")
    public String saveUser(@Validated @ModelAttribute("user") User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) throws ParseException {
        if (userService.userExists(user.getId())) {
            bindingResult.rejectValue("id", "error.user", "Tài khoản '" + user.getId() + "' đã tồn tại.");
        }
        if (userService.userExistss(user.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Email '" + user.getEmail() + "' đã tồn tại.");
        }
        if (user.getPassWord() != null && user.getRePassWord() != null) {
            if (!user.getPassWord().equals(user.getRePassWord())) {
                bindingResult.rejectValue("rePassWord", "error.user", "Mật khẩu phải trùng nhau");
            }
        }
        if (bindingResult.hasFieldErrors()) {
            return "user/create";
        } else {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            user.setPassWord(passwordEncoder.encode(user.getPassWord()));
            user.setRePassWord(user.getPassWord());
            user.setImg("/img/avatar/default.jpg");
            user.setCreateDate(new java.sql.Date(System.currentTimeMillis()));

            com.tttn.webthitracnghiem.model.Role role = roleService.findByName("ROLE_USER");
            if (role != null) {
                java.util.Set<com.tttn.webthitracnghiem.model.Role> roles = new java.util.HashSet<>();
                roles.add(role);
                user.setRoles(roles);
            }
            userService.save(user);
            redirectAttributes.addFlashAttribute("message", "Thêm người dùng thành công!");
            return "redirect:/user/list";
        }
    }

    @GetMapping("/user/create-teacher")
    public String showCreateTeacherForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        User currentUser = (auth != null) ? userService.findById(auth.getName()) : null;
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isSchoolAdmin", isSchoolAdmin);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", new User());
        model.addAttribute("provinces", locationService.getAllProvinces());
        return "user/create-teacher";
    }

    @PostMapping("/user/create-teacher")
    public String saveTeacher(@Validated @ModelAttribute("user") User user,
                              BindingResult bindingResult,
                              @RequestParam(value = "schoolId", required = false) Integer schoolId,
                              @RequestParam(value = "roleType", defaultValue = "ROLE_TEACHER") String roleType,
                              @RequestParam(value = "schoolLevel", required = false) Integer schoolLevel,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = (auth != null) ? userService.findById(auth.getName()) : null;

        if (isSchoolAdmin && currentUser != null) {
            if (currentUser.getSchool() != null) {
                schoolId = currentUser.getSchool().getId();
            }
            roleType = "ROLE_TEACHER";
            if (currentUser.getSchoolLevel() != null) {
                schoolLevel = currentUser.getSchoolLevel(); // Trường đơn cấp: kế thừa cấp học của Admin Trường
            }
            // Trường toàn cấp/liên cấp (currentUser.getSchoolLevel() == null): nhận schoolLevel từ form (nếu để trống = toàn cấp)
        }

        if (schoolId == null) {
            model.addAttribute("schoolError", "Vui lòng chọn trường học!");
        }
        if (userService.userExists(user.getId())) {
            bindingResult.rejectValue("id", "error.user", "Tên tài khoản '" + user.getId() + "' đã tồn tại.");
        }
        if (userService.userExistss(user.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Email '" + user.getEmail() + "' đã tồn tại.");
        }
        if (user.getPassWord() != null && user.getRePassWord() != null) {
            if (!user.getPassWord().equals(user.getRePassWord())) {
                bindingResult.rejectValue("rePassWord", "error.user", "Mật khẩu nhập lại không khớp!");
            }
        }
        if (bindingResult.hasErrors() || schoolId == null) {
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isSchoolAdmin", isSchoolAdmin);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("provinces", locationService.getAllProvinces());
            model.addAttribute("selectedSchoolId", schoolId);
            model.addAttribute("selectedRoleType", roleType);
            model.addAttribute("selectedSchoolLevel", schoolLevel);
            return "user/create-teacher";
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassWord(passwordEncoder.encode(user.getPassWord()));
        user.setRePassWord(user.getPassWord());
        user.setImg("/img/avatar/default.jpg");
        user.setCreateDate(new java.sql.Date(System.currentTimeMillis()));

        com.tttn.webthitracnghiem.model.School school = locationService.findSchoolById(schoolId);
        user.setSchool(school);
        user.setSchoolLevel(schoolLevel);

        com.tttn.webthitracnghiem.model.Role role = roleService.findByName(roleType);
        if (role == null) {
            role = roleService.findByName("ROLE_TEACHER");
        }
        if (role != null) {
            java.util.Set<com.tttn.webthitracnghiem.model.Role> roles = new java.util.HashSet<>();
            roles.add(role);
            user.setRoles(roles);
        }

        userService.save(user);
        String label = "ROLE_SCHOOL_ADMIN".equals(roleType) ? "Admin Trường" : "Giáo viên";
        String levelTxt = schoolLevel != null ? " (" + user.getSchoolLevelName() + ")" : "";
        redirectAttributes.addFlashAttribute("message", "Cấp tài khoản " + label + levelTxt + " thành công cho " + user.getFullName() + " (" + (school != null ? school.getName() : "") + ")!");
        if (isSchoolAdmin) {
            return "redirect:/student-tracking";
        }
        return "redirect:/user/list";
    }

    @GetMapping("/user/editMember/{id}")
    public String editMember(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        String denied = editPermissionError(user);
        if (denied != null) {
            redirectAttributes.addFlashAttribute("error", denied);
            return "redirect:" + userManagementUrl();
        }
        model.addAttribute("user", user);
        model.addAttribute("backUrl", userManagementUrl());
        return "user/editMember";
    }

    @PostMapping("/user/update")
    public String update(@Validated User user, BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        User user1 = userService.findById(user.getId());
        String denied = editPermissionError(user1);
        if (denied != null) {
            redirectAttributes.addFlashAttribute("error", denied);
            return "redirect:" + userManagementUrl();
        }
        // The form only edits profile fields; password constraints on the entity do not apply here.
        if (bindingResult.getFieldErrors().stream().anyMatch(error -> EDITABLE_PROFILE_FIELDS.contains(error.getField()))) {
            model.addAttribute("backUrl", userManagementUrl());
            return "user/editMember";
        }
        user1.setFullName(user.getFullName().trim());
        user1.setEmail(user.getEmail().trim());
        user1.setAddress(user.getAddress().trim());
        user1.setPhoneNumber(user.getPhoneNumber().trim());
        userService.save(user1);
        redirectAttributes.addFlashAttribute("message", "Đã cập nhật thông tin " + user1.getFullName() + ".");
        return "redirect:" + userManagementUrl();
    }

    private static final Set<String> EDITABLE_PROFILE_FIELDS = Set.of("fullName", "email", "address", "phoneNumber");

    /**
     * Returns why the current account may not edit {@code target}, or null when it may.
     * A principal only edits teachers and students of their own school.
     */
    private String editPermissionError(User target) {
        if (target == null) {
            return "Không tìm thấy tài khoản.";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(auth, "ROLE_ADMIN")) {
            return null;
        }
        if (!hasAuthority(auth, "ROLE_SCHOOL_ADMIN")) {
            return "Bạn không có quyền sửa tài khoản này.";
        }
        Set<String> targetRoles = target.getRoles() == null ? Set.of() : target.getRoles().stream()
                .map(com.tttn.webthitracnghiem.model.Role::getRoleName)
                .collect(Collectors.toSet());
        if (targetRoles.contains("ROLE_ADMIN") || targetRoles.contains("ROLE_SCHOOL_ADMIN")
                || (!targetRoles.contains("ROLE_TEACHER") && !targetRoles.contains("ROLE_USER"))) {
            return "Hiệu trưởng chỉ được sửa thông tin giáo viên và học sinh.";
        }
        User currentUser = userService.findById(auth.getName());
        Integer currentSchoolId = currentUser != null && currentUser.getSchool() != null
                ? currentUser.getSchool().getId() : null;
        if (currentSchoolId == null || !Objects.equals(currentSchoolId, schoolIdForUser(target))) {
            return "Hiệu trưởng chỉ được sửa tài khoản thuộc trường mình.";
        }
        return null;
    }

    private String userManagementUrl() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasAuthority(auth, "ROLE_ADMIN") ? "/user/list" : "/school-admin/users";
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority(authority));
    }

    @PostMapping("/user/delete")
    public String deletePost(@RequestParam("id") String id,
                             @RequestParam(value = "password", required = false) String password,
                             @RequestParam(value = "returnUrl", required = false) String returnUrl,
                             RedirectAttributes ra) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thực hiện thao tác này!");
            return "redirect:/login";
        }
        User currentUser = userService.findById(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        String fallbackUrl = isSchoolAdmin ? "/school-admin/users" : "/user/list";
        String redirectTarget = (returnUrl != null && !returnUrl.trim().isEmpty() && returnUrl.startsWith("/"))
                ? returnUrl : fallbackUrl;

        if (!isAdmin && !isSchoolAdmin) {
            ra.addFlashAttribute("error", "Chỉ Quản trị viên hệ thống hoặc Hiệu trưởng mới có quyền xóa tài khoản!");
            return "redirect:" + redirectTarget;
        }

        User user = userService.findById(id);
        if (user == null) {
            ra.addFlashAttribute("error", "Không tìm thấy tài khoản cần xóa!");
            return "redirect:" + redirectTarget;
        }

        if (user.getRoles() != null && user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()))) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản Quản trị viên hệ thống.");
            return "redirect:" + redirectTarget;
        }

        if (isSchoolAdmin && !isAdmin) {
            boolean isStudent = user.getRoles().stream().anyMatch(role -> "ROLE_USER".equals(role.getRoleName()));
            boolean isTeacher = user.getRoles().stream().anyMatch(role -> "ROLE_TEACHER".equals(role.getRoleName()));
            Integer currentSchoolId = currentUser != null && currentUser.getSchool() != null
                    ? currentUser.getSchool().getId() : null;
            Integer targetSchoolId = schoolIdForUser(user);
            boolean sameSchool = currentSchoolId != null && Objects.equals(currentSchoolId, targetSchoolId);
            if ((!isStudent && !isTeacher) || !sameSchool) {
                ra.addFlashAttribute("error", "Hiệu trưởng chỉ được xóa giáo viên hoặc học sinh thuộc trường mình.");
                return "redirect:" + redirectTarget;
            }
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (password == null || password.trim().isEmpty() || currentUser == null || !encoder.matches(password.trim(), currentUser.getPassWord())) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa (ẩn) tài khoản.");
            return "redirect:" + redirectTarget;
        }

        try {
            userService.delete(user);
            ra.addFlashAttribute("message", "Đã xóa (vô hiệu hóa) tài khoản " + user.getFullName() + " thành công!");
        } catch (DataAccessException ex) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản vì vẫn còn dữ liệu liên quan.");
        }
        return "redirect:" + redirectTarget;
    }

    @GetMapping("/user/delete/{id}")
    public String deleteLegacy(@PathVariable("id") String id,
                               @RequestParam(value = "returnUrl", required = false) String returnUrl,
                               RedirectAttributes ra) {
        String fallbackUrl = "/user/list";
        String redirectTarget = (returnUrl != null && !returnUrl.trim().isEmpty() && returnUrl.startsWith("/"))
                ? returnUrl : fallbackUrl;
        ra.addFlashAttribute("error", "Hành động xóa tài khoản yêu cầu xác thực mật khẩu qua biểu mẫu bảo mật!");
        return "redirect:" + redirectTarget;
    }

    public String delete(String id, String returnUrl, RedirectAttributes ra) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = authentication != null ? userService.findById(authentication.getName()) : null;
        boolean isSchoolAdmin = authentication != null && authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        String fallbackUrl = isSchoolAdmin ? "/school-admin/users" : "/user/list";
        String redirectTarget = (returnUrl != null && !returnUrl.trim().isEmpty() && returnUrl.startsWith("/"))
                ? returnUrl : fallbackUrl;

        User user = userService.findById(id);
        if (user == null || user.getRoles() == null || user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()))) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản quản trị hệ thống.");
            return "redirect:" + redirectTarget;
        }

        if (isSchoolAdmin) {
            boolean isStudent = user.getRoles().stream().anyMatch(role -> "ROLE_USER".equals(role.getRoleName()));
            boolean isTeacher = user.getRoles().stream().anyMatch(role -> "ROLE_TEACHER".equals(role.getRoleName()));
            Integer currentSchoolId = currentUser != null && currentUser.getSchool() != null
                    ? currentUser.getSchool().getId() : null;
            Integer targetSchoolId = schoolIdForUser(user);
            boolean sameSchool = currentSchoolId != null && Objects.equals(currentSchoolId, targetSchoolId);
            if ((!isStudent && !isTeacher) || !sameSchool) {
                ra.addFlashAttribute("error", "Hiệu trưởng chỉ được xóa giáo viên hoặc học sinh thuộc trường mình.");
                return "redirect:" + redirectTarget;
            }
        }

        try {
            userService.delete(user);
            ra.addFlashAttribute("message", "Đã xóa (vô hiệu hóa) tài khoản " + user.getFullName() + " thành công!");
        } catch (DataAccessException ex) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản vì vẫn còn dữ liệu liên quan.");
        }
        return "redirect:" + redirectTarget;
    }

    public String delete(String id, RedirectAttributes ra) {
        return delete(id, null, ra);
    }

    private Integer schoolIdForUser(User user) {
        if (user == null) {
            return null;
        }
        if (user.getTrackingClass() != null && user.getTrackingClass().getSchool() != null) {
            return user.getTrackingClass().getSchool().getId();
        }
        return user.getSchool() != null ? user.getSchool().getId() : null;
    }

    @PostMapping("/user/update-role")
    public String updateRole(@RequestParam("id") String id,
                             @RequestParam("roleType") String roleType,
                             RedirectAttributes ra) {
        User user = userService.findById(id);
        if (user == null) {
            ra.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/user/list?viewMode=all";
        }
        if (!Arrays.asList("ROLE_USER", "ROLE_TEACHER", "ROLE_SCHOOL_ADMIN").contains(roleType)) {
            ra.addFlashAttribute("error", "Vai trò không hợp lệ.");
            return "redirect:/user/list?viewMode=all";
        }
        com.tttn.webthitracnghiem.model.Role role = roleService.findByName(roleType);
        if (role == null) {
            ra.addFlashAttribute("error", "Vai trò chưa được cấu hình trong hệ thống.");
            return "redirect:/user/list?viewMode=all";
        }
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        userService.save(user);
        ra.addFlashAttribute("message", "Đã cập nhật vai trò cho tài khoản " + id + ".");
        return "redirect:/user/list?viewMode=all";
    }

    /**
     * Kiểm tra quyền cấp lại mật khẩu theo phân cấp:
     * - ROLE_ADMIN: cấp lại cho Hiệu trưởng (ROLE_SCHOOL_ADMIN), Giáo viên (ROLE_TEACHER), Học sinh (ROLE_USER).
     * - ROLE_SCHOOL_ADMIN: cấp lại cho Giáo viên và Học sinh thuộc cùng trường mình.
     * - ROLE_TEACHER: cấp lại cho Học sinh thuộc lớp mình chủ nhiệm hoặc phụ trách giảng dạy.
     * - Không ai được cấp lại mật khẩu cho tài khoản ADMIN qua chức năng phân cấp này.
     * Trả về null nếu được phép, hoặc thông báo lỗi nếu không được phép.
     */
    public String checkResetPasswordPermission(User currentUser, User targetUser) {
        if (targetUser == null) {
            return "Không tìm thấy tài khoản cần cấp lại mật khẩu.";
        }
        if (currentUser == null) {
            return "Bạn chưa đăng nhập.";
        }

        Set<String> currentRoles = currentUser.getRoles() == null ? Collections.emptySet() :
                currentUser.getRoles().stream().map(com.tttn.webthitracnghiem.model.Role::getRoleName).collect(Collectors.toSet());
        Set<String> targetRoles = targetUser.getRoles() == null ? Collections.emptySet() :
                targetUser.getRoles().stream().map(com.tttn.webthitracnghiem.model.Role::getRoleName).collect(Collectors.toSet());

        boolean isCurrentAdmin = currentRoles.contains("ROLE_ADMIN");
        boolean isCurrentSchoolAdmin = currentRoles.contains("ROLE_SCHOOL_ADMIN");
        boolean isCurrentTeacher = currentRoles.contains("ROLE_TEACHER");

        boolean isTargetAdmin = targetRoles.contains("ROLE_ADMIN");
        boolean isTargetSchoolAdmin = targetRoles.contains("ROLE_SCHOOL_ADMIN");
        boolean isTargetTeacher = targetRoles.contains("ROLE_TEACHER");
        boolean isTargetStudent = targetRoles.contains("ROLE_USER") || (!isTargetAdmin && !isTargetSchoolAdmin && !isTargetTeacher);

        // 1. Không thể cấp lại mật khẩu cho tài khoản ADMIN qua chức năng phân cấp này
        if (isTargetAdmin) {
            return "Không thể cấp lại mật khẩu cho tài khoản Quản trị viên hệ thống.";
        }

        // 2. ADMIN: Quyền cao nhất, có thể cấp lại mật khẩu cho Hiệu trưởng, Giáo viên, Học sinh
        if (isCurrentAdmin) {
            return null;
        }

        // 3. HIỆU TRƯỞNG: Cấp lại cho Giáo viên và Học sinh thuộc trường mình
        if (isCurrentSchoolAdmin) {
            if (isTargetSchoolAdmin) {
                return "Hiệu trưởng không thể cấp lại mật khẩu cho Hiệu trưởng khác.";
            }
            if (!isTargetTeacher && !isTargetStudent) {
                return "Hiệu trưởng chỉ được cấp lại mật khẩu cho Giáo viên và Học sinh.";
            }
            Integer currentSchoolId = currentUser.getSchool() != null ? currentUser.getSchool().getId() : null;
            Integer targetSchoolId = schoolIdForUser(targetUser);
            if (currentSchoolId == null || !Objects.equals(currentSchoolId, targetSchoolId)) {
                return "Hiệu trưởng chỉ được cấp lại mật khẩu cho cán bộ, giáo viên và học sinh thuộc trường mình.";
            }
            return null;
        }

        // 4. GIÁO VIÊN / GVCN: Cấp lại cho Học sinh thuộc lớp mình
        if (isCurrentTeacher) {
            if (isTargetSchoolAdmin || isTargetTeacher) {
                return "Giáo viên không có quyền cấp lại mật khẩu cho Ban giám hiệu hoặc Giáo viên khác.";
            }
            if (!isTargetStudent) {
                return "Giáo viên chỉ có thể cấp lại mật khẩu cho học sinh.";
            }

            StudentTrackingClass studentClass = targetUser.getTrackingClass();
            if (studentClass == null) {
                return "Học sinh này hiện chưa thuộc lớp học nào mà bạn quản lý.";
            }

            boolean isHomeroom = studentClass.getHomeroomTeacher() != null
                    && Objects.equals(studentClass.getHomeroomTeacher().getId(), currentUser.getId());

            boolean isAssignedSubject = false;
            if (assignmentRepository != null) {
                List<ClassSubjectAssignment> assignments = assignmentRepository.findByTrackingClassId(studentClass.getId());
                if (assignments != null) {
                    isAssignedSubject = assignments.stream()
                            .anyMatch(a -> a.getTeacher() != null && Objects.equals(a.getTeacher().getId(), currentUser.getId()));
                }
            }

            if (!isHomeroom && !isAssignedSubject) {
                return "Giáo viên chỉ được cấp lại mật khẩu cho học sinh thuộc lớp mình chủ nhiệm hoặc phụ trách giảng dạy.";
            }
            return null;
        }

        return "Bạn không có quyền thực hiện chức năng này.";
    }

    private String getUserRoleLabel(User user) {
        if (user == null || user.getRoles() == null) return "người dùng";
        for (com.tttn.webthitracnghiem.model.Role r : user.getRoles()) {
            if ("ROLE_ADMIN".equals(r.getRoleName())) return "Quản trị viên";
            if ("ROLE_SCHOOL_ADMIN".equals(r.getRoleName())) return "Hiệu trưởng / Quản lý trường";
            if ("ROLE_TEACHER".equals(r.getRoleName())) return "Giáo viên";
            if ("ROLE_USER".equals(r.getRoleName())) return "Học sinh";
        }
        return "người dùng";
    }

    private String determineDefaultReturnUrl(User currentUser) {
        if (currentUser == null) return "/";
        Set<String> roles = currentUser.getRoles() == null ? Collections.emptySet() :
                currentUser.getRoles().stream().map(com.tttn.webthitracnghiem.model.Role::getRoleName).collect(Collectors.toSet());
        if (roles.contains("ROLE_ADMIN")) return "/user/list";
        if (roles.contains("ROLE_SCHOOL_ADMIN")) return "/school-admin/users";
        if (roles.contains("ROLE_TEACHER")) return "/teacher/homeroom";
        return "/";
    }

    @PostMapping("/user/reset-password")
    public String resetPassword(@RequestParam("userId") String userId,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null) ? userService.findById(auth.getName()) : null;
        User targetUser = userService.findById(userId != null ? userId.trim() : "");

        String fallbackUrl = determineDefaultReturnUrl(currentUser);
        String redirectTarget = (returnUrl != null && !returnUrl.trim().isEmpty()) ? returnUrl.trim() : fallbackUrl;

        String permissionDenied = checkResetPasswordPermission(currentUser, targetUser);
        if (permissionDenied != null) {
            redirectAttributes.addFlashAttribute("error", permissionDenied);
            return "redirect:" + redirectTarget;
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "redirect:" + redirectTarget;
        }

        if (confirmPassword != null && !confirmPassword.trim().isEmpty() && !newPassword.trim().equals(confirmPassword.trim())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp với mật khẩu mới.");
            return "redirect:" + redirectTarget;
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encoded = passwordEncoder.encode(newPassword.trim());
        targetUser.setPassWord(encoded);
        targetUser.setRePassWord(encoded);
        userService.save(targetUser);

        String roleLabel = getUserRoleLabel(targetUser);
        redirectAttributes.addFlashAttribute("message",
                "Đã cấp lại mật khẩu thành công cho " + roleLabel + " " + targetUser.getFullName() + " (@" + targetUser.getId() + ")!");
        return "redirect:" + redirectTarget;
    }

    @GetMapping("/user/editPass/{id}")
    public String editPass(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null) ? userService.findById(auth.getName()) : null;
        User targetUser = userService.findById(id);

        String denied = checkResetPasswordPermission(currentUser, targetUser);
        if (denied != null) {
            redirectAttributes.addFlashAttribute("error", denied);
            return "redirect:" + determineDefaultReturnUrl(currentUser);
        }

        model.addAttribute("user", targetUser);
        model.addAttribute("backUrl", determineDefaultReturnUrl(currentUser));
        return "user/editPass";
    }

    @PostMapping("/user/updatePass")
    public String updatePass(@RequestParam("id") String id,
                             @RequestParam("newPassword") String newPassword,
                             @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                             @RequestParam(value = "returnUrl", required = false) String returnUrl,
                             RedirectAttributes redirectAttributes) {
        return resetPassword(id, newPassword, confirmPassword, returnUrl, redirectAttributes);
    }
}
