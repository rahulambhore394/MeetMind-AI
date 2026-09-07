package com.developer_rahul.meetmind_ai.core.network.token

import kotlinx.coroutines.flow.SharedFlow

interface TokenProvider {
    fun getToken(): String?
    fun saveToken(token: String)
    fun getUserId(): Long
    fun saveUserId(userId: Long)
    fun getUserName(): String?
    fun saveUserName(name: String)
    fun getUserEmail(): String?
    fun saveUserEmail(email: String)
    fun clearToken()
    fun hasToken(): Boolean
    val unauthorizedEvent: SharedFlow<Unit>
    fun notifyUnauthorized()
}
