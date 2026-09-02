package com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)
