package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.ChatRoom;
import com.tttn.webthitracnghiem.model.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {
    Optional<ChatRoom> findByRoomTypeAndSchoolId(ChatRoomType roomType, Integer schoolId);

    List<ChatRoom> findAllByOrderByRoomTypeAscNameAsc();
}
