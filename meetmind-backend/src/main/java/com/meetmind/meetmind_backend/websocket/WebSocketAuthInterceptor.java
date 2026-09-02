package com.meetmind.meetmind_backend.websocket;


import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;

import com.meetmind.meetmind_backend.participant.ParticipantRepository;
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

public class WebSocketAuthInterceptor
        implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            ParticipantRepository participantRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
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

        if (StompCommand.CONNECT.equals(
                accessor.getCommand()
        )) {

            String authHeader =
                    accessor.getFirstNativeHeader(
                            "Authorization"
                    );

            if (authHeader == null ||
                    !authHeader.startsWith("Bearer ")) {

                throw new IllegalArgumentException(
                        "Missing WebSocket Authorization"
                );
            }

            String token =
                    authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {

                throw new IllegalArgumentException(
                        "Invalid JWT"
                );
            }

            Long userId =
                    jwtService.extractUserId(token);

            User user =
                    userRepository
                            .findById(userId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found"
                                    )
                            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_USER"
                                    )
                            )
                    );

            accessor.setUser(authentication);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null) {
                Long meetingId = null;
                if (destination.startsWith("/topic/meetings/")) {
                    String subPath = destination.substring("/topic/meetings/".length());
                    int slashIdx = subPath.indexOf('/');
                    String meetingIdStr = slashIdx == -1 ? subPath : subPath.substring(0, slashIdx);
                    try {
                        meetingId = Long.parseLong(meetingIdStr);
                    } catch (NumberFormatException ignored) {}
                } else if (destination.startsWith("/user/queue/meetings/")) {
                    String subPath = destination.substring("/user/queue/meetings/".length());
                    int slashIdx = subPath.indexOf('/');
                    String meetingIdStr = slashIdx == -1 ? subPath : subPath.substring(0, slashIdx);
                    try {
                        meetingId = Long.parseLong(meetingIdStr);
                    } catch (NumberFormatException ignored) {}
                }

                if (meetingId != null) {
                    Principal principal = accessor.getUser();
                    if (principal instanceof UsernamePasswordAuthenticationToken auth) {
                        User user = (User) auth.getPrincipal();
                        boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(meetingId, user.getId());
                        if (!isParticipant) {
                            return null; // Silent drop of unauthorized subscription
                        }
                    } else {
                        return null; // Silent drop if not authenticated
                    }
                }
            }
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}