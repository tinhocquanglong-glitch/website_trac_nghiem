package com.tttn.webthitracnghiem.controller;

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
    public String delete(@PathVariable String id,RedirectAttributes ra){
        if (!canManageCurriculum()) {
            return "redirect:/class/list";
        }
        Classes classes = classesService.findById(Integer.parseInt(id));
        classesService.remove(classes);
        ra.addFlashAttribute("message","Xóa lớp học thành công!");
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
