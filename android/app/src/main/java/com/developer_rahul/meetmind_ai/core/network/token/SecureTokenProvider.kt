package com.developer_rahul.meetmind_ai.core.network.token

import android.content.Context
import android.content.SharedPreferences
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

    private val sharedPreferences: SharedPreferences = createEncryptedSharedPreferences(context)

    companion object {
        private const val PREFS_NAME = "meetmind_secure_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"

        private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                context.deleteSharedPreferences(PREFS_NAME)
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
        }
    }

    override fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    override fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun getUserId(): Long {
        val savedId = sharedPreferences.getLong(KEY_USER_ID, -1L)
        if (savedId != -1L) return savedId

        val token = getToken() ?: return -1L
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(
                    android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING),
                    Charsets.UTF_8
                )
                val jsonObject = org.json.JSONObject(payloadJson)
                val sub = jsonObject.optString("sub")
                val parsedId = sub.toLongOrNull() ?: -1L
                if (parsedId != -1L) {
                    saveUserId(parsedId)
                }
                parsedId
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    override fun saveUserId(userId: Long) {
        sharedPreferences.edit().putLong(KEY_USER_ID, userId).apply()
    }

    override fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    override fun saveUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }

    override fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    override fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    override fun clearToken() {
        sharedPreferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    override fun hasToken(): Boolean {
        return getToken() != null
    }
}
