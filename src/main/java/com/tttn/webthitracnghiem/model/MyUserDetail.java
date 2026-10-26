package com.tttn.webthitracnghiem.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MyUserDetail implements UserDetails {
    private User user;
    private final boolean schoolLocked;
    private final boolean schoolDeleted;

    public MyUserDetail(User user) {
        this.user = user;
        this.schoolDeleted = belongsToDeletedSchool(user);
        this.schoolLocked = belongsToLockedSchool(user);
    }

    private static School getSchoolOfUser(User user) {
        if (user == null) {
            return null;
        }
        if (user.getTrackingClass() != null && user.getTrackingClass().getSchool() != null) {
            return user.getTrackingClass().getSchool();
        }
        return user.getSchool();
    }

    private static boolean isSystemAdmin(User user) {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()));
    }

    /** Accounts of a deleted school cannot sign in; the system admin is never locked out. */
    private static boolean belongsToDeletedSchool(User user) {
        if (isSystemAdmin(user)) {
            return false;
        }
        School school = getSchoolOfUser(user);
        return school != null && school.isDeleted();
    }

    /** Accounts of a temporarily locked school cannot sign in. */
    private static boolean belongsToLockedSchool(User user) {
        if (isSystemAdmin(user)) {
            return false;
        }
        School school = getSchoolOfUser(user);
        return school != null && !school.isDeleted() && !school.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()){
                grantedAuthorityList.add(new SimpleGrantedAuthority(role.getRoleName()));
            }
        }
        return grantedAuthorityList;
    }

    @Override
    public String getPassword() {
        return user.getPassWord();
    }

    @Override
    public String getUsername() {
        return user.getId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !schoolLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return (user == null || user.isActive()) && !schoolDeleted;
    }
}
