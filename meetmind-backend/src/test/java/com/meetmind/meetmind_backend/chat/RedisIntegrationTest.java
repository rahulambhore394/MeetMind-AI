package com.meetmind.meetmind_backend.chat;

import com.meetmind.meetmind_backend.config.RateLimiterService;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingService;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.meeting.dto.MeetingResponse;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.websocket.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
public class RedisIntegrationTest {

    @Autowired
    private PresenceService presenceService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private MeetingService meetingService;

    @MockitoSpyBean
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private User host;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        // Clear DB
        meetingRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Clear mock Redis mappings
        stringRedisTemplate.delete("user:presence:1");
        stringRedisTemplate.delete("user:sessions:1");
        stringRedisTemplate.delete("meeting:presence:10");

        host = new User();
        host.setName("Host User");
        host.setEmail("host@redis.com");
        host.setPassword("password");
        host = userRepository.save(host);

        meeting = new Meeting();
        meeting.setTitle("Redis Test Meeting");
        meeting.setHost(host);
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        meeting = meetingRepository.save(meeting);
    }

    @Test
    void testUserPresence_OnlineOffline() {
        Long userId = host.getId();
        String sessionId = "session-abc";

        // 1. Initial State: offline
        assertFalse(presenceService.isUserOnline(userId));

        // 2. Mark Online
        presenceService.markUserOnline(userId, sessionId);
        assertTrue(presenceService.isUserOnline(userId));

        // 3. Mark Offline
        presenceService.markUserOffline(userId, sessionId);
        assertFalse(presenceService.isUserOnline(userId));
    }

    @Test
    void testUserPresence_MultipleSessions() {
        Long userId = host.getId();
        String session1 = "session-1";
        String session2 = "session-2";

        // Connect session 1
        presenceService.markUserOnline(userId, session1);
        assertTrue(presenceService.isUserOnline(userId));

        // Connect session 2
        presenceService.markUserOnline(userId, session2);
        assertTrue(presenceService.isUserOnline(userId));

        // Disconnect session 1 (should stay online due to session 2)
        presenceService.markUserOffline(userId, session1);
        assertTrue(presenceService.isUserOnline(userId));

        // Disconnect session 2 (should now become offline)
        presenceService.markUserOffline(userId, session2);
        assertFalse(presenceService.isUserOnline(userId));
    }

    @Test
    void testMeetingPresence() {
        Long userId = host.getId();
        Long meetingId = meeting.getId();
        String sessionId = "session-123";

        // Add to meeting
        presenceService.addUserToMeeting(userId, meetingId, sessionId);

        Set<Long> participants = presenceService.getOnlineMeetingParticipants(meetingId);
        assertTrue(participants.contains(userId));

        // Remove from meeting
        presenceService.removeUserFromMeeting(userId, meetingId, sessionId);
        participants = presenceService.getOnlineMeetingParticipants(meetingId);
        assertFalse(participants.contains(userId));
    }

    @Test
    void testMeetingCache_HitAndInvalidation() {
        Long meetingId = meeting.getId();

        // First call: Should fetch from Database
        MeetingResponse response1 = meetingService.getMeeting(meetingId, host.getId());
        assertNotNull(response1);
        verify(meetingRepository, times(1)).findById(meetingId);

        // Second call: Should fetch from Cache (no database call)
        MeetingResponse response2 = meetingService.getMeeting(meetingId, host.getId());
        assertNotNull(response2);
        verify(meetingRepository, times(1)).findById(meetingId); // Call count remains 1

        // Invalidation: Start the meeting
        meetingService.startMeeting(meetingId, host.getId());

        // Third call: Cache evicted, should hit database again
        MeetingResponse response3 = meetingService.getMeeting(meetingId, host.getId());
        assertNotNull(response3);
        verify(meetingRepository, times(3)).findById(meetingId); // Call count increased to 3 (first + startMeeting + third)
    }

    @Test
    void testRateLimiter_AllowedAndExceeded() {
        String key = "test-user";
        String op = "chat";
        int limit = 3;
        int window = 5;

        // Clear ZSet rate limiting keys
        stringRedisTemplate.delete("ratelimit:" + key + ":" + op);

        // 1st request - allowed
        assertTrue(rateLimiterService.isAllowed(key, op, limit, window));
        // 2nd request - allowed
        assertTrue(rateLimiterService.isAllowed(key, op, limit, window));
        // 3rd request - allowed
        assertTrue(rateLimiterService.isAllowed(key, op, limit, window));

        // 4th request - blocked (limit exceeded)
        assertFalse(rateLimiterService.isAllowed(key, op, limit, window));
    }

    @Test
    void testRedisUnavailable_FailsOpen() {
        // Mock a StringRedisTemplate that throws exception
        StringRedisTemplate badTemplate = mock(StringRedisTemplate.class);
        when(badTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis connection error"));

        RateLimiterService badLimiter = new RateLimiterService(badTemplate);
        
        // Should fail-open and return true (allow request) on Redis failures
        assertTrue(badLimiter.isAllowed("user-1", "chat", 5, 10));
    }
}
