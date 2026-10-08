package com.hyperchunk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.hyperchunk.app.ACTION_UPDATE_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, HyperChunkWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
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
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_header, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_task_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_time_row, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_next_task, pendingIntent)

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
                views.setTextViewText(R.id.widget_status_pill, "NOW ACTIVE")
                views.setTextColor(R.id.widget_status_pill, 0xFFA8FF78.toInt())
                views.setTextViewText(R.id.widget_task_title, activeChunk.title)
                views.setTextViewText(
                    R.id.widget_task_time,
                    "${activeChunk.startTime12h} – ${activeChunk.endTime12h}"
                )
                val remaining = activeChunk.remainingMinutes(currentMinute)
                val remainingStr = if (remaining >= 60) {
                    "${remaining / 60}h ${remaining % 60}m left"
                } else {
                    "${remaining}m left"
                }
                views.setTextViewText(R.id.widget_time_remaining, remainingStr)

                val progressPercent = (activeChunk.progress(currentMinute) * 100).toInt().coerceIn(0, 100)
                views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)

                if (nextChunk != null) {
                    views.setTextViewText(
                        R.id.widget_next_task,
                        "Next: ${nextChunk.title} (${nextChunk.startTime12h})"
                    )
                } else {
                    views.setTextViewText(R.id.widget_next_task, "Last chunk scheduled today")
                }
            } else if (nextChunk != null) {
                // There is an upcoming chunk today
                views.setTextViewText(R.id.widget_status_pill, "UPCOMING")
                views.setTextColor(R.id.widget_status_pill, 0xFF00F2FE.toInt())
                views.setTextViewText(R.id.widget_task_title, nextChunk.title)
                views.setTextViewText(
                    R.id.widget_task_time,
                    "${nextChunk.startTime12h} – ${nextChunk.endTime12h}"
                )
                val minsUntil = nextChunk.startMinuteOfDay - currentMinute
                val untilStr = if (minsUntil >= 60) {
                    "Starts in ${minsUntil / 60}h ${minsUntil % 60}m"
                } else {
                    "Starts in ${minsUntil}m"
                }
                views.setTextViewText(R.id.widget_time_remaining, untilStr)
                views.setProgressBar(R.id.widget_progress, 100, 0, false)

                // Check if there is another chunk after nextChunk
                val laterChunks = repo.getChunksForDay(dayOfWeek).filter { it.startMinuteOfDay >= nextChunk.endMinuteOfDay }
                if (laterChunks.isNotEmpty()) {
                    val after = laterChunks.first()
                    views.setTextViewText(R.id.widget_next_task, "Later: ${after.title} (${after.startTime12h})")
                } else {
                    views.setTextViewText(R.id.widget_next_task, "Upcoming on today's schedule")
                }
            } else {
                // No active or upcoming chunks
                views.setTextViewText(R.id.widget_status_pill, "ALL CLEAR")
                views.setTextColor(R.id.widget_status_pill, 0xFF8E9BAE.toInt())
                views.setTextViewText(R.id.widget_task_title, "No Chunks Right Now")
                views.setTextViewText(R.id.widget_task_time, "Tap to plan your schedule")
                views.setTextViewText(R.id.widget_time_remaining, "—")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setTextViewText(R.id.widget_next_task, "Open HyperChunk to add time blocks")
            }

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("HyperChunkWidget", "Failed to update widget $appWidgetId", e)
            }
        }
    }
}
