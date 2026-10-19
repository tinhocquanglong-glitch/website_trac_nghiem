package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
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

        // Lấy danh sách các trường và người quản lý trường
        List<School> schools = schoolRepository.findAllByActiveTrueOrderByNameAsc();
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
            schoolLevel = currentUser.getSchoolLevel(); // Giáo viên kế thừa cấp học từ Admin Trường
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
    public String editMember(@PathVariable("id") String id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "user/editMember";
    }

    @PostMapping("/user/update")
    public String update(@Validated User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasFieldErrors()) {
            return "user/editMember";
        } else {
            User user1 = userService.findById(user.getId());
            user1.setFullName(user.getFullName());
            user1.setEmail(user.getEmail());
            user1.setAddress(user.getAddress());
            user1.setPhoneNumber(user.getPhoneNumber());
            userService.save(user1);
            redirectAttributes.addFlashAttribute("message", "Cập Nhật Thành Công !");
            return "redirect:/user/list";
        }
    }

    @GetMapping("/user/delete/{id}")
    public String delete(@PathVariable("id") String id, RedirectAttributes ra) {
        User user = userService.findById(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = authentication != null && authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = authentication != null ? userService.findById(authentication.getName()) : null;

        if (user == null || user.getRoles() == null || user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()))) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản quản trị hệ thống.");
            return isSchoolAdmin ? "redirect:/school-admin/users" : "redirect:/user/list";
        }
        if (isSchoolAdmin) {
            boolean isStudent = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_USER".equals(role.getRoleName()));
            Integer currentSchoolId = currentUser != null && currentUser.getSchool() != null
                    ? currentUser.getSchool().getId() : null;
            Integer studentSchoolId = schoolIdForUser(user);
            boolean sameSchool = currentSchoolId != null
                    && Objects.equals(currentSchoolId, studentSchoolId);
            if (!isStudent || !sameSchool) {
                ra.addFlashAttribute("error", "Hiệu trưởng chỉ được xóa học sinh thuộc trường mình.");
                return "redirect:/school-admin/users";
            }
        }
        try {
            userService.delete(user);
        } catch (DataAccessException ex) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản vì vẫn còn dữ liệu liên quan.");
            return isSchoolAdmin ? "redirect:/school-admin/users" : "redirect:/user/list";
        }
        ra.addFlashAttribute("message","Xóa người dùng thành công!");
        return isSchoolAdmin ? "redirect:/school-admin/users" : "redirect:/user/list";
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

    @GetMapping("/user/editPass/{id}")
    public String editPass(@PathVariable("id") String id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "user/editPass";
    }

    @PostMapping("/user/updatePass")
    public String updatePass(@Valid @ModelAttribute User user, BindingResult bindingResult, Model model,
                             @RequestParam("oldPass") String oldPass, RedirectAttributes redirectAttributes) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String oldPass2 = userService.findByPass(user.getId());
        if (!passwordEncoder.matches(oldPass,oldPass2)){
            model.addAttribute("messages","Mật Khẩu Cũ Không Đúng !");
            return "user/editPass";
        }
        if (user.getPassWord() != null && user.getRePassWord() != null) {
            if (!user.getPassWord().equals(user.getRePassWord())) {
                bindingResult.addError(new FieldError("user", "rePassWord", "Mật khẩu phải trùng nhau"));
            }
        }
        if (bindingResult.hasFieldErrors()) {
            return "user/editPass";
        }
        User user1 = userService.findById(user.getId());
        user1.setPassWord(passwordEncoder.encode(user.getPassWord()));
        user1.setRePassWord(passwordEncoder.encode(user.getRePassWord()));
        userService.save(user1);
        redirectAttributes.addFlashAttribute("message", "Cập Nhật Thành Công !");
        return "redirect:/user/list";
    }
}
