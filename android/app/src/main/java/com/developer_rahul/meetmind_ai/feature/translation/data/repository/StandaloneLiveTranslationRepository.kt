package com.developer_rahul.meetmind_ai.feature.translation.data.repository

import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import com.developer_rahul.meetmind_ai.feature.translation.domain.repository.LiveTranslationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StandaloneLiveTranslationRepository : LiveTranslationRepository {

    private val _subtitles = MutableSharedFlow<Subtitle>(extraBufferCapacity = 16)
    override val subtitles: SharedFlow<Subtitle> = _subtitles.asSharedFlow()

    override suspend fun getTranslations(meetingId: Long, targetLanguage: String?): List<Subtitle> {
        return emptyList()
    }

    override suspend fun setLanguagePreference(meetingId: Long, targetLanguage: String) {
        // No-op for standalone offline mode
    }

    override suspend fun sendLiveSpeech(meetingId: Long, text: String, sourceLanguage: String) {
        val mockSubtitle = Subtitle(
            id = System.currentTimeMillis(),
            meetingId = meetingId,
            speaker = "Rahul Ambhore",
            originalText = text,
            translatedText = "[Offline Translation]: $text",
            sourceLanguage = sourceLanguage,
            targetLanguage = "hi",
            timestamp = System.currentTimeMillis()
        )
        _subtitles.tryEmit(mockSubtitle)
    }

    override fun subscribeToSubtitles(meetingId: Long, targetLanguage: String) {
        // No-op for standalone offline mode
    }

    override fun unsubscribeFromSubtitles(meetingId: Long, targetLanguage: String) {
        // No-op for standalone offline mode
    }
}
