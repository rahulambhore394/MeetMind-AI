package com.meetmind.meetmind_backend.representative;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiRepresentativeRepository extends JpaRepository<AiRepresentative, Long> {

    Optional<AiRepresentative> findByMeetingIdAndOwnerId(Long meetingId, Long ownerId);

    List<AiRepresentative> findByMeetingId(Long meetingId);

    List<AiRepresentative> findByMeetingIdAndStatus(Long meetingId, RepresentativeStatus status);

    List<AiRepresentative> findByOwnerId(Long ownerId);
}
