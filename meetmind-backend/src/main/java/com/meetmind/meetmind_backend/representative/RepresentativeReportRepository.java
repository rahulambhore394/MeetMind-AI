package com.meetmind.meetmind_backend.representative;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RepresentativeReportRepository extends JpaRepository<RepresentativeReport, Long> {

    Optional<RepresentativeReport> findByRepresentativeId(Long representativeId);

    Optional<RepresentativeReport> findByMeetingIdAndOwnerId(Long meetingId, Long ownerId);
}
