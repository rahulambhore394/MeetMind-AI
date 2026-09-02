package com.meetmind.meetmind_backend.representative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiRepresentativeService {

    private static final Logger log = LoggerFactory.getLogger(AiRepresentativeService.class);

    private final AiRepresentativeRepository representativeRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final RepresentativeReportService reportService;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public AiRepresentativeService(
            AiRepresentativeRepository representativeRepository,
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository,
            UserRepository userRepository,
            RepresentativeReportService reportService,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper
    ) {
        this.representativeRepository = representativeRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiRepresentative createRepresentative(
            Long meetingId,
            User owner,
            List<String> monitoredTopics,
            List<String> monitoredQuestions,
            List<String> importantPeople,
            String reportPreferences,
            String notificationPreferences
    ) {
        verifyMeetingAccess(meetingId, owner.getId());

        AiRepresentative rep = representativeRepository.findByMeetingIdAndOwnerId(meetingId, owner.getId())
                .orElseGet(AiRepresentative::new);

        if (rep.getStatus() == RepresentativeStatus.ACTIVE || rep.getStatus() == RepresentativeStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify representative in " + rep.getStatus() + " status");
        }

        rep.setMeetingId(meetingId);
        rep.setOwnerId(owner.getId());
        rep.setStatus(RepresentativeStatus.SCHEDULED);
        rep.setMonitoredTopicsJson(toJson(monitoredTopics));
        rep.setMonitoredQuestionsJson(toJson(monitoredQuestions));
        rep.setImportantPeopleJson(toJson(importantPeople));
        rep.setReportPreferencesJson(reportPreferences != null ? reportPreferences : "FULL_REPORT");
        rep.setNotificationPreferencesJson(notificationPreferences != null ? notificationPreferences : "IMMEDIATE");
        rep.setConsentDisclosure(true);

        rep = representativeRepository.save(rep);
        log.info("AiRepresentativeService: Created representative {} for owner {} in meeting {}", rep.getId(), owner.getId(), meetingId);
        return rep;
    }

    @Transactional
    public AiRepresentative uploadMedia(Long meetingId, Long representativeId, String mediaPath, User owner) {
        AiRepresentative rep = getRepresentativeAndVerifyOwner(meetingId, representativeId, owner);

        if (mediaPath == null || mediaPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media storage path cannot be empty");
        }

        Path path = Path.of(mediaPath);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media file does not exist on disk: " + mediaPath);
        }

        if (Files.isDirectory(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media path points to a directory: " + mediaPath);
        }

        try {
            if (Files.size(path) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media file is empty (0 bytes): " + mediaPath);
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot validate media file: " + e.getMessage());
        }

        rep.setMediaStoragePath(mediaPath);
        rep.setStatus(RepresentativeStatus.SCHEDULED);
        rep = representativeRepository.save(rep);
        log.info("AiRepresentativeService: Approved media uploaded for representative {}", rep.getId());
        return rep;
    }

    @Transactional
    public AiRepresentative cancelRepresentative(Long meetingId, Long representativeId, User owner) {
        AiRepresentative rep = getRepresentativeAndVerifyOwner(meetingId, representativeId, owner);
        rep.setStatus(RepresentativeStatus.CANCELLED);
        rep.setEndedAt(LocalDateTime.now());
        rep = representativeRepository.save(rep);
        log.info("AiRepresentativeService: Representative {} cancelled by owner {}", rep.getId(), owner.getId());
        return rep;
    }

    @Transactional
    public List<AiRepresentative> activateForMeeting(Long meetingId) {
        List<AiRepresentative> reps = representativeRepository.findByMeetingId(meetingId);
        List<AiRepresentative> activated = new ArrayList<>();

        for (AiRepresentative rep : reps) {
            if (rep.getStatus() == RepresentativeStatus.SCHEDULED || rep.getStatus() == RepresentativeStatus.CREATED) {
                User owner = userRepository.findById(rep.getOwnerId()).orElse(null);
                String ownerName = owner != null ? owner.getName() : "User #" + rep.getOwnerId();

                // Create or resolve automated MeetingParticipant
                Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
                if (meeting != null && owner != null) {
                    MeetingParticipant agentPart = participantRepository.findByMeetingIdAndUserId(meetingId, owner.getId())
                            .orElseGet(() -> {
                                MeetingParticipant mp = new MeetingParticipant();
                                mp.setMeeting(meeting);
                                mp.setUser(owner);
                                return mp;
                            });
                    agentPart.setRole(ParticipantRole.AUTOMATED_AGENT);
                    agentPart.setStatus(ParticipantStatus.ACCEPTED);
                    agentPart.setJoinedAt(LocalDateTime.now());
                    participantRepository.save(agentPart);
                }

                rep.setStatus(RepresentativeStatus.ACTIVE);
                rep.setStartedAt(LocalDateTime.now());
                rep = representativeRepository.save(rep);
                activated.add(rep);

                // Broadcast visible disclosure
                String disclosureText = "DISCLOSURE: Automated AI Representative for " + ownerName + " has joined the meeting.";
                log.info("AiRepresentativeService: {}", disclosureText);
                messagingTemplate.convertAndSend(
                        "/topic/meetings/" + meetingId + "/chat",
                        Map.of(
                                "type", "SYSTEM_DISCLOSURE",
                                "message", disclosureText,
                                "representativeId", rep.getId(),
                                "isAutomated", true
                        )
                );

                eventPublisher.publishEvent(new SpringRepresentativeActivatedEvent(
                        this, rep.getId(), meetingId, rep.getOwnerId()
                ));
            }
        }
        return activated;
    }

    @Transactional
    public List<AiRepresentative> completeForMeeting(Long meetingId) {
        List<AiRepresentative> reps = representativeRepository.findByMeetingId(meetingId);
        List<AiRepresentative> completed = new ArrayList<>();

        for (AiRepresentative rep : reps) {
            if (rep.getStatus() == RepresentativeStatus.ACTIVE || rep.getStatus() == RepresentativeStatus.SCHEDULED || rep.getStatus() == RepresentativeStatus.CREATED) {
                rep.setStatus(RepresentativeStatus.COMPLETED);
                rep.setEndedAt(LocalDateTime.now());
                rep = representativeRepository.save(rep);

                // Generate post-meeting report
                reportService.generateReport(rep);
                completed.add(rep);
            }
        }
        return completed;
    }

    @Transactional
    public AiRepresentative failRepresentative(Long representativeId, String reason) {
        AiRepresentative rep = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new IllegalArgumentException("Representative not found: " + representativeId));
        rep.setStatus(RepresentativeStatus.FAILED);
        rep.setEndedAt(LocalDateTime.now());
        log.warn("AiRepresentativeService: Representative {} failed: {}", representativeId, reason);
        return representativeRepository.save(rep);
    }

    public AiRepresentative getRepresentativeForMeeting(Long meetingId, User user) {
        verifyMeetingAccess(meetingId, user.getId());
        return representativeRepository.findByMeetingIdAndOwnerId(meetingId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No AI Representative configured for this meeting"));
    }

    private AiRepresentative getRepresentativeAndVerifyOwner(Long meetingId, Long representativeId, User owner) {
        AiRepresentative rep = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Representative not found"));

        if (!rep.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Representative does not belong to meeting: " + meetingId);
        }

        if (!rep.getOwnerId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: You do not own this representative");
        }
        return rep;
    }

    private void verifyMeetingAccess(Long meetingId, Long userId) {
        boolean isMeetingPresent = meetingRepository.existsById(meetingId);
        if (!isMeetingPresent) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found");
        }
        boolean isHost = meetingRepository.findById(meetingId)
                .map(m -> m.getHost().getId().equals(userId))
                .orElse(false);
        boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
        if (!isHost && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User is not a host or participant in this meeting");
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
