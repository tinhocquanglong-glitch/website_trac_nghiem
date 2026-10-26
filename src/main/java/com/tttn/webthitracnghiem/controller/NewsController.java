package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.INewsService;
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
@RequestMapping("/news")
public class NewsController {

    @Autowired
    private INewsService newsService;
    @Autowired
    private IUserService userService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public String index(@RequestParam("keyword") Optional<String> name,
                        @RequestParam("create") Optional<Boolean> create,
                        @RequestParam("update") Optional<Boolean> update,
                        Model model, @PageableDefault(value = 5) Pageable pageable){
        if(create.isPresent()){
            model.addAttribute("message","Thêm tin tức thành công!");
        }
        if(update.isPresent()){
            model.addAttribute("message","Chỉnh sửa tin tức thành công!");
        }
        Page<News> news;
        if(name.isPresent()){
            news = newsService.findByTitle(name.get(),pageable);
            model.addAttribute("newses",news);
            model.addAttribute("keyword",name.get());
            return "news/list";
        }
        news = newsService.findAll(pageable);
        model.addAttribute("newses",news);
        return "news/list";
    }

    @GetMapping("/create")
    public String create(Model model){
        model.addAttribute("news",new News());
        return "news/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model){
        News news = newsService.findById(Integer.parseInt(id));
        model.addAttribute("news",news);
        return "news/edit";
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
            ra.addFlashAttribute("error", "Chỉ Quản trị viên hệ thống hoặc Hiệu trưởng mới có quyền xóa tin tức!");
            return "redirect:/news/list";
        }

        News news = newsService.findById(id);
        if (news == null) {
            ra.addFlashAttribute("error", "Không tìm thấy tin tức cần xóa!");
            return "redirect:/news/list";
        }

        if (isSchoolAdmin && !isAdmin) {
            if (news.getSchool() != null && currentUser.getSchool() != null
                    && !news.getSchool().getId().equals(currentUser.getSchool().getId())) {
                ra.addFlashAttribute("error", "Bạn chỉ có thể xóa tin tức thuộc trường học của mình!");
                return "redirect:/news/list";
            }
        }

        if (password == null || password.trim().isEmpty() || currentUser == null || !passwordEncoder.matches(password.trim(), currentUser.getPassWord())) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không chính xác! Không thể xóa (ẩn) tin tức.");
            return "redirect:/news/list";
        }

        newsService.remove(news);
        ra.addFlashAttribute("message", "Đã xóa (ẩn) tin tức thành công!");
        return "redirect:/news/list";
    }

    @GetMapping("/delete/{id}")
    public String removeLegacy(@PathVariable String id, RedirectAttributes ra){
        ra.addFlashAttribute("error", "Hành động xóa tin tức yêu cầu xác thực mật khẩu qua biểu mẫu bảo mật!");
        return "redirect:/news/list";
    }
}
