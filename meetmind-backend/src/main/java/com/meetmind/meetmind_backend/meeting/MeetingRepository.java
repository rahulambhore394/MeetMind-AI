package com.meetmind.meetmind_backend.meeting;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository
        extends JpaRepository<Meeting, Long> {

    @EntityGraph(attributePaths = {"host"})
    List<Meeting> findByHostId(Long hostId);

    @EntityGraph(attributePaths = {"host"})
    List<Meeting> findByStatus(MeetingStatus status);

    @EntityGraph(attributePaths = {"host"})
    Optional<Meeting> findByMeetingCode(String meetingCode);

    @Override
    @EntityGraph(attributePaths = {"host"})
    Optional<Meeting> findById(Long id);
}
