package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.service.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
public class MemberController {
    @Autowired
    IUserService userService;

    @Autowired
    IResultService rService;
    @Autowired
    private IExamService examService;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IQService iqService;
    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/signup")
    public String showSignUp(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/signup")
    public String signUp(@Valid @ModelAttribute User user, BindingResult bindingResult, Model model) {
        if (userService.userExists(user.getId())) {
            bindingResult.addError(new FieldError("user", "id", "Tài khoản '" + user.getId() + "' đã tồn tại."));
        }
        if (userService.userExistss(user.getEmail())) {
            bindingResult.addError(new FieldError("user", "email", "Email '" + user.getEmail() + "' đã tồn tại."));
        }
        if (user.getPassWord() != null && user.getRePassWord() != null) {
            if (!user.getPassWord().equals(user.getRePassWord())) {
                bindingResult.addError(new FieldError("user", "rePassWord", "Mật khẩu phải trùng nhau"));
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("username", user.getId());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("pass", user.getPassWord());
            model.addAttribute("rePass", user.getRePassWord());
            return "register";
        }
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassWord(passwordEncoder.encode(user.getPassWord()));
        user.setRePassWord(user.getPassWord());
        user.setImg("/img/avatar/default.jpg");

        // Phân quyền User
        Role role = roleService.findByName("ROLE_USER");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        model.addAttribute("message", "Đăng kí thành công !");
        userService.save(user);
        return "login";
    }


    @GetMapping(value = "/view")
    public String showMemberView(@RequestParam Optional<Boolean> update, Model model) {
        if (update.isPresent()) {
            model.addAttribute("message", "Cập nhập ảnh đại diện thành công!");
        }
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findById(userName);
        model.addAttribute("users", user);
        return "member/view";
    }

//    @PostMapping("/edit")
//    public String editView(@Valid @ModelAttribute("users") UserRequest userRequest,BindingResult bindingResult,Model model){
//        if (bindingResult.hasErrors()){
//            return "member/view";
//        } else {
//            User find = userService.findById(userRequest.getId());
//            userRequest.setRoles(find.getRoles());
//            userService.save(userRequest);
//        }
//        model.addAttribute("message","Cập Nhật Thành Công !");
//        model.addAttribute("users",userRequest);
//        return "member/view";
//    }

    @GetMapping(value = "/editMember")
    public String showMemberEdit(Model model) {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findById(uname);
        model.addAttribute("users", user);
        model.addAttribute("userName", user.getId());
        return "member/editMember";
    }

    @PostMapping("/editMember")
    public String editMember(@Valid @ModelAttribute("users") User users, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userName", users.getId());
            return "member/editMember";
        } else {
            User user = userService.findById(users.getId());
            users.setRoles(user.getRoles());
            userService.save(users);
            model.addAttribute("message", "Cập Nhật Thành Công !");
            model.addAttribute("userName", users.getId());
            return "member/editMember";
        }
    }

    @GetMapping(value = "/editPass")
    public String showMemberEditPass(Model model) {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();
        User users = userService.findById(uname);
        model.addAttribute("users", users);
        return "member/editPass";
    }

    @PostMapping("/editPass")
    public String editPass(@Valid @ModelAttribute("users") User users, BindingResult bindingResult, Model model,
                           @RequestParam("oldPass") String oldPass, RedirectAttributes redirectAttributes) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String oldPass2 = userService.findByPass(users.getId());
        if (!passwordEncoder.matches(oldPass, oldPass2)) {
            model.addAttribute("message", "Mật Khẩu Không Đúng !");
            return "member/editPass";
        }
        if (users.getPassWord() != null && users.getRePassWord() != null) {
            if (!users.getPassWord().equals(users.getRePassWord())) {
                bindingResult.addError(new FieldError("users", "rePassWord", "Mật khẩu phải trùng nhau"));
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("userName", users.getId());
            return "member/editPass";
        } else {
            users.setPassWord(passwordEncoder.encode(users.getPassWord()));
            users.setRePassWord(passwordEncoder.encode(users.getRePassWord()));
            User user = userService.findById(users.getId());
            users.setRoles(user.getRoles());
            userService.save(users);
            redirectAttributes.addFlashAttribute("message", "Cập nhật mật khẩu thành công ! ");
            return "redirect:/editMember";
        }
    }

    @ModelAttribute("exams")
    public Iterable<Exam> showAll() {
        return examService.findAll();
    }

    @GetMapping(value = "/history")
    public String showHistory(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<Result> results = rService.findByHistory(username);
        model.addAttribute("results", results);
        model.addAttribute("sum", rService.findSum(username));
        model.addAttribute("avg", rService.findAvg(username));
        return "member/history";
    }
}
