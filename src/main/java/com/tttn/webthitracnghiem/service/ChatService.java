package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.api.ChatMessagePageResponse;
import com.tttn.webthitracnghiem.api.ChatMessageResponse;
import com.tttn.webthitracnghiem.api.ChatRoomResponse;
import com.tttn.webthitracnghiem.model.ChatMessage;
import com.tttn.webthitracnghiem.model.ChatRoom;
import com.tttn.webthitracnghiem.model.ChatRoomType;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ChatMessageRepository;
import com.tttn.webthitracnghiem.repository.ChatRoomRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final ChatAuthorizationService authorizationService;

    public ChatService(ChatRoomRepository roomRepository,
                       ChatMessageRepository messageRepository,
                       SchoolRepository schoolRepository,
                       UserRepository userRepository,
                       ChatAuthorizationService authorizationService) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public List<ChatRoomResponse> listRooms(String username) {
        ensureRooms();
        User user = findUser(username);
        return roomRepository.findAllByOrderByRoomTypeAscNameAsc().stream()
                .filter(room -> authorizationService.canAccess(user, room))
                .map(this::toRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse listMessages(String username, Integer roomId, Pageable pageable) {
        User user = findUser(username);
        ChatRoom room = findRoom(roomId);
        authorizationService.assertCanAccess(user, room);

        Page<ChatMessage> page = messageRepository.findByRoomId(roomId, pageable);
        List<ChatMessageResponse> messages = new ArrayList<>(page.getContent().stream()
                .map(this::toMessageResponse)
                .toList());
        messages.sort(Comparator.comparing(ChatMessageResponse::createdAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return new ChatMessagePageResponse(messages, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public ChatMessageResponse saveMessage(String username, Integer roomId, String rawContent) {
        User user = findUser(username);
        ChatRoom room = findRoom(roomId);
        authorizationService.assertCanAccess(user, room);

        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Tin nhắn không được để trống.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Tin nhắn không được vượt quá " + MAX_CONTENT_LENGTH + " ký tự.");
        }

        ChatMessage message = messageRepository.save(new ChatMessage(
                room, user, authorizationService.primaryRole(user), content));
        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public ChatRoom findRoomForUser(String username, Integer roomId) {
        User user = findUser(username);
        ChatRoom room = findRoom(roomId);
        authorizationService.assertCanAccess(user, room);
        return room;
    }

    @Transactional
    public void ensureRooms() {
        if (roomRepository.findByRoomTypeAndSchoolId(ChatRoomType.GLOBAL_ADMIN, null).isEmpty()) {
            roomRepository.save(new ChatRoom(ChatRoomType.GLOBAL_ADMIN, null,
                    "Nhóm 1 - Quản trị hệ thống và hiệu trưởng"));
        }

        for (School school : schoolRepository.findAllByActiveTrueOrderByNameAsc()) {
            if (roomRepository.findByRoomTypeAndSchoolId(ChatRoomType.SCHOOL_STAFF, school.getId()).isEmpty()) {
                roomRepository.save(new ChatRoom(ChatRoomType.SCHOOL_STAFF, school,
                        "Nhóm 2 - Giáo viên " + school.getName()));
            }
        }
    }

    private User findUser(String username) {
        return userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
    }

    private ChatRoom findRoom(Integer roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm chat."));
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room) {
        School school = room.getSchool();
        return new ChatRoomResponse(room.getId(), room.getRoomType().name(), room.getName(),
                school == null ? null : school.getId(), school == null ? null : school.getName());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        User sender = message.getSender();
        return new ChatMessageResponse(message.getId(), message.getRoom().getId(), sender.getId(),
                sender.getFullName(), message.getSenderRole(), message.getContent(), message.getCreatedAt());
    }
}
