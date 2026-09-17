package com.meetmind.meetmind_backend.meeting;



import com.meetmind.meetmind_backend.meeting.dto.CreateMeetingRequest;
import com.meetmind.meetmind_backend.meeting.dto.MeetingResponse;
import com.meetmind.meetmind_backend.meeting.dto.UpdateMeetingRequest;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.websocket.MeetingEvent;
import com.meetmind.meetmind_backend.websocket.MeetingEventPublisher;
import com.meetmind.meetmind_backend.websocket.MeetingEventType;
import com.meetmind.meetmind_backend.event.SpringMeetingStartedEvent;
import com.meetmind.meetmind_backend.event.SpringMeetingEndedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeetingService {
    private final MeetingEventPublisher eventPublisher;
    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.meetmind.meetmind_backend.notification.EmailService emailService;

    public MeetingService(
            MeetingRepository meetingRepository,
            UserRepository userRepository,
            ParticipantRepository participantRepository,
            MeetingEventPublisher eventPublisher,
            ApplicationEventPublisher applicationEventPublisher,
            com.meetmind.meetmind_backend.notification.EmailService emailService
    ) {

        this.meetingRepository =
                meetingRepository;

        this.userRepository =
                userRepository;

        this.participantRepository =
                participantRepository;

        this.eventPublisher =
                eventPublisher;

        this.applicationEventPublisher =
                applicationEventPublisher;

        this.emailService =
                emailService;
    }


    // ==========================================
    // CREATE MEETING
    // ==========================================

    private MeetingResponse toResponse(Meeting meeting, Long userId) {
        if (userId == null) {
            return new MeetingResponse(meeting);
        }
        return participantRepository.findByMeetingIdAndUserId(meeting.getId(), userId)
                .map(p -> {
                    boolean hasJoined = (p.getStatus() == ParticipantStatus.LEFT || p.getStatus() == ParticipantStatus.JOINED || p.getJoinedAt() != null);
                    return new MeetingResponse(meeting, hasJoined, p.getStatus().name());
                })
                .orElseGet(() -> new MeetingResponse(meeting, false, null));
    }

    public MeetingResponse createMeeting(
            CreateMeetingRequest request,
            Long currentUserId
    ) {

        User host = userRepository
                .findById(currentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        Meeting meeting = new Meeting();

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setScheduledAt(request.getScheduledAt());
        meeting.setHost(host);
        meeting.setStatus(MeetingStatus.SCHEDULED);

        String randomCode = "mm-" + (100 + (int)(Math.random() * 899)) + "-" + java.util.UUID.randomUUID().toString().substring(0, 4);
        meeting.setMeetingCode(randomCode);

        Meeting savedMeeting =
                meetingRepository.save(meeting);

        // Host also becomes a participant
        MeetingParticipant hostParticipant =
                new MeetingParticipant();

        hostParticipant.setMeeting(savedMeeting);
        hostParticipant.setUser(host);
        hostParticipant.setRole(ParticipantRole.HOST);
        hostParticipant.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(hostParticipant);

        // Process invited email participants if any
        if (request.getInvitedEmails() != null && !request.getInvitedEmails().isEmpty()) {
            java.util.List<String> cleanEmails = new java.util.ArrayList<>();
            for (String email : request.getInvitedEmails()) {
                if (email != null && !email.trim().isEmpty()) {
                    String cleanEmail = email.trim();
                    cleanEmails.add(cleanEmail);
                    userRepository.findByEmail(cleanEmail).ifPresent(invitedUser -> {
                        if (!invitedUser.getId().equals(host.getId())) {
                            MeetingParticipant invitedPart = new MeetingParticipant();
                            invitedPart.setMeeting(savedMeeting);
                            invitedPart.setUser(invitedUser);
                            invitedPart.setRole(ParticipantRole.PARTICIPANT);
                            invitedPart.setStatus(ParticipantStatus.INVITED);
                            participantRepository.save(invitedPart);
                        }
                    });
                }
            }

            if (!cleanEmails.isEmpty()) {
                emailService.sendBatchMeetingInvitationEmailsAsync(
                        cleanEmails,
                        host.getName(),
                        savedMeeting.getTitle(),
                        savedMeeting.getDescription(),
                        savedMeeting.getMeetingCode(),
                        savedMeeting.getScheduledAt(),
                        email -> userRepository.findByEmail(email).isPresent()
                );
            }
        }

        return toResponse(savedMeeting, currentUserId);
    }

    @Transactional
    public MeetingResponse getMeetingByCode(String code, Long userId) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseGet(() -> {
                    try {
                        Long id = Long.parseLong(code);
                        return meetingRepository.findById(id).orElse(null);
                    } catch (Exception e) {
                        return null;
                    }
                });

        if (meeting == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found for code: " + code);
        }

        // Add user as participant if joining by valid code
        if (!meeting.getHost().getId().equals(userId) && !participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
            User joiningUser = userRepository.findById(userId).orElseThrow();
            MeetingParticipant part = new MeetingParticipant();
            part.setMeeting(meeting);
            part.setUser(joiningUser);
            part.setRole(ParticipantRole.PARTICIPANT);
            part.setStatus(ParticipantStatus.ACCEPTED);
            participantRepository.save(part);
        }

        return toResponse(meeting, userId);
    }

    @CacheEvict(value = "meetings", key = "#meetingId")
    @Transactional
    public MeetingResponse startMeeting(
            Long meetingId,
            Long currentUserId
    ) {

        Meeting meeting =
                meetingRepository
                        .findById(meetingId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Meeting not found"
                                )
                        );


        // Only host can start

        verifyHost(
                meeting,
                currentUserId
        );


        // Meeting must be scheduled

        if (meeting.getStatus() == MeetingStatus.LIVE) {
            return toResponse(meeting, currentUserId);
        }

        if (meeting.getStatus() != MeetingStatus.SCHEDULED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only scheduled or live meetings can be accessed"
            );
        }


        meeting.setStatus(
                MeetingStatus.LIVE
        );

        meeting.setStartedAt(
                LocalDateTime.now()
        );

        meeting.setEmptySince(
                LocalDateTime.now()
        );


        Meeting savedMeeting =
                meetingRepository.save(meeting);


        MeetingEvent event =
                new MeetingEvent(
                        MeetingEventType.MEETING_STARTED,
                        meetingId,
                        currentUserId,
                        meeting.getHost().getName(),
                        "Meeting has started"
                );


        eventPublisher.publish(
                meetingId,
                event
        );

        applicationEventPublisher.publishEvent(
                new SpringMeetingStartedEvent(
                        this,
                        meetingId,
                        currentUserId,
                        meeting.getHost().getName(),
                        meeting.getTitle()
                )
        );

        return toResponse(savedMeeting, currentUserId);
    }

    @CacheEvict(value = "meetings", key = "#meetingId")
    @Transactional
    public MeetingResponse endMeeting(
            Long meetingId,
            Long currentUserId
    ) {

        Meeting meeting =
                meetingRepository
                        .findById(meetingId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Meeting not found"
                                )
                        );


        // Only host can end

        verifyHost(
                meeting,
                currentUserId
        );


        // Meeting must be live

        if (
                meeting.getStatus()
                        != MeetingStatus.LIVE
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only live meetings can be ended"
            );
        }


        meeting.setStatus(
                MeetingStatus.ENDED
        );

        meeting.setEndedAt(
                LocalDateTime.now()
        );


        Meeting savedMeeting =
                meetingRepository.save(meeting);
        MeetingEvent event =
                new MeetingEvent(
                        MeetingEventType.MEETING_ENDED,
                        meetingId,
                        currentUserId,
                        meeting.getHost().getName(),
                        "Meeting has ended"
                );


        eventPublisher.publish(
                meetingId,
                event
        );

        applicationEventPublisher.publishEvent(
                new SpringMeetingEndedEvent(
                        this,
                        meetingId,
                        currentUserId,
                        meeting.getHost().getName()
                )
        );

        return toResponse(savedMeeting, currentUserId);
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingStatus(
            Long meetingId,
            Long userId
    ) {

        Meeting meeting =
                meetingRepository
                        .findById(meetingId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Meeting not found"
                                )
                        );

        verifyParticipantOrHost(meeting, userId);

        return toResponse(meeting, userId);
    }


    // ==========================================
    // GET ALL MEETINGS FOR USER
    // ==========================================

    @Transactional(readOnly = true)
    public List<MeetingResponse> getAllMeetings(Long userId) {
        // Return only meetings where user is host or participant
        List<Long> meetingIds = participantRepository.findByUserId(userId)
                .stream()
                .map(p -> p.getMeeting().getId())
                .toList();

        return meetingRepository.findAllById(meetingIds)
                .stream()
                .map(m -> toResponse(m, userId))
                .toList();
    }


    // ==========================================
    // GET SINGLE MEETING
    // ==========================================

    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long meetingId, Long userId) {

        Meeting meeting =
                meetingRepository
                        .findById(meetingId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Meeting not found"
                                )
                        );
        
        verifyParticipantOrHost(meeting, userId);

        return toResponse(meeting, userId);
    }


    // ==========================================
    // UPDATE MEETING
    // ==========================================

    @CacheEvict(value = "meetings", key = "#meetingId")
    public MeetingResponse updateMeeting(
            Long meetingId,
            UpdateMeetingRequest request,
            Long currentUserId
    ) {

        Meeting meeting =
                meetingRepository
                        .findById(meetingId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Meeting not found"
                                )
                        );

        if (
                meeting.getStatus() == MeetingStatus.LIVE
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Live meeting cannot be modified"
            );
        }

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setScheduledAt(request.getScheduledAt());

        Meeting updatedMeeting =
                meetingRepository.save(meeting);

        return new MeetingResponse(updatedMeeting);
    }


    // ==========================================
    // DELETE MEETING
    // ==========================================

    @CacheEvict(value = "meetings", key = "#meetingId")
    public void deleteMeeting(
            Long meetingId,
            Long currentUserId
    ) {

        Meeting meeting =
                meetingRepository
                        .findById(meetingId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Meeting not found"
                                )
                        );

        verifyHost(meeting, currentUserId);

        participantRepository.deleteByMeetingId(meetingId);
        meetingRepository.delete(meeting);
    }


    // ==========================================
    // AUTHORIZATION
    // ==========================================

    private void verifyHost(
            Meeting meeting,
            Long currentUserId
    ) {

        Long hostId =
                meeting.getHost().getId();

        if (!hostId.equals(currentUserId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the meeting host can perform this action"
            );
        }
    }

    private void verifyParticipantOrHost(
            Meeting meeting,
            Long currentUserId
    ) {
        if (meeting.getHost().getId().equals(currentUserId)) {
            return;
        }
        if (!participantRepository.existsByMeetingIdAndUserId(meeting.getId(), currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not a participant of this meeting"
            );
        }
    }

    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 15000)
    public void autoEndEmptyMeetings() {
        List<Meeting> liveMeetings = meetingRepository.findByStatus(MeetingStatus.LIVE);
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        for (Meeting meeting : liveMeetings) {
            LocalDateTime emptySince = meeting.getEmptySince();
            long activeCount = participantRepository.countByMeetingIdAndStatus(meeting.getId(), ParticipantStatus.JOINED);
            if (activeCount > 0) {
                if (emptySince != null) {
                    meeting.setEmptySince(null);
                    meetingRepository.save(meeting);
                }
                continue;
            }
            if (emptySince == null) {
                emptySince = meeting.getStartedAt() != null ? meeting.getStartedAt() : LocalDateTime.now();
                meeting.setEmptySince(emptySince);
                meetingRepository.save(meeting);
            }
            if (emptySince.isBefore(tenMinutesAgo)) {
                meeting.setStatus(MeetingStatus.ENDED);
                meeting.setEndedAt(LocalDateTime.now());
                meetingRepository.save(meeting);

                MeetingEvent event = new MeetingEvent(
                        MeetingEventType.MEETING_ENDED,
                        meeting.getId(),
                        meeting.getHost().getId(),
                        meeting.getHost().getName(),
                        "Meeting automatically ended after 10 minutes of inactivity"
                );
                eventPublisher.publish(meeting.getId(), event);
                applicationEventPublisher.publishEvent(
                        new SpringMeetingEndedEvent(
                                this,
                                meeting.getId(),
                                meeting.getHost().getId(),
                                meeting.getHost().getName()
                        )
                );
            }
        }
    }
}
