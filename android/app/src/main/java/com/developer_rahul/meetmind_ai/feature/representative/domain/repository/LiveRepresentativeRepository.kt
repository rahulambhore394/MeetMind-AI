package com.developer_rahul.meetmind_ai.feature.representative.domain.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.AiRepresentative
import com.developer_rahul.meetmind_ai.feature.representative.domain.model.RepresentativeReport
import kotlinx.coroutines.flow.SharedFlow

interface LiveRepresentativeRepository {
    val representativeEvents: SharedFlow<Map<String, String>>
    
    suspend fun createRepresentative(
        meetingId: Long,
        monitoredTopics: List<String>,
        monitoredQuestions: List<String>,
        importantPeople: List<String>,
        reportPreferences: String? = null,
        notificationPreferences: String? = null
    ): NetworkResult<AiRepresentative>

    suspend fun getRepresentative(meetingId: Long): NetworkResult<AiRepresentative>
    suspend fun cancelRepresentative(meetingId: Long, representativeId: Long): NetworkResult<AiRepresentative>
    suspend fun getReport(meetingId: Long, representativeId: Long): NetworkResult<RepresentativeReport>
    suspend fun uploadMedia(meetingId: Long, representativeId: Long, mediaStoragePath: String): NetworkResult<AiRepresentative>
}
