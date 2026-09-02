package com.meetmind.meetmind_backend.chat;

import com.meetmind.meetmind_backend.chat.dto.ChatMessageRequest;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import com.meetmind.meetmind_backend.config.RateLimiterService;
import com.meetmind.meetmind_backend.user.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimiterService rateLimiterService;
    private final int chatLimit;
    private final int chatWindowSeconds;

    public ChatWebSocketController(
            ChatService chatService,
            SimpMessagingTemplate messagingTemplate,
            RateLimiterService rateLimiterService,
            @Value("${ratelimit.chat.limit:10}") int chatLimit,
            @Value("${ratelimit.chat.window-seconds:10}") int chatWindowSeconds
    ) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.rateLimiterService = rateLimiterService;
        this.chatLimit = chatLimit;
        this.chatWindowSeconds = chatWindowSeconds;
    }


    @MessageMapping("/chat.send")
    public void sendMessage(
            ChatMessageRequest request,
            Principal principal
    ) {
        Long userId = getUserId(principal);
        if (userId != null) {
            boolean allowed = rateLimiterService.isAllowed("user:" + userId, "chat", chatLimit, chatWindowSeconds);
            if (!allowed) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Chat rate limit exceeded. Please wait a moment."
                );
            }
        }

        ChatMessageResponse response =
                chatService.sendMessage(
                        request,
                        principal
                );


        messagingTemplate.convertAndSend(
                "/topic/meetings/"
                        + request.getMeetingId()
                        + "/chat",
                response
        );
    }

    @MessageExceptionHandler(ResponseStatusException.class)
    public void handleException(Principal principal, ResponseStatusException ex) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    ex.getReason()
            );
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
