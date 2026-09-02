package com.meetmind.meetmind_backend.chat;

import com.meetmind.meetmind_backend.chat.dto.ChatMessageRequest;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ChatService chatService;

    private User host;
    private User participantUser;
    private User nonParticipantUser;
    private Meeting meeting;
    private UsernamePasswordAuthenticationToken hostPrincipal;
    private UsernamePasswordAuthenticationToken participantPrincipal;
    private UsernamePasswordAuthenticationToken nonParticipantPrincipal;

    @BeforeEach
    void setUp() throws Exception {
        host = new User();
        host.setName("Host User");
        host.setEmail("host@meetmind.com");
        setId(host, 1L);

        participantUser = new User();
        participantUser.setName("Participant User");
        participantUser.setEmail("participant@meetmind.com");
        setId(participantUser, 2L);

        nonParticipantUser = new User();
        nonParticipantUser.setName("Non-Participant User");
        nonParticipantUser.setEmail("stranger@meetmind.com");
        setId(nonParticipantUser, 3L);

        meeting = new Meeting();
        meeting.setTitle("Test Meeting");
        meeting.setHost(host);
        setId(meeting, 100L);

        hostPrincipal = new UsernamePasswordAuthenticationToken(host, null);
        participantPrincipal = new UsernamePasswordAuthenticationToken(participantUser, null);
        nonParticipantPrincipal = new UsernamePasswordAuthenticationToken(nonParticipantUser, null);
    }

    private void setId(Object obj, Long id) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(obj, id);
    }

    // ==========================================
    // SEND MESSAGE TESTS
    // ==========================================

    @Test
    void sendMessage_Success_AsHost() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(100L);
        request.setMessage("Hello team!");

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        
        ChatMessage savedMsg = new ChatMessage();
        savedMsg.setMeetingId(100L);
        savedMsg.setSenderId(1L);
        savedMsg.setMessage("Hello team!");
        savedMsg.setMessageType("TEXT");
        try {
            setId(savedMsg, 500L);
        } catch (Exception e) {}

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        ChatMessageResponse response = chatService.sendMessage(request, hostPrincipal);

        assertNotNull(response);
        assertEquals(500L, response.getMessageId());
        assertEquals("Hello team!", response.getMessage());
        assertEquals("Host User", response.getSenderName());
        assertEquals(1L, response.getSenderId());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_Success_AsParticipant() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(100L);
        request.setMessage("Hey all!");

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(true);

        ChatMessage savedMsg = new ChatMessage();
        savedMsg.setMeetingId(100L);
        savedMsg.setSenderId(2L);
        savedMsg.setMessage("Hey all!");
        savedMsg.setMessageType("TEXT");
        try {
            setId(savedMsg, 501L);
        } catch (Exception e) {}

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        ChatMessageResponse response = chatService.sendMessage(request, participantPrincipal);

        assertNotNull(response);
        assertEquals(501L, response.getMessageId());
        assertEquals("Hey all!", response.getMessage());
        assertEquals("Participant User", response.getSenderName());
        assertEquals(2L, response.getSenderId());
    }

    @Test
    void sendMessage_Forbidden_AsNonParticipant() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(100L);
        request.setMessage("Spamming!");

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(100L, 3L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatService.sendMessage(request, nonParticipantPrincipal);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("is not a participant"));
    }

    @Test
    void sendMessage_NotFound_MeetingDoesNotExist() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(999L);
        request.setMessage("Hello");

        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatService.sendMessage(request, hostPrincipal);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ==========================================
    // GET MESSAGES TESTS
    // ==========================================

    @Test
    void getMessages_Success_AsParticipant() {
        Pageable pageable = PageRequest.of(0, 10);
        
        ChatMessage msg1 = new ChatMessage();
        msg1.setMeetingId(100L);
        msg1.setSenderId(1L);
        msg1.setMessage("Welcome");
        msg1.setMessageType("TEXT");
        
        ChatMessage msg2 = new ChatMessage();
        msg2.setMeetingId(100L);
        msg2.setSenderId(2L);
        msg2.setMessage("Thanks");
        msg2.setMessageType("TEXT");

        Page<ChatMessage> chatMessagesPage = new PageImpl<>(List.of(msg1, msg2), pageable, 2);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(true);
        when(chatMessageRepository.findByMeetingIdAndDeletedFalse(100L, pageable)).thenReturn(chatMessagesPage);
        when(userRepository.findAllById(any())).thenReturn(List.of(host, participantUser));

        Page<ChatMessageResponse> result = chatService.getMessages(100L, pageable, participantPrincipal);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        
        List<ChatMessageResponse> content = result.getContent();
        assertEquals("Welcome", content.get(0).getMessage());
        assertEquals("Host User", content.get(0).getSenderName());
        assertEquals("Thanks", content.get(1).getMessage());
        assertEquals("Participant User", content.get(1).getSenderName());
    }

    @Test
    void getMessages_Forbidden_AsNonParticipant() {
        Pageable pageable = PageRequest.of(0, 10);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(100L, 3L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            chatService.getMessages(100L, pageable, nonParticipantPrincipal);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
