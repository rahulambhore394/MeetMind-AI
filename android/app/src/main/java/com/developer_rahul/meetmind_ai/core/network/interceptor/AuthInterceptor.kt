package com.developer_rahul.meetmind_ai.core.network.interceptor

import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider.getToken()

        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())
        
        if (response.code == 401) {
            tokenProvider.notifyUnauthorized()
        }
        
        return response
    }
}
