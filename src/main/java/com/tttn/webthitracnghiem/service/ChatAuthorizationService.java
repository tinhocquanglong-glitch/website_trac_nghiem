package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ChatRoom;
import com.tttn.webthitracnghiem.model.ChatRoomType;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ChatAuthorizationService {

    public boolean canAccess(User user, ChatRoom room) {
        if (user == null || room == null || user.getRoles() == null) {
            return false;
        }

        if (hasRole(user, "ROLE_ADMIN")) {
            return true;
        }

        if (room.getRoomType() == ChatRoomType.GLOBAL_ADMIN) {
            return hasRole(user, "ROLE_SCHOOL_ADMIN");
        }

        if (room.getRoomType() != ChatRoomType.SCHOOL_STAFF
                || room.getSchool() == null
                || user.getSchool() == null
                || !room.getSchool().getId().equals(user.getSchool().getId())) {
            return false;
        }

        return hasRole(user, "ROLE_SCHOOL_ADMIN") || hasRole(user, "ROLE_TEACHER");
    }

    public void assertCanAccess(User user, ChatRoom room) {
        if (!canAccess(user, room)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập nhóm chat này.");
        }
    }

    public String primaryRole(User user) {
        Set<Role> roles = user.getRoles();
        if (roles != null) {
            if (hasRole(user, "ROLE_ADMIN")) return "ROLE_ADMIN";
            if (hasRole(user, "ROLE_SCHOOL_ADMIN")) return "ROLE_SCHOOL_ADMIN";
            if (hasRole(user, "ROLE_TEACHER")) return "ROLE_TEACHER";
            if (hasRole(user, "ROLE_USER")) return "ROLE_USER";
        }
        return "ROLE_UNKNOWN";
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getRoleName()));
    }
}
