package com.meetmind.meetmind_backend.chat;

import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
public class ChatController {

    private final ChatService chatService;

    public ChatController(
            ChatService chatService
    ) {
        this.chatService = chatService;
    }


    @GetMapping("/{meetingId}/messages")
    public Page<ChatMessageResponse> getMessages(
            @PathVariable Long meetingId,
            @RequestParam(
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    defaultValue = "50"
            ) int size,
            Authentication authentication
    ) {

        Pageable pageable =
                PageRequest.of(page, size);

        return chatService.getMessages(
                meetingId,
                pageable,
                authentication
        );
    }
}