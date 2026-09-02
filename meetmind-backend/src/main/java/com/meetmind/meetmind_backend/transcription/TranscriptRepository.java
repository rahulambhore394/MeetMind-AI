package com.meetmind.meetmind_backend.transcription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TranscriptRepository extends JpaRepository<MeetingTranscript, Long> {

    List<MeetingTranscript> findByMeetingId(Long meetingId);

    Optional<MeetingTranscript> findByRecordingId(Long recordingId);

    boolean existsByRecordingId(Long recordingId);
}
