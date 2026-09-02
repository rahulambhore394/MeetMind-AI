package com.meetmind.meetmind_backend.chat;


import com.meetmind.meetmind_backend.chat.dto.ChatMessageRequest;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.meetmind.meetmind_backend.event.SpringChatMessageSentEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            ParticipantRepository participantRepository,
            MeetingRepository meetingRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.chatMessageRepository =
                chatMessageRepository;

        this.userRepository =
                userRepository;

        this.participantRepository =
                participantRepository;

        this.meetingRepository =
                meetingRepository;

        this.applicationEventPublisher =
                applicationEventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(
            Long meetingId,
            Pageable pageable,
            Principal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        User currentUser = extractUserFromPrincipal(principal);

        // Verify meeting exists
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        // Verify user is participant or host of the meeting
        boolean isParticipant = meeting.getHost().getId().equals(currentUser.getId()) ||
                participantRepository.existsByMeetingIdAndUserId(meetingId, currentUser.getId());

        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view messages for this meeting");
        }

        Page<ChatMessage> messages = chatMessageRepository.findByMeetingIdAndDeletedFalse(meetingId, pageable);

        List<Long> senderIds = messages.getContent().stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();

        Map<Long, String> senderNamesMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return messages.map(msg -> {
            ChatMessageResponse response = new ChatMessageResponse();
            response.setMessageId(msg.getId());
            response.setMeetingId(msg.getMeetingId());
            response.setSenderId(msg.getSenderId());
            response.setSenderName(senderNamesMap.getOrDefault(msg.getSenderId(), "Unknown"));
            response.setMessage(msg.getMessage());
            response.setMessageType(msg.getMessageType());
            response.setSentAt(msg.getCreatedAt());
            return response;
        });
    }

    @Transactional
    public ChatMessageResponse sendMessage(
            ChatMessageRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (request.getMeetingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting ID is required");
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        User user = extractUserFromPrincipal(principal);

        // Verify meeting exists
        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        boolean participant = meeting.getHost().getId().equals(user.getId()) ||
                participantRepository.existsByMeetingIdAndUserId(request.getMeetingId(), user.getId());

        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this meeting");
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMeetingId(request.getMeetingId());
        chatMessage.setSenderId(user.getId());
        chatMessage.setMessage(request.getMessage().trim());
        chatMessage.setMessageType("TEXT");

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        applicationEventPublisher.publishEvent(
                new SpringChatMessageSentEvent(
                        this,
                        saved.getMeetingId(),
                        user.getId(),
                        user.getName(),
                        saved.getId(),
                        saved.getMessage(),
                        saved.getCreatedAt()
                )
        );

        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(saved.getId());
        response.setMeetingId(saved.getMeetingId());
        response.setSenderId(saved.getSenderId());
        response.setSenderName(user.getName());
        response.setMessage(saved.getMessage());
        response.setMessageType(saved.getMessageType());
        response.setSentAt(saved.getCreatedAt());

        return response;
    }

    private User extractUserFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (User) auth.getPrincipal();
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}