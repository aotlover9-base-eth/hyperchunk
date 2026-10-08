package com.hyperchunk.app.data.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class ChunkCategory(
    val displayName: String,
    val defaultColor: String
) {
    SLEEP("Sleep", "#6366F1"),
    ROUTINE("Routine", "#10B981"),
    STUDY("Study / Lecture", "#3B82F6"),
    WORK("Deep Work", "#F59E0B"),
    MEAL("Meals & Food", "#FB923C"),
    REST("Rest & Free", "#8B5CF6"),
    FITNESS("Fitness / Gym", "#EF4444"),
    CUSTOM("Custom", "#06B6D4")
}

data class TimeChunk(
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val title: String,
    val startMinuteOfDay: Int, // 0..1439 (e.g. 7:30 = 450)
    val endMinuteOfDay: Int,   // 1..1440 (e.g. 10:00 = 600)
    val category: ChunkCategory = ChunkCategory.CUSTOM,
    val colorHex: String = category.defaultColor,
    val hasAlarm: Boolean = false,
    val notes: String = ""
) {
    val startTimeFormatted: String
        get() {
            val h = (startMinuteOfDay / 60) % 24
            val m = startMinuteOfDay % 60
            return String.format("%02d:%02d", h, m)
        }

    val endTimeFormatted: String
        get() {
            val h = (endMinuteOfDay / 60) % 24
            val m = endMinuteOfDay % 60
            return String.format("%02d:%02d", h, m)
        }

    val startTime12h: String
        get() {
            val h = (startMinuteOfDay / 60) % 24
            val m = startMinuteOfDay % 60
            val hour12 = if (h % 12 == 0) 12 else (h % 12)
            val amPm = if (h < 12) "AM" else "PM"
            return String.format("%d:%02d %s", hour12, m, amPm)
        }

    val endTime12h: String
        get() {
            val h = (endMinuteOfDay / 60) % 24
            val m = endMinuteOfDay % 60
            val hour12 = if (h % 12 == 0) 12 else (h % 12)
            val amPm = if (h < 12) "AM" else "PM"
            return String.format("%d:%02d %s", hour12, m, amPm)
        }

    val timeRange12h: String
        get() = "$startTime12h – $endTime12h"

    val timeRange24h: String
        get() = "$startTimeFormatted – $endTimeFormatted"

    val durationMinutes: Int
        get() = if (endMinuteOfDay >= startMinuteOfDay) {
            endMinuteOfDay - startMinuteOfDay
        } else {
            (1440 - startMinuteOfDay) + endMinuteOfDay
        }

    val durationFormatted: String
        get() {
            val h = durationMinutes / 60
            val m = durationMinutes % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }

    fun isCurrentlyActive(currentMinute: Int): Boolean {
        return if (endMinuteOfDay >= startMinuteOfDay) {
            currentMinute in startMinuteOfDay until endMinuteOfDay
        } else {
            currentMinute >= startMinuteOfDay || currentMinute < endMinuteOfDay
        }
    }

    fun progress(currentMinute: Int): Float {
        if (!isCurrentlyActive(currentMinute)) return 0f
        val elapsed = if (endMinuteOfDay >= startMinuteOfDay) {
            currentMinute - startMinuteOfDay
        } else {
            if (currentMinute >= startMinuteOfDay) currentMinute - startMinuteOfDay
            else (1440 - startMinuteOfDay) + currentMinute
        }
        val total = durationMinutes.coerceAtLeast(1)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun remainingMinutes(currentMinute: Int): Int {
        if (!isCurrentlyActive(currentMinute)) return 0
        val remaining = if (endMinuteOfDay >= startMinuteOfDay) {
            endMinuteOfDay - currentMinute
        } else {
            if (currentMinute >= startMinuteOfDay) (1440 - currentMinute) + endMinuteOfDay
            else endMinuteOfDay - currentMinute
        }
        return remaining.coerceAtLeast(0)
    }
}

fun ChunkCategory.toMonochromeColor(): Color = when (this) {
    ChunkCategory.SLEEP -> Color(0xFF475569)   // Deep slate
    ChunkCategory.ROUTINE -> Color(0xFF94A3B8) // Cool silver
    ChunkCategory.STUDY -> Color(0xFFE2E8F0)   // Bright platinum
    ChunkCategory.WORK -> Color(0xFFFFFFFF)    // Pure white (peak focus)
    ChunkCategory.MEAL -> Color(0xFFCBD5E1)    // Soft titanium
    ChunkCategory.REST -> Color(0xFF64748B)    // Charcoal slate
    ChunkCategory.FITNESS -> Color(0xFFF1F5F9) // Stark silver
    ChunkCategory.CUSTOM -> Color(0xFFD1D5DB)  // Neutral gray
}

fun TimeChunk.toMonochromeColor(): Color {
    return if (colorHex.equals(category.defaultColor, ignoreCase = true)) {
        category.toMonochromeColor()
    } else {
        val parsed = com.hyperchunk.app.ui.theme.parseHexColor(colorHex, category.toMonochromeColor())
        val lum = 0.2126f * parsed.red + 0.7152f * parsed.green + 0.0722f * parsed.blue
        val adjusted = 0.45f + 0.55f * lum
        Color(adjusted, adjusted, adjusted)
    }
}
