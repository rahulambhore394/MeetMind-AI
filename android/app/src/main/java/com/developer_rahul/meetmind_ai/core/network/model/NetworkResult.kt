package com.developer_rahul.meetmind_ai.core.network.model

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val error: MeetMindError) : NetworkResult<Nothing>
    object Loading : NetworkResult<Nothing>
}
