package com.meetmind.meetmind_backend.websocket;

import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            ParticipantRepository participantRepository,
            MeetingRepository meetingRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.meetingRepository = meetingRepository;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
                org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing WebSocket Authorization");
            }

            String token = authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {
                throw new IllegalArgumentException("Invalid JWT");
            }

            Long userId = jwtService.extractUserId(token);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new com.meetmind.meetmind_backend.auth.UserPrincipal(user),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            accessor.setUser(authentication);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (principal == null) {
                log.warn("Dropping SUBSCRIBE to {}: unauthenticated session", destination);
                return null;
            }

            if (destination != null) {
                // If destination targets meeting chat, ensure caller is a participant or host
                if (destination.startsWith("/topic/meetings/") && destination.endsWith("/chat")) {
                    String subPath = destination.substring("/topic/meetings/".length());
                    int slashIdx = subPath.indexOf('/');
                    String meetingIdStr = slashIdx == -1 ? subPath : subPath.substring(0, slashIdx);
                    try {
                        Long meetingId = Long.parseLong(meetingIdStr);
                        User user = extractUser(principal);
                        if (user != null) {
                            boolean isAuthorized = meetingRepository.findById(meetingId)
                                    .map(m -> (m.getHost() != null && m.getHost().getId().equals(user.getId()))
                                            || participantRepository.existsByMeetingIdAndUserId(meetingId, user.getId()))
                                    .orElse(false);
                            if (!isAuthorized) {
                                log.warn("Dropping unauthorized SUBSCRIBE to {} for user {}", destination, user.getId());
                                return null;
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private User extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof com.meetmind.meetmind_backend.auth.UserPrincipal up) {
                return up.getUser();
            }
            if (auth.getPrincipal() instanceof User u) {
                return u;
            }
        }
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }
}
