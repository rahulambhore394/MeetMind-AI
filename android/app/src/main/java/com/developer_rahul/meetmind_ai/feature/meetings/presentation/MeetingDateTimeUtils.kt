package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class FormattedMeetingTime(
    val dateText: String,
    val timeText: String,
    val relativeBadge: String,
    val isToday: Boolean,
    val isPast: Boolean,
    val localDate: LocalDate?
)

object MeetingDateTimeUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun parseMeetingTime(scheduledAt: String?): FormattedMeetingTime {
        if (scheduledAt.isNullOrBlank()) {
            return FormattedMeetingTime(
                dateText = "Not scheduled",
                timeText = "",
                relativeBadge = "Unscheduled",
                isToday = false,
                isPast = false,
                localDate = null
            )
        }

        try {
            val ldt: LocalDateTime = when {
                scheduledAt.endsWith("Z") -> {
                    val instant = Instant.parse(scheduledAt)
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                }
                scheduledAt.contains("T") -> {
                    val cleaned = if (scheduledAt.length > 19) scheduledAt.substring(0, 19) else scheduledAt
                    LocalDateTime.parse(cleaned)
                }
                else -> {
                    // Try pattern "yyyy-MM-dd HH:mm:ss"
                    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    LocalDateTime.parse(scheduledAt, fmt)
                }
            }

            val now = LocalDateTime.now()
            val meetingDate = ldt.toLocalDate()
            val today = LocalDate.now()
            val isToday = meetingDate == today
            val isTomorrow = meetingDate == today.plusDays(1)
            val isYesterday = meetingDate == today.minusDays(1)
            val isPast = ldt.isBefore(now)

            val dateText = when {
                isToday -> "Today"
                isTomorrow -> "Tomorrow"
                isYesterday -> "Yesterday"
                meetingDate.year == today.year -> ldt.format(dateFormatter)
                else -> ldt.format(fullDateFormatter)
            }

            val timeText = ldt.format(timeFormatter)

            val relativeBadge = when {
                isPast && ChronoUnit.HOURS.between(ldt, now) < 2 -> "Just ended"
                isPast -> "Ended"
                ChronoUnit.MINUTES.between(now, ldt) in 0..15 -> "Starts in ${ChronoUnit.MINUTES.between(now, ldt)}m"
                ChronoUnit.HOURS.between(now, ldt) in 0..2 -> "Starts in ${ChronoUnit.HOURS.between(now, ldt)}h"
                isToday -> "Today at $timeText"
                isTomorrow -> "Tomorrow at $timeText"
                else -> dateText
            }

            return FormattedMeetingTime(
                dateText = dateText,
                timeText = timeText,
                relativeBadge = relativeBadge,
                isToday = isToday,
                isPast = isPast,
                localDate = meetingDate
            )
        } catch (_: Exception) {
            // Graceful fallback for non-ISO custom formats
            return FormattedMeetingTime(
                dateText = scheduledAt,
                timeText = "",
                relativeBadge = scheduledAt,
                isToday = scheduledAt.contains("Today", ignoreCase = true),
                isPast = false,
                localDate = null
            )
        }
    }

    fun matchesDate(scheduledAt: String?, targetDate: LocalDate): Boolean {
        if (scheduledAt.isNullOrBlank()) return false
        val parsed = parseMeetingTime(scheduledAt)
        if (parsed.localDate != null) {
            return parsed.localDate == targetDate
        }

        // Substring fallback
        val iso = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
        val dayMonth = targetDate.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
        val monthDay = targetDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        val altDay = targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
        return scheduledAt.contains(iso) || 
               scheduledAt.contains(dayMonth, ignoreCase = true) || 
               scheduledAt.contains(monthDay, ignoreCase = true) ||
               scheduledAt.contains(altDay)
    }
}

