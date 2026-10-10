package com.hyperchunk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.hyperchunk.app.MainActivity
import com.hyperchunk.app.R
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.model.toMonochromeColor
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

        private fun composeColorToArgb(color: androidx.compose.ui.graphics.Color): Int {
            val a = (color.alpha * 255f).toInt().coerceIn(0, 255)
            val r = (color.red * 255f).toInt().coerceIn(0, 255)
            val g = (color.green * 255f).toInt().coerceIn(0, 255)
            val b = (color.blue * 255f).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }

        private fun createWidgetIconBitmap(accentColorInt: Int): Bitmap {
            val sizePx = 64
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val cx = sizePx / 2f
            val cy = sizePx / 2f
            val radius = sizePx * 0.44f

            // Outer ring tinted with accent/theme color
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = accentColorInt
            }
            canvas.drawCircle(cx, cy, radius, ringPaint)

            // Inner dark center
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = 0xFF151A24.toInt()
            }
            canvas.drawCircle(cx, cy, radius * 0.58f, innerPaint)

            // Crisp white clock hands
            val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3.2f
                strokeCap = Paint.Cap.ROUND
                color = Color.WHITE
            }
            val handPath = Path().apply {
                moveTo(cx, cy - radius * 0.42f)
                lineTo(cx, cy)
                lineTo(cx + radius * 0.35f, cy)
            }
            canvas.drawPath(handPath, handPaint)
            return bitmap
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

            // Sync with current app settings
            val settings = try {
                SettingsRepository(context).settings.value
            } catch (_: Exception) {
                com.hyperchunk.app.data.model.AppSettings()
            }
            val isMonochrome = (settings.paletteMode == PaletteMode.MONOCHROME)
            val accentColorInt = try {
                Color.parseColor(settings.accentTheme.colorHex)
            } catch (_: Exception) {
                0xFFF43F5E.toInt()
            }

            // Sync Header: Title and Dot Icon
            val headerColor = if (isMonochrome) Color.WHITE else accentColorInt
            views.setTextViewText(R.id.widget_app_title, "HYPERCHUNK")
            views.setTextColor(R.id.widget_app_title, headerColor)
            try {
                views.setImageViewBitmap(R.id.widget_icon, createWidgetIconBitmap(headerColor))
            } catch (_: Exception) {}

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

            val activeGlowColorInt: Int

            if (activeChunk != null) {
                val activeChunkColorInt = if (isMonochrome) {
                    composeColorToArgb(activeChunk.toMonochromeColor())
                } else {
                    try {
                        Color.parseColor(activeChunk.colorHex)
                    } catch (_: Exception) {
                        accentColorInt
                    }
                }
                activeGlowColorInt = activeChunkColorInt

                views.setTextViewText(R.id.widget_status_pill, "NOW ACTIVE")
                views.setTextColor(R.id.widget_status_pill, activeChunkColorInt)
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
                views.setTextColor(R.id.widget_time_remaining, activeChunkColorInt)

                val progressPercent = (activeChunk.progress(currentMinute) * 100).toInt().coerceIn(0, 100)
                views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    views.setColorStateList(
                        R.id.widget_progress,
                        "setProgressTintList",
                        ColorStateList.valueOf(activeChunkColorInt)
                    )
                }

                if (nextChunk != null) {
                    views.setTextViewText(
                        R.id.widget_next_task,
                        "Next: ${nextChunk.title} (${nextChunk.startTime12h})"
                    )
                } else {
                    views.setTextViewText(R.id.widget_next_task, "Last chunk scheduled today")
                }
            } else if (nextChunk != null) {
                val nextChunkColorInt = if (isMonochrome) {
                    composeColorToArgb(nextChunk.toMonochromeColor())
                } else {
                    try {
                        Color.parseColor(nextChunk.colorHex)
                    } catch (_: Exception) {
                        accentColorInt
                    }
                }
                activeGlowColorInt = nextChunkColorInt

                views.setTextViewText(R.id.widget_status_pill, "UPCOMING")
                views.setTextColor(R.id.widget_status_pill, nextChunkColorInt)
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
                views.setTextColor(R.id.widget_time_remaining, nextChunkColorInt)
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    views.setColorStateList(
                        R.id.widget_progress,
                        "setProgressTintList",
                        ColorStateList.valueOf(nextChunkColorInt)
                    )
                }

                val laterChunks = repo.getChunksForDay(dayOfWeek).filter { it.startMinuteOfDay >= nextChunk.endMinuteOfDay }
                if (laterChunks.isNotEmpty()) {
                    val after = laterChunks.first()
                    views.setTextViewText(R.id.widget_next_task, "Later: ${after.title} (${after.startTime12h})")
                } else {
                    views.setTextViewText(R.id.widget_next_task, "Upcoming on today's schedule")
                }
            } else {
                activeGlowColorInt = accentColorInt
                val mutedColor = 0xFF8E9BAE.toInt()

                views.setTextViewText(R.id.widget_status_pill, "ALL CLEAR")
                views.setTextColor(R.id.widget_status_pill, mutedColor)
                views.setTextViewText(R.id.widget_task_title, "No Chunks Right Now")
                views.setTextViewText(R.id.widget_task_time, "Tap to plan your schedule")
                views.setTextViewText(R.id.widget_time_remaining, "—")
                views.setTextColor(R.id.widget_time_remaining, mutedColor)
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    views.setColorStateList(
                        R.id.widget_progress,
                        "setProgressTintList",
                        ColorStateList.valueOf(mutedColor)
                    )
                }
                views.setTextViewText(R.id.widget_next_task, "Open HyperChunk to add time blocks")
            }

            // 24-Hour Perimeter Color Edge Ring
            val todayChunks = repo.getChunksForDay(dayOfWeek)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isPortrait = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val maxW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) ?: 0
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0

            val widthDp = when {
                isPortrait && minW > 0 -> minW.toFloat()
                !isPortrait && maxW > 0 -> maxW.toFloat()
                minW > 0 -> minW.toFloat()
                else -> 320f
            }

            val heightDp = when {
                isPortrait && minH > 0 -> minH.toFloat()
                !isPortrait && minH > 0 -> minH.toFloat()
                maxH > 0 -> (maxH * 0.75f)
                else -> 110f
            }

            val ringBitmap = renderEdgeRingBitmap(
                context = context,
                chunks = todayChunks,
                currentMinute = currentMinute,
                widthDp = widthDp,
                heightDp = heightDp,
                isMonochrome = isMonochrome,
                accentColorInt = accentColorInt,
                activeGlowColorInt = activeGlowColorInt
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
            widthDp: Float,
            heightDp: Float,
            isMonochrome: Boolean,
            accentColorInt: Int,
            activeGlowColorInt: Int
        ): Bitmap? {
            try {
                val aspectRatio = (widthDp / heightDp).coerceIn(1.8f, 4.5f)
                val w = 720
                val h = (w / aspectRatio).toInt().coerceIn(160, 400)

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Density scale relative to DP
                val densityScale = w.toFloat() / widthDp
                val strokeWidthPx = 3.5f * densityScale
                val cornerRadiusPx = 18f * densityScale
                val inset = strokeWidthPx / 2f + 1.2f * densityScale

                val x0 = inset
                val y0 = inset
                val x1 = w.toFloat() - inset
                val y1 = h.toFloat() - inset
                val r = (cornerRadiusPx - inset).coerceAtLeast(4f)
                val cx = w / 2f

                val path = Path().apply {
                    moveTo(cx, y0)
                    // Top straight edge
                    lineTo(x1 - r, y0)
                    // Top-right corner
                    arcTo(RectF(x1 - 2f * r, y0, x1, y0 + 2f * r), -90f, 90f)
                    // Right straight edge (straight line along the right border)
                    lineTo(x1, y1 - r)
                    // Bottom-right corner
                    arcTo(RectF(x1 - 2f * r, y1 - 2f * r, x1, y1), 0f, 90f)
                    // Bottom straight edge
                    lineTo(x0 + r, y1)
                    // Bottom-left corner
                    arcTo(RectF(x0, y1 - 2f * r, x0 + 2f * r, y1), 90f, 90f)
                    // Left straight edge (straight line along the left border)
                    lineTo(x0, y0 + r)
                    // Top-left corner
                    arcTo(RectF(x0, y0, x0 + 2f * r, y0 + 2f * r), 180f, 90f)
                    // Back to top center
                    lineTo(cx, y0)
                    close()
                }

                val pathMeasure = PathMeasure(path, true)
                val totalLength = pathMeasure.length
                if (totalLength <= 0f) return bitmap

                // 1. Subtle dark background track
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidthPx
                    strokeCap = Paint.Cap.ROUND
                    color = 0x22FFFFFF // translucent subtle guide track
                }
                canvas.drawPath(path, bgPaint)

                // 2. Chunks along the perimeter
                val gap = 2f * densityScale
                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val startDist = (chunk.startMinuteOfDay / 1440f) * totalLength
                    val chunkDist = (chunk.durationMinutes / 1440f) * totalLength

                    val adjStart = startDist + (gap / 2f)
                    val adjLen = (chunkDist - gap).coerceAtLeast(1f)
                    val adjEnd = adjStart + adjLen

                    val chunkColorInt = if (isMonochrome) {
                        composeColorToArgb(chunk.toMonochromeColor())
                    } else {
                        try {
                            Color.parseColor(chunk.colorHex)
                        } catch (_: Exception) {
                            accentColorInt
                        }
                    }

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        this.strokeWidth = strokeWidthPx
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

                // Glowing aura matching active chunk or accent color
                val glowColor = if (isMonochrome) {
                    0x66FFFFFF
                } else {
                    (0x66 shl 24) or (activeGlowColorInt and 0x00FFFFFF)
                }
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = glowColor
                }
                canvas.drawCircle(pos[0], pos[1], strokeWidthPx * 1.8f, glowPaint)

                // Crisp solid white center bead
                val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                canvas.drawCircle(pos[0], pos[1], strokeWidthPx * 0.9f, beadPaint)

                return bitmap
            } catch (e: Exception) {
                android.util.Log.e("HyperChunkWidget", "Failed to render edge ring bitmap", e)
                return null
            }
        }
    }
}
