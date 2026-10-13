package com.tttn.webthitracnghiem.api;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Integer roomId,
        String senderId,
        String senderName,
        String senderRole,
        String content,
        LocalDateTime createdAt
) {
}
