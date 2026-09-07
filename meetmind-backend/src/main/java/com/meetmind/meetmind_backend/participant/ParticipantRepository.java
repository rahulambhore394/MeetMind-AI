package com.meetmind.meetmind_backend.participant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    @EntityGraph(attributePaths = {"user", "meeting", "meeting.host"})
    List<MeetingParticipant> findByMeetingId(Long meetingId);

    @EntityGraph(attributePaths = {"user", "meeting", "meeting.host"})
    List<MeetingParticipant> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "meeting", "meeting.host"})
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    @Override
    @EntityGraph(attributePaths = {"user", "meeting", "meeting.host"})
    Optional<MeetingParticipant> findById(Long id);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    long countByMeetingIdAndStatus(Long meetingId, ParticipantStatus status);

    void deleteByMeetingId(Long meetingId);
}