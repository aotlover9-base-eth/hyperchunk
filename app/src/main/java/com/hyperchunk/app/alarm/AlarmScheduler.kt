package com.hyperchunk.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.storage.ChunkRepository
import java.util.Calendar

object AlarmScheduler {

    fun scheduleChunkAlarm(context: Context, chunk: TimeChunk) {
        if (!chunk.hasAlarm) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_CHUNK_ID, chunk.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, chunk.title)
            putExtra(AlarmReceiver.EXTRA_TIME, "${chunk.startTimeFormatted} - ${chunk.endTimeFormatted}")
            putExtra(AlarmReceiver.EXTRA_NOTES, chunk.notes)
            putExtra(AlarmReceiver.EXTRA_DAY, chunk.dayOfWeek)
        }

        val requestCode = chunk.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = calculateNextTriggerMillis(chunk.dayOfWeek, chunk.startMinuteOfDay)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for strict background permission
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelChunkAlarm(context: Context, chunkId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            chunkId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllAlarms(context: Context) {
        val repo = ChunkRepository(context)
        val allChunks = repo.chunks.value
        allChunks.filter { it.hasAlarm }.forEach { chunk ->
            scheduleChunkAlarm(context, chunk)
        }
    }

    private fun calculateNextTriggerMillis(dayOfWeek: Int, startMinute: Int): Long {
        val cal = Calendar.getInstance()
        val targetDay = when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        cal.set(Calendar.HOUR_OF_DAY, startMinute / 60)
        cal.set(Calendar.MINUTE, startMinute % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysDiff = targetDay - currentDay
        if (daysDiff < 0 || (daysDiff == 0 && cal.timeInMillis <= System.currentTimeMillis())) {
            daysDiff += 7
        }

        cal.add(Calendar.DAY_OF_YEAR, daysDiff)
        return cal.timeInMillis
    }
}
