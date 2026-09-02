package com.meetmind.meetmind_backend.representative;

import com.meetmind.meetmind_backend.event.SpringMeetingEndedEvent;
import com.meetmind.meetmind_backend.event.SpringMeetingStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RepresentativeMeetingEventListener {

    private static final Logger log = LoggerFactory.getLogger(RepresentativeMeetingEventListener.class);

    private final AiRepresentativeService representativeService;

    public RepresentativeMeetingEventListener(AiRepresentativeService representativeService) {
        this.representativeService = representativeService;
    }

    @EventListener
    public void handleMeetingStarted(SpringMeetingStartedEvent event) {
        log.info("RepresentativeMeetingEventListener: Handling MEETING_STARTED for meeting {}", event.getMeetingId());
        try {
            representativeService.activateForMeeting(event.getMeetingId());
        } catch (Exception e) {
            log.error("Failed to activate AI representatives for meeting {}", event.getMeetingId(), e);
        }
    }

    @EventListener
    public void handleMeetingEnded(SpringMeetingEndedEvent event) {
        log.info("RepresentativeMeetingEventListener: Handling MEETING_ENDED for meeting {}", event.getMeetingId());
        try {
            representativeService.completeForMeeting(event.getMeetingId());
        } catch (Exception e) {
            log.error("Failed to complete AI representatives for meeting {}", event.getMeetingId(), e);
        }
    }
}
