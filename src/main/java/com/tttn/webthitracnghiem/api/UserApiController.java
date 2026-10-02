package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.UserRequest;
import com.tttn.webthitracnghiem.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserApiController {
    @Autowired
    private IUserService userService;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> save(@ModelAttribute UserRequest userRequest){
        User user = userService.findById(userRequest.getId());
        userRequest.setRoles(user.getRoles());
        return ResponseEntity.ok(userService.save(userRequest));
    }
}
