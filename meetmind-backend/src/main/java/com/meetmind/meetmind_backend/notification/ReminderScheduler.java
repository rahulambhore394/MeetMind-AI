package com.meetmind.meetmind_backend.notification;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final NotificationService notificationService;

    public ReminderScheduler(
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository,
            NotificationService notificationService
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 60000) // Check every minute
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindow = now.plusMinutes(15);

        // Find meetings starting in exactly 15 minutes (approx)
        // In a real app, you'd track which reminders have already been sent
        List<Meeting> upcoming = meetingRepository.findByStatus(MeetingStatus.SCHEDULED);

        for (Meeting meeting : upcoming) {
            if (meeting.getScheduledAt().isAfter(now) && meeting.getScheduledAt().isBefore(reminderWindow)) {
                // Check if reminder already sent (omitted for simplicity in this phase)
                log.info("Sending reminder for meeting: {}", meeting.getTitle());
                notifyParticipants(meeting);
            }
        }
    }

    private void notifyParticipants(Meeting meeting) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meeting.getId());
        for (MeetingParticipant p : participants) {
            notificationService.createNotification(
                    p.getUser().getId(),
                    "MEETING_REMINDER",
                    "Meeting Reminder",
                    "Your meeting '" + meeting.getTitle() + "' starts soon.",
                    meeting.getId(),
                    null
            );
        }
    }
}
