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

    public MyUserDetail(User user) {
        this.user = user;
        this.schoolLocked = belongsToLockedSchool(user);
    }

    /** Accounts of a locked school cannot sign in; the system admin is never locked out. */
    private static boolean belongsToLockedSchool(User user) {
        boolean systemAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()));
        if (systemAdmin) {
            return false;
        }
        School school = user.getTrackingClass() != null && user.getTrackingClass().getSchool() != null
                ? user.getTrackingClass().getSchool()
                : user.getSchool();
        return school != null && !school.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        for (Role role : user.getRoles()){
            grantedAuthorityList.add(new SimpleGrantedAuthority(role.getRoleName()));
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
        return true;
    }
}

