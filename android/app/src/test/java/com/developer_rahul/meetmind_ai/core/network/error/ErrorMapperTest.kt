package com.developer_rahul.meetmind_ai.core.network.error

import com.developer_rahul.meetmind_ai.core.network.model.ErrorType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ErrorMapperTest {

    @Test
    fun `map SocketTimeoutException to TIMEOUT`() {
        val error = ErrorMapper.mapToMeetMindError(SocketTimeoutException())
        assertEquals(ErrorType.TIMEOUT, error.type)
    }

    @Test
    fun `map IOException to NETWORK_ERROR`() {
        val error = ErrorMapper.mapToMeetMindError(IOException())
        assertEquals(ErrorType.NETWORK_ERROR, error.type)
    }

    @Test
    fun `map 401 HttpException to UNAUTHORIZED`() {
        val response = Response.error<Any>(401, "".toResponseBody(null))
        val error = ErrorMapper.mapToMeetMindError(HttpException(response))
        assertEquals(ErrorType.UNAUTHORIZED, error.type)
    }

    @Test
    fun `map 403 HttpException to FORBIDDEN`() {
        val response = Response.error<Any>(403, "".toResponseBody(null))
        val error = ErrorMapper.mapToMeetMindError(HttpException(response))
        assertEquals(ErrorType.FORBIDDEN, error.type)
    }

    @Test
    fun `map 404 HttpException to NOT_FOUND`() {
        val response = Response.error<Any>(404, "".toResponseBody(null))
        val error = ErrorMapper.mapToMeetMindError(HttpException(response))
        assertEquals(ErrorType.NOT_FOUND, error.type)
    }

    @Test
    fun `map 500 HttpException to SERVER_ERROR`() {
        val response = Response.error<Any>(500, "".toResponseBody(null))
        val error = ErrorMapper.mapToMeetMindError(HttpException(response))
        assertEquals(ErrorType.SERVER_ERROR, error.type)
    }
}
