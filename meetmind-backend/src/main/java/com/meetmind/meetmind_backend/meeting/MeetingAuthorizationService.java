package com.meetmind.meetmind_backend.meeting;

import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import org.springframework.stereotype.Service;

@Service
public class MeetingAuthorizationService {

    private final ParticipantRepository participantRepository;

    public MeetingAuthorizationService(
            ParticipantRepository participantRepository
    ) {
        this.participantRepository =
                participantRepository;
    }

    public boolean isParticipant(
            Long meetingId,
            Long userId
    ) {

        return participantRepository
                .existsByMeetingIdAndUserId(
                        meetingId,
                        userId
                );
    }
}
