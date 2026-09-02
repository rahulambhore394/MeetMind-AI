package com.developer_rahul.meetmind_ai.core.network.token

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SecureTokenProvider(context: Context) : TokenProvider {

    private val _unauthorizedEvent = MutableSharedFlow<Unit>()
    override val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent.asSharedFlow()

    override fun notifyUnauthorized() {
        _unauthorizedEvent.tryEmit(Unit)
    }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "meetmind_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    override fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun getUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1L)
    }

    override fun saveUserId(userId: Long) {
        sharedPreferences.edit().putLong(KEY_USER_ID, userId).apply()
    }

    override fun clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).remove(KEY_USER_ID).apply()
    }

    override fun hasToken(): Boolean {
        return getToken() != null
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
    }
}
