package com.hyperchunk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import com.hyperchunk.app.MainActivity
import com.hyperchunk.app.R
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.data.storage.SettingsRepository
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

            val isMonochrome = try {
                SettingsRepository(context).settings.value.paletteMode == PaletteMode.MONOCHROME
            } catch (_: Exception) {
                false
            }

            if (activeChunk != null) {
                views.setTextViewText(R.id.widget_status_pill, "NOW ACTIVE")
                views.setTextColor(R.id.widget_status_pill, if (isMonochrome) 0xFFFFFFFF.toInt() else 0xFFA8FF78.toInt())
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
                if (isMonochrome) {
                    views.setTextColor(R.id.widget_time_remaining, 0xFFE2E8F0.toInt())
                }

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
                views.setTextColor(R.id.widget_status_pill, if (isMonochrome) 0xFFCBD5E1.toInt() else 0xFF00F2FE.toInt())
                views.setTextViewText(R.id.widget_task_title, nextChunk.title)
                views.setTextViewText(
                    R.id.widget_task_time,
                    "${nextChunk.startTime12h} – ${nextChunk.endTime12h}"
                )
                val minsUntil = if (nextChunk.startMinuteOfDay >= currentMinute) {
                    nextChunk.startMinuteOfDay - currentMinute
                } else {
                    (1440 - currentMinute) + nextChunk.startMinuteOfDay
                }
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

            // 24-Hour Perimeter Color Edge Ring
            val todayChunks = repo.getChunksForDay(dayOfWeek)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260) ?: 260
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130) ?: 130
            val ringBitmap = renderEdgeRingBitmap(
                context = context,
                chunks = todayChunks,
                currentMinute = currentMinute,
                widthDp = if (minW > 0) minW else 260,
                heightDp = if (minH > 0) minH else 130,
                isMonochrome = isMonochrome
            )
            if (ringBitmap != null) {
                views.setImageViewBitmap(R.id.widget_edge_ring, ringBitmap)
            }

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("HyperChunkWidget", "Failed to update widget $appWidgetId", e)
            }
        }

        private fun renderEdgeRingBitmap(
            context: Context,
            chunks: List<TimeChunk>,
            currentMinute: Int,
            widthDp: Int,
            heightDp: Int,
            isMonochrome: Boolean
        ): Bitmap? {
            try {
                val density = context.resources.displayMetrics.density
                val w = (widthDp * 1.5f).toInt().coerceIn(360, 640)
                val h = (heightDp * 1.5f).toInt().coerceIn(180, 360)

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                val strokeWidth = 3.5f * density
                val cornerRadius = 28f * density
                val inset = strokeWidth / 2f + 1.5f * density

                val x0 = inset
                val y0 = inset
                val x1 = w - inset
                val y1 = h - inset
                val r = (cornerRadius - inset).coerceAtLeast(4f)
                val cx = w / 2f

                val path = Path().apply {
                    moveTo(cx, y0)
                    lineTo(x1 - r, y0)
                    arcTo(RectF(x1 - 2 * r, y0, x1, y0 + 2 * r), -90f, 90f)
                    lineTo(x1, y1 - r)
                    arcTo(RectF(x1 - 2 * r, y1 - 2 * r, x1, y1), 0f, 90f)
                    lineTo(x0 + r, y1)
                    arcTo(RectF(x0, y1 - 2 * r, x0 + 2 * r, y1), 90f, 90f)
                    lineTo(x0, y0 + r)
                    arcTo(RectF(x0, y0, x0 + 2 * r, y0 + 2 * r), 180f, 90f)
                    lineTo(cx, y0)
                    close()
                }

                val pathMeasure = PathMeasure(path, true)
                val totalLength = pathMeasure.length
                if (totalLength <= 0f) return bitmap

                // 1. Subtle dark background track
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeCap = Paint.Cap.ROUND
                    color = 0x22FFFFFF // translucent subtle guide track
                }
                canvas.drawPath(path, bgPaint)

                // 2. Chunks along the perimeter
                val gap = 2f * density
                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val startDist = (chunk.startMinuteOfDay / 1440f) * totalLength
                    val chunkDist = (chunk.durationMinutes / 1440f) * totalLength

                    val adjStart = startDist + (gap / 2f)
                    val adjLen = (chunkDist - gap).coerceAtLeast(1f)
                    val adjEnd = adjStart + adjLen

                    val chunkColorInt = if (isMonochrome) {
                        Color.WHITE
                    } else {
                        try {
                            Color.parseColor(chunk.colorHex)
                        } catch (_: Exception) {
                            0xFF00F2FE.toInt()
                        }
                    }

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        this.strokeWidth = strokeWidth
                        strokeCap = Paint.Cap.ROUND
                        color = chunkColorInt
                    }

                    if (adjEnd <= totalLength) {
                        val seg = Path()
                        pathMeasure.getSegment(adjStart, adjEnd, seg, true)
                        canvas.drawPath(seg, paint)
                    } else {
                        val firstLen = totalLength - adjStart
                        val secondLen = adjEnd - totalLength
                        if (firstLen > 0f) {
                            val seg1 = Path()
                            pathMeasure.getSegment(adjStart, totalLength, seg1, true)
                            canvas.drawPath(seg1, paint)
                        }
                        if (secondLen > 0f) {
                            val seg2 = Path()
                            pathMeasure.getSegment(0f, secondLen, seg2, true)
                            canvas.drawPath(seg2, paint)
                        }
                    }
                }

                // 3. Current Minute Live Sun Bead
                val needleDist = ((currentMinute % 1440) / 1440f) * totalLength
                val pos = FloatArray(2)
                pathMeasure.getPosTan(needleDist, pos, null)

                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = 0x66FFFFFF
                }
                canvas.drawCircle(pos[0], pos[1], strokeWidth * 1.6f, glowPaint)

                val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                canvas.drawCircle(pos[0], pos[1], strokeWidth * 0.85f, beadPaint)

                return bitmap
            } catch (e: Exception) {
                android.util.Log.e("HyperChunkWidget", "Failed to render edge ring bitmap", e)
                return null
            }
        }
    }
}
