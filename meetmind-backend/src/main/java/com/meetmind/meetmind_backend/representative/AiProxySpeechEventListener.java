package com.meetmind.meetmind_backend.representative;

import com.meetmind.meetmind_backend.event.SpringChatMessageSentEvent;
import com.meetmind.meetmind_backend.event.SpringLiveSpeechTranscribedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AiProxySpeechEventListener {

    private static final Logger log = LoggerFactory.getLogger(AiProxySpeechEventListener.class);

    private final AiProxySpeechService speechService;

    public AiProxySpeechEventListener(AiProxySpeechService speechService) {
        this.speechService = speechService;
    }

    @EventListener
    public void onChatMessageSent(SpringChatMessageSentEvent event) {
        try {
            speechService.handleIncomingChatMessage(
                    event.getMeetingId(),
                    event.getUserId(),
                    event.getSenderName(),
                    event.getMessage()
            );
        } catch (Exception e) {
            log.error("AiProxySpeechEventListener: Error handling in-meeting chat message for AI speech synthesis", e);
        }
    }

    @EventListener
    public void onLiveSpeechTranscribed(SpringLiveSpeechTranscribedEvent event) {
        try {
            speechService.handleIncomingChatMessage(
                    event.getMeetingId(),
                    event.getUserId(),
                    event.getSpeaker(),
                    event.getSpeechText()
            );
        } catch (Exception e) {
            log.error("AiProxySpeechEventListener: Error handling live speech transcript for AI speech synthesis", e);
        }
    }
}
