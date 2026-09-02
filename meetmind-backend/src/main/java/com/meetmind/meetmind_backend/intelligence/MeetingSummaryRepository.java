package com.meetmind.meetmind_backend.intelligence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {

    Optional<MeetingSummary> findByMeetingId(Long meetingId);

    Optional<MeetingSummary> findByTranscriptId(Long transcriptId);

    boolean existsByMeetingId(Long meetingId);
}
