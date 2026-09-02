package com.developer_rahul.meetmind_ai.feature.representative.domain.model

data class RepresentativeReport(
    val id: Long,
    val representativeId: Long,
    val ownerId: Long,
    val meetingId: Long,
    val summary: String,
    val importantDiscussions: List<String>,
    val decisions: List<String>,
    val actionItems: List<String>,
    val ownerRelevantQuestions: List<String>,
    val monitoredTopicsFound: List<String>,
    val transcriptReferences: List<String>,
    val attendanceTimeline: Map<String, String>,
    val createdAt: String
)
