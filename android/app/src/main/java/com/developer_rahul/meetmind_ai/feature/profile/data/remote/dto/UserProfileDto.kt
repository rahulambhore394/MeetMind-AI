package com.developer_rahul.meetmind_ai.feature.profile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
    val createdAt: String? = null
)
