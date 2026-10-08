package com.hyperchunk.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.widget.HyperChunkWidgetProvider

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val chunkId = intent.getStringExtra(EXTRA_CHUNK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Time Chunk"
        val timeSlot = intent.getStringExtra(EXTRA_TIME) ?: ""
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: ""

        // 1. Fire heads-up sound & vibration notification
        NotificationHelper.showChunkNotification(
            context = context,
            notificationId = chunkId.hashCode(),
            title = title,
            timeSlot = timeSlot,
            notes = notes
        )

        // 2. Refresh homescreen widget to reflect active chunk
        HyperChunkWidgetProvider.updateAllWidgets(context)

        // 3. Reschedule alarm for next week
        val repo = ChunkRepository(context)
        val chunk = repo.chunks.value.firstOrNull { it.id == chunkId }
        if (chunk != null && chunk.hasAlarm) {
            AlarmScheduler.scheduleChunkAlarm(context, chunk)
        }
    }

    companion object {
        const val EXTRA_CHUNK_ID = "extra_chunk_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_NOTES = "extra_notes"
        const val EXTRA_DAY = "extra_day"
    }
}
