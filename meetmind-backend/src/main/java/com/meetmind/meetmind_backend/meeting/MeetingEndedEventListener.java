package com.meetmind.meetmind_backend.meeting;

import com.meetmind.meetmind_backend.event.SpringMeetingEndedEvent;
import com.meetmind.meetmind_backend.intelligence.MeetingIntelligenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MeetingEndedEventListener {

    private static final Logger log = LoggerFactory.getLogger(MeetingEndedEventListener.class);
    private final MeetingIntelligenceService meetingIntelligenceService;

    public MeetingEndedEventListener(MeetingIntelligenceService meetingIntelligenceService) {
        this.meetingIntelligenceService = meetingIntelligenceService;
    }

    @Async
    @EventListener
    public void handleMeetingEnded(SpringMeetingEndedEvent event) {
        log.info("MeetingEndedEventListener: Received SpringMeetingEndedEvent for meeting {}. Auto-generating summary report.", event.getMeetingId());
        try {
            meetingIntelligenceService.autoGenerateSummaryOnMeetingEnd(event.getMeetingId());
        } catch (Exception e) {
            log.error("MeetingEndedEventListener: Failed to auto-generate summary report for meeting {}", event.getMeetingId(), e);
        }
    }
}
