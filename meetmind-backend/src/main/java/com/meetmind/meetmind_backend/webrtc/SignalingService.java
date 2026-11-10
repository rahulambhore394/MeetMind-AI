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

    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.ConcurrentHashMap<Long, java.util.Set<String>>> memoryPresence =
            new java.util.concurrent.ConcurrentHashMap<>();

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

        boolean isHost = meeting.getHost().getId().equals(userId);
        java.util.Optional<com.meetmind.meetmind_backend.participant.MeetingParticipant> existingPart =
                participantRepository.findByMeetingIdAndUserId(meetingId, userId);

        if (existingPart.isPresent() && existingPart.get().getStatus() == com.meetmind.meetmind_backend.participant.ParticipantStatus.REMOVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have been removed from this meeting by the host");
        }

        if (meeting.isLocked() && !isHost) {
            if (existingPart.isEmpty() ||
                (existingPart.get().getRole() != com.meetmind.meetmind_backend.participant.ParticipantRole.HOST &&
                 existingPart.get().getRole() != com.meetmind.meetmind_backend.participant.ParticipantRole.CO_HOST &&
                 existingPart.get().getStatus() != com.meetmind.meetmind_backend.participant.ParticipantStatus.JOINED)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This meeting has been locked by the host");
            }
        }

        boolean isAuthorized = isHost || existingPart.isPresent();

        if (!isAuthorized) {
            userRepository.findById(userId).ifPresent(user -> {
                com.meetmind.meetmind_backend.participant.MeetingParticipant newPart =
                        new com.meetmind.meetmind_backend.participant.MeetingParticipant();
                newPart.setMeeting(meeting);
                newPart.setUser(user);
                newPart.setRole(com.meetmind.meetmind_backend.participant.ParticipantRole.PARTICIPANT);
                newPart.setStatus(com.meetmind.meetmind_backend.participant.ParticipantStatus.JOINED);
                newPart.setJoinedAt(java.time.LocalDateTime.now());
                participantRepository.save(newPart);
            });
        }
    }

    public boolean isHostOrCoHost(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return false;
        if (meeting.getHost() != null && meeting.getHost().getId().equals(userId)) {
            return true;
        }
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(p -> p.getRole() == com.meetmind.meetmind_backend.participant.ParticipantRole.HOST ||
                          p.getRole() == com.meetmind.meetmind_backend.participant.ParticipantRole.CO_HOST)
                .orElse(false);
    }

    public void evictUserPresence(Long meetingId, Long userId) {
        Map<Long, Set<String>> localMeetingPresence = memoryPresence.get(meetingId);
        if (localMeetingPresence != null) {
            localMeetingPresence.remove(userId);
        }
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            Set<String> members = redisTemplate.opsForSet().members(presenceKey);
            if (members != null) {
                for (String member : members) {
                    if (member.startsWith(userId + ":")) {
                        redisTemplate.opsForSet().remove(presenceKey, member);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Redis error on evictUserPresence for user {} in meeting {}: {}", userId, meetingId, e.getMessage());
        }
    }

    public void registerPresence(Long meetingId, Long userId, String sessionId) {
        memoryPresence
                .computeIfAbsent(meetingId, k -> new java.util.concurrent.ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet())
                .add(sessionId);
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            String sessionKey = "session:signaling:meeting:" + sessionId;

            redisTemplate.opsForSet().add(presenceKey, redisPresenceMember(userId, sessionId));
            redisTemplate.opsForValue().set(sessionKey, String.valueOf(meetingId), 24, TimeUnit.HOURS);
            redisTemplate.expire(presenceKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis error on registerPresence for user {} in meeting {}: {}", userId, meetingId, e.getMessage());
        }
    }

    public boolean evictPresence(Long meetingId, Long userId, String sessionId) {
        Map<Long, Set<String>> localMeetingPresence = memoryPresence.get(meetingId);
        boolean userStillActiveInMemory = false;
        if (localMeetingPresence != null) {
            Set<String> sessions = localMeetingPresence.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    localMeetingPresence.remove(userId);
                } else {
                    userStillActiveInMemory = true;
                }
            }
            if (localMeetingPresence.isEmpty()) {
                memoryPresence.remove(meetingId);
            }
        }
        boolean userStillActiveInRedis = false;
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            String sessionKey = "session:signaling:meeting:" + sessionId;

            redisTemplate.opsForSet().remove(presenceKey, redisPresenceMember(userId, sessionId));
            redisTemplate.delete(sessionKey);
            Set<String> members = redisTemplate.opsForSet().members(presenceKey);
            String userPrefix = userId + ":";
            if (members != null) {
                userStillActiveInRedis = members.stream().anyMatch(member -> member.startsWith(userPrefix));
            }
        } catch (Exception e) {
            log.warn("Redis error on evictPresence for user {} in meeting {}: {}", userId, meetingId, e.getMessage());
        }
        return !(userStillActiveInMemory || userStillActiveInRedis);
    }

    public Set<Long> getActivePeers(Long meetingId, Long excludeUserId) {
        Set<Long> result = new java.util.HashSet<>();
        
        // Only active signaling sessions count as peers. Database JOINED status is
        // historical and must not be used to create WebRTC connections.
        Map<Long, Set<String>> localMeetingPresence = memoryPresence.get(meetingId);
        if (localMeetingPresence != null) {
            result.addAll(localMeetingPresence.keySet());
        }

        // 2. Check Redis presence if available
        try {
            String presenceKey = "meeting:signaling:presence:" + meetingId;
            Set<String> members = redisTemplate.opsForSet().members(presenceKey);
            if (members != null) {
                for (String m : members) {
                    try {
                        int separator = m.indexOf(':');
                        String userId = separator >= 0 ? m.substring(0, separator) : m;
                        result.add(Long.parseLong(userId));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Redis error on getActivePeers for meeting {}: {}", meetingId, e.getMessage());
        }

        result.remove(excludeUserId);
        return result;
    }

    private String redisPresenceMember(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    public Map<Long, String> getPeerNames(Set<Long> peerIds) {
        if (peerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(peerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
