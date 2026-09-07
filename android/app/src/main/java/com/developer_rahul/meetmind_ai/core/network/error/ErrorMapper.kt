package com.developer_rahul.meetmind_ai.core.network.error

import com.developer_rahul.meetmind_ai.core.network.model.ErrorType
import com.developer_rahul.meetmind_ai.core.network.model.MeetMindError
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

object ErrorMapper {
    fun mapToMeetMindError(throwable: Throwable): MeetMindError {
        return when (throwable) {
            is SocketTimeoutException -> MeetMindError(ErrorType.TIMEOUT, "Request timed out")
            is IOException -> MeetMindError(ErrorType.NETWORK_ERROR, "Network Error: ${throwable.message}")
            is HttpException -> {
                val type = when (throwable.code()) {
                    401 -> ErrorType.UNAUTHORIZED
                    403 -> ErrorType.FORBIDDEN
                    404 -> ErrorType.NOT_FOUND
                    in 400..499 -> ErrorType.VALIDATION_ERROR
                    in 500..599 -> ErrorType.SERVER_ERROR
                    else -> ErrorType.UNKNOWN_ERROR
                }
                val errorMessage = extractErrorMessage(throwable) ?: throwable.message()
                MeetMindError(type, errorMessage, throwable.code())
            }
            else -> MeetMindError(ErrorType.UNKNOWN_ERROR, throwable.localizedMessage ?: "An unexpected error occurred")
        }
    }

    private fun extractErrorMessage(httpException: HttpException): String? {
        return try {
            val errorJson = httpException.response()?.errorBody()?.string()
            if (!errorJson.isNullOrBlank()) {
                val jsonObject = JSONObject(errorJson)
                if (jsonObject.has("message")) {
                    jsonObject.getString("message")
                } else if (jsonObject.has("error")) {
                    jsonObject.getString("error")
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
