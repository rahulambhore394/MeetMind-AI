package com.developer_rahul.meetmind_ai.core.webrtc.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class IceServerDto(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)
