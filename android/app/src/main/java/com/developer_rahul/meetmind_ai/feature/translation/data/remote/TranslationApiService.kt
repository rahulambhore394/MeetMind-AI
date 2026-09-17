package com.developer_rahul.meetmind_ai.feature.translation.data.remote

import com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LiveTranslationDto
import retrofit2.http.*

interface TranslationApiService {

    @GET("api/meetings/{meetingId}/translations")
    suspend fun getTranslations(
        @Path("meetingId") meetingId: Long,
        @Query("targetLanguage") targetLanguage: String? = null
    ): List<LiveTranslationDto>

    @POST("api/meetings/{meetingId}/translations/preference")
    suspend fun setLanguagePreference(
        @Path("meetingId") meetingId: Long,
        @Body body: Map<String, String>
    ): Map<String, String>

    @POST("api/meetings/{meetingId}/translations/live")
    suspend fun translateLive(
        @Path("meetingId") meetingId: Long,
        @Body body: com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto.LiveTranslateRequestDto
    ): List<LiveTranslationDto>
}
