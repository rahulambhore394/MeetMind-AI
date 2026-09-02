package com.meetmind.meetmind_backend.translation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveTranslationRepository extends JpaRepository<LiveTranslation, Long> {

    List<LiveTranslation> findByMeetingIdOrderByCreatedAtAsc(Long meetingId);

    List<LiveTranslation> findByMeetingIdAndTargetLanguageOrderByCreatedAtAsc(Long meetingId, String targetLanguage);

    List<LiveTranslation> findByTranscriptSegmentId(Long transcriptSegmentId);

    boolean existsByTranscriptSegmentIdAndTargetLanguage(Long transcriptSegmentId, String targetLanguage);
}
