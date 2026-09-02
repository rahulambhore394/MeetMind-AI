package com.meetmind.meetmind_backend.transcription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    List<TranscriptSegment> findByTranscriptIdOrderBySegmentIndex(Long transcriptId);

    void deleteByTranscriptId(Long transcriptId);
}
