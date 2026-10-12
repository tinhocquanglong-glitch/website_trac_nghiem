package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import com.tttn.webthitracnghiem.service.StudentTrackingExcelExporter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

import java.io.IOException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student-tracking")
public class StudentTrackingController {
    private final StudentTrackingService trackingService;
    private final StudentTrackingExcelExporter excelExporter;
    private final IUserService userService;
    private final UserRepository userRepository;

    public StudentTrackingController(StudentTrackingService trackingService,
                                     StudentTrackingExcelExporter excelExporter) {
        this(trackingService, excelExporter, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StudentTrackingController(StudentTrackingService trackingService,
                                     StudentTrackingExcelExporter excelExporter,
                                     IUserService userService,
                                     UserRepository userRepository) {
        this.trackingService = trackingService;
        this.excelExporter = excelExporter;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String index(@RequestParam(name = "q", defaultValue = "") String query,
                        @RequestParam(required = false) Integer classId,
                        Model model) {
        String cleanQuery = query == null ? "" : query.trim();
        model.addAttribute("trackingClasses", trackingService.listClasses());
        model.addAttribute("searchResults", trackingService.searchStudents(cleanQuery, classId));
        model.addAttribute("query", cleanQuery);
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("searchPerformed", !cleanQuery.isEmpty());
        return "student-tracking/list";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWorkbook(@RequestParam(required = false) Integer classId) throws IOException {
        byte[] workbook = excelExporter.export(classId);
        String filename = classId == null
                ? "so-theo-doi-tat-ca-lop.xlsx"
                : "so-theo-doi-lop-" + classId + ".xlsx";
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
                              RedirectAttributes redirectAttributes) {
        try {
            var created = trackingService.createClass(className, schoolYear);
            redirectAttributes.addFlashAttribute("message", "Da them lop theo doi thanh cong.");
            return redirectToSheet(created.getId(), 1, 9);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student-tracking";
        }
    }

    @PostMapping("/classes/{classId}/delete")
    public String deleteClass(@PathVariable Integer classId, RedirectAttributes redirectAttributes) {
        try {
            trackingService.deleteClass(classId);
            redirectAttributes.addFlashAttribute("message", "Da xoa lop theo doi.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student-tracking";
    }

    @GetMapping("/classes/{classId}")
    public String sheet(@PathVariable Integer classId,
                        @RequestParam(defaultValue = "1") int semester,
                        @RequestParam(required = false) Integer month,
                        Model model) {
        int selectedMonth = month == null ? trackingService.defaultMonth(semester) : month;
        model.addAttribute("trackingClass", trackingService.findClass(classId));
        model.addAttribute("trackingClasses", trackingService.listClasses());
        model.addAttribute("sheetForm", trackingService.getSheet(classId, semester, selectedMonth));
        model.addAttribute("semester", semester);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("months", trackingService.monthsForSemester(semester));
        model.addAttribute("attendanceStatuses", AttendanceStatus.values());

        // Thêm thông tin trường của giáo viên hiện tại để hiển thị nút đồng bộ
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            User currentUser = userService.findById(auth.getName());
            if (currentUser != null && currentUser.getSchool() != null) {
                model.addAttribute("currentUserSchool", currentUser.getSchool());
            }
        }

        return "student-tracking/sheet";
    }

    @PostMapping("/classes/{classId}/students")
    public String addStudent(@PathVariable Integer classId,
                             @RequestParam String fullName,
                             @RequestParam(defaultValue = "1") int semester,
                             @RequestParam(required = false) Integer month,
                             RedirectAttributes redirectAttributes) {
        try {
            trackingService.addStudent(classId, fullName);
            redirectAttributes.addFlashAttribute("message", "Da them hoc sinh.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, safeMonth(semester, month));
    }

    /**
     * Đồng bộ học sinh đang học trên web (cùng trường với giáo viên) vào lớp theo dõi
     */
    @PostMapping("/classes/{classId}/sync-students")
    public String syncStudentsFromWeb(@PathVariable Integer classId,
                                      @RequestParam(defaultValue = "1") int semester,
                                      @RequestParam(required = false) Integer month,
                                      RedirectAttributes redirectAttributes) {
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
                            RedirectAttributes redirectAttributes) {
        int semester = sheetForm.getSemester() == null ? 1 : sheetForm.getSemester();
        int month = safeMonth(semester, sheetForm.getMonth());
        try {
            trackingService.saveSheet(classId, sheetForm);
            redirectAttributes.addFlashAttribute("message", "Da luu so theo doi thang " + month + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, month);
    }

    @PostMapping("/classes/{classId}/students/{studentId}/delete")
    public String deleteStudent(@PathVariable Integer classId,
                                @PathVariable Integer studentId,
                                @RequestParam(defaultValue = "1") int semester,
                                @RequestParam(required = false) Integer month,
                                RedirectAttributes redirectAttributes) {
        try {
            trackingService.deleteStudent(classId, studentId);
            redirectAttributes.addFlashAttribute("message", "Da xoa hoc sinh khoi lop.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester, safeMonth(semester, month));
    }

    @PostMapping("/import")
    public String importWorkbook(@RequestParam MultipartFile file,
                                 @RequestParam(defaultValue = "1") int semester,
                                 @RequestParam(required = false) Integer month,
                                 RedirectAttributes redirectAttributes) {
        int selectedMonth = safeMonth(semester, month);
        try {
            StudentTrackingService.ImportSummary summary = trackingService.importWorkbook(file, semester, selectedMonth);
            redirectAttributes.addFlashAttribute("message", "Da import " + summary.getClassCount()
                    + " lop va " + summary.getStudentCount() + " hoc sinh vao thang " + selectedMonth + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Khong the doc file Excel. Vui long kiem tra lai file.");
        }
        return "redirect:/student-tracking";
    }

    private String redirectToSheet(Integer classId, int semester, int month) {
        int safeSemester = semester == 2 ? 2 : 1;
        return "redirect:/student-tracking/classes/" + classId + "?semester=" + safeSemester + "&month=" + month;
    }

    private int safeMonth(int semester, Integer month) {
        return month == null ? trackingService.defaultMonth(semester) : month;
    }
}
