package com.hyperchunk.app.data.model

import java.util.UUID

enum class ChunkCategory(
    val displayName: String,
    val defaultColor: String
) {
    SLEEP("Sleep", "#6C5CE7"),
    ROUTINE("Routine", "#00CEC9"),
    STUDY("Study / Lecture", "#0984E3"),
    WORK("Deep Work", "#E17055"),
    MEAL("Meals & Food", "#FDCB6E"),
    REST("Rest & Free", "#A29BFE"),
    FITNESS("Fitness / Gym", "#00B894"),
    CUSTOM("Custom", "#74B9FF")
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
