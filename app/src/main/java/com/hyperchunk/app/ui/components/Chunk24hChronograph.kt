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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision 24-Hour Polar Chronograph Dial matching reference design.
 * Features:
 * - Rounded capsule arc segments (StrokeCap.Round) with calibrated angular spacing
 * - 8 outer cardinal time labels (12 AM, 3 AM, 6 AM, 9 AM, 12 PM, 3 PM, 6 PM, 9 PM)
 * - Dual radial tick marks at all 8 cardinal/octant points (inner and outer ticks)
 * - 24 inner rim hour index notches
 * - Live sun indicator bead with multi-layer glowing halo aura
 * - Center complication: bold white digital clock + rounded status capsule pill
 */
@Composable
fun Chunk24hChronograph(
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
    val infiniteTransition = rememberInfiniteTransition(label = "needlePulse")
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
            .padding(vertical = if (compactMode) 12.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dial Canvas Container
        val dialSize = if (compactMode) 262.dp else 288.dp
        Box(
            modifier = Modifier
                .size(dialSize)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = size.minDimension
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Layout Radii Calculations
                val strokePx = if (compactMode) 18.dp.toPx() else 21.dp.toPx()
                val outerLabelRadius = (minDim / 2f) - 13.dp.toPx()
                val outerTickOuter = outerLabelRadius - 13.dp.toPx()
                val outerRimRadius = outerTickOuter - 6.dp.toPx()
                val radius = outerRimRadius - (strokePx / 2f)
                val innerRimRadius = radius - (strokePx / 2f)
                val innerTickInner = innerRimRadius - 7.dp.toPx()

                val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                // 1. Dark Concentric Track Background Ring
                drawArc(
                    color = Color(0xFF111520),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )

                // Subtle inner & outer track boundary guide circles
                drawCircle(
                    color = Color(0xFF1E2536),
                    radius = innerRimRadius,
                    center = centerOffset,
                    style = Stroke(width = 0.8.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF1E2536),
                    radius = outerRimRadius,
                    center = centerOffset,
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // 2. Cardinal and Octant Radial Tick Marks (0, 3, 6, 9, 12, 15, 18, 21)
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
                    fontSize = if (compactMode) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0)
                )

                for ((hour, labelText) in cardinalHours) {
                    val angleDeg = (hour / 24f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    // Draw Outer Label
                    val labelCenter = Offset(
                        centerOffset.x + outerLabelRadius * cosA,
                        centerOffset.y + outerLabelRadius * sinA
                    )
                    val textLayout = textMeasurer.measure(labelText, labelStyle)
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            labelCenter.x - textLayout.size.width / 2f,
                            labelCenter.y - textLayout.size.height / 2f
                        )
                    )

                    // Draw Outer Radial Tick (pointing from outer rim outwards toward label)
                    drawLine(
                        color = Color.White,
                        start = Offset(centerOffset.x + outerRimRadius * cosA, centerOffset.y + outerRimRadius * sinA),
                        end = Offset(centerOffset.x + outerTickOuter * cosA, centerOffset.y + outerTickOuter * sinA),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw Inner Radial Tick (pointing from inner rim inwards toward center)
                    drawLine(
                        color = Color.White,
                        start = Offset(centerOffset.x + innerRimRadius * cosA, centerOffset.y + innerRimRadius * sinA),
                        end = Offset(centerOffset.x + innerTickInner * cosA, centerOffset.y + innerTickInner * sinA),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. Subtle 1-Hour Index Ticks on Inner Rim
                for (h in 0 until 24) {
                    if (h % 3 == 0) continue // Skip octants already drawn with bold white ticks
                    val angleDeg = (h / 24f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    val subTickInner = innerRimRadius - 3.5.dp.toPx()
                    drawLine(
                        color = Color(0xFF2C374C),
                        start = Offset(centerOffset.x + innerRimRadius * cosA, centerOffset.y + innerRimRadius * sinA),
                        end = Offset(centerOffset.x + subTickInner * cosA, centerOffset.y + subTickInner * sinA),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 4. Rounded Capsule Arc Segments (StrokeCap.Round)
                // Half-cap angular clearance calculation: capAngle = asin((strokePx/2) / radius) in degrees
                val capRadiusRatio = (strokePx / 2f) / radius.coerceAtLeast(1f)
                val capAngleDeg = (asin(capRadiusRatio.coerceIn(0f, 0.99f)) * (180f / PI.toFloat()))
                val desiredGapDeg = 2.4f // Visible gap between adjacent rounded ends

                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val rawSweep = (chunk.durationMinutes / 1440f) * 360f
                    val rawStartAngle = (chunk.startMinuteOfDay / 1440f) * 360f - 90f

                    val chunkColor = if (isMonochrome) {
                        chunk.toMonochromeColor()
                    } else {
                        parseHexColor(chunk.colorHex, appColors.primary)
                    }

                    // Angular clearance: cap extends capAngleDeg beyond start and end
                    val totalCapAllowance = (2f * capAngleDeg) + desiredGapDeg

                    val (drawStart, drawSweep) = if (rawSweep > totalCapAllowance) {
                        Pair(
                            rawStartAngle + capAngleDeg + (desiredGapDeg / 2f),
                            rawSweep - totalCapAllowance
                        )
                    } else {
                        // For very small chunks, render a minimal rounded capsule centered at mid-slot
                        val midAngle = rawStartAngle + (rawSweep / 2f)
                        Pair(midAngle - 0.5f, 1.0f)
                    }

                    val isActive = (activeChunk?.id == chunk.id) && isToday

                    // Active chunk glowing back-aura
                    if (isActive) {
                        drawArc(
                            color = if (isMonochrome) Color.White.copy(alpha = 0.30f) else chunkColor.copy(alpha = 0.28f),
                            startAngle = drawStart,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - (radius + 2.dp.toPx()), centerOffset.y - (radius + 2.dp.toPx())),
                            size = Size((radius + 2.dp.toPx()) * 2f, (radius + 2.dp.toPx()) * 2f),
                            style = Stroke(width = strokePx + 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Primary Segment Capsule with buttery smooth rounded caps
                    drawArc(
                        color = chunkColor,
                        startAngle = drawStart,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                // 5. Live Glowing Sun Sweep Bead Needle
                val needleAngleDeg = (currentMinute / 1440f) * 360f - 90f
                val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                val needleCos = cos(needleAngleRad).toFloat()
                val needleSin = sin(needleAngleRad).toFloat()

                val needleCenter = Offset(
                    centerOffset.x + radius * needleCos,
                    centerOffset.y + radius * needleSin
                )

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

            // Center Digital Complication Readout
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
                    fontSize = if (compactMode) 32.sp else 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sleek Capsule Pill Complication
                when {
                    isToday && activeChunk != null -> {
                        val remaining = activeChunk.remainingMinutes(currentMinute)
                        val chunkColor = if (isMonochrome) {
                            Color.White
                        } else {
                            parseHexColor(activeChunk.colorHex, appColors.primary)
                        }

                        val remainingText = if (remaining >= 60) {
                            "${remaining / 60}h ${remaining % 60}m"
                        } else {
                            "${remaining}m"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131722))
                                .border(
                                    width = 1.dp,
                                    color = if (isMonochrome) Color.White.copy(alpha = 0.45f) else chunkColor.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${activeChunk.title} • $remainingText",
                                fontSize = if (compactMode) 12.sp else 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMonochrome) Color.White else chunkColor,
                                maxLines = 1
                            )
                        }
                    }

                    isToday && nextChunk != null -> {
                        val minsUntil = if (nextChunk.startMinuteOfDay >= currentMinute) {
                            nextChunk.startMinuteOfDay - currentMinute
                        } else {
                            (1440 - currentMinute) + nextChunk.startMinuteOfDay
                        }
                        val untilText = if (minsUntil >= 60) "${minsUntil / 60}h ${minsUntil % 60}m" else "${minsUntil}m"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0xFF283144), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Next: ${nextChunk.title} • in $untilText",
                                fontSize = if (compactMode) 11.5.sp else 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFF38BDF8),
                                maxLines = 1
                            )
                        }
                    }

                    !isToday && chunks.isNotEmpty() -> {
                        val totalMins = chunks.sumOf { it.durationMinutes }
                        val h = totalMins / 60
                        val m = totalMins % 60
                        val timeStr = if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0xFF283144), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayName • $timeStr planned",
                                fontSize = if (compactMode) 11.5.sp else 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFF38BDF8),
                                maxLines = 1
                            )
                        }
                    }

                    chunks.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0xFF232A3B), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isToday) "Unscheduled • Tap + to Plan" else "$dayName • Tap + to Plan",
                                fontSize = if (compactMode) 11.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0xFF232A3B), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Free Slot • All Clear",
                                fontSize = if (compactMode) 11.5.sp else 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
