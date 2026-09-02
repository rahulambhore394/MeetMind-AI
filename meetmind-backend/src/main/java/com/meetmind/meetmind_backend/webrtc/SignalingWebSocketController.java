package com.meetmind.meetmind_backend.webrtc;

import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.webrtc.dto.SignalingMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

@Controller
public class SignalingWebSocketController {

    private final SignalingService signalingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public SignalingWebSocketController(
            SignalingService signalingService,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository
    ) {
        this.signalingService = signalingService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @MessageMapping("/meetings/{meetingId}/signaling")
    public void handleSignalingMessage(
            @DestinationVariable Long meetingId,
            @Payload SignalingMessage message,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        if (principal == null) {
            return;
        }

        User user = extractUser(principal);
        signalingService.validateParticipant(meetingId, user.getId());

        String sessionId = headerAccessor.getSessionId();

        // Enforce sender information dynamically to prevent spoofing
        message.setMeetingId(meetingId);
        message.setSenderId(user.getId());
        message.setSenderName(user.getName());

        if ("JOIN".equalsIgnoreCase(message.getType())) {
            signalingService.registerPresence(meetingId, user.getId(), sessionId);

            // Fetch other peers already in this live session
            Set<Long> activePeers = signalingService.getActivePeers(meetingId, user.getId());
            Map<Long, String> peerNames = signalingService.getPeerNames(activePeers);

            // Format PEER_LIST payload as: id1:name1,id2:name2
            StringBuilder sb = new StringBuilder();
            peerNames.forEach((id, name) -> sb.append(id).append(":").append(name).append(","));
            String peerPayload = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";

            SignalingMessage peerListMsg = new SignalingMessage(
                    meetingId,
                    0L,
                    "SYSTEM",
                    user.getId(),
                    "PEER_LIST",
                    peerPayload
            );

            // Send peer list directly to the joining client's private user queue
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/meetings/" + meetingId + "/signaling",
                    peerListMsg
            );

            // Broadcast JOIN to all other subscribers in the meeting
            messagingTemplate.convertAndSend("/topic/meetings/" + meetingId + "/signaling", message);

        } else if ("LEAVE".equalsIgnoreCase(message.getType())) {
            signalingService.evictPresence(meetingId, user.getId(), sessionId);
            
            // Broadcast LEAVE to all subscribers
            messagingTemplate.convertAndSend("/topic/meetings/" + meetingId + "/signaling", message);

        } else if (message.getReceiverId() != null) {
            // Direct message (OFFER, ANSWER, ICE_CANDIDATE)
            signalingService.validateParticipant(meetingId, message.getReceiverId());
            User receiver = userRepository.findById(message.getReceiverId()).orElse(null);
            if (receiver != null) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getEmail(),
                        "/queue/meetings/" + meetingId + "/signaling",
                        message
                );
            }
        }
    }

    private User extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (User) auth.getPrincipal();
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
