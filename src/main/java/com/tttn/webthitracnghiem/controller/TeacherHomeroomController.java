package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IRoleService;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/teacher/homeroom")
public class TeacherHomeroomController {

    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private StudentTrackingService trackingService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping
    public String index(@RequestParam(required = false) Integer schoolId,
                        @RequestParam(required = false) Integer grade,
                        @RequestParam(required = false) Integer classId,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(name = "address", defaultValue = "") String addressKeyword,
                        Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = userService.findById(auth.getName());
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        boolean isTeacher = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER"));

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isSchoolAdmin", isSchoolAdmin);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("keyword", keyword);
        addressKeyword = addressKeyword == null ? "" : addressKeyword.trim();
        model.addAttribute("addressKeyword", addressKeyword);

        School targetSchool = null;
        List<School> allSchools = new ArrayList<>();
        Map<Integer, List<StudentTrackingClass>> classesByGrade = new TreeMap<>();
        List<StudentTrackingClass> availableClasses = new ArrayList<>();
        StudentTrackingClass selectedClass = null;
        List<User> students = new ArrayList<>();
        List<User> unassignedStudents = new ArrayList<>();

        if (isAdmin) {
            // ADMIN TOÀN QUYỀN: Xem theo Trường -> Khối -> Lớp -> Học sinh
            List<School> activeSchools = schoolRepository.findAllByActiveTrueOrderByNameAsc();
            if (!addressKeyword.isEmpty()) {
                for (School school : activeSchools) {
                    if (SchoolAddressMatcher.matches(school, addressKeyword)) {
                        allSchools.add(school);
                    }
                }
            }
            model.addAttribute("allSchools", allSchools);

            if (schoolId != null) {
                targetSchool = (addressKeyword.isEmpty() ? activeSchools : allSchools).stream()
                        .filter(school -> school.getId().equals(schoolId))
                        .findFirst()
                        .orElse(null);
            }

            if (targetSchool != null) {
                availableClasses = trackingClassRepository.findBySchoolIdOrderBySchoolYearDescClassNameAsc(targetSchool.getId());
                for (StudentTrackingClass c : availableClasses) {
                    int g = c.getGrade() != null ? c.getGrade() : 0;
                    classesByGrade.computeIfAbsent(g, k -> new ArrayList<>()).add(c);
                }

                if (classId != null) {
                    for (StudentTrackingClass c : availableClasses) {
                        if (c.getId().equals(classId)) {
                            selectedClass = c;
                            break;
                        }
                    }
                }
                if (selectedClass == null && !availableClasses.isEmpty()) {
                    // Nếu có chỉ định grade thì chọn lớp đầu tiên của grade đó
                    if (grade != null && classesByGrade.containsKey(grade) && !classesByGrade.get(grade).isEmpty()) {
                        selectedClass = classesByGrade.get(grade).get(0);
                    } else {
                        selectedClass = availableClasses.get(0);
                    }
                }
            }

        } else if (isSchoolAdmin) {
            // HIỆU TRƯỞNG: Cố định trường mình, quản lý Khối -> Lớp -> Học sinh
            targetSchool = currentUser.getSchool();
            if (targetSchool != null) {
                availableClasses = trackingClassRepository.findBySchoolIdOrderBySchoolYearDescClassNameAsc(targetSchool.getId());
                for (StudentTrackingClass c : availableClasses) {
                    int g = c.getGrade() != null ? c.getGrade() : 0;
                    classesByGrade.computeIfAbsent(g, k -> new ArrayList<>()).add(c);
                }

                if (classId != null) {
                    for (StudentTrackingClass c : availableClasses) {
                        if (c.getId().equals(classId)) {
                            selectedClass = c;
                            break;
                        }
                    }
                }
                if (selectedClass == null && !availableClasses.isEmpty()) {
                    if (grade != null && classesByGrade.containsKey(grade) && !classesByGrade.get(grade).isEmpty()) {
                        selectedClass = classesByGrade.get(grade).get(0);
                    } else {
                        selectedClass = availableClasses.get(0);
                    }
                }
            }

        } else {
            // GIÁO VIÊN CHỦ NHIỆM: Xem các lớp do chính mình chủ nhiệm
            availableClasses = trackingClassRepository.findByHomeroomTeacherIdOrderBySchoolYearDescClassNameAsc(currentUser.getId());
            for (StudentTrackingClass c : availableClasses) {
                int g = c.getGrade() != null ? c.getGrade() : 0;
                classesByGrade.computeIfAbsent(g, k -> new ArrayList<>()).add(c);
            }
            if (classId != null) {
                for (StudentTrackingClass c : availableClasses) {
                    if (c.getId().equals(classId)) {
                        selectedClass = c;
                        break;
                    }
                }
            }
            if (selectedClass == null && !availableClasses.isEmpty()) {
                selectedClass = availableClasses.get(0);
            }
            if (selectedClass != null) {
                targetSchool = selectedClass.getSchool();
            } else if (currentUser.getSchool() != null) {
                targetSchool = currentUser.getSchool();
            }
        }

        // Lấy danh sách học sinh của lớp được chọn
        if (selectedClass != null) {
            students = userRepository.findStudentsByClassIdAndKeyword(selectedClass.getId(), keyword.trim());

            int schId = (selectedClass.getSchool() != null) ? selectedClass.getSchool().getId()
                    : (targetSchool != null ? targetSchool.getId() : 0);
            if (schId > 0) {
                unassignedStudents = userRepository.findUnassignedStudentsBySchool(schId);
            } else {
                unassignedStudents = userRepository.findAllUnassignedStudents();
            }
        }

        model.addAttribute("targetSchool", targetSchool);
        model.addAttribute("classesByGrade", classesByGrade);
        model.addAttribute("selectedGrade", selectedClass != null ? selectedClass.getGrade() : grade);
        model.addAttribute("availableClasses", availableClasses);
        model.addAttribute("selectedClass", selectedClass);
        model.addAttribute("students", students);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("unassignedStudents", unassignedStudents);

        return "teacher/homeroom";
    }

    @PostMapping("/create-student")
    public String createStudent(@RequestParam Integer classId,
                                @RequestParam String username,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam String password,
                                @RequestParam(required = false) String phoneNumber,
                                RedirectAttributes redirectAttributes) {
        if (!hasManagePermission(classId)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền quản lý lớp học này.");
            return "redirect:/teacher/homeroom";
        }

        String cleanUsername = (username != null) ? username.trim() : "";
        String cleanFullName = (fullName != null) ? fullName.trim() : "";
        if (cleanUsername.isEmpty() || cleanFullName.isEmpty() || password == null
                || password.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập đầy đủ thông tin: Tên đăng nhập, họ tên và mật khẩu.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        if (userService.userExists(cleanUsername)) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản '" + cleanUsername + "' đã tồn tại trên hệ thống!");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        String studentEmail = (email != null && !email.trim().isEmpty())
                ? email.trim().toLowerCase(Locale.ROOT)
                : (cleanUsername.toLowerCase(Locale.ROOT) + "@student.school.vn");
        if (!isValidEmail(studentEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email khong dung dinh dang.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }
        if (userService.userExistss(studentEmail)) {
            studentEmail = cleanUsername.toLowerCase(Locale.ROOT) + "_" + System.currentTimeMillis() + "@student.school.vn";
        }

        StudentTrackingClass trackingClass = trackingClassRepository.findById(classId).orElse(null);
        if (trackingClass == null) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại.");
            return "redirect:/teacher/homeroom";
        }

        User student = new User();
        student.setId(cleanUsername);
        student.setPassWord(passwordEncoder.encode(password.trim()));
        student.setRePassWord(student.getPassWord());
        student.setFullName(cleanFullName);
        student.setEmail(studentEmail);
        String studentPhone = (phoneNumber != null && !phoneNumber.trim().isEmpty())
                ? phoneNumber.trim() : "0900000000";
        if (!studentPhone.matches("^((\\(84\\)\\+)|(0))[1-9][\\d]{8}$")) {
            redirectAttributes.addFlashAttribute("error", "So dien thoai khong dung dinh dang.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }
        student.setPhoneNumber(studentPhone);
        student.setAddress("Học sinh lớp " + trackingClass.getClassName());
        student.setImg("/img/avatar/default.jpg");
        student.setCreateDate(new java.sql.Date(System.currentTimeMillis()));
        student.setSchool(trackingClass.getSchool());
        student.setTrackingClass(trackingClass);

        if (trackingClass.getGrade() != null) {
            int g = trackingClass.getGrade();
            if (g <= 5) student.setSchoolLevel(1);
            else if (g <= 9) student.setSchoolLevel(2);
            else student.setSchoolLevel(3);
        }

        Role roleStudent = roleService.findByName("ROLE_USER");
        if (roleStudent == null) {
            redirectAttributes.addFlashAttribute("error", "He thong chua co role hoc sinh.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }
        student.setRoles(Set.of(roleStudent));
        try {
            userService.save(student);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", "Khong the tao tai khoan. Vui long kiem tra lai thong tin va thu lai.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        try {
            trackingService.addStudent(classId, cleanFullName);
        } catch (Exception ignored) {}

        redirectAttributes.addFlashAttribute("message", "Đã cấp tài khoản học sinh [" + cleanUsername + "] thành công vào lớp " + trackingClass.getClassName() + "!");
        return "redirect:/teacher/homeroom?classId=" + classId;
    }

    @PostMapping("/add-student")
    public String addExistingStudent(@RequestParam Integer classId,
                                     @RequestParam String studentUsername,
                                     RedirectAttributes redirectAttributes) {
        if (!hasManagePermission(classId)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền quản lý lớp học này.");
            return "redirect:/teacher/homeroom";
        }

        User student = userService.findById(studentUsername != null ? studentUsername.trim() : "");
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản học sinh.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        if (student.getTrackingClass() != null && !student.getTrackingClass().getId().equals(classId)) {
            redirectAttributes.addFlashAttribute("error", "Học sinh [" + student.getFullName() + "] đã thuộc lớp " + student.getTrackingClass().getClassName() + "!");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        StudentTrackingClass trackingClass = trackingClassRepository.findById(classId).orElse(null);
        if (trackingClass == null) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại.");
            return "redirect:/teacher/homeroom";
        }

        student.setTrackingClass(trackingClass);
        if (trackingClass.getSchool() != null) {
            student.setSchool(trackingClass.getSchool());
        }
        if (trackingClass.getGrade() != null) {
            int g = trackingClass.getGrade();
            if (g <= 5) student.setSchoolLevel(1);
            else if (g <= 9) student.setSchoolLevel(2);
            else student.setSchoolLevel(3);
        }
        userService.save(student);

        try {
            trackingService.addStudent(classId, student.getFullName());
        } catch (Exception ignored) {}

        redirectAttributes.addFlashAttribute("message", "Đã tiếp nhận học sinh [" + student.getFullName() + "] vào lớp " + trackingClass.getClassName() + " thành công!");
        return "redirect:/teacher/homeroom?classId=" + classId;
    }

    @PostMapping("/remove-student")
    public String removeStudent(@RequestParam Integer classId,
                                @RequestParam String studentUsername,
                                RedirectAttributes redirectAttributes) {
        if (!hasManagePermission(classId)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền quản lý lớp học này.");
            return "redirect:/teacher/homeroom";
        }

        User student = userService.findById(studentUsername != null ? studentUsername.trim() : "");
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy học sinh.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        if (student.getTrackingClass() != null && student.getTrackingClass().getId().equals(classId)) {
            student.setTrackingClass(null);
            userService.save(student);
            redirectAttributes.addFlashAttribute("message", "Đã rút học sinh [" + student.getFullName() + "] khỏi lớp.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Học sinh không thuộc lớp này.");
        }

        return "redirect:/teacher/homeroom?classId=" + classId;
    }

    @PostMapping("/reset-password")
    public String resetStudentPassword(@RequestParam Integer classId,
                                       @RequestParam String studentUsername,
                                       @RequestParam String newPassword,
                                       @RequestParam(required = false) String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!hasManagePermission(classId)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền quản lý lớp học này.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        User student = userService.findById(studentUsername != null ? studentUsername.trim() : "");
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy học sinh.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        if (student.getTrackingClass() == null || !student.getTrackingClass().getId().equals(classId)) {
            redirectAttributes.addFlashAttribute("error", "Học sinh không thuộc lớp này.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        if (confirmPassword != null && !confirmPassword.trim().isEmpty() && !newPassword.trim().equals(confirmPassword.trim())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp với mật khẩu mới.");
            return "redirect:/teacher/homeroom?classId=" + classId;
        }

        student.setPassWord(passwordEncoder.encode(newPassword.trim()));
        student.setRePassWord(student.getPassWord());
        userService.save(student);

        redirectAttributes.addFlashAttribute("message", "Đã cấp lại mật khẩu thành công cho học sinh " + student.getFullName() + " (@" + student.getId() + ")!");
        return "redirect:/teacher/homeroom?classId=" + classId;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9][A-Za-z0-9_.-]*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");
    }

    private boolean hasManagePermission(Integer classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (isAdmin) return true;

        User currentUser = userService.findById(auth.getName());
        if (currentUser == null) return false;

        StudentTrackingClass trackingClass = trackingClassRepository.findById(classId).orElse(null);
        if (trackingClass == null) return false;

        boolean isSchoolAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        if (isSchoolAdmin && currentUser.getSchool() != null && trackingClass.getSchool() != null
                && currentUser.getSchool().getId().equals(trackingClass.getSchool().getId())) {
            return true;
        }

        return trackingClass.getHomeroomTeacher() != null
                && trackingClass.getHomeroomTeacher().getId().equals(currentUser.getId());
    }
}
