package com.developer_rahul.meetmind_ai.core.network.interceptor

import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthInterceptorTest {

    private val tokenProvider = mockk<TokenProvider>()
    private val authInterceptor = AuthInterceptor(tokenProvider)

    @Test
    fun `when token exists, it is added to the header`() {
        // Arrange
        val token = "test_token"
        every { tokenProvider.getToken() } returns token
        
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val interceptedRequest = firstArg<Request>()
            Response.Builder()
                .request(interceptedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        // Act
        authInterceptor.intercept(chain)

        // Assert
        verify {
            chain.proceed(withArg {
                assertEquals("Bearer $token", it.header("Authorization"))
            })
        }
    }

    @Test
    fun `when token does not exist, Authorization header is not added`() {
        // Arrange
        every { tokenProvider.getToken() } returns null
        
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val interceptedRequest = firstArg<Request>()
            Response.Builder()
                .request(interceptedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        // Act
        authInterceptor.intercept(chain)

        // Assert
        verify {
            chain.proceed(withArg {
                assertEquals(null, it.header("Authorization"))
            })
        }
    }
}
