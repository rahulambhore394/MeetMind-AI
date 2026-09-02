package com.meetmind.meetmind_backend.chat;

import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ChatControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getMessages_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/meetings/100/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@meetmind.com", roles = "USER")
    void getMessages_Authenticated_ReturnsMessages() throws Exception {
        ChatMessageResponse res1 = new ChatMessageResponse();
        res1.setMessageId(1L);
        res1.setMeetingId(100L);
        res1.setSenderId(10L);
        res1.setSenderName("John Doe");
        res1.setMessage("Hello guys");
        res1.setMessageType("TEXT");
        res1.setSentAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 50);
        when(chatService.getMessages(eq(100L), any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(List.of(res1), pageable, 1));

        mockMvc.perform(get("/api/meetings/100/messages")
                        .param("page", "0")
                        .param("size", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].messageId").value(1L))
                .andExpect(jsonPath("$.content[0].senderName").value("John Doe"))
                .andExpect(jsonPath("$.content[0].message").value("Hello guys"));
    }
}
