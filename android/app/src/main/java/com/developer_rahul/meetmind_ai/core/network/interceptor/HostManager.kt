package com.developer_rahul.meetmind_ai.core.network.interceptor

import android.util.Log

object HostManager {
    @Volatile
    var activeHost: String? = null
        private set

    fun setActiveHost(host: String) {
        if (activeHost != host) {
            Log.i("HostManager", "Switched active network host to: $host")
            activeHost = host
        }
    }

    fun clearActiveHost() {
        activeHost = null
    }
}
