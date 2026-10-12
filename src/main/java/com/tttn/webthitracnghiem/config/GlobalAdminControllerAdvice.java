package com.tttn.webthitracnghiem.config;

import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalAdminControllerAdvice {

    @Autowired
    private IUserService userService;

    @ModelAttribute("admin")
    public User populateCurrentAdmin(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userService.findById(auth.getName());
            if (user != null) {
                session.setAttribute("admin", user);
                return user;
            }
        }
        return null;
    }
}
