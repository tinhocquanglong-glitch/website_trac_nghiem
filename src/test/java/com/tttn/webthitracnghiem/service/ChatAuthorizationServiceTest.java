package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ChatRoom;
import com.tttn.webthitracnghiem.model.ChatRoomType;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatAuthorizationServiceTest {
    private final ChatAuthorizationService authorizationService = new ChatAuthorizationService();

    @Test
    void fullAdminCanAccessGlobalAndEverySchoolRoom() {
        User admin = user("ROLE_ADMIN", null);
        ChatRoom global = new ChatRoom(ChatRoomType.GLOBAL_ADMIN, null, "global");
        ChatRoom schoolRoom = new ChatRoom(ChatRoomType.SCHOOL_STAFF, school(1), "school");

        assertThat(authorizationService.canAccess(admin, global)).isTrue();
        assertThat(authorizationService.canAccess(admin, schoolRoom)).isTrue();
    }

    @Test
    void schoolAdminCanAccessGlobalAndOwnSchoolOnly() {
        User admin = user("ROLE_SCHOOL_ADMIN", school(1));
        ChatRoom global = new ChatRoom(ChatRoomType.GLOBAL_ADMIN, null, "global");
        ChatRoom ownSchool = new ChatRoom(ChatRoomType.SCHOOL_STAFF, school(1), "own");
        ChatRoom otherSchool = new ChatRoom(ChatRoomType.SCHOOL_STAFF, school(2), "other");

        assertThat(authorizationService.canAccess(admin, global)).isTrue();
        assertThat(authorizationService.canAccess(admin, ownSchool)).isTrue();
        assertThat(authorizationService.canAccess(admin, otherSchool)).isFalse();
    }

    @Test
    void teacherCanAccessOwnSchoolRoomButNotGlobalRoom() {
        User teacher = user("ROLE_TEACHER", school(1));
        ChatRoom ownSchool = new ChatRoom(ChatRoomType.SCHOOL_STAFF, school(1), "own");
        ChatRoom global = new ChatRoom(ChatRoomType.GLOBAL_ADMIN, null, "global");

        assertThat(authorizationService.canAccess(teacher, ownSchool)).isTrue();
        assertThat(authorizationService.canAccess(teacher, global)).isFalse();
    }

    private User user(String roleName, School school) {
        User user = new User();
        user.setId(roleName.toLowerCase());
        user.setRoles(Set.of(new Role(roleName)));
        user.setSchool(school);
        return user;
    }

    private School school(Integer id) {
        School school = new School();
        school.setId(id);
        school.setName("School " + id);
        return school;
    }
}
