package com.developer_rahul.meetmind_ai.feature.translation.domain.repository

import com.developer_rahul.meetmind_ai.feature.translation.domain.model.Subtitle
import kotlinx.coroutines.flow.SharedFlow

interface LiveTranslationRepository {
    val subtitles: SharedFlow<Subtitle>
    
    suspend fun setLanguagePreference(meetingId: Long, targetLanguage: String)
    suspend fun sendLiveSpeech(meetingId: Long, text: String, sourceLanguage: String)
    fun subscribeToSubtitles(meetingId: Long, targetLanguage: String)
    fun unsubscribeFromSubtitles(meetingId: Long, targetLanguage: String)
}
