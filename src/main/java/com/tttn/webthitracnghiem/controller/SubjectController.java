package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/subject")
public class SubjectController {

    @Autowired
    private ISubjectService subjectService;
    @Autowired
    private IUserService userService;
    @Autowired
    private ISubjectClassService subjectClassService;
    @Autowired
    private SubjectClassRepository subjectClassRepository;
    @Autowired
    private IClassesService classesService;
    @Autowired
    private IChapterService chapterService;
    @Autowired
    private ILessonService lessonService;
    @Autowired
    private CurriculumService curriculumService;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? userService.findById(auth.getName()) : null;
    }

    private School currentSchool() {
        User user = currentUser();
        return user != null ? user.getSchool() : null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private boolean isSchoolAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_SCHOOL_ADMIN".equals(a.getAuthority()));
    }

    private boolean canManageSubjects() {
        return isAdmin() || isSchoolAdmin();
    }

    @GetMapping("/list")
    public String index(@RequestParam("keyword") Optional<String> name,
                        @RequestParam("create") Optional<Boolean> create,
                        @RequestParam("update") Optional<Boolean> update,
                        Model model, @PageableDefault(value = 5) Pageable pageable) {
        if (create.isPresent()) {
            model.addAttribute("message", "Tạo môn học thành công!");
        }
        if (update.isPresent()) {
            model.addAttribute("message", "Chỉnh sửa môn học thành công!");
        }

        User user = currentUser();
        boolean admin = isAdmin();
        boolean schoolAdmin = isSchoolAdmin();
        School school = currentSchool();
        Integer schoolId = school != null ? school.getId() : null;

        Page<Subject> subjects;
        if (admin) {
            if (name.isPresent() && !name.get().isBlank()) {
                subjects = subjectService.findByNameSubject(name.get().trim(), pageable);
                model.addAttribute("keyword", name.get().trim());
            } else {
                subjects = subjectService.findAll(pageable);
            }
        } else {
            if (name.isPresent() && !name.get().isBlank()) {
                subjects = subjectService.findByNameAndSchoolOrGlobal(name.get().trim(), schoolId, pageable);
                model.addAttribute("keyword", name.get().trim());
            } else {
                subjects = subjectService.findBySchoolOrGlobal(schoolId, pageable);
            }
        }

        model.addAttribute("subjects", subjects);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isSchoolAdmin", schoolAdmin);
        model.addAttribute("currentSchool", school);
        model.addAttribute("currentUser", user);
        return "subject/list";
    }

    @GetMapping("/list/{id}")
    public String getSubject(@RequestParam Optional<String> create,
                             @RequestParam Optional<String> update,
                             @PathVariable Integer id, Model model,
                             @PageableDefault(value = 10) Pageable pageable) {
        boolean admin = isAdmin();
        School school = currentSchool();
        Integer schoolId = school != null ? school.getId() : null;

        Page<SubjectClasses> page;
        if (admin) {
            page = subjectClassService.findByClassId(id, pageable);
        } else {
            page = subjectClassService.findByClassIdAndSchoolOrGlobal(id, schoolId, pageable);
        }

        if (create.isPresent()) {
            model.addAttribute("message", "Thêm môn học thành công");
        }
        if (update.isPresent()) {
            model.addAttribute("message", "Cập nhập môn học thành công");
        }
        model.addAttribute("class", classesService.findById(id));
        model.addAttribute("subjects", page);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isSchoolAdmin", isSchoolAdmin());
        return "class/subjectList";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model, RedirectAttributes ra) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list";
        }
        Subject subject = subjectService.findById(id);
        if (subject == null) {
            ra.addFlashAttribute("error", "Không tìm thấy môn học!");
            return "redirect:/subject/list";
        }
        if (!subjectService.canManage(subject, currentUser(), isAdmin())) {
            ra.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa môn học này!");
            return "redirect:/subject/list";
        }
        List<SubjectClasses> existingSc = subjectClassRepository.findBySubject(subject.getId());
        if (subject.getSchool() != null) {
            existingSc = existingSc.stream()
                    .filter(sc -> sc.getSchool() != null && sc.getSchool().getId().equals(subject.getSchool().getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            existingSc = existingSc.stream()
                    .filter(sc -> sc.getSchool() == null)
                    .collect(java.util.stream.Collectors.toList());
        }
        List<Integer> assignedClassIds = existingSc.stream()
                .filter(sc -> sc.getClasses() != null)
                .map(sc -> sc.getClasses().getId())
                .collect(java.util.stream.Collectors.toList());

        SubjectRequest request = new SubjectRequest();
        request.setId(subject.getId());
        request.setName(subject.getName());
        request.setClassIds(assignedClassIds);

        model.addAttribute("subject", subject);
        model.addAttribute("subjectRequest", request);
        model.addAttribute("assignedClassIds", assignedClassIds);
        model.addAttribute("classesList", classesService.findAll());
        model.addAttribute("isAdmin", isAdmin());
        model.addAttribute("isSchoolAdmin", isSchoolAdmin());
        model.addAttribute("currentSchool", currentSchool());
        return "subject/edit";
    }

    @GetMapping("/create")
    public String create(Model model) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list";
        }
        model.addAttribute("subject", new SubjectRequest());
        model.addAttribute("classesList", classesService.findAll());
        model.addAttribute("isAdmin", isAdmin());
        model.addAttribute("isSchoolAdmin", isSchoolAdmin());
        model.addAttribute("currentSchool", currentSchool());
        return "subject/add";
    }

    @PostMapping("/create")
    public String create(@Validated @ModelAttribute SubjectRequest subject, BindingResult bindingResult,
                         RedirectAttributes re, Model model) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("classesList", classesService.findAll());
            model.addAttribute("isAdmin", isAdmin());
            model.addAttribute("isSchoolAdmin", isSchoolAdmin());
            model.addAttribute("currentSchool", currentSchool());
            return "subject/add";
        } else {
            if (subject.getImage() != null && subject.getImage().getOriginalFilename() != null
                    && subject.getImage().getOriginalFilename().isEmpty()) {
                subject.setImage(null);
            }
            School school = isAdmin() ? null : currentSchool();
            subjectService.save(subject, school);
            String scopeMsg = isAdmin() ? "toàn quốc (tất cả các trường)" : (school != null ? "trường " + school.getName() : "");
            re.addFlashAttribute("message", "Tạo môn học thành công cho " + scopeMsg + "!");
            return "redirect:/subject/list";
        }
    }

    @PostMapping("/update")
    public String update(@Validated @ModelAttribute SubjectRequest subject, BindingResult bindingResult,
                         RedirectAttributes re, Model model) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list";
        }
        Subject existing = subjectService.findById(subject.getId());
        if (existing == null || !subjectService.canManage(existing, currentUser(), isAdmin())) {
            re.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa môn học này!");
            return "redirect:/subject/list";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("subject", existing);
            model.addAttribute("classesList", classesService.findAll());
            model.addAttribute("isAdmin", isAdmin());
            model.addAttribute("isSchoolAdmin", isSchoolAdmin());
            model.addAttribute("currentSchool", currentSchool());
            return "subject/edit";
        }
        if (subject.getImage() != null && subject.getImage().getOriginalFilename() != null
                && subject.getImage().getOriginalFilename().isEmpty()) {
            subject.setImage(null);
        }
        School school = existing.getSchool();
        subjectService.save(subject, school);
        re.addFlashAttribute("message", "Cập nhật môn học thành công!");
        return "redirect:/subject/list";
    }

    @PostMapping("/delete")
    public String remove(@RequestParam("id") Integer id,
                         @RequestParam("password") String password,
                         RedirectAttributes ra) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list";
        }
        User user = currentUser();
        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập lại!");
            return "redirect:/subject/list";
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (password == null || password.trim().isEmpty() || !encoder.matches(password.trim(), user.getPassWord())) {
            ra.addFlashAttribute("error", "Mật khẩu xác thực không chính xác! Không thể xóa môn học.");
            return "redirect:/subject/list";
        }
        Subject subject = subjectService.findById(id);
        if (subject == null) {
            ra.addFlashAttribute("error", "Không tìm thấy môn học!");
            return "redirect:/subject/list";
        }
        if (!subjectService.canManage(subject, user, isAdmin())) {
            ra.addFlashAttribute("error", "Bạn không có quyền xóa môn học này!");
            return "redirect:/subject/list";
        }
        try {
            subjectService.remove(subject);
            ra.addFlashAttribute("message", "Đã xóa môn học '" + subject.getName() + "' thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Đã xảy ra lỗi khi xóa môn học!");
        }
        return "redirect:/subject/list";
    }

    @GetMapping("/delete/{id}")
    public String removeGet(@PathVariable Integer id, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Hành động xóa môn học yêu cầu xác thực mật khẩu qua biểu mẫu!");
        return "redirect:/subject/list";
    }

    @GetMapping("/copy/{id}")
    public String copySubject(@PathVariable Integer id, RedirectAttributes ra) {
        if (!isSchoolAdmin()) {
            ra.addFlashAttribute("error", "Chỉ hiệu trưởng (Admin trường) mới có thể sao chép môn học về trường!");
            return "redirect:/subject/list";
        }
        School school = currentSchool();
        if (school == null) {
            ra.addFlashAttribute("error", "Tài khoản của bạn chưa được liên kết với trường học nào!");
            return "redirect:/subject/list";
        }
        Subject copied = subjectService.copyToSchool(id, school);
        if (copied != null) {
            ra.addFlashAttribute("message", "Đã sao chép môn '" + copied.getName() + "' về trường của bạn thành công!");
        } else {
            ra.addFlashAttribute("error", "Không thể sao chép môn học!");
        }
        return "redirect:/subject/list";
    }

    @GetMapping("/class/create/{classId}")
    public String createSubject(Model model, @PathVariable Integer classId) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list/" + classId;
        }
        boolean admin = isAdmin();
        School school = currentSchool();
        Integer schoolId = school != null ? school.getId() : null;

        List<SubjectClasses> subjectClassesList;
        if (admin) {
            subjectClassesList = subjectClassService.findByClassId(classId);
        } else {
            subjectClassesList = subjectClassService.findByClassIdAndSchoolOrGlobal(classId, schoolId);
        }

        List<Subject> subjectList = new ArrayList<>();
        for (SubjectClasses sc : subjectClassesList) {
            if (sc.getSubject() != null) {
                subjectList.add(sc.getSubject());
            }
        }

        List<Subject> allAvailableSubjects;
        if (admin) {
            allAvailableSubjects = subjectService.getAll();
        } else {
            allAvailableSubjects = subjectService.getBySchoolOrGlobal(schoolId);
        }

        model.addAttribute("subjectClasses", subjectClassesList);
        model.addAttribute("subjects", allAvailableSubjects);
        model.addAttribute("subjectOfClass", subjectList);
        model.addAttribute("class", classesService.findById(classId));
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isSchoolAdmin", isSchoolAdmin());
        return "class/addSubjectToClass";
    }

    @GetMapping("/class/{classId}/{subjectId}")
    public String deleteSubject(@PathVariable("classId") Integer classId,
                                @PathVariable("subjectId") Integer subjectId,
                                RedirectAttributes ra) {
        if (!canManageSubjects()) {
            return "redirect:/subject/list/" + classId;
        }
        boolean admin = isAdmin();
        School school = currentSchool();
        Integer schoolId = school != null ? school.getId() : null;

        SubjectClasses subjectClasses;
        if (admin) {
            subjectClasses = subjectClassService.findByClassAndSubject(classId, subjectId);
        } else {
            if (schoolId != null) {
                subjectClasses = subjectClassRepository.findByClassAndSubjectAndSchool(classId, subjectId, schoolId).orElse(null);
            } else {
                subjectClasses = null;
            }
            if (subjectClasses == null) {
                ra.addFlashAttribute("error", "Bạn không thể xóa môn học chung của hệ thống!");
                return "redirect:/subject/list/" + classId;
            }
        }

        if (subjectClasses == null || subjectClasses.getId() == 0) {
            ra.addFlashAttribute("error", "Không tìm thấy môn học trong khối!");
            return "redirect:/subject/list/" + classId;
        }

        try {
            curriculumService.deleteSubjectClass(subjectClasses);
            ra.addFlashAttribute("message", "Xóa thành công môn học khỏi khối!");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/subject/list/" + classId;
    }
}
