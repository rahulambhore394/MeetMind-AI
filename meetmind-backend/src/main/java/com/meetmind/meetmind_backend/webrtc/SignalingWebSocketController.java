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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

@Controller
public class SignalingWebSocketController {
    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketController.class);

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
        log.debug("Signaling {} received: meeting={}, sender={}, receiver={}, session={}",
                message.getType(), meetingId, user.getId(), message.getReceiverId(), sessionId);

        if ("JOIN".equalsIgnoreCase(message.getType())) {
            signalingService.registerPresence(meetingId, user.getId(), sessionId);

            // Fetch other peers already in this live session
            Set<Long> activePeers = signalingService.getActivePeers(meetingId, user.getId());
            log.info("Signaling JOIN: meeting={}, user={}, activePeers={}", meetingId, user.getId(), activePeers);
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

            // Deliver JOIN only to peers already present in signaling. Sending it
            // to the topic also delivers it back to the sender, which can make a
            // client create a peer connection to itself.
            for (Long activePeerId : activePeers) {
                User activePeer = userRepository.findById(activePeerId).orElse(null);
                if (activePeer != null) {
                    messagingTemplate.convertAndSendToUser(
                            activePeer.getEmail(),
                            "/queue/meetings/" + meetingId + "/signaling",
                            message
                    );
                }
            }

        } else if ("LEAVE".equalsIgnoreCase(message.getType())) {
            if (signalingService.evictPresence(meetingId, user.getId(), sessionId)) {
                messagingTemplate.convertAndSend("/topic/meetings/" + meetingId + "/signaling", message);
            }

        } else if (message.getReceiverId() != null) {
            // Direct message (OFFER, ANSWER, ICE_CANDIDATE)
            signalingService.validateParticipant(meetingId, message.getReceiverId());
            User receiver = userRepository.findById(message.getReceiverId()).orElse(null);
            if (receiver != null) {
                log.info("Routing {}: meeting={}, sender={}, receiver={}, destination=/user/{}/queue/meetings/{}/signaling",
                        message.getType(), meetingId, user.getId(), receiver.getId(), receiver.getEmail(), meetingId);
                messagingTemplate.convertAndSendToUser(
                        receiver.getEmail(),
                        "/queue/meetings/" + meetingId + "/signaling",
                        message
                );
            } else {
                log.warn("Dropping {} for missing receiver {} in meeting {}",
                        message.getType(), message.getReceiverId(), meetingId);
            }
        } else {
            // Broadcast room-wide signaling messages (e.g. MEDIA_UPDATE, SCREEN_SHARE_STOPPED) to all meeting participants
            messagingTemplate.convertAndSend("/topic/meetings/" + meetingId + "/signaling", message);
        }
    }

    @MessageExceptionHandler(ResponseStatusException.class)
    public void handleException(Principal principal, ResponseStatusException exception) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    exception.getReason()
            );
        }
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
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
