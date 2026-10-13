package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ChatRoom;
import com.tttn.webthitracnghiem.model.ChatRoomType;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ChatMessageRepository;
import com.tttn.webthitracnghiem.repository.ChatRoomRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    @Test
    void blankMessageIsRejectedBeforeItIsPersisted() {
        ChatRoomRepository roomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository messageRepository = mock(ChatMessageRepository.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatAuthorizationService authorizationService = new ChatAuthorizationService();
        ChatService service = new ChatService(roomRepository, messageRepository, schoolRepository,
                userRepository, authorizationService);

        User teacher = new User();
        teacher.setId("teacher");
        teacher.setRoles(Set.of(new Role("ROLE_TEACHER")));
        teacher.setSchool(school(1));
        ChatRoom room = new ChatRoom(ChatRoomType.SCHOOL_STAFF, school(1), "school");
        room.setId(7);
        when(userRepository.findById("teacher")).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(7)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.saveMessage("teacher", 7, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được để trống");
    }

    private School school(Integer id) {
        School school = new School();
        school.setId(id);
        school.setName("School " + id);
        return school;
    }
}
