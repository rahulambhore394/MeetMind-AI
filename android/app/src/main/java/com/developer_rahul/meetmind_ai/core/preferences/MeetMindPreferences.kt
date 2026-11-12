package com.developer_rahul.meetmind_ai.core.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeetMindPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Meeting Defaults
    private val _muteMicOnJoin = MutableStateFlow(prefs.getBoolean(KEY_MUTE_MIC, false))
    val muteMicOnJoin: StateFlow<Boolean> = _muteMicOnJoin.asStateFlow()

    private val _turnOffCameraOnJoin = MutableStateFlow(prefs.getBoolean(KEY_TURN_OFF_CAMERA, false))
    val turnOffCameraOnJoin: StateFlow<Boolean> = _turnOffCameraOnJoin.asStateFlow()

    private val _hdVideoEnabled = MutableStateFlow(prefs.getBoolean(KEY_HD_VIDEO, true))
    val hdVideoEnabled: StateFlow<Boolean> = _hdVideoEnabled.asStateFlow()

    private val _mirrorFrontCamera = MutableStateFlow(prefs.getBoolean(KEY_MIRROR_FRONT_CAM, true))
    val mirrorFrontCamera: StateFlow<Boolean> = _mirrorFrontCamera.asStateFlow()

    private val _noiseCancellation = MutableStateFlow(prefs.getBoolean(KEY_NOISE_CANCELLATION, true))
    val noiseCancellation: StateFlow<Boolean> = _noiseCancellation.asStateFlow()

    // Appearance & Localization
    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, "Dark Neon") ?: "Dark Neon")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "English (US)") ?: "English (US)")
    val language: StateFlow<String> = _language.asStateFlow()

    // Notifications
    private val _meetingReminders = MutableStateFlow(prefs.getBoolean(KEY_MEETING_REMINDERS, true))
    val meetingReminders: StateFlow<Boolean> = _meetingReminders.asStateFlow()

    private val _aiSummaryAlerts = MutableStateFlow(prefs.getBoolean(KEY_AI_SUMMARY_ALERTS, true))
    val aiSummaryAlerts: StateFlow<Boolean> = _aiSummaryAlerts.asStateFlow()

    private val _chatAlerts = MutableStateFlow(prefs.getBoolean(KEY_CHAT_ALERTS, true))
    val chatAlerts: StateFlow<Boolean> = _chatAlerts.asStateFlow()

    private val _soundVibration = MutableStateFlow(prefs.getBoolean(KEY_SOUND_VIBRATION, true))
    val soundVibration: StateFlow<Boolean> = _soundVibration.asStateFlow()

    // Transcription & AI Settings
    private val _autoTranscription = MutableStateFlow(prefs.getBoolean(KEY_AUTO_TRANSCRIPTION, true))
    val autoTranscription: StateFlow<Boolean> = _autoTranscription.asStateFlow()

    private val _transcriptionLanguage = MutableStateFlow(prefs.getString(KEY_TRANSCRIPTION_LANG, "English (US)") ?: "English (US)")
    val transcriptionLanguage: StateFlow<String> = _transcriptionLanguage.asStateFlow()

    private val _speakerDiarization = MutableStateFlow(prefs.getBoolean(KEY_SPEAKER_DIARIZATION, true))
    val speakerDiarization: StateFlow<Boolean> = _speakerDiarization.asStateFlow()

    private val _saveLocalTranscripts = MutableStateFlow(prefs.getBoolean(KEY_SAVE_LOCAL_TRANSCRIPTS, true))
    val saveLocalTranscripts: StateFlow<Boolean> = _saveLocalTranscripts.asStateFlow()

    // AI Representative
    private val _autoJoinWhenBusy = MutableStateFlow(prefs.getBoolean(KEY_AUTO_JOIN_BUSY, true))
    val autoJoinWhenBusy: StateFlow<Boolean> = _autoJoinWhenBusy.asStateFlow()

    private val _strictPrivacyMode = MutableStateFlow(prefs.getBoolean(KEY_STRICT_PRIVACY, false))
    val strictPrivacyMode: StateFlow<Boolean> = _strictPrivacyMode.asStateFlow()

    private val _aiProxyLatency = MutableStateFlow(prefs.getString(KEY_AI_PROXY_LATENCY, "Natural (1.5s)") ?: "Natural (1.5s)")
    val aiProxyLatency: StateFlow<String> = _aiProxyLatency.asStateFlow()

    // Security & Privacy
    private val _passcodeLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_PASSCODE_LOCK, false))
    val passcodeLockEnabled: StateFlow<Boolean> = _passcodeLockEnabled.asStateFlow()

    private val _biometricAuthEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_AUTH, false))
    val biometricAuthEnabled: StateFlow<Boolean> = _biometricAuthEnabled.asStateFlow()

    // Setters
    fun setMuteMicOnJoin(enabled: Boolean) {
        _muteMicOnJoin.value = enabled
        prefs.edit().putBoolean(KEY_MUTE_MIC, enabled).apply()
    }

    fun setTurnOffCameraOnJoin(enabled: Boolean) {
        _turnOffCameraOnJoin.value = enabled
        prefs.edit().putBoolean(KEY_TURN_OFF_CAMERA, enabled).apply()
    }

    fun setHdVideoEnabled(enabled: Boolean) {
        _hdVideoEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HD_VIDEO, enabled).apply()
    }

    fun setMirrorFrontCamera(enabled: Boolean) {
        _mirrorFrontCamera.value = enabled
        prefs.edit().putBoolean(KEY_MIRROR_FRONT_CAM, enabled).apply()
    }

    fun setNoiseCancellation(enabled: Boolean) {
        _noiseCancellation.value = enabled
        prefs.edit().putBoolean(KEY_NOISE_CANCELLATION, enabled).apply()
    }

    fun setTheme(theme: String) {
        _theme.value = theme
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun setLanguage(language: String) {
        _language.value = language
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun setMeetingReminders(enabled: Boolean) {
        _meetingReminders.value = enabled
        prefs.edit().putBoolean(KEY_MEETING_REMINDERS, enabled).apply()
    }

    fun setAiSummaryAlerts(enabled: Boolean) {
        _aiSummaryAlerts.value = enabled
        prefs.edit().putBoolean(KEY_AI_SUMMARY_ALERTS, enabled).apply()
    }

    fun setChatAlerts(enabled: Boolean) {
        _chatAlerts.value = enabled
        prefs.edit().putBoolean(KEY_CHAT_ALERTS, enabled).apply()
    }

    fun setSoundVibration(enabled: Boolean) {
        _soundVibration.value = enabled
        prefs.edit().putBoolean(KEY_SOUND_VIBRATION, enabled).apply()
    }

    fun setAutoTranscription(enabled: Boolean) {
        _autoTranscription.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_TRANSCRIPTION, enabled).apply()
    }

    fun setTranscriptionLanguage(lang: String) {
        _transcriptionLanguage.value = lang
        prefs.edit().putString(KEY_TRANSCRIPTION_LANG, lang).apply()
    }

    fun setSpeakerDiarization(enabled: Boolean) {
        _speakerDiarization.value = enabled
        prefs.edit().putBoolean(KEY_SPEAKER_DIARIZATION, enabled).apply()
    }

    fun setSaveLocalTranscripts(enabled: Boolean) {
        _saveLocalTranscripts.value = enabled
        prefs.edit().putBoolean(KEY_SAVE_LOCAL_TRANSCRIPTS, enabled).apply()
    }

    fun setAutoJoinWhenBusy(enabled: Boolean) {
        _autoJoinWhenBusy.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_JOIN_BUSY, enabled).apply()
    }

    fun setStrictPrivacyMode(enabled: Boolean) {
        _strictPrivacyMode.value = enabled
        prefs.edit().putBoolean(KEY_STRICT_PRIVACY, enabled).apply()
    }

    fun setAiProxyLatency(latency: String) {
        _aiProxyLatency.value = latency
        prefs.edit().putString(KEY_AI_PROXY_LATENCY, latency).apply()
    }

    fun setPasscodeLockEnabled(enabled: Boolean) {
        _passcodeLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PASSCODE_LOCK, enabled).apply()
    }

    fun setBiometricAuthEnabled(enabled: Boolean) {
        _biometricAuthEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BIOMETRIC_AUTH, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "meetmind_app_preferences"

        private const val KEY_MUTE_MIC = "pref_mute_mic_join"
        private const val KEY_TURN_OFF_CAMERA = "pref_turn_off_cam_join"
        private const val KEY_HD_VIDEO = "pref_hd_video"
        private const val KEY_MIRROR_FRONT_CAM = "pref_mirror_front_cam"
        private const val KEY_NOISE_CANCELLATION = "pref_noise_cancellation"

        private const val KEY_THEME = "pref_theme"
        private const val KEY_LANGUAGE = "pref_language"

        private const val KEY_MEETING_REMINDERS = "pref_meeting_reminders"
        private const val KEY_AI_SUMMARY_ALERTS = "pref_ai_summary_alerts"
        private const val KEY_CHAT_ALERTS = "pref_chat_alerts"
        private const val KEY_SOUND_VIBRATION = "pref_sound_vibration"

        private const val KEY_AUTO_TRANSCRIPTION = "pref_auto_transcription"
        private const val KEY_TRANSCRIPTION_LANG = "pref_transcription_lang"
        private const val KEY_SPEAKER_DIARIZATION = "pref_speaker_diarization"
        private const val KEY_SAVE_LOCAL_TRANSCRIPTS = "pref_save_local_transcripts"

        private const val KEY_AUTO_JOIN_BUSY = "pref_auto_join_busy"
        private const val KEY_STRICT_PRIVACY = "pref_strict_privacy"
        private const val KEY_AI_PROXY_LATENCY = "pref_ai_proxy_latency"

        private const val KEY_PASSCODE_LOCK = "pref_passcode_lock"
        private const val KEY_BIOMETRIC_AUTH = "pref_biometric_auth"

        @Volatile
        private var INSTANCE: MeetMindPreferences? = null

        fun getInstance(context: Context): MeetMindPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeetMindPreferences(context).also { INSTANCE = it }
            }
        }
    }
}

