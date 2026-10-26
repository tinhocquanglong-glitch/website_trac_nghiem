package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IClassAssignmentService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import com.tttn.webthitracnghiem.service.StudentTrackingExcelExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student-tracking")
public class StudentTrackingController {
    private final StudentTrackingService trackingService;
    private final StudentTrackingExcelExporter excelExporter;
    private final IUserService userService;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    @Autowired(required = false)
    private IClassAssignmentService assignmentService;

    @Autowired(required = false)
    private ISubjectService subjectService;

    public StudentTrackingController(StudentTrackingService trackingService,
                                     StudentTrackingExcelExporter excelExporter) {
        this(trackingService, excelExporter, null, null, null);
    }

    @Autowired
    public StudentTrackingController(StudentTrackingService trackingService,
                                     StudentTrackingExcelExporter excelExporter,
                                     IUserService userService,
                                     UserRepository userRepository,
                                     SchoolRepository schoolRepository) {
        this.trackingService = trackingService;
        this.excelExporter = excelExporter;
        this.userService = userService;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
    }

    @GetMapping
    public String index(@RequestParam(name = "q", defaultValue = "") String query,
                        @RequestParam(required = false) Integer classId,
                        @RequestParam(required = false) Integer schoolId,
                        @RequestParam(required = false) Integer grade,
                        @RequestParam(required = false) Boolean unassigned,
                        @RequestParam(name = "address", defaultValue = "") String addressKeyword,
                        Model model) {
        String cleanQuery = query == null ? "" : query.trim();
        addressKeyword = addressKeyword == null ? "" : addressKeyword.trim();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null && auth.isAuthenticated() && userService != null)
                ? userService.findById(auth.getName()) : null;
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        boolean isAdminHierarchy = isAdmin && schoolRepository != null;
        boolean canCreateClasses = isSchoolAdmin && !isAdmin;

        List<StudentTrackingClass> classes;
        List<School> trackingSchools = new ArrayList<>();
        Map<Integer, List<Integer>> gradesBySchool = new LinkedHashMap<>();
        Map<Integer, Integer> classCountBySchool = new LinkedHashMap<>();
        Map<Integer, Integer> classCountByGrade = new LinkedHashMap<>();
        List<StudentTrackingClass> unassignedClasses = List.of();
        School selectedSchool = null;
        Integer selectedGrade = null;
        boolean selectedUnassigned = Boolean.TRUE.equals(unassigned);

        if (isAdminHierarchy) {
            List<School> activeSchools = schoolRepository.findAllByActiveTrueOrderByNameAsc();
            if (!addressKeyword.isEmpty()) {
                for (School school : activeSchools) {
                    if (SchoolAddressMatcher.matches(school, addressKeyword)) {
                        trackingSchools.add(school);
                    }
                }
            } else {
                trackingSchools.addAll(activeSchools);
            }
            List<StudentTrackingClass> allClasses = trackingService.listClasses();
            unassignedClasses = trackingService.listClassesWithoutSchool();
            for (School school : trackingSchools) {
                gradesBySchool.put(school.getId(), new ArrayList<>());
                classCountBySchool.put(school.getId(), 0);
            }
            for (StudentTrackingClass trackingClass : allClasses) {
                if (trackingClass.getSchool() == null) {
                    continue;
                }
                Integer schoolIdForClass = trackingClass.getSchool().getId();
                if (classCountBySchool.containsKey(schoolIdForClass)) {
                    classCountBySchool.merge(schoolIdForClass, 1, Integer::sum);
                }
                if (trackingClass.getGrade() == null) {
                    continue;
                }
                List<Integer> grades = gradesBySchool.get(schoolIdForClass);
                if (grades != null && !grades.contains(trackingClass.getGrade())) {
                    grades.add(trackingClass.getGrade());
                }
            }
            gradesBySchool.values().forEach(grades -> grades.sort(Integer::compareTo));

            if (schoolId != null) {
                selectedSchool = (addressKeyword.isEmpty() ? activeSchools : trackingSchools).stream()
                        .filter(school -> school.getId().equals(schoolId))
                        .findFirst()
                        .orElse(null);
            }
            if (selectedUnassigned) {
                classes = unassignedClasses;
            } else if (selectedSchool == null) {
                classes = List.of();
            } else {
                List<StudentTrackingClass> schoolClasses = trackingService.listClassesForSchool(selectedSchool.getId());
                List<Integer> availableGrades = new ArrayList<>();
                for (StudentTrackingClass trackingClass : schoolClasses) {
                    if (trackingClass.getGrade() != null) {
                        if (!availableGrades.contains(trackingClass.getGrade())) {
                            availableGrades.add(trackingClass.getGrade());
                        }
                    }
                }
                availableGrades.sort(Integer::compareTo);
                gradesBySchool.put(selectedSchool.getId(), availableGrades);
                for (Integer availableGrade : availableGrades) {
                    int gradeClassCount = 0;
                    for (StudentTrackingClass schoolClass : schoolClasses) {
                        if (availableGrade.equals(schoolClass.getGrade())) {
                            gradeClassCount++;
                        }
                    }
                    classCountByGrade.put(availableGrade, gradeClassCount);
                }
                if (grade != null && availableGrades.contains(grade)) {
                    selectedGrade = grade;
                    classes = trackingService.listClassesForSchoolAndGrade(selectedSchool.getId(), grade);
                } else {
                    classes = List.of();
                }
            }
        } else if (isSchoolAdmin && currentUser != null && currentUser.getSchool() != null) {
            classes = trackingService.listClassesForSchool(currentUser.getSchool().getId());
        } else if (currentUser != null) {
            Integer userSchoolId = (currentUser.getSchool() != null) ? currentUser.getSchool().getId() : null;
            classes = trackingService.listClassesForTeacher(currentUser.getId(), userSchoolId);
        } else {
            classes = trackingService.listClasses();
        }

        model.addAttribute("trackingClasses", classes);
        model.addAttribute("searchResults", trackingService.searchStudents(cleanQuery, classId));
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("searchPerformed", !cleanQuery.isEmpty());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdminOrSchoolAdmin", isAdmin || isSchoolAdmin);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isSchoolAdmin", isSchoolAdmin);
        model.addAttribute("isAdminHierarchy", isAdminHierarchy);
        model.addAttribute("canCreateClasses", canCreateClasses);
        model.addAttribute("trackingSchools", trackingSchools);
        model.addAttribute("gradesBySchool", gradesBySchool);
        model.addAttribute("classCountBySchool", classCountBySchool);
        model.addAttribute("classCountByGrade", classCountByGrade);
        model.addAttribute("unassignedClasses", unassignedClasses);
        model.addAttribute("unassignedClassCount", unassignedClasses.size());
        model.addAttribute("selectedSchool", selectedSchool);
        model.addAttribute("selectedSchoolId", schoolId);
        model.addAttribute("selectedGrade", selectedGrade);
        model.addAttribute("selectedUnassigned", selectedUnassigned);
        model.addAttribute("addressKeyword", addressKeyword);
        model.addAttribute("hasAddressSearch", !addressKeyword.isEmpty());

        if ((isAdmin || isSchoolAdmin) && userRepository != null) {
            if (currentUser != null && currentUser.getSchool() != null) {
                model.addAttribute("schoolTeachers", userRepository.findTeachersBySchool(currentUser.getSchool().getId()));
            } else if (isAdmin) {
                model.addAttribute("schoolTeachers", userRepository.findAllTeachers());
            }
        }

        return "student-tracking/list";
    }

    @Autowired(required = false)
    private com.tttn.webthitracnghiem.service.IRoleService roleService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWorkbook(@RequestParam(required = false) Integer classId,
                                                @RequestParam(required = false) Integer subjectId) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null && auth.isAuthenticated() && userService != null)
                ? userService.findById(auth.getName()) : null;
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        StudentTrackingClass trackingClass = classId == null ? null : trackingService.findClass(classId);
        boolean isHomeroom = (currentUser != null && trackingClass != null && trackingClass.getHomeroomTeacher() != null
                && trackingClass.getHomeroomTeacher().getId().equals(currentUser.getId()));
        boolean canManage = isAdmin || isSchoolAdmin || isHomeroom;

        Integer exportSubjectId = subjectId;
        String exportTeacherId = null;
        String exportSubjectName = null;

        if (classId != null) {
            List<ClassSubjectAssignment> classAssignments = (assignmentService != null)
                    ? assignmentService.findByClassId(classId) : List.of();
            if (classAssignments == null) {
                classAssignments = List.of();
            }

            if (exportSubjectId == null && !canManage && currentUser != null) {
                List<ClassSubjectAssignment> myAssignments = assignmentsOf(currentUser, classId);
                if (!myAssignments.isEmpty()) {
                    exportSubjectId = myAssignments.get(0).getSubject().getId();
                }
            }

            if (exportSubjectId != null) {
                final Integer sId = exportSubjectId;
                ClassSubjectAssignment assignment = classAssignments.stream()
                        .filter(a -> a.getSubject() != null && sId.equals(a.getSubject().getId()))
                        .findFirst()
                        .orElse(null);
                if (assignment != null) {
                    exportSubjectName = assignment.getSubject().getName();
                    exportTeacherId = assignment.getTeacher() != null ? assignment.getTeacher().getId() : null;
                } else if (subjectService != null) {
                    Subject subject = subjectService.findById(exportSubjectId);
                    if (subject != null) {
                        exportSubjectName = subject.getName();
                    }
                    exportTeacherId = currentUser != null ? currentUser.getId() : null;
                }
            }
        }

        byte[] workbook = (exportSubjectId != null)
                ? excelExporter.export(classId, exportSubjectId, exportSubjectName, exportTeacherId)
                : excelExporter.export(classId);

        String filename;
        if (classId == null) {
            filename = "so-theo-doi-tat-ca-lop.xlsx";
        } else {
            String classNameSafe = trackingClass != null
                    ? trackingClass.getClassName().replaceAll("[^a-zA-Z0-9_-]", "_")
                    : String.valueOf(classId);
            if (exportSubjectName != null && !exportSubjectName.isBlank()) {
                String subSlug = Normalizer.normalize(exportSubjectName, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replace('đ', 'd').replace('Đ', 'D')
                        .replaceAll("[^a-zA-Z0-9_-]", "_");
                filename = "so-theo-doi-lop-" + classNameSafe + "-mon-" + subSlug + ".xlsx";
            } else {
                filename = "so-theo-doi-lop-" + classNameSafe + ".xlsx";
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(workbook);
    }

    @PostMapping("/classes")
    public String createClass(@RequestParam String className,
                              @RequestParam String schoolYear,
                              @RequestParam(required = false) Integer grade,
                              @RequestParam(required = false) String homeroomTeacherId,
                              RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null && auth.isAuthenticated() && userService != null)
                ? userService.findById(auth.getName()) : null;
        Integer schoolId = (currentUser != null && currentUser.getSchool() != null)
                ? currentUser.getSchool().getId() : null;

        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        // Cấu trúc lớp thuộc phạm vi từng trường, chỉ admin-hiệu trưởng được tạo.
        if (!isSchoolAdmin || isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin-hiệu trưởng mới có quyền tạo lớp mới.");
            return "redirect:/student-tracking";
        }


        // Kiểm tra khoanh vùng cấp học của tài khoản:
        if (currentUser != null && currentUser.getSchoolLevel() != null && grade != null) {
            int lvl = currentUser.getSchoolLevel();
            boolean validGrade = false;
            if (lvl == 1 && grade >= 1 && grade <= 5) validGrade = true;
            else if (lvl == 2 && grade >= 6 && grade <= 9) validGrade = true;
            else if (lvl == 3 && grade >= 10 && grade <= 12) validGrade = true;

            if (!validGrade) {
                redirectAttributes.addFlashAttribute("error", "Tài khoản của bạn thuộc " + currentUser.getSchoolLevelName() + ", không thể tạo lớp thuộc Khối " + grade + "!");
                return "redirect:/student-tracking";
            }
        }

        try {
            var created = trackingService.createClass(className, schoolYear, grade, schoolId, homeroomTeacherId);
            redirectAttributes.addFlashAttribute("message", "Đã thêm lớp " + className + " thành công.");
            return redirectToSheet(created.getId(), 1, 9);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student-tracking";
        }
    }

    @PostMapping("/classes/{classId}/delete")
    public String deleteClass(@PathVariable Integer classId,
                              @RequestParam(value = "password", required = false) String password,
                              RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        if (!isAdmin && !isSchoolAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ Ban giám hiệu (Hiệu trưởng) hoặc Quản trị viên mới có quyền xóa lớp học.");
            return "redirect:/student-tracking";
        }
        User currentUser = (auth != null && userService != null) ? userService.findById(auth.getName()) : null;
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại!");
            return "redirect:/student-tracking";
        }

        // Xác thực mật khẩu tài khoản người thực hiện
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (password == null || password.trim().isEmpty() || !encoder.matches(password.trim(), currentUser.getPassWord())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa lớp học.");
            return "redirect:/student-tracking";
        }

        StudentTrackingClass trackingClass;
        try {
            trackingClass = trackingService.findClass(classId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy lớp học cần xóa.");
            return "redirect:/student-tracking";
        }

        // Hiệu trưởng chỉ được xóa lớp thuộc trường của mình
        if (isSchoolAdmin && !isAdmin) {
            Integer userSchoolId = currentUser.getSchool() != null ? currentUser.getSchool().getId() : null;
            Integer classSchoolId = trackingClass.getSchool() != null ? trackingClass.getSchool().getId() : null;
            if (userSchoolId == null || !userSchoolId.equals(classSchoolId)) {
                redirectAttributes.addFlashAttribute("error", "Hiệu trưởng chỉ được quyền xóa lớp thuộc trường mình quản lý!");
                return "redirect:/student-tracking";
            }
        }

        try {
            trackingService.deleteClass(classId);
            redirectAttributes.addFlashAttribute("message", "Đã xóa (ẩn) lớp " + trackingClass.getClassName() + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa lớp: " + ex.getMessage());
        }
        return "redirect:/student-tracking";
    }

    public String deleteClass(Integer classId, RedirectAttributes redirectAttributes) {
        return deleteClass(classId, null, redirectAttributes);
    }

    @GetMapping("/classes/{classId}")
    public String sheet(@PathVariable Integer classId,
                        @RequestParam(defaultValue = "1") int semester,
                        @RequestParam(required = false) Integer month,
                        @RequestParam(required = false) Integer subjectId,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        int selectedMonth = month == null ? trackingService.defaultMonth(semester) : month;
        StudentTrackingClass trackingClass = trackingService.findClass(classId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null && auth.isAuthenticated() && userService != null)
                ? userService.findById(auth.getName()) : null;
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        boolean isHomeroom = (currentUser != null && trackingClass.getHomeroomTeacher() != null
                && trackingClass.getHomeroomTeacher().getId().equals(currentUser.getId()));
        boolean canManage = isAdmin || isSchoolAdmin || isHomeroom;

        List<ClassSubjectAssignment> classAssignments = (assignmentService != null)
                ? assignmentService.findByClassId(classId) : List.of();
        if (classAssignments == null) {
            classAssignments = List.of();
        }
        List<ClassSubjectAssignment> myAssignments = assignmentsOf(currentUser, classId);

        // Danh sách sổ môn khả dụng:
        // Quản lý (Admin, BGH, GVCN) được xem tất cả các môn đã phân công của lớp.
        // GVBM chỉ được xem các môn mà mình được phân công dạy ở lớp này.
        List<ClassSubjectAssignment> availableBooks = canManage ? classAssignments : myAssignments;

        ClassSubjectAssignment bookAssignment = null;
        if (subjectId != null) {
            bookAssignment = availableBooks.stream()
                    .filter(a -> a.getSubject() != null && subjectId.equals(a.getSubject().getId()))
                    .findFirst()
                    .orElse(null);
            if (bookAssignment == null) {
                redirectAttributes.addFlashAttribute("error", "Môn học này chưa được phân công hoặc bạn không có quyền xem sổ môn này.");
                return "redirect:/student-tracking/classes/" + classId + "?semester=" + semester + "&month=" + selectedMonth;
            }
        } else if (!canManage) {
            if (!myAssignments.isEmpty()) {
                bookAssignment = myAssignments.get(0);
            } else {
                redirectAttributes.addFlashAttribute("error", "Bạn chưa được phân công dạy môn nào ở lớp "
                        + trackingClass.getClassName() + " nên không thể mở sổ theo dõi của lớp này.");
                return "redirect:/student-tracking";
            }
        }

        Integer bookSubjectId = bookAssignment == null ? null : bookAssignment.getSubject().getId();
        String bookSubjectName = bookAssignment == null ? null : (bookAssignment.getSubject() != null ? bookAssignment.getSubject().getName() : null);
        String bookTeacherId = bookAssignment == null ? null : (bookAssignment.getTeacher() != null ? bookAssignment.getTeacher().getId() : null);
        String bookTeacherName = bookAssignment == null ? null : (bookAssignment.getTeacher() != null ? bookAssignment.getTeacher().getFullName() : null);
        boolean isAssignedTeacherOfBook = (currentUser != null && bookTeacherId != null && bookTeacherId.equals(currentUser.getId()));

        model.addAttribute("trackingClass", trackingClass);
        model.addAttribute("trackingClasses", trackingService.listClasses());
        model.addAttribute("sheetForm", trackingService.getSheet(classId, semester, selectedMonth, bookTeacherId, bookSubjectId));
        model.addAttribute("semester", semester);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("months", trackingService.monthsForSemester(semester));
        model.addAttribute("attendanceStatuses", AttendanceStatus.values());
        model.addAttribute("availableBooks", availableBooks);
        model.addAttribute("assignments", classAssignments);
        model.addAttribute("myAssignments", myAssignments);
        model.addAttribute("homeroomBook", bookAssignment == null);
        model.addAttribute("bookSubjectId", bookSubjectId);
        model.addAttribute("bookSubjectName", bookSubjectName);
        model.addAttribute("bookTeacherId", bookTeacherId);
        model.addAttribute("bookTeacherName", bookTeacherName);
        model.addAttribute("isAssignedTeacherOfBook", isAssignedTeacherOfBook);
        model.addAttribute("canEditThisBook", (bookAssignment == null ? canManage : (isAssignedTeacherOfBook || canManage)));

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHomeroom", isHomeroom);
        model.addAttribute("canManage", canManage);
        model.addAttribute("isAdminOrSchoolAdmin", isAdmin || isSchoolAdmin);

        Integer targetSchoolId = (trackingClass.getSchool() != null)
                ? trackingClass.getSchool().getId()
                : (currentUser != null && currentUser.getSchool() != null ? currentUser.getSchool().getId() : null);

        if (targetSchoolId != null && userRepository != null) {
            model.addAttribute("schoolTeachers", userRepository.findTeachersBySchool(targetSchoolId));
        } else if (isAdmin && userRepository != null) {
            model.addAttribute("schoolTeachers", userRepository.findAllTeachers());
        }

        if (currentUser != null && currentUser.getSchool() != null) {
            model.addAttribute("currentUserSchool", currentUser.getSchool());
        }

        if (assignmentService != null) {
            var assignments = assignmentService.findByClassId(classId);
            model.addAttribute("assignments", assignments);

            if (currentUser != null && assignments != null) {
                String mySubjects = assignments.stream()
                        .filter(a -> a.getTeacher() != null && a.getTeacher().getId().equals(currentUser.getId()))
                        .map(a -> a.getSubject() != null ? a.getSubject().getName() : "")
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.joining(", "));
                model.addAttribute("myAssignedSubjects", mySubjects.isEmpty() ? null : mySubjects);
            }
        }
        if (subjectService != null) {
            model.addAttribute("allSubjects", subjectService.findAll());
        }

        // Danh sách học sinh tự do chưa có lớp để GVCN / Admin có thể thêm vào lớp
        if (canManage && userRepository != null) {
            List<User> unassigned;
            if (targetSchoolId != null) {
                unassigned = userRepository.findUnassignedStudentsBySchool(targetSchoolId);
            } else {
                unassigned = userRepository.findAllUnassignedStudents();
            }
            model.addAttribute("unassignedStudents", unassigned);
        }

        return "student-tracking/sheet";
    }

    @PostMapping("/classes/{classId}/create-student-account")
    public String createStudentAccount(@PathVariable Integer classId,
                                       @RequestParam String username,
                                       @RequestParam String fullName,
                                       @RequestParam(required = false) String email,
                                       @RequestParam String password,
                                       @RequestParam(required = false) String phoneNumber,
                                       RedirectAttributes redirectAttributes) {
        if (!checkCanManageClass(classId)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ GVCN hoặc Quản trị viên mới có quyền cấp tài khoản học sinh cho lớp này.");
            return "redirect:/student-tracking/classes/" + classId;
        }

        String cleanUsername = (username != null) ? username.trim() : "";
        String cleanFullName = (fullName != null) ? fullName.trim() : "";
        if (cleanUsername.isEmpty() || cleanFullName.isEmpty() || password == null
                || password.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập đầy đủ tên đăng nhập, họ tên và mật khẩu.");
            return "redirect:/student-tracking/classes/" + classId;
        }

        if (userService != null && userService.userExists(cleanUsername)) {
            redirectAttributes.addFlashAttribute("error", "Tên tài khoản '" + cleanUsername + "' đã tồn tại trên hệ thống!");
            return "redirect:/student-tracking/classes/" + classId;
        }

        String studentEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : (cleanUsername + "@student.school.vn");
        studentEmail = studentEmail.toLowerCase(java.util.Locale.ROOT);
        if (!isValidEmail(studentEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email khong dung dinh dang.");
            return "redirect:/student-tracking/classes/" + classId;
        }
        if (userService != null && userService.userExistss(studentEmail)) {
            studentEmail = cleanUsername.toLowerCase(java.util.Locale.ROOT) + "_" + System.currentTimeMillis() + "@student.school.vn";
        }

        StudentTrackingClass trackingClass;
        try {
            trackingClass = trackingService.findClass(classId);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", "Khong tim thay lop hoc.");
            return "redirect:/student-tracking/classes/" + classId;
        }

        String studentPhone = phoneNumber != null && !phoneNumber.trim().isEmpty()
                ? phoneNumber.trim() : "0900000000";
        if (!studentPhone.matches("^((\\(84\\)\\+)|(0))[1-9][\\d]{8}$")) {
            redirectAttributes.addFlashAttribute("error", "So dien thoai khong dung dinh dang.");
            return "redirect:/student-tracking/classes/" + classId;
        }

        User student = new User();
        student.setId(cleanUsername);
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        student.setPassWord(encoder.encode(password.trim()));
        student.setRePassWord(student.getPassWord());
        student.setFullName(cleanFullName);
        student.setEmail(studentEmail);
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

        if (roleService != null) {
            var roleStudent = roleService.findByName("ROLE_USER");
            if (roleStudent == null) {
                redirectAttributes.addFlashAttribute("error", "He thong chua co role hoc sinh.");
                return "redirect:/student-tracking/classes/" + classId;
            }
            student.setRoles(java.util.Set.of(roleStudent));
        }
        if (userService != null) {
            try {
                userService.save(student);
            } catch (RuntimeException ex) {
                redirectAttributes.addFlashAttribute("error", "Khong the tao tai khoan. Vui long kiem tra lai thong tin va thu lai.");
                return "redirect:/student-tracking/classes/" + classId;
            }
        }

        try {
            trackingService.addStudent(classId, cleanFullName);
        } catch (Exception ignored) {
        }

        redirectAttributes.addFlashAttribute("message", "Đã cấp tài khoản học sinh [" + cleanUsername + "] thành công và thêm vào lớp " + trackingClass.getClassName() + "!");
        return "redirect:/student-tracking/classes/" + classId;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9][A-Za-z0-9_.-]*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");
    }

    @PostMapping("/classes/{classId}/assignments")
    public String assignSubjectTeacher(@PathVariable Integer classId,
                                       @RequestParam Integer subjectId,
                                       @RequestParam String teacherId,
                                       RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        if (!isAdmin && !isSchoolAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ Ban giám hiệu / Quản trị viên mới có quyền phân công giáo viên bộ môn.");
            return "redirect:/student-tracking/classes/" + classId;
        }
        if (assignmentService == null) {
            redirectAttributes.addFlashAttribute("error", "Chức năng phân công chưa khả dụng");
            return "redirect:/student-tracking/classes/" + classId;
        }
        try {
            assignmentService.assignTeacher(classId, subjectId, teacherId);
            redirectAttributes.addFlashAttribute("message", "Đã phân công giáo viên bộ môn thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student-tracking/classes/" + classId;
    }

    @PostMapping("/classes/{classId}/assignments/{assignmentId}/delete")
    public String removeAssignment(@PathVariable Integer classId,
                                   @PathVariable Integer assignmentId,
                                   @RequestParam(value = "password", required = false) String password,
                                   RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        if (!isAdmin && !isSchoolAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ Ban giám hiệu / Quản trị viên mới có quyền hủy phân công giáo viên bộ môn.");
            return "redirect:/student-tracking/classes/" + classId;
        }
        if (password != null && !password.trim().isEmpty()) {
            User currentUser = (auth != null && userService != null) ? userService.findById(auth.getName()) : null;
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            if (currentUser != null && !encoder.matches(password.trim(), currentUser.getPassWord())) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể hủy phân công giáo viên bộ môn.");
                return "redirect:/student-tracking/classes/" + classId;
            }
        }
        if (assignmentService == null) {
            redirectAttributes.addFlashAttribute("error", "Chức năng phân công chưa khả dụng");
            return "redirect:/student-tracking/classes/" + classId;
        }
        try {
            assignmentService.removeAssignment(assignmentId);
            redirectAttributes.addFlashAttribute("message", "Đã hủy phân công bộ môn thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student-tracking/classes/" + classId;
    }

    @PostMapping("/classes/{classId}/students")
    public String addStudent(@PathVariable Integer classId,
                             @RequestParam String fullName,
                             @RequestParam(defaultValue = "1") int semester,
                             @RequestParam(required = false) Integer month,
                             RedirectAttributes redirectAttributes) {
        if (!checkCanManageClass(classId)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ GVCN hoặc Quản trị viên mới có quyền thêm học sinh vào lớp.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }
        try {
            trackingService.addStudent(classId, fullName);
            redirectAttributes.addFlashAttribute("message", "Đã thêm học sinh.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, safeMonth(semester, month));
    }

    /**
     * Thêm học sinh đã có tài khoản (đăng ký tự do / chưa có lớp) vào lớp.
     */
    @PostMapping("/classes/{classId}/add-existing-student")
    public String addExistingStudent(@PathVariable Integer classId,
                                     @RequestParam String studentUsername,
                                     @RequestParam(defaultValue = "1") int semester,
                                     @RequestParam(required = false) Integer month,
                                     RedirectAttributes redirectAttributes) {
        if (!checkCanManageClass(classId)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ GVCN hoặc Quản trị viên mới có quyền thêm học sinh vào lớp.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }
        if (studentUsername == null || studentUsername.trim().isEmpty() || userService == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn học sinh.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        User student = userService.findById(studentUsername.trim());
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản học sinh.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        // Nếu học sinh đã thuộc một lớp khác rồi thì thôi
        if (student.getTrackingClass() != null && !student.getTrackingClass().getId().equals(classId)) {
            redirectAttributes.addFlashAttribute("error", "Học sinh [" + student.getFullName() + "] đã thuộc lớp " + student.getTrackingClass().getClassName() + "! Không thể thêm vào lớp này.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        StudentTrackingClass trackingClass = trackingService.findClass(classId);

        // Gán lớp và trường cho học sinh
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

        // Thêm vào danh sách theo dõi sổ nếu chưa có
        try {
            trackingService.addStudent(classId, student.getFullName());
        } catch (IllegalArgumentException ignored) {
            // Đã có trong sổ theo dõi
        }

        redirectAttributes.addFlashAttribute("message", "Đã tiếp nhận học sinh [" + student.getFullName() + "] vào lớp " + trackingClass.getClassName() + " thành công!");
        return redirectToSheet(classId, semester, safeMonth(semester, month));
    }

    @PostMapping("/classes/{classId}/sync-students")
    public String syncStudentsFromWeb(@PathVariable Integer classId,
                                      @RequestParam(defaultValue = "1") int semester,
                                      @RequestParam(required = false) Integer month,
                                      RedirectAttributes redirectAttributes) {
        if (!checkCanManageClass(classId)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ GVCN hoặc Quản trị viên mới có quyền đồng bộ học sinh vào lớp.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Bạn chưa đăng nhập.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        User currentUser = userService.findById(auth.getName());
        School school = (currentUser != null) ? currentUser.getSchool() : null;
        if (school == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Tài khoản của bạn chưa được gán trường học. Không thể đồng bộ.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        try {
            int schoolId = school.getId();
            StudentTrackingService.SyncResult result = trackingService.syncStudentsFromWeb(
                    classId, schoolId, () -> userRepository.findStudentsBySchool(schoolId));

            redirectAttributes.addFlashAttribute("message",
                    "Đồng bộ thành công! Đã thêm " + result.getAddedCount()
                    + " học sinh mới (tổng " + result.getTotalFromWeb() + " học sinh trường "
                    + school.getName() + " trên web).");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, safeMonth(semester, month));
    }

    @PostMapping("/classes/{classId}/records")
    public String saveSheet(@PathVariable Integer classId,
                            @ModelAttribute TrackingSheetForm sheetForm,
                            @RequestParam(required = false) Integer subjectId,
                            RedirectAttributes redirectAttributes) {
        int semester = sheetForm.getSemester() == null ? 1 : sheetForm.getSemester();
        int month = safeMonth(semester, sheetForm.getMonth());
        try {
            if (subjectId == null) {
                if (!checkCanManageClass(classId)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Chỉ GVCN hoặc Ban giám hiệu mới được ghi sổ chủ nhiệm của lớp.");
                    return redirectToSheet(classId, semester, month);
                }
                trackingService.saveSheet(classId, sheetForm);
                redirectAttributes.addFlashAttribute("message", "Đã lưu sổ chủ nhiệm tháng " + month + ".");
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User currentUser = auth != null && userService != null ? userService.findById(auth.getName()) : null;
                List<ClassSubjectAssignment> classAssignments = (assignmentService != null)
                        ? assignmentService.findByClassId(classId) : List.of();
                if (classAssignments == null) {
                    classAssignments = List.of();
                }
                ClassSubjectAssignment assignment = classAssignments.stream()
                        .filter(a -> a.getSubject() != null && subjectId.equals(a.getSubject().getId()))
                        .findFirst()
                        .orElse(null);
                if (assignment == null) {
                    redirectAttributes.addFlashAttribute("error", "Môn học này chưa được phân công cho giáo viên nào ở lớp.");
                    return redirectToSheet(classId, semester, month);
                }
                String targetTeacherId = assignment.getTeacher() != null ? assignment.getTeacher().getId() : null;
                boolean isAssignedTeacher = currentUser != null && targetTeacherId != null && targetTeacherId.equals(currentUser.getId());
                boolean canManage = checkCanManageClass(classId);
                if (!isAssignedTeacher && !canManage) {
                    redirectAttributes.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa sổ môn học này.");
                    return redirectToSheet(classId, semester, month, subjectId);
                }
                trackingService.saveSheet(classId, sheetForm, targetTeacherId, subjectId);
                redirectAttributes.addFlashAttribute("message", "Đã lưu sổ môn " + assignment.getSubject().getName()
                        + " tháng " + month + ".");
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, month, subjectId);
    }

    /** Subject assignments of {@code user} in the class. */
    private List<ClassSubjectAssignment> assignmentsOf(User user, Integer classId) {
        if (user == null || assignmentService == null) {
            return List.of();
        }
        List<ClassSubjectAssignment> assignments = assignmentService.findByClassId(classId);
        if (assignments == null) {
            return List.of();
        }
        return assignments.stream()
                .filter(a -> a.getTeacher() != null && user.getId().equals(a.getTeacher().getId()))
                .filter(a -> a.getSubject() != null)
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/classes/{classId}/students/{studentId}/delete")
    public String deleteStudent(@PathVariable Integer classId,
                                @PathVariable Integer studentId,
                                @RequestParam(value = "password", required = false) String password,
                                @RequestParam(defaultValue = "1") int semester,
                                @RequestParam(required = false) Integer month,
                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        if (!isAdmin && !isSchoolAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ Ban giám hiệu (Hiệu trưởng) hoặc Quản trị viên mới có quyền xóa học sinh khỏi lớp.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        User currentUser = (auth != null && userService != null) ? userService.findById(auth.getName()) : null;
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại!");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        // Xác thực mật khẩu tài khoản người thực hiện
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (password == null || password.trim().isEmpty() || !encoder.matches(password.trim(), currentUser.getPassWord())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa học sinh.");
            return redirectToSheet(classId, semester, safeMonth(semester, month));
        }

        StudentTrackingClass trackingClass;
        try {
            trackingClass = trackingService.findClass(classId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy lớp học.");
            return "redirect:/student-tracking";
        }

        // Hiệu trưởng chỉ được xóa học sinh trong lớp thuộc trường mình quản lý
        if (isSchoolAdmin && !isAdmin) {
            Integer userSchoolId = currentUser.getSchool() != null ? currentUser.getSchool().getId() : null;
            Integer classSchoolId = trackingClass.getSchool() != null ? trackingClass.getSchool().getId() : null;
            if (userSchoolId == null || !userSchoolId.equals(classSchoolId)) {
                redirectAttributes.addFlashAttribute("error", "Hiệu trưởng chỉ được quyền xóa học sinh trong lớp thuộc trường mình quản lý!");
                return redirectToSheet(classId, semester, safeMonth(semester, month));
            }
        }

        try {
            trackingService.deleteStudent(classId, studentId);
            redirectAttributes.addFlashAttribute("message", "Đã xóa (ẩn) học sinh khỏi lớp thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, safeMonth(semester, month));
    }

    public String deleteStudent(Integer classId, Integer studentId, int semester, Integer month, RedirectAttributes redirectAttributes) {
        return deleteStudent(classId, studentId, null, semester, month, redirectAttributes);
    }

    @PostMapping("/import")
    public String importWorkbook(@RequestParam MultipartFile file,
                                 @RequestParam(defaultValue = "1") int semester,
                                 @RequestParam(required = false) Integer month,
                                 RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        if (!isSchoolAdmin || isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ admin-hiệu trưởng mới có quyền nhập lớp từ Excel.");
            return "redirect:/student-tracking";
        }
        int selectedMonth = safeMonth(semester, month);
        User currentUser = userService != null ? userService.findById(auth.getName()) : null;
        Integer schoolId = currentUser != null && currentUser.getSchool() != null ? currentUser.getSchool().getId() : null;
        try {
            StudentTrackingService.ImportSummary summary = trackingService.importWorkbook(file, semester, selectedMonth, schoolId);
            redirectAttributes.addFlashAttribute("message", "Đã import " + summary.getClassCount()
                    + " lớp và " + summary.getStudentCount() + " học sinh vào tháng " + selectedMonth + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể đọc file Excel. Vui lòng kiểm tra lại file.");
        }
        return "redirect:/student-tracking";
    }

    /**
     * Đổi hoặc rút GVCN cho lớp – CHỈ ADMIN / SCHOOL ADMIN mới có quyền.
     * homeroomTeacherId rỗng hoặc null => rút GVCN (trả về chưa phân công).
     */
    @PostMapping("/classes/{classId}/homeroom")
    public String updateHomeroom(@PathVariable Integer classId,
                                 @RequestParam(required = false) String homeroomTeacherId,
                                 @RequestParam(required = false) String redirectUrl,
                                 RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        if (!isAdmin && !isSchoolAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ Ban giám hiệu / Quản trị viên mới có quyền thay đổi Giáo viên chủ nhiệm.");
            return (redirectUrl != null && !redirectUrl.isEmpty()) ? "redirect:" + redirectUrl : "redirect:/student-tracking/classes/" + classId;
        }
        try {
            String teacherId = (homeroomTeacherId != null && !homeroomTeacherId.trim().isEmpty())
                    ? homeroomTeacherId.trim() : null;
            trackingService.updateHomeroomTeacher(classId, teacherId);
            if (teacherId == null) {
                redirectAttributes.addFlashAttribute("message", "Đã rút nhiệm vụ GVCN của lớp. Lớp hiện chưa có GVCN.");
            } else {
                redirectAttributes.addFlashAttribute("message", "Đã cập nhật Giáo viên chủ nhiệm thành công.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật GVCN: " + ex.getMessage());
        }
        return (redirectUrl != null && !redirectUrl.isEmpty()) ? "redirect:" + redirectUrl : "redirect:/student-tracking/classes/" + classId;
    }


    private String redirectToSheet(Integer classId, int semester, int month) {
        return redirectToSheet(classId, semester, month, null);
    }

    private String redirectToSheet(Integer classId, int semester, int month, Integer subjectId) {
        int safeSemester = semester == 2 ? 2 : 1;
        return "redirect:/student-tracking/classes/" + classId + "?semester=" + safeSemester + "&month=" + month
                + (subjectId == null ? "" : "&subjectId=" + subjectId);
    }

    private int safeMonth(int semester, Integer month) {
        return month == null ? trackingService.defaultMonth(semester) : month;
    }

    private boolean checkCanManageClass(Integer classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (isAdmin) return true;
        boolean isSchoolAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));
        User currentUser = (userService != null) ? userService.findById(auth.getName()) : null;
        if (currentUser == null) return false;
        try {
            StudentTrackingClass trackingClass = trackingService.findClass(classId);
            if (isSchoolAdmin && currentUser.getSchool() != null && trackingClass.getSchool() != null
                    && currentUser.getSchool().getId().equals(trackingClass.getSchool().getId())) {
                return true;
            }
            if (trackingClass.getHomeroomTeacher() != null
                    && currentUser.getId().equals(trackingClass.getHomeroomTeacher().getId())) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
