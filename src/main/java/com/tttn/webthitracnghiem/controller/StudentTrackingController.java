package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.service.StudentTrackingService;
import com.tttn.webthitracnghiem.service.StudentTrackingExcelExporter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public StudentTrackingController(StudentTrackingService trackingService,
                                     StudentTrackingExcelExporter excelExporter) {
        this.trackingService = trackingService;
        this.excelExporter = excelExporter;
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
            return redirectToSheet(created.getId(), 1);
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
                        Model model) {
        model.addAttribute("trackingClass", trackingService.findClass(classId));
        model.addAttribute("trackingClasses", trackingService.listClasses());
        model.addAttribute("sheetForm", trackingService.getSheet(classId, semester));
        model.addAttribute("semester", semester);
        model.addAttribute("attendanceStatuses", AttendanceStatus.values());
        return "student-tracking/sheet";
    }

    @PostMapping("/classes/{classId}/students")
    public String addStudent(@PathVariable Integer classId,
                             @RequestParam String fullName,
                             @RequestParam(defaultValue = "1") int semester,
                             RedirectAttributes redirectAttributes) {
        try {
            trackingService.addStudent(classId, fullName);
            redirectAttributes.addFlashAttribute("message", "Da them hoc sinh.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester);
    }

    @PostMapping("/classes/{classId}/records")
    public String saveSheet(@PathVariable Integer classId,
                            @ModelAttribute TrackingSheetForm sheetForm,
                            RedirectAttributes redirectAttributes) {
        int semester = sheetForm.getSemester() == null ? 1 : sheetForm.getSemester();
        try {
            trackingService.saveSheet(classId, sheetForm);
            redirectAttributes.addFlashAttribute("message", "Da luu so theo doi hoc ky " + semester + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester);
    }

    @PostMapping("/classes/{classId}/students/{studentId}/delete")
    public String deleteStudent(@PathVariable Integer classId,
                                @PathVariable Integer studentId,
                                @RequestParam(defaultValue = "1") int semester,
                                RedirectAttributes redirectAttributes) {
        try {
            trackingService.deleteStudent(classId, studentId);
            redirectAttributes.addFlashAttribute("message", "Da xoa hoc sinh khoi lop.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToSheet(classId, semester);
    }

    @PostMapping("/import")
    public String importWorkbook(@RequestParam MultipartFile file,
                                 @RequestParam(defaultValue = "1") int semester,
                                 RedirectAttributes redirectAttributes) {
        try {
            StudentTrackingService.ImportSummary summary = trackingService.importWorkbook(file, semester);
            redirectAttributes.addFlashAttribute("message", "Da import " + summary.getClassCount()
                    + " lop va " + summary.getStudentCount() + " hoc sinh vao hoc ky " + semester + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Khong the doc file Excel. Vui long kiem tra lai file.");
        }
        return "redirect:/student-tracking";
    }

    private String redirectToSheet(Integer classId, int semester) {
        int safeSemester = semester == 2 ? 2 : 1;
        return "redirect:/student-tracking/classes/" + classId + "?semester=" + safeSemester;
    }
}
