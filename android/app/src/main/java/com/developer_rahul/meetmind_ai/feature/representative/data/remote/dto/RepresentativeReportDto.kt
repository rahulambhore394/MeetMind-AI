package com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RepresentativeReportDto(
    val id: Long,
    val representativeId: Long,
    val ownerId: Long,
    val meetingId: Long,
    val summary: String? = null,
    val importantDiscussionsJson: String? = null,
    val decisionsJson: String? = null,
    val actionItemsJson: String? = null,
    val ownerRelevantQuestionsJson: String? = null,
    val monitoredTopicsFoundJson: String? = null,
    val transcriptReferencesJson: String? = null,
    val attendanceTimelineJson: String? = null,
    val createdAt: String
)
