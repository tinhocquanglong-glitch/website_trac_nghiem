package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatApiControllerTest {
    @Test
    void createsMessageThroughHttpFallbackWhenWebSocketIsUnavailable() throws Exception {
        ChatService chatService = mock(ChatService.class);
        ChatApiController controller = new ChatApiController(chatService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        Principal principal = () -> "teacher";
        when(chatService.saveMessage(eq("teacher"), eq(7), eq("hello")))
                .thenReturn(new ChatMessageResponse(11L, 7, "teacher", "Teacher", "ROLE_TEACHER",
                        "hello", LocalDateTime.of(2026, 10, 19, 10, 0)));

        mvc.perform(post("/api/chat/rooms/7/messages")
                        .principal(principal)
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.content").value("hello"));
    }
}
