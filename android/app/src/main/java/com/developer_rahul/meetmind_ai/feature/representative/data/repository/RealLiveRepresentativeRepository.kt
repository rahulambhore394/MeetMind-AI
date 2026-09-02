package com.developer_rahul.meetmind_ai.feature.representative.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.RepresentativeApiService
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.AiRepresentativeDto
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.CreateRepresentativeRequestDto
import com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto.RepresentativeReportDto
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.RepresentativeReport
import com.developer_rahul.meetmind_ai.feature.representative.domain.repository.LiveRepresentativeRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json

class RealLiveRepresentativeRepository(
    private val apiService: RepresentativeApiService,
    private val webSocketManager: MeetingWebSocketManager
) : LiveRepresentativeRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val representativeEvents: SharedFlow<Map<String, String>> = webSocketManager.recordingEvents

    override suspend fun createRepresentative(
        meetingId: Long,
        monitoredTopics: List<String>,
        monitoredQuestions: List<String>,
        importantPeople: List<String>,
        reportPreferences: String?,
        notificationPreferences: String?
    ): NetworkResult<AiRepresentative> {
        return try {
            val request = CreateRepresentativeRequestDto(
                monitoredTopics, monitoredQuestions, importantPeople, 
                reportPreferences, notificationPreferences
            )
            val response = apiService.createRepresentative(meetingId, request)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getRepresentative(meetingId: Long): NetworkResult<AiRepresentative> {
        return try {
            val response = apiService.getRepresentative(meetingId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun cancelRepresentative(meetingId: Long, representativeId: Long): NetworkResult<AiRepresentative> {
        return try {
            val response = apiService.cancelRepresentative(meetingId, representativeId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getReport(meetingId: Long, representativeId: Long): NetworkResult<RepresentativeReport> {
        return try {
            val response = apiService.getReport(meetingId, representativeId)
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun uploadMedia(meetingId: Long, representativeId: Long, mediaStoragePath: String): NetworkResult<AiRepresentative> {
        return try {
            val response = apiService.uploadMedia(meetingId, representativeId, mapOf("mediaStoragePath" to mediaStoragePath))
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    private fun AiRepresentativeDto.toDomain(): AiRepresentative {
        return AiRepresentative(
            id = id,
            ownerId = ownerId,
            meetingId = meetingId,
            status = status,
            mediaStoragePath = mediaStoragePath,
            monitoredTopics = fromJsonList(monitoredTopicsJson),
            monitoredQuestions = fromJsonList(monitoredQuestionsJson),
            importantPeople = fromJsonList(importantPeopleJson),
            reportPreferences = reportPreferencesJson ?: "FULL_REPORT",
            notificationPreferences = notificationPreferencesJson ?: "IMMEDIATE",
            consentDisclosure = consentDisclosure,
            startedAt = startedAt,
            endedAt = endedAt,
            createdAt = createdAt
        )
    }

    private fun RepresentativeReportDto.toDomain(): RepresentativeReport {
        return RepresentativeReport(
            id = id,
            representativeId = representativeId,
            ownerId = ownerId,
            meetingId = meetingId,
            summary = summary ?: "",
            importantDiscussions = fromJsonList(importantDiscussionsJson),
            decisions = fromJsonList(decisionsJson),
            actionItems = fromJsonList(actionItemsJson),
            ownerRelevantQuestions = fromJsonList(ownerRelevantQuestionsJson),
            monitoredTopicsFound = fromJsonList(monitoredTopicsFoundJson),
            transcriptReferences = fromJsonList(transcriptReferencesJson),
            attendanceTimeline = fromJsonMap(attendanceTimelineJson),
            createdAt = createdAt
        )
    }

    private fun fromJsonList(jsonStr: String?): List<String> {
        if (jsonStr == null || jsonStr.isEmpty() || jsonStr == "[]") return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fromJsonMap(jsonStr: String?): Map<String, String> {
        if (jsonStr == null || jsonStr.isEmpty() || jsonStr == "{}") return emptyMap()
        return try {
            json.decodeFromString<Map<String, String>>(jsonStr)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
