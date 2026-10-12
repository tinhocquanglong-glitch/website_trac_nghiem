package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Province;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.ILocationService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/school")
public class SchoolController {

    @Autowired
    private ILocationService locationService;

    @Autowired
    private IUserService userService;

    @ModelAttribute("admin")
    public User currentAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findById(username);
    }

    @ModelAttribute("provinces")
    public List<Province> provinces() {
        return locationService.getAllProvinces();
    }

    @GetMapping("/list")
    public String listSchools(
            @RequestParam("keyword") Optional<String> keyword,
            @RequestParam("provinceCode") Optional<Integer> provinceCode,
            Model model,
            @PageableDefault(size = 10) Pageable pageable) {

        String kw = keyword.orElse(null);
        Integer provCode = provinceCode.orElse(null);

        Page<School> schools = locationService.searchSchools(kw, provCode, pageable);
        model.addAttribute("schools", schools);
        model.addAttribute("keyword", kw);
        model.addAttribute("selectedProvinceCode", provCode);

        return "school/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("school", new School());
        return "school/add";
    }

    @PostMapping("/add")
    public String addSchool(@Valid @ModelAttribute("school") School school,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            return "school/add";
        }
        locationService.saveSchool(school);
        redirectAttributes.addFlashAttribute("message", "Thêm trường học thành công!");
        return "redirect:/school/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        School school = locationService.findSchoolById(id);
        if (school == null) {
            return "redirect:/school/list";
        }
        model.addAttribute("school", school);
        if (school.getProvinceCode() != null) {
            model.addAttribute("districts", locationService.getDistrictsByProvince(school.getProvinceCode()));
        }
        if (school.getDistrictCode() != null) {
            model.addAttribute("wards", locationService.getWardsByDistrict(school.getDistrictCode()));
        }
        return "school/edit";
    }

    @PostMapping("/update")
    public String updateSchool(@Valid @ModelAttribute("school") School school,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "school/edit";
        }
        locationService.saveSchool(school);
        redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin trường học thành công!");
        return "redirect:/school/list";
    }

    @GetMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        School school = locationService.findSchoolById(id);
        if (school != null) {
            school.setActive(!school.isActive());
            locationService.saveSchool(school);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái trường học!");
        }
        return "redirect:/school/list";
    }
}
