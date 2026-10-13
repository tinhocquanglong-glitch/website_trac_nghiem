package com.tttn.webthitracnghiem.api;

public record ChatRoomResponse(
        Integer id,
        String type,
        String name,
        Integer schoolId,
        String schoolName
) {
}
