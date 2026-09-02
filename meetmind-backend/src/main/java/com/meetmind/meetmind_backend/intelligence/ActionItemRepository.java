package com.meetmind.meetmind_backend.intelligence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    List<ActionItem> findByMeetingId(Long meetingId);

    List<ActionItem> findByMeetingSummaryId(Long meetingSummaryId);

    void deleteByMeetingSummaryId(Long meetingSummaryId);
}
