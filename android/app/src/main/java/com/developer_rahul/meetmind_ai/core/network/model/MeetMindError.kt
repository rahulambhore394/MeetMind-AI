package com.developer_rahul.meetmind_ai.core.network.model

data class MeetMindError(
    val type: ErrorType,
    val message: String? = null,
    val code: Int? = null
)

enum class ErrorType {
    NETWORK_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    VALIDATION_ERROR,
    SERVER_ERROR,
    TIMEOUT,
    UNKNOWN_ERROR
}
