package com.meetmind.meetmind_backend.recording;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecordingRepository extends JpaRepository<MeetingRecording, Long> {
    List<MeetingRecording> findByMeetingId(Long meetingId);
    Optional<MeetingRecording> findByMeetingIdAndStatus(Long meetingId, RecordingStatus status);
}
