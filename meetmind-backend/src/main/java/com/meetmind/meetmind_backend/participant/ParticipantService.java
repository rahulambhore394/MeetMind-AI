package com.meetmind.meetmind_backend.participant;


import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.dto.InviteParticipantRequest;
import com.meetmind.meetmind_backend.participant.dto.ParticipantResponse;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.websocket.MeetingEvent;
import com.meetmind.meetmind_backend.websocket.MeetingEventPublisher;
import com.meetmind.meetmind_backend.websocket.MeetingEventType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import com.meetmind.meetmind_backend.event.SpringParticipantJoinedEvent;
import com.meetmind.meetmind_backend.event.SpringParticipantLeftEvent;
import com.meetmind.meetmind_backend.event.SpringMeetingInvitationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final MeetingEventPublisher eventPublisher;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.meetmind.meetmind_backend.notification.EmailService emailService;


    public ParticipantService(
            ParticipantRepository participantRepository,
            MeetingRepository meetingRepository,
            UserRepository userRepository,
            MeetingEventPublisher eventPublisher,
            ApplicationEventPublisher applicationEventPublisher,
            com.meetmind.meetmind_backend.notification.EmailService emailService
    ) {

        this.participantRepository =
                participantRepository;

        this.meetingRepository =
                meetingRepository;

        this.userRepository =
                userRepository;

        this.eventPublisher =
                eventPublisher;

        this.applicationEventPublisher =
                applicationEventPublisher;

        this.emailService =
                emailService;
    }


    // =====================================================
    // INVITE PARTICIPANT
    // =====================================================

    @Transactional
    public ParticipantResponse inviteParticipant(
            Long meetingId,
            InviteParticipantRequest request,
            Long currentUserId
    ) {

        Meeting meeting =
                getMeeting(meetingId);


        // Allow host or any participant of the meeting to invite
        verifyParticipantOrHost(
                meeting,
                currentUserId
        );


        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        if (email.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        // Find invited user if registered

        java.util.Optional<User> optionalUser =
                userRepository.findByEmail(email);

        boolean isRegistered = optionalUser.isPresent();
        User user = optionalUser.orElse(null);

        // Don't allow current user to invite themselves

        if (isRegistered && user.getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot invite yourself"
            );
        }

        MeetingParticipant participant = null;

        if (isRegistered) {
            // Check duplicate or reuse
            java.util.Optional<MeetingParticipant> existingOpt =
                    participantRepository.findByMeetingIdAndUserId(meetingId, user.getId());
            if (existingOpt.isPresent()) {
                participant = existingOpt.get();
            } else {
                participant = new MeetingParticipant();
                participant.setMeeting(meeting);
                participant.setUser(user);
                participant.setRole(ParticipantRole.PARTICIPANT);
                participant.setStatus(ParticipantStatus.INVITED);
                participant = participantRepository.save(participant);

                applicationEventPublisher.publishEvent(
                    new SpringMeetingInvitationEvent(
                        this, meetingId, user.getId(), meeting.getTitle(), meeting.getHost().getName()
                    )
                );
            }
        }

        if (meeting != null && meeting.getHost() != null) {
            org.hibernate.Hibernate.initialize(meeting.getHost());
        }
        if (user != null) {
            org.hibernate.Hibernate.initialize(user);
        }

        // Send Email Invitation to all users (registered or unregistered)
        emailService.sendMeetingInvitationEmail(
                email,
                meeting.getHost().getName(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getMeetingCode(),
                meeting.getScheduledAt(),
                isRegistered
        );

        if (participant != null) {
            return new ParticipantResponse(participant);
        } else {
            return new ParticipantResponse(email, "PARTICIPANT", "INVITED");
        }
    }


    // =====================================================
    // GET PARTICIPANTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<ParticipantResponse>
    getParticipants(
            Long meetingId,
            Long currentUserId
    ) {

        Meeting meeting =
                getMeeting(meetingId);


        verifyParticipantOrHost(
                meeting,
                currentUserId
        );


        return participantRepository
                .findByMeetingId(meetingId)
                .stream()
                .map(ParticipantResponse::new)
                .toList();
    }


    // =====================================================
    // ACCEPT INVITATION
    // =====================================================

    @Transactional
    public ParticipantResponse acceptInvitation(
            Long meetingId,
            Long currentUserId
    ) {

        MeetingParticipant participant =
                getParticipant(
                        meetingId,
                        currentUserId
                );

        if (
                participant.getStatus()
                        != ParticipantStatus.INVITED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invitation cannot be accepted"
            );
        }


        participant.setStatus(
                ParticipantStatus.ACCEPTED
        );


        MeetingParticipant saved =
                participantRepository.save(
                        participant
                );
        MeetingEvent event =
                new MeetingEvent(
                        MeetingEventType.PARTICIPANT_LEFT,
                        meetingId,
                        currentUserId,
                        participant.getUser().getName(),
                        participant.getUser().getName()
                                + " left the meeting"
                );


        eventPublisher.publish(
                meetingId,
                event
        );

        return new ParticipantResponse(
                saved
        );
    }


    // =====================================================
    // DECLINE INVITATION
    // =====================================================

    @Transactional
    public ParticipantResponse declineInvitation(
            Long meetingId,
            Long currentUserId
    ) {

        MeetingParticipant participant =
                getParticipant(
                        meetingId,
                        currentUserId
                );


        if (
                participant.getStatus()
                        != ParticipantStatus.INVITED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invitation cannot be declined"
            );
        }


        participant.setStatus(
                ParticipantStatus.DECLINED
        );


        MeetingParticipant saved =
                participantRepository.save(
                        participant
                );
        MeetingEvent event =
                new MeetingEvent(
                        MeetingEventType.PARTICIPANT_DECLINED,
                        meetingId,
                        currentUserId,
                        participant.getUser().getName(),
                        participant.getUser().getName()
                                + " declined the invitation"
                );


        eventPublisher.publish(
                meetingId,
                event
        );

        return new ParticipantResponse(
                saved
        );
    }


    // =====================================================
    // JOIN MEETING
    // =====================================================

    @Transactional
    public ParticipantResponse joinMeeting(
            Long meetingId,
            Long currentUserId
    ) {

        Meeting meeting =
                getMeeting(meetingId);


        if (
                meeting.getStatus()
                        != MeetingStatus.LIVE
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Meeting is not live"
            );
        }


        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meetingId, currentUserId)
                .orElseGet(() -> {
                    User current = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    MeetingParticipant newPart = new MeetingParticipant();
                    newPart.setMeeting(meeting);
                    newPart.setUser(current);
                    newPart.setRole(meeting.getHost().getId().equals(currentUserId) ? ParticipantRole.HOST : ParticipantRole.PARTICIPANT);
                    newPart.setStatus(ParticipantStatus.ACCEPTED);
                    return participantRepository.save(newPart);
                });

        ParticipantStatus status = participant.getStatus();

        if (
                status != ParticipantStatus.ACCEPTED
                        && status != ParticipantStatus.LEFT
                        && status != ParticipantStatus.JOINED
                        && status != ParticipantStatus.INVITED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot join this meeting"
            );
        }


        participant.setStatus(
                ParticipantStatus.JOINED
        );

        participant.setJoinedAt(
                LocalDateTime.now()
        );

        participant.setLeftAt(null);


        MeetingParticipant saved =
                participantRepository.save(
                        participant
                );

        if (meeting.getEmptySince() != null) {
            meeting.setEmptySince(null);
            meetingRepository.save(meeting);
        }

        MeetingEvent event =
                new MeetingEvent(
                        MeetingEventType.PARTICIPANT_JOINED,
                        meetingId,
                        currentUserId,
                        participant.getUser().getName(),
                        participant.getUser().getName()
                                + " joined the meeting"
                );


        eventPublisher.publish(
                meetingId,
                event
        );

        applicationEventPublisher.publishEvent(
                new SpringParticipantJoinedEvent(
                        this,
                        meetingId,
                        currentUserId,
                        participant.getUser().getName()
                )
        );

        return new ParticipantResponse(
                saved
        );
    }

    // =====================================================
    // LEAVE MEETING
    // =====================================================

    @Transactional
    public ParticipantResponse leaveMeeting(
            Long meetingId,
            Long currentUserId
    ) {

        MeetingParticipant participant =
                getParticipant(
                        meetingId,
                        currentUserId
                );


        if (
                participant.getStatus()
                        != ParticipantStatus.JOINED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You are not currently in this meeting"
            );
        }


        participant.setStatus(
                ParticipantStatus.LEFT
        );

        participant.setLeftAt(
                LocalDateTime.now()
        );


        MeetingParticipant saved =
                participantRepository.save(
                        participant
                );

        long activeCount = participantRepository.countByMeetingIdAndStatus(meetingId, ParticipantStatus.JOINED);
        if (activeCount == 0) {
            Meeting meeting = participant.getMeeting();
            meeting.setEmptySince(LocalDateTime.now());
            meetingRepository.save(meeting);
        }
        MeetingEvent event =
                new MeetingEvent(
                        MeetingEventType.PARTICIPANT_LEFT,
                        meetingId,
                        currentUserId,
                        participant.getUser().getName(),
                        participant.getUser().getName()
                                + " left the meeting"
                );


        eventPublisher.publish(
                meetingId,
                event
        );

        applicationEventPublisher.publishEvent(
                new SpringParticipantLeftEvent(
                        this,
                        meetingId,
                        currentUserId,
                        participant.getUser().getName()
                )
        );

        return new ParticipantResponse(
                saved
        );
    }


    // =====================================================
    // GET MEETING
    // =====================================================

    private Meeting getMeeting(
            Long meetingId
    ) {

        return meetingRepository
                .findById(meetingId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Meeting not found"
                        )
                );
    }


    // =====================================================
    // GET PARTICIPANT
    // =====================================================

    private MeetingParticipant getParticipant(
            Long meetingId,
            Long userId
    ) {

        return participantRepository
                .findByMeetingIdAndUserId(
                        meetingId,
                        userId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "You are not a participant of this meeting"
                        )
                );
    }


    // =====================================================
    // VERIFY HOST
    // =====================================================

    private void verifyHost(
            Meeting meeting,
            Long currentUserId
    ) {

        if (
                !meeting
                        .getHost()
                        .getId()
                        .equals(currentUserId)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the meeting host can perform this action"
            );
        }
    }


    // =====================================================
    // VERIFY PARTICIPANT OR HOST
    // =====================================================

    private void verifyParticipantOrHost(
            Meeting meeting,
            Long currentUserId
    ) {

        if (
                meeting
                        .getHost()
                        .getId()
                        .equals(currentUserId)
        ) {

            return;
        }


        if (
                !participantRepository
                        .existsByMeetingIdAndUserId(
                                meeting.getId(),
                                currentUserId
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not part of this meeting"
            );
        }
    }
}
