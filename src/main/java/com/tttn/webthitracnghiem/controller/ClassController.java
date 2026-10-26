package com.tttn.webthitracnghiem.controller;
import com.tttn.webthitracnghiem.model.User;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.service.IClassesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/class")
public class ClassController {
    @Autowired
    private IClassesService classesService;
    @Autowired
    private com.tttn.webthitracnghiem.service.IUserService userService;
    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;
    @GetMapping("/list")
    public String index(Model model, @PageableDefault(value = 10) Pageable pageable){
        Page<Classes> classes;
        classes = classesService.findAll(pageable);
        model.addAttribute("classes",classes);
        return "class/list";
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model){
        if (!canManageCurriculum()) {
            return "redirect:/class/list";
        }
        Classes classes = classesService.findById(Integer.parseInt(id));
        model.addAttribute("class",classes);
        return "class/edit";
    }
    @GetMapping("/create")
    public String create(Model model){
        if (!canManageCurriculum()) {
            return "redirect:/class/list";
        }
        model.addAttribute("class",new Classes());
        return "class/add";
    }
    @PostMapping("/create")
    public String save(@ModelAttribute Classes classes, RedirectAttributes ra){
        if (!canManageCurriculum()) {
            return "redirect:/class/list";
        }
        classesService.save(classes);
        ra.addFlashAttribute("message","Thêm lớp học thành công!");
        return "redirect:/class/list";
    }
    @GetMapping("/delete/{id}")
    public String deleteLegacy(@PathVariable String id, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Thao tác xóa khối học yêu cầu quyền Quản trị viên cấp cao và xác thực mật khẩu!");
        return "redirect:/class/list";
    }

    @PostMapping("/delete")
    public String deletePost(@RequestParam("id") Integer id,
                             @RequestParam(value = "password", required = false) String password,
                             RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thực hiện!");
            return "redirect:/login";
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            ra.addFlashAttribute("error", "Chỉ Quản trị viên hệ thống (Admin) mới có quyền xóa khối học!");
            return "redirect:/class/list";
        }
        User currentUser = userService.findById(auth.getName());
        if (currentUser == null || password == null || password.trim().isEmpty() || !passwordEncoder.matches(password.trim(), currentUser.getPassWord())) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa khối học.");
            return "redirect:/class/list";
        }

        Classes classes = classesService.findById(id);
        if (classes == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khối học!");
            return "redirect:/class/list";
        }

        if (classes.getSubjectClasses() != null && !classes.getSubjectClasses().isEmpty()) {
            ra.addFlashAttribute("error", "Không thể xóa khối học đang chứa môn học và chương trình đào tạo!");
            return "redirect:/class/list";
        }

        classesService.remove(classes);
        ra.addFlashAttribute("message", "Xóa khối học thành công!");
        return "redirect:/class/list";
    }
    @PostMapping("/update")
    public String update(@ModelAttribute Classes classes, RedirectAttributes ra){
        if (!canManageCurriculum()) {
            return "redirect:/class/list";
        }
        classesService.save(classes);
        ra.addFlashAttribute("message","Cập nhập lớp học thành công!");
        return "redirect:/class/list";
    }

    private boolean canManageCurriculum() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_SCHOOL_ADMIN".equals(authority.getAuthority())
                        || "ROLE_TEACHER".equals(authority.getAuthority()));
    }
}
