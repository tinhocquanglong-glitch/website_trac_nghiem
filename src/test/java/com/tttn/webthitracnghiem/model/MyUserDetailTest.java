package com.tttn.webthitracnghiem.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MyUserDetailTest {

    @Test
    void accountsOfLockedSchoolCannotSignInButSystemAdminCan() {
        School lockedSchool = new School();
        lockedSchool.setActive(false);
        lockedSchool.setDeleted(false);

        assertThat(new MyUserDetail(user("ROLE_TEACHER", lockedSchool)).isAccountNonLocked()).isFalse();
        assertThat(new MyUserDetail(user("ROLE_SCHOOL_ADMIN", lockedSchool)).isAccountNonLocked()).isFalse();
        assertThat(new MyUserDetail(user("ROLE_ADMIN", lockedSchool)).isAccountNonLocked()).isTrue();
    }

    @Test
    void accountsOfDeletedSchoolAreDisabledButSystemAdminIsNotDisabled() {
        School deletedSchool = new School();
        deletedSchool.setActive(false);
        deletedSchool.setDeleted(true);

        MyUserDetail teacherDetail = new MyUserDetail(user("ROLE_TEACHER", deletedSchool));
        assertThat(teacherDetail.isEnabled()).isFalse();

        MyUserDetail principalDetail = new MyUserDetail(user("ROLE_SCHOOL_ADMIN", deletedSchool));
        assertThat(principalDetail.isEnabled()).isFalse();

        MyUserDetail studentDetail = new MyUserDetail(user("ROLE_USER", deletedSchool));
        assertThat(studentDetail.isEnabled()).isFalse();

        MyUserDetail adminDetail = new MyUserDetail(user("ROLE_ADMIN", deletedSchool));
        assertThat(adminDetail.isEnabled()).isTrue();
    }

    @Test
    void studentIsLockedThroughTheSchoolOfTheirClass() {
        School activeSchool = new School();
        School lockedSchool = new School();
        lockedSchool.setActive(false);
        lockedSchool.setDeleted(false);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setSchool(lockedSchool);
        User student = user("ROLE_USER", activeSchool);
        student.setTrackingClass(trackingClass);

        assertThat(new MyUserDetail(student).isAccountNonLocked()).isFalse();
        assertThat(new MyUserDetail(user("ROLE_USER", activeSchool)).isAccountNonLocked()).isTrue();
    }

    private static User user(String roleName, School school) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setId(roleName.toLowerCase());
        user.setRoles(Set.of(role));
        user.setSchool(school);
        return user;
    }
}
