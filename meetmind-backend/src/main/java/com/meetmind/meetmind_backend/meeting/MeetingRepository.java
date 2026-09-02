package com.meetmind.meetmind_backend.meeting;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository
        extends JpaRepository<Meeting, Long> {

    List<Meeting> findByHostId(Long hostId);

    List<Meeting> findByStatus(MeetingStatus status);

    java.util.Optional<Meeting> findByMeetingCode(String meetingCode);
}
