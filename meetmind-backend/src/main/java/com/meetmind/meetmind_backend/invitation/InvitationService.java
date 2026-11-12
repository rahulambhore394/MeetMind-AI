package com.meetmind.meetmind_backend.invitation;

import com.meetmind.meetmind_backend.invitation.dto.InvitationDto;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.websocket.MeetingEvent;
import com.meetmind.meetmind_backend.websocket.MeetingEventPublisher;
import com.meetmind.meetmind_backend.websocket.MeetingEventType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class InvitationService {

    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingEventPublisher eventPublisher;

    public InvitationService(
            ParticipantRepository participantRepository,
            MeetingRepository meetingRepository,
            UserRepository userRepository,
            MeetingEventPublisher eventPublisher
    ) {
        this.participantRepository = participantRepository;
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> getMyInvitations(Long userId) {
        List<MeetingParticipant> participants = participantRepository.findByUserId(userId);

        return participants.stream()
                .filter(p -> p.getMeeting() != null && !p.getMeeting().getHost().getId().equals(userId))
                .map(InvitationDto::new)
                .sorted(Comparator.comparing((InvitationDto dto) -> "INVITED".equalsIgnoreCase(dto.getStatus()) ? 0 : 1)
                        .thenComparing(InvitationDto::getMeetingId, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public InvitationDto getInvitationByCode(String code, Long currentUserId) {
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

        String status = "INVITED";
        if (currentUserId != null) {
            Optional<MeetingParticipant> opt = participantRepository.findByMeetingIdAndUserId(meeting.getId(), currentUserId);
            if (opt.isPresent()) {
                status = opt.get().getStatus().name();
            }
        }

        return new InvitationDto(meeting, status);
    }

    @Transactional
    public InvitationDto acceptInvitation(Long meetingId, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, currentUserId)
                .orElseGet(() -> {
                    MeetingParticipant newP = new MeetingParticipant();
                    newP.setMeeting(meeting);
                    newP.setUser(user);
                    newP.setRole(ParticipantRole.PARTICIPANT);
                    newP.setStatus(ParticipantStatus.INVITED);
                    return newP;
                });

        participant.setStatus(ParticipantStatus.ACCEPTED);
        MeetingParticipant saved = participantRepository.save(participant);

        MeetingEvent event = new MeetingEvent(
                MeetingEventType.PARTICIPANT_ACCEPTED,
                meetingId,
                currentUserId,
                user.getName(),
                user.getName() + " accepted the meeting invitation"
        );
        eventPublisher.publish(meetingId, event);

        return new InvitationDto(saved);
    }

    @Transactional
    public InvitationDto declineInvitation(Long meetingId, Long currentUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invitation found for this meeting"));

        participant.setStatus(ParticipantStatus.DECLINED);
        MeetingParticipant saved = participantRepository.save(participant);

        MeetingEvent event = new MeetingEvent(
                MeetingEventType.PARTICIPANT_DECLINED,
                meetingId,
                currentUserId,
                user.getName(),
                user.getName() + " declined the meeting invitation"
        );
        eventPublisher.publish(meetingId, event);

        return new InvitationDto(saved);
    }

    @Transactional
    public InvitationDto acceptInvitationByCode(String code, Long currentUserId) {
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

        return acceptInvitation(meeting.getId(), currentUserId);
    }
}

