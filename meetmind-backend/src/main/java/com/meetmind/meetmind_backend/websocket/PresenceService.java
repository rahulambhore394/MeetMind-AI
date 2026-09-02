package com.meetmind.meetmind_backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markUserOnline(Long userId, String sessionId) {
        try {
            String sessionsKey = "user:sessions:" + userId;
            String presenceKey = "user:presence:" + userId;

            redisTemplate.opsForSet().add(sessionsKey, sessionId);
            redisTemplate.opsForValue().set(presenceKey, "online", 24, TimeUnit.HOURS);
            redisTemplate.expire(sessionsKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to mark user {} online in Redis: {}", userId, e.getMessage());
        }
    }

    public void markUserOffline(Long userId, String sessionId) {
        try {
            String sessionsKey = "user:sessions:" + userId;
            String presenceKey = "user:presence:" + userId;

            redisTemplate.opsForSet().remove(sessionsKey, sessionId);

            Long size = redisTemplate.opsForSet().size(sessionsKey);
            if (size == null || size == 0) {
                redisTemplate.delete(presenceKey);
                redisTemplate.delete(sessionsKey);
            }
        } catch (Exception e) {
            log.warn("Failed to mark user {} offline in Redis: {}", userId, e.getMessage());
        }
    }

    public void addUserToMeeting(Long userId, Long meetingId, String sessionId) {
        try {
            String meetingKey = "meeting:presence:" + meetingId;
            String userMeetingSessionsKey = "user:meeting-sessions:" + userId + ":" + meetingId;
            String sessionMeetingKey = "session:meeting:" + sessionId;

            redisTemplate.opsForSet().add(meetingKey, String.valueOf(userId));
            redisTemplate.opsForSet().add(userMeetingSessionsKey, sessionId);
            redisTemplate.opsForValue().set(sessionMeetingKey, String.valueOf(meetingId), 24, TimeUnit.HOURS);

            redisTemplate.expire(meetingKey, 24, TimeUnit.HOURS);
            redisTemplate.expire(userMeetingSessionsKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to add user {} to meeting {} presence in Redis: {}", userId, meetingId, e.getMessage());
        }
    }

    public void removeUserFromMeeting(Long userId, Long meetingId, String sessionId) {
        try {
            String meetingKey = "meeting:presence:" + meetingId;
            String userMeetingSessionsKey = "user:meeting-sessions:" + userId + ":" + meetingId;
            String sessionMeetingKey = "session:meeting:" + sessionId;

            redisTemplate.opsForSet().remove(userMeetingSessionsKey, sessionId);
            redisTemplate.delete(sessionMeetingKey);

            Long size = redisTemplate.opsForSet().size(userMeetingSessionsKey);
            if (size == null || size == 0) {
                redisTemplate.opsForSet().remove(meetingKey, String.valueOf(userId));
                redisTemplate.delete(userMeetingSessionsKey);
            }
        } catch (Exception e) {
            log.warn("Failed to remove user {} from meeting {} presence in Redis: {}", userId, meetingId, e.getMessage());
        }
    }

    public Set<Long> getOnlineMeetingParticipants(Long meetingId) {
        try {
            String meetingKey = "meeting:presence:" + meetingId;
            Set<String> members = redisTemplate.opsForSet().members(meetingKey);
            if (members == null) {
                return Collections.emptySet();
            }
            return members.stream().map(Long::parseLong).collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to get online participants for meeting {} from Redis: {}", meetingId, e.getMessage());
            return Collections.emptySet();
        }
    }

    public boolean isUserOnline(Long userId) {
        try {
            String presenceKey = "user:presence:" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey));
        } catch (Exception e) {
            log.warn("Failed to check user {} online status in Redis: {}", userId, e.getMessage());
            return false;
        }
    }
}
