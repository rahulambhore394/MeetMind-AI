package com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String,
    val userId: Long,
    val name: String,
    val email: String
)
