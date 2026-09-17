package com.meetmind.meetmind_backend.representative;

import com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class AiProxyWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(AiProxyWebSocketController.class);

    private final AiProxySpeechService speechService;

    public AiProxyWebSocketController(AiProxySpeechService speechService) {
        this.speechService = speechService;
    }

    /**
     * In-call STOMP destination for asking a question directly to the AI Representative.
     * Destination: /app/meetings/{meetingId}/ai-proxy/ask
     * Payload: { "representativeId": 123, "query": "What is the budget?", "language": "en" }
     */
    @MessageMapping("/meetings/{meetingId}/ai-proxy/ask")
    public void askRepresentative(
            @DestinationVariable Long meetingId,
            @Payload Map<String, Object> payload,
            Principal principal
    ) {
        try {
            Long representativeId = payload.get("representativeId") != null
                    ? Long.valueOf(payload.get("representativeId").toString())
                    : null;
            String query = payload.get("query") != null ? payload.get("query").toString() : "";
            String language = payload.get("language") != null ? payload.get("language").toString() : "en";

            if (representativeId != null && !query.isBlank()) {
                log.info("AiProxyWebSocketController: Direct speech query in meeting {} for rep {}: '{}'",
                        meetingId, representativeId, query);
                speechService.directQueryRepresentative(meetingId, representativeId, query, language);
            }
        } catch (Exception e) {
            log.error("AiProxyWebSocketController: Failed to process direct speech query in meeting {}", meetingId, e);
        }
    }
}
