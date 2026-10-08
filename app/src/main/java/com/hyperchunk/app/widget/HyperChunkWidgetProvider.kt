package com.hyperchunk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hyperchunk.app.MainActivity
import com.hyperchunk.app.R
import com.hyperchunk.app.data.storage.ChunkRepository
import java.util.Calendar

class HyperChunkWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.hyperchunk.app.ACTION_UPDATE_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, HyperChunkWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_hyperchunk)

            // Click opens app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // Calculate current day and minute
            val cal = Calendar.getInstance()
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
            val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

            val repo = ChunkRepository(context)
            val (activeChunk, nextChunk) = repo.getCurrentAndNextChunk(dayOfWeek, currentMinute)

            if (activeChunk != null) {
                views.setTextViewText(R.id.widget_task_title, activeChunk.title)
                views.setTextViewText(
                    R.id.widget_task_time,
                    "${activeChunk.startTimeFormatted} – ${activeChunk.endTimeFormatted}"
                )
                val remaining = activeChunk.remainingMinutes(currentMinute)
                val remainingStr = if (remaining >= 60) {
                    "${remaining / 60}h ${remaining % 60}m left"
                } else {
                    "${remaining}m left"
                }
                views.setTextViewText(R.id.widget_time_remaining, remainingStr)

                val progressPercent = (activeChunk.progress(currentMinute) * 100).toInt()
                views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)
                views.setTextViewText(R.id.widget_status_pill, "NOW ACTIVE")
            } else {
                views.setTextViewText(R.id.widget_task_title, "No Active Chunk")
                views.setTextViewText(R.id.widget_task_time, "Enjoy your free time")
                views.setTextViewText(R.id.widget_time_remaining, "—")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setTextViewText(R.id.widget_status_pill, "FREE SLOT")
            }

            if (nextChunk != null) {
                views.setTextViewText(
                    R.id.widget_next_task,
                    "Next: ${nextChunk.title} (${nextChunk.startTimeFormatted})"
                )
            } else {
                views.setTextViewText(R.id.widget_next_task, "No more chunks scheduled today")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
