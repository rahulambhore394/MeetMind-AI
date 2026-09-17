package com.developer_rahul.meetmind_ai.feature.translation.data.repository

import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LiveTranslationDto
import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.domain.repository.LiveTranslationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

import com.developer_rahul.meetmind_ai.feature.translation.data.remote.TranslationApiService

class RealLiveTranslationRepository(
    private val apiService: TranslationApiService,
    private val webSocketManager: MeetingWebSocketManager
) : LiveTranslationRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _subtitles = MutableSharedFlow<Subtitle>(replay = 1, extraBufferCapacity = 64)
    override val subtitles: SharedFlow<Subtitle> = _subtitles.asSharedFlow()

    init {
        scope.launch {
            webSocketManager.translations.collect { dto ->
                _subtitles.emit(dto.toDomain())
            }
        }
    }

    override suspend fun getTranslations(meetingId: Long, targetLanguage: String?): List<Subtitle> {
        return try {
            val dtoList = apiService.getTranslations(meetingId, targetLanguage)
            dtoList.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun setLanguagePreference(meetingId: Long, targetLanguage: String) {
        try {
            apiService.setLanguagePreference(meetingId, mapOf("targetLanguage" to targetLanguage))
        } catch (_: Exception) {}
        webSocketManager.sendLanguagePreference(meetingId, targetLanguage)
    }

    override suspend fun sendLiveSpeech(meetingId: Long, text: String, sourceLanguage: String) {
        webSocketManager.sendTranslationRequest(meetingId, text, sourceLanguage)
    }

    override fun subscribeToSubtitles(meetingId: Long, targetLanguage: String) {
        webSocketManager.subscribeToTranslation(meetingId, targetLanguage)
    }

    override fun unsubscribeFromSubtitles(meetingId: Long, targetLanguage: String) {
        webSocketManager.unsubscribeFromTranslation(meetingId, targetLanguage)
    }

    private fun LiveTranslationDto.toDomain(): Subtitle {
        return Subtitle(
            id = id,
            meetingId = meetingId,
            speaker = speaker ?: "Unknown",
            originalText = sourceText,
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            timestamp = timestamp
        )
    }
}
