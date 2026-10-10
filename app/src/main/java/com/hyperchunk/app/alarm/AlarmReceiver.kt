package com.hyperchunk.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.widget.HyperChunkWidgetProvider
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val chunkId = intent.getStringExtra(EXTRA_CHUNK_ID) ?: return
        val repo = ChunkRepository(context)
        val chunk = repo.chunks.value.firstOrNull { it.id == chunkId }

        // Guard 1: Chunk existence & alarm flag validation
        // If chunk was deleted or alarm was toggled off, cancel the alarm immediately and suppress notification.
        if (chunk == null || !chunk.hasAlarm) {
            Log.d(TAG, "Suppressed alarm for chunkId=$chunkId (chunk missing or hasAlarm=false). Cancelling.")
            AlarmScheduler.cancelChunkAlarm(context, chunkId)
            return
        }

        // Guard 2: Day-of-week validation
        // Prevent alarms for other days (e.g. Monday chunk delayed or dispatched on Tuesday).
        val cal = Calendar.getInstance()
        val todayDayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        if (chunk.dayOfWeek != todayDayOfWeek) {
            Log.w(
                TAG,
                "Suppressed alarm for '${chunk.title}'. Scheduled for day ${chunk.dayOfWeek}, but current day is $todayDayOfWeek. Rescheduling for correct day."
            )
            AlarmScheduler.scheduleChunkAlarm(context, chunk)
            return
        }

        // Guard 3: Stale window validation
        // If device was asleep or Doze held the alarm for >15 minutes past start time, suppress to avoid waking/disturbing user in class.
        val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val minuteDiff = kotlin.math.abs(currentMinute - chunk.startMinuteOfDay)
        if (minuteDiff > 15 && minuteDiff < 1425) {
            Log.w(
                TAG,
                "Suppressed stale alarm for '${chunk.title}' (current minute $currentMinute vs start ${chunk.startMinuteOfDay}, diff $minuteDiff m). Rescheduling for next week."
            )
            AlarmScheduler.scheduleChunkAlarm(context, chunk)
            return
        }

        // 1. Fire heads-up sound & vibration notification using confirmed live chunk data
        val safeId = kotlin.math.abs(chunkId.hashCode()).coerceAtLeast(1)
        NotificationHelper.showChunkNotification(
            context = context,
            notificationId = safeId,
            title = chunk.title,
            timeSlot = "${chunk.startTime12h} – ${chunk.endTime12h}",
            notes = chunk.notes
        )

        // 2. Refresh homescreen widget to reflect active chunk
        HyperChunkWidgetProvider.updateAllWidgets(context)

        // 3. Reschedule alarm for next week's occurrence
        AlarmScheduler.scheduleChunkAlarm(context, chunk)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_CHUNK_ID = "extra_chunk_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_NOTES = "extra_notes"
        const val EXTRA_DAY = "extra_day"
    }
}
