package com.developer_rahul.meetmind_ai.core.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class FallbackNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHost = originalRequest.url.host

        if (originalHost.contains("onrender.com")) {
            return chain.proceed(originalRequest)
        }

        // 1. If an active host was already validated and is working, use it directly (0ms failover delay!)
        val active = HostManager.activeHost
        if (active != null && active != originalHost) {
            val isHttpsDomain = active.contains("loca.lt") || active.contains("onrender.com")
            val builder = originalRequest.url.newBuilder().host(active)
            if (isHttpsDomain) {
                builder.scheme("https").port(443)
            } else {
                builder.scheme("http").port(8080)
            }
            val fastUrl = builder.build()
            val fastRequest = originalRequest.newBuilder().url(fastUrl).build()
            try {
                return chain.proceed(fastRequest)
            } catch (e: IOException) {
                Log.w("FallbackNetwork", "Active host $active failed (${e.message}), clearing and trying candidates...")
                HostManager.clearActiveHost()
            }
        }

        // 2. Try the primary request
        try {
            val response = chain.proceed(originalRequest)
            HostManager.setActiveHost(originalHost)
            return response
        } catch (e: IOException) {
            Log.w("FallbackNetwork", "Primary host $originalHost failed: ${e.message}")
        }

        // 3. Fallback candidates: 10.232.5.113 (Wi-Fi), 127.0.0.1 (USB adb reverse), 10.0.2.2 (Emulator)
        val fallbackHosts = listOf("10.232.5.113", "127.0.0.1", "10.0.2.2")
            .filter { it != originalHost }

        for (fallbackHost in fallbackHosts) {
            try {
                val newUrl = originalRequest.url.newBuilder()
                    .scheme("http")
                    .host(fallbackHost)
                    .port(8080)
                    .build()
                val newRequest = originalRequest.newBuilder().url(newUrl).build()
                val response = chain.proceed(newRequest)
                HostManager.setActiveHost(fallbackHost)
                return response
            } catch (_: IOException) {
                // Try next fallback host
            }
        }

        throw IOException("Failed to connect to $originalHost or local fallback hosts (10.232.5.113, 127.0.0.1).")
    }
}
