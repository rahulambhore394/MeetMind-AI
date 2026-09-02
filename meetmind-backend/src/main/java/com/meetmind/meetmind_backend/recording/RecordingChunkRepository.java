package com.meetmind.meetmind_backend.recording;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecordingChunkRepository extends JpaRepository<RecordingChunk, Long> {
    List<RecordingChunk> findByRecordingIdOrderByChunkIndex(Long recordingId);
}
