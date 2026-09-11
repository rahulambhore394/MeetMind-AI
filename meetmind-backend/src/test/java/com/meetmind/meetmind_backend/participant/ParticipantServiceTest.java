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
}
