package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.repository.RoleRepository;
import com.tttn.webthitracnghiem.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements IRoleService {
    @Autowired
    private RoleRepository roleRepository;
    @Override
    public Role findByName(String roleName) {
        return roleRepository.findByRoleName(roleName).orElse(null);
    }
}
