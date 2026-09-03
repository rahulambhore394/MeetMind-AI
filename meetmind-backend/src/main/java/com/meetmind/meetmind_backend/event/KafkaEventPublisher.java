package com.meetmind.meetmind_backend.event;

import com.meetmind.meetmind_backend.recording.SpringRecordingCompletedEvent;
import com.meetmind.meetmind_backend.recording.SpringRecordingReadyForTranscriptionEvent;
import com.meetmind.meetmind_backend.transcription.SpringTranscriptionCompletedEvent;
import com.meetmind.meetmind_backend.intelligence.SpringActionItemAssignedEvent;
import com.meetmind.meetmind_backend.event.SpringMeetingInvitationEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final org.springframework.beans.factory.ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider;

    private void addCorrelationId(Map<String, Object> metadata) {
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            metadata.put("requestId", requestId);
        }
    }

    public KafkaEventPublisher(org.springframework.beans.factory.ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    private void sendEvent(String topic, String key, MeetMindEvent kafkaEvent) {
        KafkaTemplate<Object, Object> template = kafkaTemplateProvider.getIfAvailable();
        if (template != null) {
            template.send(topic, key, kafkaEvent);
        } else {
            log.debug("Kafka is disabled; skipping event publish to topic {}", topic);
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("DIAGNOSTIC: KafkaEventPublisher bean has been successfully created!");
        log.info("KafkaEventPublisher bean initialized.");
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Transaction committed. Executing Kafka publishing task.");
                    System.out.println("KAFKA PUBLISHER: Transaction committed. Executing task.");
                    runnable.run();
                }
            });
        } else {
            log.info("No active transaction found. Executing Kafka publishing task immediately.");
            System.out.println("KAFKA PUBLISHER: No active transaction. Executing immediately.");
            runnable.run();
        }
    }

    @EventListener
    public void handleMeetingStarted(SpringMeetingStartedEvent event) {
        System.out.println("KAFKA PUBLISHER: RECEIVED handleMeetingStarted event for " + event.getMeetingId());
        runAfterCommit(() -> {
            log.info("Publishing MEETING_STARTED to Kafka for meeting {}", event.getMeetingId());
            System.out.println("KAFKA PUBLISHER: SENDING MEETING_STARTED for " + event.getMeetingId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", event.getTitle());
            metadata.put("hostName", event.getHostName());
            addCorrelationId(metadata);

            MeetMindEvent kafkaEvent = new MeetMindEvent(
                    UUID.randomUUID().toString(),
                    "MEETING_STARTED",
                    Instant.now().toEpochMilli(),
                    1,
                    event.getMeetingId(),
                    event.getHostId(),
                    metadata
            );

            sendEvent("meeting-lifecycle-events", String.valueOf(event.getMeetingId()), kafkaEvent);
        });
    }

    @EventListener
    public void handleMeetingInvitation(SpringMeetingInvitationEvent event) {
        log.info("Publishing MEETING_INVITATION to Kafka for user {} in meeting {}",
                event.getInvitedUserId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("meetingTitle", event.getMeetingTitle());
        metadata.put("hostName", event.getHostName());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "MEETING_INVITATION",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                event.getInvitedUserId(),
                metadata
        );

        sendEvent("participant-lifecycle-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleMeetingEnded(SpringMeetingEndedEvent event) {
        System.out.println("KAFKA PUBLISHER: RECEIVED handleMeetingEnded event for " + event.getMeetingId());
        runAfterCommit(() -> {
            log.info("Publishing MEETING_ENDED to Kafka for meeting {}", event.getMeetingId());
            System.out.println("KAFKA PUBLISHER: SENDING MEETING_ENDED for " + event.getMeetingId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("hostName", event.getHostName());
            addCorrelationId(metadata);

            MeetMindEvent kafkaEvent = new MeetMindEvent(
                    UUID.randomUUID().toString(),
                    "MEETING_ENDED",
                    Instant.now().toEpochMilli(),
                    1,
                    event.getMeetingId(),
                    event.getHostId(),
                    metadata
            );

            sendEvent("meeting-lifecycle-events", String.valueOf(event.getMeetingId()), kafkaEvent);
        });
    }

    @EventListener
    public void handleParticipantJoined(SpringParticipantJoinedEvent event) {
        System.out.println("KAFKA PUBLISHER: RECEIVED handleParticipantJoined event for " + event.getUserId());
        runAfterCommit(() -> {
            log.info("Publishing PARTICIPANT_JOINED to Kafka for user {} in meeting {}", event.getUserId(), event.getMeetingId());
            System.out.println("KAFKA PUBLISHER: SENDING PARTICIPANT_JOINED for " + event.getUserId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", event.getUserName());
            addCorrelationId(metadata);

            MeetMindEvent kafkaEvent = new MeetMindEvent(
                    UUID.randomUUID().toString(),
                    "PARTICIPANT_JOINED",
                    Instant.now().toEpochMilli(),
                    1,
                    event.getMeetingId(),
                    event.getUserId(),
                    metadata
            );

            sendEvent("participant-lifecycle-events", String.valueOf(event.getMeetingId()), kafkaEvent);
        });
    }

    @EventListener
    public void handleParticipantLeft(SpringParticipantLeftEvent event) {
        System.out.println("KAFKA PUBLISHER: RECEIVED handleParticipantLeft event for " + event.getUserId());
        runAfterCommit(() -> {
            log.info("Publishing PARTICIPANT_LEFT to Kafka for user {} in meeting {}", event.getUserId(), event.getMeetingId());
            System.out.println("KAFKA PUBLISHER: SENDING PARTICIPANT_LEFT for " + event.getUserId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", event.getUserName());
            addCorrelationId(metadata);

            MeetMindEvent kafkaEvent = new MeetMindEvent(
                    UUID.randomUUID().toString(),
                    "PARTICIPANT_LEFT",
                    Instant.now().toEpochMilli(),
                    1,
                    event.getMeetingId(),
                    event.getUserId(),
                    metadata
            );

            sendEvent("participant-lifecycle-events", String.valueOf(event.getMeetingId()), kafkaEvent);
        });
    }

    @EventListener
    public void handleChatMessageSent(SpringChatMessageSentEvent event) {
        System.out.println("KAFKA PUBLISHER: RECEIVED handleChatMessageSent event for " + event.getMessageId());
        runAfterCommit(() -> {
            log.info("Publishing CHAT_MESSAGE_SENT to Kafka for message {} in meeting {}", event.getMessageId(), event.getMeetingId());
            System.out.println("KAFKA PUBLISHER: SENDING CHAT_MESSAGE_SENT for " + event.getMessageId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("messageId", event.getMessageId());
            metadata.put("message", event.getMessage());
            metadata.put("senderName", event.getSenderName());
            metadata.put("sentAt", event.getSentAt().toString());

            MeetMindEvent kafkaEvent = new MeetMindEvent(
                    UUID.randomUUID().toString(),
                    "CHAT_MESSAGE_SENT",
                    Instant.now().toEpochMilli(),
                    1,
                    event.getMeetingId(),
                    event.getUserId(),
                    metadata
            );

            sendEvent("chat-message-events", String.valueOf(event.getMeetingId()), kafkaEvent);
        });
    }

    @EventListener
    public void handleRecordingCompleted(SpringRecordingCompletedEvent event) {
        log.info("Publishing RECORDING_COMPLETED to Kafka for recording {} in meeting {}",
                event.getRecordingId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recordingId", event.getRecordingId());
        metadata.put("storagePath", event.getStoragePath());
        metadata.put("language", event.getLanguage());
        metadata.put("ownerId", event.getOwnerId());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "RECORDING_COMPLETED",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                event.getOwnerId(),
                metadata
        );

        sendEvent("recording-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleRecordingReadyForTranscription(SpringRecordingReadyForTranscriptionEvent event) {
        log.info("Publishing RECORDING_READY_FOR_TRANSCRIPTION to Kafka for recording {} in meeting {}",
                event.getRecordingId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recordingId", event.getRecordingId());
        metadata.put("storagePath", event.getStoragePath());
        metadata.put("language", event.getLanguage());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "RECORDING_READY_FOR_TRANSCRIPTION",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                null,
                metadata
        );

        sendEvent("transcription-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleTranscriptionCompleted(SpringTranscriptionCompletedEvent event) {
        log.info("Publishing TRANSCRIPTION_COMPLETED to Kafka for transcript {} in meeting {}",
                event.getTranscriptId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transcriptId", event.getTranscriptId());
        metadata.put("recordingId", event.getRecordingId());
        metadata.put("segmentCount", event.getSegmentCount());
        metadata.put("language", event.getLanguage());
        metadata.put("success", event.isSuccess());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "TRANSCRIPTION_COMPLETED",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                null,
                metadata
        );

        sendEvent("transcription-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleAiSummaryReady(com.meetmind.meetmind_backend.intelligence.SpringAiSummaryReadyEvent event) {
        log.info("Publishing AI_SUMMARY_READY to Kafka for summary {} in meeting {}",
                event.getMeetingSummaryId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("meetingSummaryId", event.getMeetingSummaryId());
        metadata.put("transcriptId", event.getTranscriptId());
        metadata.put("actionItemCount", event.getActionItemCount());
        metadata.put("success", event.isSuccess());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "AI_SUMMARY_READY",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                null,
                metadata
        );

        sendEvent("intelligence-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleActionItemAssigned(SpringActionItemAssignedEvent event) {
        log.info("Publishing ACTION_ITEM_ASSIGNED to Kafka for item {} in meeting {}",
                event.getActionItemId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionItemId", event.getActionItemId());
        metadata.put("assignedUser", event.getAssignedUser());
        metadata.put("description", event.getDescription());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "ACTION_ITEM_ASSIGNED",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                null,
                metadata
        );

        sendEvent("intelligence-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleRepresentativeActivated(com.meetmind.meetmind_backend.representative.SpringRepresentativeActivatedEvent event) {
        log.info("Publishing REPRESENTATIVE_ACTIVATED to Kafka for representative {} in meeting {}",
                event.getRepresentativeId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("representativeId", event.getRepresentativeId());
        metadata.put("ownerId", event.getOwnerId());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "REPRESENTATIVE_ACTIVATED",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                event.getOwnerId(),
                metadata
        );

        sendEvent("representative-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }

    @EventListener
    public void handleRepresentativeReportReady(com.meetmind.meetmind_backend.representative.SpringRepresentativeReportReadyEvent event) {
        log.info("Publishing REPRESENTATIVE_REPORT_READY to Kafka for report {} in meeting {}",
                event.getReportId(), event.getMeetingId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reportId", event.getReportId());
        metadata.put("representativeId", event.getRepresentativeId());
        metadata.put("ownerId", event.getOwnerId());

        MeetMindEvent kafkaEvent = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "REPRESENTATIVE_REPORT_READY",
                Instant.now().toEpochMilli(),
                1,
                event.getMeetingId(),
                event.getOwnerId(),
                metadata
        );

        sendEvent("representative-events", String.valueOf(event.getMeetingId()), kafkaEvent);
    }
}
