package com.meetmind.meetmind_backend.webrtc;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SignalingService {
    private static final Logger log = LoggerFactory.getLogger(SignalingService.class);

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.Set<Long>> memoryPresence = new java.util.concurrent.ConcurrentHashMap<>();

    public SignalingService(
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository,
            UserRepository userRepository,
            StringRedisTemplate redisTemplate
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    public void validateParticipant(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        if (meeting.getStatus() != MeetingStatus.LIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting is not live");
        }

        boolean isAuthorized = meeting.getHost().getId().equals(userId) ||
                participantRepository.existsByMeetingIdAndUserId(meetingId, userId);

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this meeting");
        }
    }

    public void registerPresence(Long meetingId, Long userId, String sessionId) {
        memoryPresence.computeIfAbsent(meetingId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(userId);
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            String sessionKey = "session:signaling:meeting:" + sessionId;

            redisTemplate.opsForSet().add(presenceKey, String.valueOf(userId));
            redisTemplate.opsForValue().set(sessionKey, String.valueOf(meetingId), 24, TimeUnit.HOURS);
            redisTemplate.expire(presenceKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis error on registerPresence for user {} in meeting {}: {}", userId, meetingId, e.getMessage());
        }
    }

    public void evictPresence(Long meetingId, Long userId, String sessionId) {
        Set<Long> localSet = memoryPresence.get(meetingId);
        if (localSet != null) {
            localSet.remove(userId);
            if (localSet.isEmpty()) {
                memoryPresence.remove(meetingId);
            }
        }
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            String sessionKey = "session:signaling:meeting:" + sessionId;

            redisTemplate.opsForSet().remove(presenceKey, String.valueOf(userId));
            redisTemplate.delete(sessionKey);
        } catch (Exception e) {
            log.warn("Redis error on evictPresence for user {} in meeting {}: {}", userId, meetingId, e.getMessage());
        }
    }

    public Set<Long> getActivePeers(Long meetingId, Long excludeUserId) {
        Set<Long> result = new java.util.HashSet<>();
        
        // 1. Check in-memory presence
        Set<Long> memoryPeers = memoryPresence.get(meetingId);
        if (memoryPeers != null) {
            result.addAll(memoryPeers);
        }

        // 2. Check Redis presence if available
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            Set<String> members = redisTemplate.opsForSet().members(presenceKey);
            if (members != null) {
                for (String m : members) {
                    try {
                        result.add(Long.parseLong(m));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Redis error on getActivePeers for meeting {}: {}", meetingId, e.getMessage());
        }

        // 3. Fallback to DB joined participants
        try {
            List<com.meetmind.meetmind_backend.participant.MeetingParticipant> joined = 
                    participantRepository.findByMeetingId(meetingId);
            for (com.meetmind.meetmind_backend.participant.MeetingParticipant p : joined) {
                if (p.getStatus() == com.meetmind.meetmind_backend.participant.ParticipantStatus.JOINED && p.getUser() != null) {
                    result.add(p.getUser().getId());
                }
            }
        } catch (Exception e) {
            log.warn("DB fallback error on getActivePeers for meeting {}: {}", meetingId, e.getMessage());
        }

        result.remove(excludeUserId);
        return result;
    }

    public Map<Long, String> getPeerNames(Set<Long> peerIds) {
        if (peerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(peerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
