package com.meetmind.meetmind_backend.websocket;

import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;
    private final StringRedisTemplate redisTemplate;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.meetmind.meetmind_backend.user.UserRepository userRepository;

    public WebSocketEventListener(
            PresenceService presenceService,
            StringRedisTemplate redisTemplate,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            com.meetmind.meetmind_backend.user.UserRepository userRepository
    ) {
        this.presenceService = presenceService;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        Long userId = getUserId(principal);

        if (userId != null && sessionId != null) {
            log.info("WebSocket Session Connected: user={}, sessionId={}", userId, sessionId);
            presenceService.markUserOnline(userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        Long userId = getUserId(principal);

        if (userId != null && sessionId != null) {
            log.info("WebSocket Session Disconnected: user={}, sessionId={}", userId, sessionId);
            
            // Clean up meeting presence for this session
            try {
                String sessionMeetingKey = "session:meeting:" + sessionId;
                String meetingIdStr = redisTemplate.opsForValue().get(sessionMeetingKey);
                if (meetingIdStr != null) {
                    Long meetingId = Long.parseLong(meetingIdStr);
                    presenceService.removeUserFromMeeting(userId, meetingId, sessionId);
                }
            } catch (Exception e) {
                log.warn("Failed to clean up meeting presence on disconnect for session {}: {}", sessionId, e.getMessage());
            }

            // Clean up WebRTC signaling presence for this session
            try {
                String sessionSignalingKey = "session:signaling:meeting:" + sessionId;
                String meetingIdStr = redisTemplate.opsForValue().get(sessionSignalingKey);
                if (meetingIdStr != null) {
                    Long meetingId = Long.parseLong(meetingIdStr);
                    
                    // Remove signaling state from Redis
                    String presenceKey = "meeting:signaling:presence:" + meetingId;
                    redisTemplate.opsForSet().remove(presenceKey, String.valueOf(userId));
                    redisTemplate.delete(sessionSignalingKey);
                    
                    // Resolve user details for name
                    String userName = "Participant";
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        userName = user.getName();
                    }

                    // Broadcast LEAVE message to signaling topic
                    com.meetmind.meetmind_backend.webrtc.dto.SignalingMessage leaveMsg = 
                        new com.meetmind.meetmind_backend.webrtc.dto.SignalingMessage(
                            meetingId,
                            userId,
                            userName,
                            null,
                            "LEAVE",
                            "Abrupt Disconnect"
                        );
                    messagingTemplate.convertAndSend("/topic/meetings/" + meetingId + "/signaling", leaveMsg);
                }
            } catch (Exception e) {
                log.warn("Failed to clean up WebRTC presence on disconnect for session {}: {}", sessionId, e.getMessage());
            }

            presenceService.markUserOffline(userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        String subscriptionId = accessor.getSubscriptionId();
        Long userId = getUserId(principal);

        if (userId != null && sessionId != null && destination != null) {
            if (destination.startsWith("/topic/meetings/") && destination.endsWith("/chat")) {
                String subPath = destination.substring("/topic/meetings/".length());
                int slashIdx = subPath.indexOf('/');
                String meetingIdStr = slashIdx == -1 ? subPath : subPath.substring(0, slashIdx);
                try {
                    Long meetingId = Long.parseLong(meetingIdStr);
                    log.info("WebSocket Subscription: user={} subscribed to meeting={}, sessionId={}", userId, meetingId, sessionId);
                    presenceService.addUserToMeeting(userId, meetingId, sessionId);

                    // Track subscription ID to meeting ID mapping in Redis for unsubscribe cleanup
                    if (subscriptionId != null) {
                        String subKey = "session:sub:" + sessionId + ":" + subscriptionId;
                        redisTemplate.opsForValue().set(subKey, String.valueOf(meetingId), 24, java.util.concurrent.TimeUnit.HOURS);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid meeting ID format in destination: {}", destination);
                }
            }
        }
    }

    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        Long userId = getUserId(principal);

        if (userId != null && sessionId != null && subscriptionId != null) {
            try {
                String subKey = "session:sub:" + sessionId + ":" + subscriptionId;
                String meetingIdStr = redisTemplate.opsForValue().get(subKey);
                if (meetingIdStr != null) {
                    Long meetingId = Long.parseLong(meetingIdStr);
                    log.info("WebSocket Unsubscription: user={} unsubscribed from meeting={}, sessionId={}", userId, meetingId, sessionId);
                    presenceService.removeUserFromMeeting(userId, meetingId, sessionId);
                    redisTemplate.delete(subKey);
                }
            } catch (Exception e) {
                log.warn("Failed to handle unsubscribe for session {}, sub {}: {}", sessionId, subscriptionId, e.getMessage());
            }
        }
    }

    private Long getUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof User user) {
                return user.getId();
            }
        }
        return null;
    }
}
