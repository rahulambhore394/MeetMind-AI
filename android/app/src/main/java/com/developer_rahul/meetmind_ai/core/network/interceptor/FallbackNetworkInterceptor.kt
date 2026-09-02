package com.developer_rahul.meetmind_ai.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class FallbackNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            val currentHost = request.url.host
            val fallbackHosts = listOf("10.70.44.195", "192.168.137.1", "127.0.0.1", "10.0.2.2")
                .filter { it != currentHost }

            for (fallbackHost in fallbackHosts) {
                try {
                    val newUrl = request.url.newBuilder()
                        .host(fallbackHost)
                        .build()
                    val newRequest = request.newBuilder().url(newUrl).build()
                    return chain.proceed(newRequest)
                } catch (_: IOException) {
                    // Try next fallback host if current one fails
                }
            }
            throw e
        }
    }
}
