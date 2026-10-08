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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.hyperchunk.app.ui.theme.HyperEmerald
import com.hyperchunk.app.ui.theme.HyperSurfaceVariant
import com.hyperchunk.app.ui.theme.LocalAppSettings
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luxury 24-Hour Polar Chronograph Dial.
 * Features:
 * - 8 outer cardinal time labels (12 AM, 3 AM, 6 AM, 9 AM, 12 PM, 3 PM, 6 PM, 9 PM)
 * - Concentric outer guide ring with radial cardinal tick marks
 * - 24 inner-rim precision hour notches
 * - Segmented chunk blocks with angular gaps and dark separator slits
 * - Live sun sweep needle with glowing aura & center bead
 * - Center complication with bold digital time & active chunk pill badge
 */
@Composable
fun Chunk24hChronograph(
    chunks: List<TimeChunk>,
    currentMinute: Int,
    activeChunk: TimeChunk?,
    modifier: Modifier = Modifier,
    compactMode: Boolean = LocalAppSettings.current.compactMode
) {
    val appColors = LocalAppThemeColors.current
    val isMonochrome = LocalAppSettings.current.paletteMode == PaletteMode.MONOCHROME
    val currentH = currentMinute / 60
    val currentM = currentMinute % 60
    val hour12 = if (currentH % 12 == 0) 12 else (currentH % 12)
    val amPm = if (currentH < 12) "AM" else "PM"
    val formattedNow = String.format("%d:%02d %s", hour12, currentM, amPm)

    // Pulsing aura animation for current time needle
    val infiniteTransition = rememberInfiniteTransition(label = "needlePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.30f,
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
            .clip(RoundedCornerShape(28.dp))
            .background(appColors.surface)
            .padding(if (compactMode) 14.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(appColors.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "24-HOUR DIAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
            }

            // Summary tag
            val totalScheduledMinutes = chunks.sumOf { it.durationMinutes }
            val scheduledHours = totalScheduledMinutes / 60f
            Text(
                text = String.format("%.1fh Scheduled", scheduledHours),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMonochrome) Color(0xFFE2E8F0) else HyperEmerald
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Circular 24-Hour Chronograph Dial Container
        val dialSize = if (compactMode) 250.dp else 274.dp
        Box(
            modifier = Modifier
                .size(dialSize)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = size.minDimension
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Layout measurements
                val strokePx = if (compactMode) 19.dp.toPx() else 22.dp.toPx()
                val outerLabelRadius = (minDim / 2f) - 15.dp.toPx()
                val outerGuideRadius = outerLabelRadius - 13.dp.toPx()
                val outerRimRadius = outerGuideRadius - 4.dp.toPx()
                val radius = outerRimRadius - (strokePx / 2f)
                val innerRimRadius = radius - (strokePx / 2f)

                val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                // 1. Outer Guideline Ring (delicate concentric line)
                drawCircle(
                    color = Color(0xFF2B354A).copy(alpha = 0.55f),
                    radius = outerGuideRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 2. Outer Cardinal Time Labels & Outer Ticks (12 AM, 3 AM, 6 AM, 9 AM, 12 PM, 3 PM, 6 PM, 9 PM)
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
                    fontSize = if (compactMode) 10.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )

                for ((hour, labelText) in cardinalHours) {
                    val angleDeg = (hour / 24f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    // Draw outer label
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

                    // Draw outer radial tick
                    val isMajor = (hour % 6 == 0) // 12 AM, 6 AM, 12 PM, 6 PM
                    val tickOuter = if (isMajor) outerGuideRadius + 3.5.dp.toPx() else outerGuideRadius + 1.dp.toPx()
                    val tickColor = if (isMajor) Color.White else Color(0xFFCBD5E1)
                    val tickStroke = if (isMajor) 2.dp.toPx() else 1.5.dp.toPx()

                    drawLine(
                        color = tickColor,
                        start = Offset(centerOffset.x + outerRimRadius * cosA, centerOffset.y + outerRimRadius * sinA),
                        end = Offset(centerOffset.x + tickOuter * cosA, centerOffset.y + tickOuter * sinA),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }

                // 3. Inner Rim Hour Ticks (all 24 hours)
                for (h in 0 until 24) {
                    val angleDeg = (h / 24f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    val isMajor = (h % 6 == 0)   // 0, 6, 12, 18
                    val isOctant = (h % 3 == 0)  // 3, 9, 15, 21

                    val (tickLen, tickColor, tickStroke) = when {
                        isMajor -> Triple(7.dp.toPx(), Color.White, 2.dp.toPx())
                        isOctant -> Triple(4.5.dp.toPx(), Color(0xFFCBD5E1), 1.5.dp.toPx())
                        else -> Triple(2.5.dp.toPx(), Color(0xFF384459), 1.1.dp.toPx())
                    }

                    val innerStart = Offset(
                        centerOffset.x + (innerRimRadius - tickLen) * cosA,
                        centerOffset.y + (innerRimRadius - tickLen) * sinA
                    )
                    val innerEnd = Offset(
                        centerOffset.x + innerRimRadius * cosA,
                        centerOffset.y + innerRimRadius * sinA
                    )

                    drawLine(
                        color = tickColor,
                        start = innerStart,
                        end = innerEnd,
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }

                // 4. Dark Frosted Track Background Ring
                drawArc(
                    color = Color(0xFF131824),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )

                // Concentric inner & outer track border rings for crisp depth
                drawCircle(
                    color = Color(0xFF1E2638),
                    radius = innerRimRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF1E2638),
                    radius = outerRimRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 5. Category Segment Arcs (with angular gaps)
                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val rawSweep = ((chunk.durationMinutes / 1440f) * 360f)
                    val startAngle = (chunk.startMinuteOfDay / 1440f) * 360f - 90f

                    // Subtract angular gap so adjacent blocks don't bleed into each other
                    val gap = if (rawSweep > 8f) 2.2f else 1.2f
                    val drawStart = startAngle + gap / 2f
                    val drawSweep = (rawSweep - gap).coerceAtLeast(1.2f)

                    val chunkColor = if (isMonochrome) {
                        chunk.toMonochromeColor()
                    } else {
                        com.hyperchunk.app.ui.theme.parseHexColor(chunk.colorHex, appColors.primary)
                    }

                    val isActive = activeChunk?.id == chunk.id

                    // Active chunk glowing aura
                    if (isActive) {
                        drawArc(
                            color = if (isMonochrome) Color.White.copy(alpha = 0.35f) else chunkColor.copy(alpha = 0.28f),
                            startAngle = drawStart,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - (radius + 2.dp.toPx()), centerOffset.y - (radius + 2.dp.toPx())),
                            size = Size((radius + 2.dp.toPx()) * 2f, (radius + 2.dp.toPx()) * 2f),
                            style = Stroke(width = strokePx + 4.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }

                    // Main crisp segment arc
                    drawArc(
                        color = chunkColor,
                        startAngle = drawStart,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                }

                // 6. Crisp Dark Radial Separator Slit Lines at every chunk boundary
                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val boundaryAngles = if (chunk.startMinuteOfDay == chunk.endMinuteOfDay) {
                        listOf((chunk.startMinuteOfDay / 1440f) * 360f - 90f)
                    } else {
                        listOf(
                            (chunk.startMinuteOfDay / 1440f) * 360f - 90f,
                            (chunk.endMinuteOfDay / 1440f) * 360f - 90f
                        )
                    }
                    for (bAngle in boundaryAngles) {
                        val bRad = Math.toRadians(bAngle.toDouble())
                        val cosB = cos(bRad).toFloat()
                        val sinB = sin(bRad).toFloat()

                        val slitStart = Offset(
                            centerOffset.x + (innerRimRadius - 1.dp.toPx()) * cosB,
                            centerOffset.y + (innerRimRadius - 1.dp.toPx()) * sinB
                        )
                        val slitEnd = Offset(
                            centerOffset.x + (outerRimRadius + 1.dp.toPx()) * cosB,
                            centerOffset.y + (outerRimRadius + 1.dp.toPx()) * sinB
                        )

                        drawLine(
                            color = Color(0xFF0F131D),
                            start = slitStart,
                            end = slitEnd,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Butt
                        )
                    }
                }

                // 7. Live Sun Needle Current Time Indicator
                val currentAngleDeg = (currentMinute / 1440f) * 360f - 90f
                val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())
                val needlePos = Offset(
                    (centerOffset.x + radius * cos(currentAngleRad)).toFloat(),
                    (centerOffset.y + radius * sin(currentAngleRad)).toFloat()
                )

                // Glowing ambient halo
                drawCircle(
                    color = Color.White.copy(alpha = 0.20f),
                    radius = 13.dp.toPx() * pulseScale,
                    center = needlePos
                )
                // Translucent backing disc
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = needlePos
                )
                // Solid crisp white center bead
                drawCircle(
                    color = Color.White,
                    radius = 4.8.dp.toPx(),
                    center = needlePos
                )
            }

            // Center Complication Information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedNow,
                    fontSize = if (compactMode) 28.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (activeChunk != null) {
                    val remaining = activeChunk.remainingMinutes(currentMinute)
                    val chunkColor = if (isMonochrome) {
                        activeChunk.toMonochromeColor()
                    } else {
                        com.hyperchunk.app.ui.theme.parseHexColor(activeChunk.colorHex, appColors.primary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isMonochrome) Color(0x33FFFFFF) else chunkColor.copy(alpha = 0.16f))
                            .border(
                                width = 1.dp,
                                color = if (isMonochrome) Color.White.copy(alpha = 0.60f) else chunkColor.copy(alpha = 0.40f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${activeChunk.title} • ${remaining}m",
                            fontSize = if (compactMode) 11.5.sp else 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMonochrome) Color.White else chunkColor,
                            maxLines = 1
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(HyperSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = Color(0xFF283042),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Free Slot",
                            fontSize = if (compactMode) 11.5.sp else 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}
