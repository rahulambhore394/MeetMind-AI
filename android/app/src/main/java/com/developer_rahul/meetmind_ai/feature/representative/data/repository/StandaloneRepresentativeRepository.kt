package com.developer_rahul.meetmind_ai.feature.representative.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.RepresentativeReport
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StandaloneRepresentativeRepository : LiveRepresentativeRepository {

    private val _representativeEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 16)
    override val representativeEvents: SharedFlow<Map<String, String>> = _representativeEvents.asSharedFlow()

    private val sampleRepresentative = AiRepresentative(
        id = 1L,
        ownerId = 101L,
        meetingId = 101L,
        status = "COMPLETED",
        mediaStoragePath = null,
        monitoredTopics = listOf("Offline Architecture", "Jetpack Compose", "Android Build"),
        monitoredQuestions = listOf("Is backend disconnected?", "Are tests passing?"),
        importantPeople = listOf("Sarah Connor", "Alex Rivera"),
        reportPreferences = "COMPREHENSIVE",
        notificationPreferences = "PUSH",
        consentDisclosure = true,
        startedAt = "2026-09-04 10:00:00",
        endedAt = "2026-09-04 10:30:00",
        createdAt = "2026-09-04 10:00:00"
    )

    private val sampleReport = RepresentativeReport(
        id = 1L,
        representativeId = 1L,
        ownerId = 101L,
        meetingId = 101L,
        summary = "Your AI Representative attended the AI Strategy meeting on your behalf. The team confirmed that the Android app is running 100% offline with standalone repositories.",
        importantDiscussions = listOf(
            "Sarah Connor presented the decoupled AppContainer design.",
            "Alex Rivera verified that zero HTTP requests are sent."
        ),
        decisions = listOf(
            "Backend disconnected completely from Android frontend."
        ),
        actionItems = listOf(
            "Confirm Gradle compilation passes cleanly."
        ),
        ownerRelevantQuestions = listOf(
            "Question: Is backend disconnected? Answer: Yes, all API services are replaced by standalone offline implementations."
        ),
        monitoredTopicsFound = listOf(
            "Offline Architecture (Discussed 3 times)",
            "Jetpack Compose (Discussed 2 times)"
        ),
        transcriptReferences = listOf(
            "[04:18] Sarah: We have disconnected the backend dependencies."
        ),
        attendanceTimeline = mapOf("10:00 AM" to "JOINED", "10:30 AM" to "LEFT"),
        createdAt = "2026-09-04 10:30:00"
    )

    override suspend fun createRepresentative(
        meetingId: Long,
        monitoredTopics: List<String>,
        monitoredQuestions: List<String>,
        importantPeople: List<String>,
        reportPreferences: String?,
        notificationPreferences: String?
    ): NetworkResult<AiRepresentative> {
        val created = sampleRepresentative.copy(
            meetingId = meetingId,
            monitoredTopics = monitoredTopics,
            monitoredQuestions = monitoredQuestions,
            importantPeople = importantPeople,
            reportPreferences = reportPreferences ?: "COMPREHENSIVE",
            notificationPreferences = notificationPreferences ?: "PUSH",
            status = "ACTIVE"
        )
        return NetworkResult.Success(created)
    }

    override suspend fun getRepresentative(meetingId: Long): NetworkResult<AiRepresentative> {
        return NetworkResult.Success(sampleRepresentative.copy(meetingId = meetingId))
    }

    override suspend fun cancelRepresentative(
        meetingId: Long,
        representativeId: Long
    ): NetworkResult<AiRepresentative> {
        return NetworkResult.Success(sampleRepresentative.copy(meetingId = meetingId, status = "CANCELLED"))
    }

    override suspend fun getReport(
        meetingId: Long,
        representativeId: Long
    ): NetworkResult<RepresentativeReport> {
        return NetworkResult.Success(sampleReport.copy(meetingId = meetingId, representativeId = representativeId))
    }

    override suspend fun uploadMedia(
        meetingId: Long,
        representativeId: Long,
        mediaStoragePath: String
    ): NetworkResult<AiRepresentative> {
        return NetworkResult.Success(sampleRepresentative.copy(meetingId = meetingId, mediaStoragePath = mediaStoragePath))
    }
}
