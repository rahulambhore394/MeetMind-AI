package com.meetmind.meetmind_backend.webrtc;

import com.meetmind.meetmind_backend.config.LiveKitProperties;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.dto.MediaPermissionsDto;
import com.meetmind.meetmind_backend.meeting.dto.MediaTokenResponse;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.user.User;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LiveKitMediaService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitMediaService.class);

    private final LiveKitProperties liveKitProperties;

    public LiveKitMediaService(LiveKitProperties liveKitProperties) {
        this.liveKitProperties = liveKitProperties;
    }

    public MediaTokenResponse generateMediaToken(Meeting meeting, User user, ParticipantRole role) {
        AccessToken token = new AccessToken(liveKitProperties.getApiKey(), liveKitProperties.getApiSecret());
        String participantId = user.getId().toString();
        String displayName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : user.getEmail();
        String roomName = "meeting-" + meeting.getId();

        token.setIdentity(participantId);
        token.setName(displayName);

        long ttlSeconds = liveKitProperties.getTokenTtlSeconds();
        token.setTtl(ttlSeconds * 1000L);

        boolean isHostOrCoHost = (role == ParticipantRole.HOST || "CO_HOST".equalsIgnoreCase(role.name()));
        boolean isGuest = ("GUEST".equalsIgnoreCase(role.name()));

        MediaPermissionsDto permissions = new MediaPermissionsDto(
                !isGuest,
                !isGuest,
                isHostOrCoHost,
                true
        );

        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName)
        );

        String jwt = token.toJwt();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);

        log.info("Issued LiveKit SFU media token for user {} ({}) in meeting {}",
                user.getId(), role, meeting.getId());

        return new MediaTokenResponse(
                meeting.getId().toString(),
                liveKitProperties.getUrl(),
                jwt,
                participantId,
                permissions,
                expiresAt
        );
    }
}
