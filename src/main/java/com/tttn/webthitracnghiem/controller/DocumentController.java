package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Document;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IDocumentService;
import com.tttn.webthitracnghiem.service.IClassesService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/document")
public class DocumentController {

    @Autowired
    private IDocumentService documentService;
    @Autowired
    private ISubjectService subjectService;
    @Autowired
    private IClassesService classesService;
    @Autowired
    private IUserService userService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public String index(@RequestParam("keyword") Optional<String> name,
                        @RequestParam("create") Optional<Boolean> create,
                        @RequestParam("update") Optional<Boolean> update,
                        Model model, @PageableDefault(value = 5) Pageable pageable){
        Page<Document> documents;
        if(create.isPresent()){
            model.addAttribute("message","Tạo tài liệu thành công!");
        }
        if(update.isPresent()){
            model.addAttribute("message","Chỉnh sửa tài liệu thành công!");
        }
        if(name.isPresent()){
            documents = documentService.findByTitle(name.get(),pageable);
            model.addAttribute("documents",documents);
            model.addAttribute("keyword",name.get());
            return "document/list";
        }
        documents = documentService.findAll(pageable);
        model.addAttribute("documents",documents);
        return "document/list";
    }

    @GetMapping("/create")
    public String create(Model model){
        model.addAttribute("document",new Document());
        model.addAttribute("subjects",subjectService.findAll());
        model.addAttribute("grades", classesService.findAll());
        return "document/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model){
        Document document = documentService.findById(Integer.parseInt(id));
        model.addAttribute("document",document);
        model.addAttribute("subjects",subjectService.findAll());
        model.addAttribute("grades", classesService.findAll());
        return "document/edit";
    }

    @PostMapping("/delete")
    public String removePost(@RequestParam("id") Integer id,
                             @RequestParam(value = "password", required = false) String password,
                             RedirectAttributes ra){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thực hiện thao tác này!");
            return "redirect:/login";
        }
        User currentUser = userService.findById(auth.getName());
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSchoolAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"));

        if (!isAdmin && !isSchoolAdmin) {
            ra.addFlashAttribute("error", "Chỉ Quản trị viên hệ thống hoặc Hiệu trưởng mới có quyền xóa tài liệu!");
            return "redirect:/document/list";
        }

        Document document = documentService.findById(id);
        if (document == null) {
            ra.addFlashAttribute("error", "Không tìm thấy tài liệu cần xóa!");
            return "redirect:/document/list";
        }

        if (isSchoolAdmin && !isAdmin) {
            if (document.getSchool() != null && currentUser.getSchool() != null
                    && !document.getSchool().getId().equals(currentUser.getSchool().getId())) {
                ra.addFlashAttribute("error", "Bạn chỉ có thể xóa tài liệu thuộc trường học của mình!");
                return "redirect:/document/list";
            }
        }

        if (password == null || password.trim().isEmpty() || currentUser == null || !passwordEncoder.matches(password.trim(), currentUser.getPassWord())) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa (ẩn) tài liệu.");
            return "redirect:/document/list";
        }

        documentService.remove(document);
        ra.addFlashAttribute("message", "Đã xóa (ẩn) tài liệu thành công!");
        return "redirect:/document/list";
    }

    @GetMapping("/delete/{id}")
    public String removeLegacy(@PathVariable String id, RedirectAttributes ra){
        ra.addFlashAttribute("error", "Hành động xóa tài liệu yêu cầu xác thực mật khẩu qua biểu mẫu bảo mật!");
        return "redirect:/document/list";
    }
}
