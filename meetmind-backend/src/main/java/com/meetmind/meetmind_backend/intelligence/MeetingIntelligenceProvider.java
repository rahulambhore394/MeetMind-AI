package com.meetmind.meetmind_backend.intelligence;

import com.meetmind.meetmind_backend.transcription.TranscriptSegment;

import java.util.List;

public interface MeetingIntelligenceProvider {

    String providerName();

    boolean supportsLanguage(String language);

    IntelligenceResult analyze(String fullText, List<TranscriptSegment> segments, String language) throws IntelligenceException;
}
