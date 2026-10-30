package com.meetmind.meetmind_backend.participant;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.dto.ParticipantResponse;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.websocket.MeetingEventPublisher;
import com.meetmind.meetmind_backend.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingEventPublisher eventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ParticipantService participantService;

    private User host;
    private User userB;
    private Meeting liveMeeting;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId(1L);
        host.setName("Host User");
        host.setEmail("host@example.com");

        userB = new User();
        userB.setId(2L);
        userB.setName("User B");
        userB.setEmail("userb@example.com");

        liveMeeting = new Meeting();
        liveMeeting.setId(100L);
        liveMeeting.setTitle("Test Multi-User Meeting");
        liveMeeting.setHost(host);
        liveMeeting.setStatus(MeetingStatus.LIVE);
        liveMeeting.setMeetingCode("mm-100-test");
    }

    @Test
    void testJoinMeeting_AutoCreatesParticipantAndPublishesEvent() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(liveMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(userB));
        when(participantRepository.save(any(MeetingParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantResponse response = participantService.joinMeeting(100L, 2L);

        assertNotNull(response);
        assertEquals("JOINED", response.getStatus());
        assertEquals("User B", response.getName());

        verify(eventPublisher, times(1)).publish(eq(100L), any());
        verify(applicationEventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void testGetParticipants_ReturnsAllParticipants() {
        MeetingParticipant partHost = new MeetingParticipant();
        partHost.setMeeting(liveMeeting);
        partHost.setUser(host);
        partHost.setRole(ParticipantRole.HOST);
        partHost.setStatus(ParticipantStatus.JOINED);

        MeetingParticipant partB = new MeetingParticipant();
        partB.setMeeting(liveMeeting);
        partB.setUser(userB);
        partB.setRole(ParticipantRole.PARTICIPANT);
        partB.setStatus(ParticipantStatus.JOINED);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(liveMeeting));
        when(participantRepository.findByMeetingId(100L)).thenReturn(List.of(partHost, partB));

        List<ParticipantResponse> participants = participantService.getParticipants(100L, 1L);

        assertEquals(2, participants.size());
        assertEquals("Host User", participants.get(0).getName());
        assertEquals("User B", participants.get(1).getName());
    }

    @Test
    void testInviteParticipant_UnregisteredUser_DispatchesEmailAndReturnsValidResponse() {
        String inviteeEmail = "rahulambhore@gmail.com";
        com.meetmind.meetmind_backend.participant.dto.InviteParticipantRequest req = new com.meetmind.meetmind_backend.participant.dto.InviteParticipantRequest();
        req.setEmail(inviteeEmail);

        // Stub host as a participant or host so verifyParticipantOrHost passes (host.id = 1L == currentUserId 1L)
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(liveMeeting));
        when(userRepository.findByEmail(inviteeEmail)).thenReturn(Optional.empty());

        // Unregistered user -> service calls userRepository.save(). Return a saved user with an ID.
        User savedInvitee = new User();
        savedInvitee.setId(99L);
        savedInvitee.setName("rahulambhore");
        savedInvitee.setEmail(inviteeEmail);
        savedInvitee.setPassword("INVITED_PLACEHOLDER_xxx");
        when(userRepository.save(any(User.class))).thenReturn(savedInvitee);

        // Service then checks if participant already exists, then saves a new participant
        when(participantRepository.findByMeetingIdAndUserId(100L, 99L)).thenReturn(Optional.empty());

        // Use Mockito mock because MeetingParticipant has no setId() (JPA-managed ID)
        MeetingParticipant savedParticipant = mock(MeetingParticipant.class);
        when(savedParticipant.getId()).thenReturn(50L);
        when(savedParticipant.getUser()).thenReturn(savedInvitee);
        when(savedParticipant.getRole()).thenReturn(ParticipantRole.PARTICIPANT);
        when(savedParticipant.getStatus()).thenReturn(ParticipantStatus.INVITED);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(savedParticipant);

        ParticipantResponse response = participantService.inviteParticipant(100L, req, 1L);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(99L, response.getUserId());
        assertEquals("rahulambhore", response.getName());
        assertEquals(inviteeEmail, response.getEmail());
        assertEquals("INVITED", response.getStatus());

        verify(emailService, times(1)).sendMeetingInvitationEmail(
                eq(inviteeEmail),
                eq("Host User"),
                eq("Test Multi-User Meeting"),
                any(),
                eq("mm-100-test"),
                any(),
                eq(false)
        );
    }

    @Test
    void testParticipantResponseSerialization_ValidJacksonJson() throws Exception {
        ParticipantResponse response = new ParticipantResponse("rahulambhore@gmail.com", "PARTICIPANT", "INVITED");

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String jsonStr = mapper.writeValueAsString(response);

        assertNotNull(jsonStr);
        assertTrue(jsonStr.contains("\"name\":\"rahulambhore@gmail.com\""));
        assertTrue(jsonStr.contains("\"email\":\"rahulambhore@gmail.com\""));
        assertTrue(jsonStr.contains("\"id\":null"));

        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(jsonStr);
        assertEquals("rahulambhore@gmail.com", node.get("name").asText());
        assertEquals("rahulambhore@gmail.com", node.get("email").asText());
        assertTrue(node.get("id").isNull());
    }
}
