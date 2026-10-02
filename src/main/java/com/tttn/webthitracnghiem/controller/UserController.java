package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@SessionAttributes("admin")
public class UserController {
    @Autowired
    IUserService userService;

    @ModelAttribute("admin")
    public User admin(){
        return userService.findById(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @GetMapping("/user/list")
    public String showList(@RequestParam("keyword") Optional<String> name,
                           Model model, @PageableDefault(value = 5) Pageable pageable) {
        Page<User> users;
        List<User> admins = new ArrayList<>();
        Iterable<User> all = userService.findAll();
        all.forEach(user -> {
            user.getRoles().forEach(role -> {
                if(role.getRoleName().equals("ROLE_ADMIN")){
                    admins.add(user);
                }
            });
        });
        model.addAttribute("admins",admins);
        if(name.isPresent()){
            users = userService.search(name.get(),pageable);
            model.addAttribute("users",users);
            model.addAttribute("keyword",name.get());
            return "user/list";
        }
        users = userService.findAll(pageable);
        model.addAttribute("users", users);
        return "user/list";
    }

    @GetMapping("/user/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "user/create";
    }

    @PostMapping("/user/create")
    public String saveUser(@Validated @ModelAttribute("user") User user, BindingResult bindingResult) throws ParseException {
        if (bindingResult.hasFieldErrors()) {
            return "user/create";
        } else {
            userService.save(user);
            return "redirect:/user/list";
        }
    }

    @GetMapping("/user/editMember/{id}")
    public String editMember(@PathVariable("id") String id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "user/editMember";
    }

    @PostMapping("/user/update")
    public String update(@Validated User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasFieldErrors()) {
            return "user/editMember";
        } else {
            User user1 = userService.findById(user.getId());
            user1.setFullName(user.getFullName());
            user1.setEmail(user.getEmail());
            user1.setAddress(user.getAddress());
            user1.setPhoneNumber(user.getPhoneNumber());
            userService.save(user1);
            redirectAttributes.addFlashAttribute("message", "Cập Nhật Thành Công !");
            return "redirect:/user/list";
        }
    }

    @GetMapping("/user/delete/{id}")
    public String delete(@PathVariable("id") String id, RedirectAttributes ra) {
        User user = userService.findById(id);
        userService.delete(user);
        ra.addFlashAttribute("message","Xóa người dùng thành công!");
        return "redirect:/user/list";
    }

    @GetMapping("/user/editPass/{id}")
    public String editPass(@PathVariable("id") String id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "user/editPass";
    }

    @PostMapping("/user/updatePass")
    public String updatePass(@Valid @ModelAttribute User user, BindingResult bindingResult, Model model,
                             @RequestParam("oldPass") String oldPass, RedirectAttributes redirectAttributes) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String oldPass2 = userService.findByPass(user.getId());
        if (!passwordEncoder.matches(oldPass,oldPass2)){
            model.addAttribute("messages","Mật Khẩu Cũ Không Đúng !");
            return "user/editPass";
        }
        if (user.getPassWord() != null && user.getRePassWord() != null) {
            if (!user.getPassWord().equals(user.getRePassWord())) {
                bindingResult.addError(new FieldError("user", "rePassWord", "Mật khẩu phải trùng nhau"));
            }
        }
        if (bindingResult.hasFieldErrors()) {
            return "user/editPass";
        }
        User user1 = userService.findById(user.getId());
        user1.setPassWord(passwordEncoder.encode(user.getPassWord()));
        user1.setRePassWord(passwordEncoder.encode(user.getRePassWord()));
        userService.save(user1);
        redirectAttributes.addFlashAttribute("message", "Cập Nhật Thành Công !");
        return "redirect:/user/list";
    }
}
