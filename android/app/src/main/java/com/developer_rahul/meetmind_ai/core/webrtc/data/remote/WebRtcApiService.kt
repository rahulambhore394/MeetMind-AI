package com.developer_rahul.meetmind_ai.core.webrtc.data.remote

import com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto.IceServerDto
import retrofit2.http.GET

interface WebRtcApiService {
    @GET("api/webrtc/ice-servers")
    suspend fun getIceServers(): List<IceServerDto>
}
