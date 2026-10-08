package com.hyperchunk.app.data.model

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
