package com.hyperchunk.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.model.toMonochromeColor
import com.hyperchunk.app.ui.theme.LocalAppSettings
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.parseHexColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision 24-Hour Squircle / Rectangle Chronograph Dial.
 * Features:
 * - 24 hours mapped uniformly around a luxury rounded rectangular perimeter track
 * - 8 cardinal points (12 AM, 3 AM, 6 AM, 9 AM, 12 PM, 3 PM, 6 PM, 9 PM)
 * - Precision radial tick marks and hour markers
 * - Chunk segments with rounded caps and glowing halos
 * - Live glowing sun bead tracing the perimeter
 * - Digital clock complication with status capsule pill
 */
@Composable
fun Chunk24hRectangleChronograph(
    chunks: List<TimeChunk>,
    currentMinute: Int,
    activeChunk: TimeChunk?,
    modifier: Modifier = Modifier,
    nextChunk: TimeChunk? = null,
    isToday: Boolean = true,
    dayName: String = "",
    compactMode: Boolean = LocalAppSettings.current.compactMode
) {
    val appColors = LocalAppThemeColors.current
    val settings = LocalAppSettings.current
    val isMonochrome = settings.paletteMode == PaletteMode.MONOCHROME

    // 12h / 24h digital time formatting
    val currentH = currentMinute / 60
    val currentM = currentMinute % 60
    val formattedTime = if (settings.use24HourFormat) {
        String.format("%02d:%02d", currentH, currentM)
    } else {
        val hour12 = if (currentH % 12 == 0) 12 else (currentH % 12)
        val amPm = if (currentH < 12) "AM" else "PM"
        String.format("%d:%02d %s", hour12, currentM, amPm)
    }

    // Pulsing aura animation for current time needle bead
    val infiniteTransition = rememberInfiniteTransition(label = "rectNeedlePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(appColors.surface)
            .padding(vertical = if (compactMode) 14.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val dialHeight = if (compactMode) 205.dp else 225.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dialHeight)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val strokePx = if (compactMode) 16.dp.toPx() else 18.dp.toPx()
                val sidePadding = 4.dp.toPx()
                val topBottomPadding = 4.dp.toPx()

                // Widescreen rectangle track bounds spreading across the full width to both borders
                val trackWidth = (size.width - strokePx - sidePadding * 2f).coerceAtLeast(20f)
                val trackHeight = (size.height - strokePx - topBottomPadding * 2f).coerceAtLeast(20f)
                val cornerR = 22.dp.toPx().coerceAtMost(minOf(trackWidth, trackHeight) / 2f)

                val x0 = (size.width - trackWidth) / 2f
                val y0 = (size.height - trackHeight) / 2f
                val x1 = x0 + trackWidth
                val y1 = y0 + trackHeight
                val cx = size.width / 2f

                // Build closed squircle perimeter path starting at Top Center (12 AM)
                val trackPath = Path().apply {
                    moveTo(cx, y0)
                    // Top-right straight segment
                    lineTo(x1 - cornerR, y0)
                    // Top-right corner arc
                    arcTo(
                        rect = Rect(x1 - 2 * cornerR, y0, x1, y0 + 2 * cornerR),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    // Right straight segment
                    lineTo(x1, y1 - cornerR)
                    // Bottom-right corner arc
                    arcTo(
                        rect = Rect(x1 - 2 * cornerR, y1 - 2 * cornerR, x1, y1),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    // Bottom straight segment
                    lineTo(x0 + cornerR, y1)
                    // Bottom-left corner arc
                    arcTo(
                        rect = Rect(x0, y1 - 2 * cornerR, x0 + 2 * cornerR, y1),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    // Left straight segment
                    lineTo(x0, y0 + cornerR)
                    // Top-left corner arc
                    arcTo(
                        rect = Rect(x0, y0, x0 + 2 * cornerR, y0 + 2 * cornerR),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    // Close back to top center
                    lineTo(cx, y0)
                    close()
                }

                val pathMeasure = PathMeasure()
                pathMeasure.setPath(trackPath, forceClosed = true)
                val totalLength = pathMeasure.length
                if (totalLength <= 0f) return@Canvas

                // 1. Dark Concentric Track Background Ring
                drawPath(
                    path = trackPath,
                    color = Color(0xFF111520),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Subtle inner & outer track boundary guide lines
                val halfStroke = strokePx / 2f
                drawRoundRect(
                    color = Color(0xFF1E2536),
                    topLeft = Offset(x0 - halfStroke, y0 - halfStroke),
                    size = Size(trackWidth + strokePx, trackHeight + strokePx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerR + halfStroke).coerceAtLeast(0f)),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF1E2536),
                    topLeft = Offset(x0 + halfStroke, y0 + halfStroke),
                    size = Size(trackWidth - strokePx, trackHeight - strokePx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerR - halfStroke).coerceAtLeast(4f)),
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // 2. Cardinal Octant Tick Marks & Inner Labels
                val cardinalHours = listOf(
                    0 to "12 AM",
                    3 to "3 AM",
                    6 to "6 AM",
                    9 to "9 AM",
                    12 to "12 PM",
                    15 to "3 PM",
                    18 to "6 PM",
                    21 to "9 PM"
                )

                val labelStyle = TextStyle(
                    fontSize = if (compactMode) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0)
                )

                for ((hour, labelText) in cardinalHours) {
                    val progress = (hour / 24f)
                    val dist = progress * totalLength
                    val pos = pathMeasure.getPosition(dist)
                    val tangent = pathMeasure.getTangent(dist)

                    val inwardNormal = Offset(-tangent.y, tangent.x)
                    val outwardNormal = Offset(tangent.y, -tangent.x)

                    // Inner label position along inward normal from track
                    val labelDist = (strokePx / 2f) + (if (compactMode) 13.dp.toPx() else 15.dp.toPx())
                    val labelCenter = pos + inwardNormal * labelDist

                    val textLayout = textMeasurer.measure(labelText, labelStyle)
                    val textLeft = (labelCenter.x - textLayout.size.width / 2f).coerceIn(4f, size.width - textLayout.size.width - 4f)
                    val textTop = (labelCenter.y - textLayout.size.height / 2f).coerceIn(4f, size.height - textLayout.size.height - 4f)
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(textLeft, textTop)
                    )

                    // Outer Radial Tick Accent
                    val tickOuterStart = pos + outwardNormal * (strokePx / 2f)
                    val tickOuterEnd = pos + outwardNormal * ((strokePx / 2f) + 2.5.dp.toPx())
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = tickOuterStart,
                        end = tickOuterEnd,
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Inner Radial Tick
                    val tickInnerStart = pos + inwardNormal * (strokePx / 2f)
                    val tickInnerEnd = pos + inwardNormal * ((strokePx / 2f) + 4.5.dp.toPx())
                    drawLine(
                        color = Color.White,
                        start = tickInnerStart,
                        end = tickInnerEnd,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. Subtle 1-Hour Index Ticks
                for (h in 0 until 24) {
                    if (h % 3 == 0) continue
                    val dist = (h / 24f) * totalLength
                    val pos = pathMeasure.getPosition(dist)
                    val tangent = pathMeasure.getTangent(dist)
                    val inwardNormal = Offset(-tangent.y, tangent.x)

                    val subInnerStart = pos + inwardNormal * (strokePx / 2f)
                    val subInnerEnd = pos + inwardNormal * ((strokePx / 2f) + 2.8.dp.toPx())
                    drawLine(
                        color = Color(0xFF2C374C),
                        start = subInnerStart,
                        end = subInnerEnd,
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 4. Segment Chunks along Squircle Perimeter
                val gapPx = 3.5.dp.toPx()

                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val startDist = (chunk.startMinuteOfDay / 1440f) * totalLength
                    val chunkDist = (chunk.durationMinutes / 1440f) * totalLength

                    val chunkColor = if (isMonochrome) {
                        chunk.toMonochromeColor()
                    } else {
                        parseHexColor(chunk.colorHex, appColors.primary)
                    }

                    val isActive = (activeChunk?.id == chunk.id) && isToday

                    if (chunk.durationMinutes >= 1440) {
                        drawPath(
                            path = trackPath,
                            color = chunkColor,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    } else {
                        val adjStart = startDist + (gapPx / 2f)
                        val adjLen = (chunkDist - gapPx).coerceAtLeast(1f)
                        val adjEnd = adjStart + adjLen

                        if (adjEnd <= totalLength) {
                            val segPath = Path()
                            pathMeasure.getSegment(adjStart, adjEnd, segPath, startWithMoveTo = true)

                            // Active chunk soft halo
                            if (isActive) {
                                val haloColor = if (isMonochrome) Color.White.copy(alpha = 0.28f) else chunkColor.copy(alpha = 0.25f)
                                drawPath(
                                    path = segPath,
                                    color = haloColor,
                                    style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Primary segment
                            drawPath(
                                path = segPath,
                                color = chunkColor,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        } else {
                            // Wraps around midnight (00:00)
                            val firstLen = totalLength - adjStart
                            val secondLen = adjEnd - totalLength

                            if (firstLen > 0f) {
                                val seg1 = Path()
                                pathMeasure.getSegment(adjStart, totalLength, seg1, startWithMoveTo = true)
                                if (isActive) {
                                    val haloColor = if (isMonochrome) Color.White.copy(alpha = 0.28f) else chunkColor.copy(alpha = 0.25f)
                                    drawPath(path = seg1, color = haloColor, style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round))
                                }
                                drawPath(path = seg1, color = chunkColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                            }

                            if (secondLen > 0f) {
                                val seg2 = Path()
                                pathMeasure.getSegment(0f, secondLen, seg2, startWithMoveTo = true)
                                if (isActive) {
                                    val haloColor = if (isMonochrome) Color.White.copy(alpha = 0.28f) else chunkColor.copy(alpha = 0.25f)
                                    drawPath(path = seg2, color = haloColor, style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round))
                                }
                                drawPath(path = seg2, color = chunkColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                            }
                        }
                    }
                }

                // 5. Live Glowing Sun Bead Needle
                val needleDist = ((currentMinute % 1440) / 1440f) * totalLength
                val needleCenter = pathMeasure.getPosition(needleDist)

                // Outer soft glowing aura
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f * pulseScale),
                    radius = if (compactMode) 18.dp.toPx() else 22.dp.toPx(),
                    center = needleCenter
                )

                // Core glowing corona
                drawCircle(
                    color = Color.White.copy(alpha = 0.38f * pulseScale),
                    radius = if (compactMode) 9.dp.toPx() else 11.dp.toPx(),
                    center = needleCenter
                )

                // Crisp solid white center bead
                drawCircle(
                    color = Color.White,
                    radius = if (compactMode) 4.5.dp.toPx() else 5.2.dp.toPx(),
                    center = needleCenter
                )
            }

            // Center Digital Time Readout (Clean & Centered)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (!isToday && dayName.isNotBlank()) {
                    Text(
                        text = dayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.3.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Large Bold Digital Clock
                Text(
                    text = formattedTime,
                    fontSize = if (compactMode) 34.sp else 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
