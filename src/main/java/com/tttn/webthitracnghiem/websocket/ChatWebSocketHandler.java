package com.tttn.webthitracnghiem.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tttn.webthitracnghiem.api.ChatMessageResponse;
import com.tttn.webthitracnghiem.service.ChatService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final ConcurrentHashMap<Integer, CopyOnWriteArraySet<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatService chatService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (username(session) == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("chatRooms", ConcurrentHashMap.newKeySet());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String username = username(session);
        if (username == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");
            Integer roomId = payload.hasNonNull("roomId") ? payload.get("roomId").asInt() : null;

            if ("subscribe".equals(type)) {
                subscribe(session, username, roomId);
            } else if ("message".equals(type)) {
                sendMessage(session, username, roomId, payload.path("content").asText(null));
            } else {
                sendError(session, "Loại dữ liệu WebSocket không hợp lệ.");
            }
        } catch (AccessDeniedException ex) {
            sendError(session, "Bạn không có quyền truy cập nhóm chat này.");
        } catch (IllegalArgumentException ex) {
            sendError(session, ex.getMessage());
        } catch (Exception ex) {
            sendError(session, "Không thể xử lý dữ liệu chat.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        for (Integer roomId : subscribedRooms(session)) {
            CopyOnWriteArraySet<WebSocketSession> roomSubscribers = subscribers.get(roomId);
            if (roomSubscribers != null) {
                roomSubscribers.remove(session);
                if (roomSubscribers.isEmpty()) {
                    subscribers.remove(roomId, roomSubscribers);
                }
            }
        }
    }

    private void subscribe(WebSocketSession session, String username, Integer roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("Thiếu mã nhóm chat.");
        }
        chatService.findRoomForUser(username, roomId);
        subscribedRooms(session).add(roomId);
        subscribers.computeIfAbsent(roomId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        send(session, objectMapper.createObjectNode().put("type", "subscribed").put("roomId", roomId));
    }

    private void sendMessage(WebSocketSession session, String username, Integer roomId, String content) throws IOException {
        if (roomId == null || !subscribedRooms(session).contains(roomId)) {
            throw new AccessDeniedException("Bạn chưa đăng ký nhận tin từ nhóm này.");
        }
        ChatMessageResponse response = chatService.saveMessage(username, roomId, content);
        String json = objectMapper.writeValueAsString(new WebSocketMessage("message", roomId, response));
        for (WebSocketSession recipient : subscribers.getOrDefault(roomId, new CopyOnWriteArraySet<>())) {
            send(recipient, json);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            send(session, objectMapper.createObjectNode().put("type", "error").put("message", message));
        } catch (Exception ignored) {
            // Client đã đóng kết nối thì không cần phát sinh thêm lỗi.
        }
    }

    private void send(WebSocketSession session, JsonNode payload) {
        send(session, payload.toString());
    }

    private void send(WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException ignored) {
            // Phiên bị đóng giữa lúc broadcast sẽ được dọn ở afterConnectionClosed.
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Integer> subscribedRooms(WebSocketSession session) {
        Object rooms = session.getAttributes().get("chatRooms");
        return rooms instanceof Set ? (Set<Integer>) rooms : Collections.emptySet();
    }

    private String username(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        return principal == null ? null : principal.getName();
    }

    private record WebSocketMessage(String type, Integer roomId, ChatMessageResponse message) {
    }
}
