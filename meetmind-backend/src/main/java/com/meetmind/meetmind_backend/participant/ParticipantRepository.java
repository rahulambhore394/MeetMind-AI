package com.meetmind.meetmind_backend.participant;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository
        extends JpaRepository<MeetingParticipant, Long> {



    List<MeetingParticipant>
    findByMeetingId(Long meetingId);

    List<MeetingParticipant>
    findByUserId(Long userId);


    Optional<MeetingParticipant>
    findByMeetingIdAndUserId(
            Long meetingId,
            Long userId
    );


    boolean existsByMeetingIdAndUserId(
            Long meetingId,
            Long userId
    );

    void deleteByMeetingId(Long meetingId);
}