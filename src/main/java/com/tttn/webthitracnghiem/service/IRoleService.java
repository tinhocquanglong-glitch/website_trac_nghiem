package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Role;

public interface IRoleService {
    Role findByName(String roleName);
}
