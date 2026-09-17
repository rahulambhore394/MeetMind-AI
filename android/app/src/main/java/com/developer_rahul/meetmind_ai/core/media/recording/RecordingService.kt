package com.developer_rahul.meetmind_ai.core.media.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.developer_rahul.meetmind_ai.R
import com.developer_rahul.meetmind_ai.MeetMindApplication

class RecordingService : Service() {

    private val CHANNEL_ID = "RecordingServiceChannel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        val isAudioOnly = intent?.getBooleanExtra(EXTRA_AUDIO_ONLY, false) ?: false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (isAudioOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Meeting Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meeting Recording")
            .setContentText("Recording in progress...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_AUDIO_ONLY = "extra_audio_only"

        fun startService(context: Context, isAudioOnly: Boolean = false) {
            val startIntent = Intent(context, RecordingService::class.java).apply {
                putExtra(EXTRA_AUDIO_ONLY, isAudioOnly)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, RecordingService::class.java)
            context.stopService(stopIntent)
        }
    }
}
