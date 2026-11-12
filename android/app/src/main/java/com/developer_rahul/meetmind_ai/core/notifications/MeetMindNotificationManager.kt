package com.developer_rahul.meetmind_ai.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.developer_rahul.meetmind_ai.MainActivity
import com.developer_rahul.meetmind_ai.R
import com.developer_rahul.meetmind_ai.core.preferences.MeetMindPreferences
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification

object MeetMindNotificationManager {

    private const val TAG = "MeetMindNotifications"

    const val CHANNEL_MEETINGS = "meetmind_meetings_channel"
    const val CHANNEL_AI_INSIGHTS = "meetmind_ai_channel"
    const val CHANNEL_CHAT = "meetmind_chat_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Meetings & Calls Channel (High Priority)
            val meetingChannel = NotificationChannel(
                CHANNEL_MEETINGS,
                "Meetings & Call Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call invites, meeting start alerts, and schedule reminders"
                enableLights(true)
                lightColor = AndroidColor.parseColor("#06B6D4") // Neon Cyan
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            // 2. AI Intelligence & Summaries Channel (Default Priority)
            val aiChannel = NotificationChannel(
                CHANNEL_AI_INSIGHTS,
                "AI Intelligence & Summaries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AI action item summaries, transcripts, and digital twin reports"
                enableLights(true)
                lightColor = AndroidColor.parseColor("#6366F1") // Electric Indigo
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
            }

            // 3. Chat Messages Channel (High Priority)
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT,
                "Meeting Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time in-meeting chat and user mentions"
                enableLights(true)
                lightColor = AndroidColor.parseColor("#10B981") // Emerald Green
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 80, 100)
            }

            notificationManager.createNotificationChannels(listOf(meetingChannel, aiChannel, chatChannel))
            Log.d(TAG, "Notification channels initialized successfully")
        }
    }

    fun showSystemNotification(context: Context, notification: Notification) {
        val prefs = MeetMindPreferences.getInstance(context)

        // Respect user settings
        when (notification.type) {
            "MEETING_INVITATION", "MEETING_STARTED", "MEETING_REMINDER" -> {
                if (!prefs.meetingReminders.value) {
                    Log.d(TAG, "Meeting notifications disabled by user preferences")
                    return
                }
            }
            "AI_SUMMARY_READY", "TRANSCRIPT_READY", "ACTION_ITEM_ASSIGNED",
            "REPRESENTATIVE_ACTIVE", "REPRESENTATIVE_REPORT_READY" -> {
                if (!prefs.aiSummaryAlerts.value) {
                    Log.d(TAG, "AI summary notifications disabled by user preferences")
                    return
                }
            }
            "NEW_MESSAGE", "CHAT_MESSAGE_SENT" -> {
                if (!prefs.chatAlerts.value) {
                    Log.d(TAG, "Chat notifications disabled by user preferences")
                    return
                }
            }
        }

        val channelId = when (notification.type) {
            "MEETING_INVITATION", "MEETING_STARTED", "MEETING_REMINDER" -> CHANNEL_MEETINGS
            "NEW_MESSAGE", "CHAT_MESSAGE_SENT" -> CHANNEL_CHAT
            else -> CHANNEL_AI_INSIGHTS
        }

        // Tap Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("meetingId", notification.relatedMeetingId?.toString())
            putExtra("type", notification.type)
            putExtra("representativeId", notification.relatedRepresentativeId?.toString())
            putExtra("notificationId", notification.id.toString())
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, notification.id.toInt(), intent, flags)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setColor(AndroidColor.parseColor("#6366F1")) // Electric Indigo
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (prefs.soundVibration.value) {
            builder.setSound(soundUri)
            builder.setVibrate(longArrayOf(0, 200, 100, 200))
        }

        // Action Buttons
        if (notification.relatedMeetingId != null) {
            val actionTitle = if (notification.type == "MEETING_STARTED" || notification.type == "MEETING_INVITATION") {
                "Join Call 🚀"
            } else if (notification.type == "AI_SUMMARY_READY") {
                "View Summary ✨"
            } else {
                "Open Meeting"
            }

            builder.addAction(R.mipmap.ic_launcher, actionTitle, pendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = if (notification.id > 0) notification.id.toInt() else (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Posted system notification: [${notification.type}] ${notification.title}")
    }
}

