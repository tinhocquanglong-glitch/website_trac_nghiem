package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByRoomId(Integer roomId, Pageable pageable);
}
