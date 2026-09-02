package com.meetmind.meetmind_backend.event;

import com.meetmind.meetmind_backend.chat.ChatService;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageRequest;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingService;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.meeting.dto.MeetingResponse;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantService;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.embedded.kafka.kraft=false"
})
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "meeting-lifecycle-events",
                "participant-lifecycle-events",
                "chat-message-events",
                "recording-events",
                "transcription-events",
                "meeting-lifecycle-events.DLT",
                "participant-lifecycle-events.DLT",
                "chat-message-events.DLT"
        },
        brokerProperties = {
                "offsets.topic.num.partitions=1",
                "transaction.state.log.num.partitions=1",
                "transaction.state.log.min.isr=1"
        }
)
public class KafkaIntegrationTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private AuditLogConsumer auditLogConsumer;

    @Autowired
    private NotificationConsumer notificationConsumer;

    @Autowired
    private AnalyticsConsumer analyticsConsumer;

    @Autowired
    private DltConsumer dltConsumer;

    @Autowired
    private EventIdempotencyRegistry idempotencyRegistry;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User host;
    private User attendee;
    private Meeting meeting;
    private MeetingParticipant participant;

    @BeforeEach
    void setUp() {
        // Clear old messages and DLTs
        auditLogConsumer.clear();
        analyticsConsumer.reset();
        dltConsumer.clear();

        // Clear DB
        participantRepository.deleteAllInBatch();
        meetingRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Create host
        host = new User();
        host.setName("Host User");
        host.setEmail("host@meetmind.com");
        host.setPassword("password");
        host = userRepository.save(host);

        // Create attendee
        attendee = new User();
        attendee.setName("Attendee User");
        attendee.setEmail("attendee@meetmind.com");
        attendee.setPassword("password");
        attendee = userRepository.save(attendee);

        // Create meeting
        meeting = new Meeting();
        meeting.setTitle("Kafka Architecture Sync");
        meeting.setDescription("Syncing Kafka integration");
        meeting.setScheduledAt(LocalDateTime.now().plusHours(1));
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setHost(host);
        meeting = meetingRepository.save(meeting);

        // Add attendee invitation
        participant = new MeetingParticipant();
        participant.setMeeting(meeting);
        participant.setUser(attendee);
        participant.setRole(ParticipantRole.PARTICIPANT);
        participant.setStatus(ParticipantStatus.ACCEPTED); // Ready to join
        participant = participantRepository.save(participant);
    }

    @AfterEach
    void tearDown() {
        participantRepository.deleteAllInBatch();
        meetingRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void testMeetingLifecycleEvents() {
        // Start Meeting
        MeetingResponse started = meetingService.startMeeting(meeting.getId(), host.getId());
        assertEquals("LIVE", started.getStatus());

        // Await consumption of MEETING_STARTED
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, analyticsConsumer.getMeetingStartedCount());
            assertFalse(auditLogConsumer.getReceivedEvents().isEmpty());
        });

        MeetMindEvent startedEvent = auditLogConsumer.getReceivedEvents().stream()
                .filter(e -> "MEETING_STARTED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        assertEquals(meeting.getId(), startedEvent.getMeetingId());
        assertEquals(host.getId(), startedEvent.getUserId());
        assertEquals("Kafka Architecture Sync", startedEvent.getMetadata().get("title"));

        // End Meeting
        MeetingResponse ended = meetingService.endMeeting(meeting.getId(), host.getId());
        assertEquals("ENDED", ended.getStatus());

        // Await consumption of MEETING_ENDED
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, analyticsConsumer.getMeetingEndedCount());
        });
    }

    @Test
    void testParticipantLifecycleEvents() {
        // Start meeting first
        meetingService.startMeeting(meeting.getId(), host.getId());

        // Join
        participantService.joinMeeting(meeting.getId(), attendee.getId());

        // Await PARTICIPANT_JOINED
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, analyticsConsumer.getParticipantJoinedCount());
        });

        // Leave
        participantService.leaveMeeting(meeting.getId(), attendee.getId());

        // Await PARTICIPANT_LEFT
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, analyticsConsumer.getParticipantLeftCount());
        });
    }

    @Test
    void testChatMessageEvents() {
        // Start meeting and join participant
        meetingService.startMeeting(meeting.getId(), host.getId());
        participantService.joinMeeting(meeting.getId(), attendee.getId());

        Principal principal = new UsernamePasswordAuthenticationToken(attendee, null, java.util.Collections.emptyList());
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(meeting.getId());
        request.setMessage("Hello Kafka Sync!");

        ChatMessageResponse response = chatService.sendMessage(request, principal);
        assertNotNull(response.getMessageId());

        // Await CHAT_MESSAGE_SENT
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, analyticsConsumer.getChatMessageCount());
        });

        MeetMindEvent chatEvent = auditLogConsumer.getReceivedEvents().stream()
                .filter(e -> "CHAT_MESSAGE_SENT".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        assertEquals(meeting.getId(), chatEvent.getMeetingId());
        assertEquals(attendee.getId(), chatEvent.getUserId());
        assertEquals("Hello Kafka Sync!", chatEvent.getMetadata().get("message"));
        assertEquals("Attendee User", chatEvent.getMetadata().get("senderName"));
    }

    @Test
    void testConsumerGroupIdempotency() {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Idempotent Sync");

        MeetMindEvent event = new MeetMindEvent(
                eventId,
                "MEETING_STARTED",
                System.currentTimeMillis(),
                1,
                meeting.getId(),
                host.getId(),
                metadata
        );

        // Publish event manually twice
        kafkaTemplate.send("meeting-lifecycle-events", String.valueOf(meeting.getId()), event);
        kafkaTemplate.send("meeting-lifecycle-events", String.valueOf(meeting.getId()), event);

        // Await analytics & audit log checking
        // Despite being published twice, the event must only be processed ONCE by each group due to Redis idempotency registry.
        await().pollDelay(2, TimeUnit.SECONDS).atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // Count must be exactly 1, not 2
            assertEquals(1, analyticsConsumer.getMeetingStartedCount());
            assertEquals(1, auditLogConsumer.getReceivedEvents().size());
        });
    }

    @Test
    void testConsumerFailureRetryAndDltRouting() {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "DLT Sync");
        // Simulated failure flag triggers RuntimeException in AuditLogConsumer
        metadata.put("fail", true);

        MeetMindEvent event = new MeetMindEvent(
                eventId,
                "MEETING_STARTED",
                System.currentTimeMillis(),
                1,
                meeting.getId(),
                host.getId(),
                metadata
        );

        // Publish event
        kafkaTemplate.send("meeting-lifecycle-events", String.valueOf(meeting.getId()), event);

        // Await retry exhaustion and arrival in DLT topic
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertFalse(dltConsumer.getDltEvents().isEmpty());
        });

        MeetMindEvent dltEvent = dltConsumer.getDltEvents().get(0);
        assertEquals(eventId, dltEvent.getEventId());
        assertEquals("MEETING_STARTED", dltEvent.getEventType());
    }

    @Test
    void testTransactionalRollbackIsolation() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        try {
            txTemplate.execute(status -> {
                // Perform a business service action (e.g. start meeting)
                meetingService.startMeeting(meeting.getId(), host.getId());

                // Throw an exception to force rollback
                throw new RuntimeException("Force rollback test");
            });
        } catch (Exception e) {
            assertEquals("Force rollback test", e.getMessage());
        }

        // Await some time to ensure event was NOT published since transaction rolled back
        await().pollDelay(2, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(0, analyticsConsumer.getMeetingStartedCount());
            assertTrue(auditLogConsumer.getReceivedEvents().isEmpty());
        });
    }
}
