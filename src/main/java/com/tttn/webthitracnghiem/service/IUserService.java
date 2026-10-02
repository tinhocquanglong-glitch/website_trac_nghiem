package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.UserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUserService {
    Page<User> findAll(Pageable pageable);
    Page<User> search(String name, Pageable pageable);
    User save(User user);
    Iterable<User> findAll();
    User findById(String id);
    void delete(User user);
    Integer findByTotalUser();
    String findByNewUser();
    boolean userExists(String id);
    boolean userExistss(String email);
    String findByPass(String id);
    List<User> getNewMember();
    int getTotalMember();
    User save(UserRequest userRequest);
}
