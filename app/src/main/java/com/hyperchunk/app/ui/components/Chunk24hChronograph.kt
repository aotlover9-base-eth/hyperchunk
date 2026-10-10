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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.ceil
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

                // 4. Annular Segments with Subtle Corner Fillets ("A Little Curl")
                val cornerRadiusPx = if (compactMode) 3.5.dp.toPx() else 4.2.dp.toPx()
                val gapDeg = 1.8f // Clean dark cut gap between adjacent chunks

                chunks.forEach { chunk ->
                    if (chunk.durationMinutes <= 0) return@forEach
                    val rawSweep = (chunk.durationMinutes / 1440f) * 360f
                    val rawStartAngle = (chunk.startMinuteOfDay / 1440f) * 360f - 90f

                    val chunkColor = if (isMonochrome) {
                        chunk.toMonochromeColor()
                    } else {
                        parseHexColor(chunk.colorHex, appColors.primary)
                    }

                    val isActive = (activeChunk?.id == chunk.id) && isToday

                    if (chunk.durationMinutes >= 1440) {
                        // Full 24-hour continuous circle
                        drawArc(
                            color = chunkColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                        )
                    } else {
                        val drawStart = rawStartAngle + (gapDeg / 2f)
                        val drawSweep = (rawSweep - gapDeg).coerceAtLeast(0.4f)

                        // Active chunk soft glowing back-halo
                        if (isActive) {
                            drawCurledAnnularSector(
                                color = if (isMonochrome) Color.White.copy(alpha = 0.28f) else chunkColor.copy(alpha = 0.25f),
                                center = centerOffset,
                                innerRadius = innerRimRadius - 2.5.dp.toPx(),
                                outerRadius = outerRimRadius + 2.5.dp.toPx(),
                                startAngleDeg = drawStart,
                                sweepAngleDeg = drawSweep,
                                cornerRadiusPx = cornerRadiusPx + 1.dp.toPx()
                            )
                        }

                        // Primary annular sector with subtly curled corners
                        drawCurledAnnularSector(
                            color = chunkColor,
                            center = centerOffset,
                            innerRadius = innerRimRadius,
                            outerRadius = outerRimRadius,
                            startAngleDeg = drawStart,
                            sweepAngleDeg = drawSweep,
                            cornerRadiusPx = cornerRadiusPx
                        )
                    }
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

/**
 * Draws an annular segment with subtly curled / rounded corners ("a little curl")
 * rather than a giant semicircle cap. The radial cut edges remain crisp and mostly flat,
 * with gentle corner fillets matching high-precision chronograph aesthetics.
 */
private fun DrawScope.drawCurledAnnularSector(
    color: Color,
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float,
    cornerRadiusPx: Float
) {
    if (sweepAngleDeg <= 0.1f) return
    val thickness = outerRadius - innerRadius
    if (thickness <= 0.5f) return

    val sweepRad = Math.toRadians(sweepAngleDeg.toDouble()).toFloat()
    val maxDeltaRad = sweepRad / 2.05f

    val deltaOut = (cornerRadiusPx / outerRadius).coerceAtMost(maxDeltaRad)
    val deltaIn = (cornerRadiusPx / innerRadius).coerceAtMost(maxDeltaRad)
    val rcClamped = cornerRadiusPx.coerceAtMost(thickness / 2.05f)

    val startRad = Math.toRadians(startAngleDeg.toDouble()).toFloat()
    val endRad = startRad + sweepRad

    val path = Path()

    // 1. Point on radial start ray below the outer corner fillet
    val pStartRadOuter = Offset(
        center.x + (outerRadius - rcClamped) * cos(startRad),
        center.y + (outerRadius - rcClamped) * sin(startRad)
    )
    path.moveTo(pStartRadOuter.x, pStartRadOuter.y)

    // Corner 1: Outer Start Corner Fillet
    val pSharpOuterStart = Offset(
        center.x + outerRadius * cos(startRad),
        center.y + outerRadius * sin(startRad)
    )
    val pOuterArcStart = Offset(
        center.x + outerRadius * cos(startRad + deltaOut),
        center.y + outerRadius * sin(startRad + deltaOut)
    )
    path.quadraticBezierTo(pSharpOuterStart.x, pSharpOuterStart.y, pOuterArcStart.x, pOuterArcStart.y)

    // Outer Arc: smoothly follow outerRadius circle clockwise
    val outerSpanRad = (endRad - deltaOut) - (startRad + deltaOut)
    if (outerSpanRad > 0f) {
        val outerSteps = ceil(Math.toDegrees(outerSpanRad.toDouble())).toInt().coerceAtLeast(1)
        for (i in 1..outerSteps) {
            val ang = (startRad + deltaOut) + (i.toFloat() / outerSteps) * outerSpanRad
            path.lineTo(
                center.x + outerRadius * cos(ang),
                center.y + outerRadius * sin(ang)
            )
        }
    }

    // Corner 2: Outer End Corner Fillet
    val pSharpOuterEnd = Offset(
        center.x + outerRadius * cos(endRad),
        center.y + outerRadius * sin(endRad)
    )
    val pEndRadOuter = Offset(
        center.x + (outerRadius - rcClamped) * cos(endRad),
        center.y + (outerRadius - rcClamped) * sin(endRad)
    )
    path.quadraticBezierTo(pSharpOuterEnd.x, pSharpOuterEnd.y, pEndRadOuter.x, pEndRadOuter.y)

    // Radial End Line
    val pEndRadInner = Offset(
        center.x + (innerRadius + rcClamped) * cos(endRad),
        center.y + (innerRadius + rcClamped) * sin(endRad)
    )
    path.lineTo(pEndRadInner.x, pEndRadInner.y)

    // Corner 3: Inner End Corner Fillet
    val pSharpInnerEnd = Offset(
        center.x + innerRadius * cos(endRad),
        center.y + innerRadius * sin(endRad)
    )
    val pInnerArcEnd = Offset(
        center.x + innerRadius * cos(endRad - deltaIn),
        center.y + innerRadius * sin(endRad - deltaIn)
    )
    path.quadraticBezierTo(pSharpInnerEnd.x, pSharpInnerEnd.y, pInnerArcEnd.x, pInnerArcEnd.y)

    // Inner Arc: smoothly follow innerRadius circle counter-clockwise without inward bulging
    val innerSpanRad = (endRad - deltaIn) - (startRad + deltaIn)
    if (innerSpanRad > 0f) {
        val innerSteps = ceil(Math.toDegrees(innerSpanRad.toDouble())).toInt().coerceAtLeast(1)
        for (i in 1..innerSteps) {
            val ang = (endRad - deltaIn) - (i.toFloat() / innerSteps) * innerSpanRad
            path.lineTo(
                center.x + innerRadius * cos(ang),
                center.y + innerRadius * sin(ang)
            )
        }
    }

    // Corner 4: Inner Start Corner Fillet
    val pSharpInnerStart = Offset(
        center.x + innerRadius * cos(startRad),
        center.y + innerRadius * sin(startRad)
    )
    val pStartRadInner = Offset(
        center.x + (innerRadius + rcClamped) * cos(startRad),
        center.y + (innerRadius + rcClamped) * sin(startRad)
    )
    path.quadraticBezierTo(pSharpInnerStart.x, pSharpInnerStart.y, pStartRadInner.x, pStartRadInner.y)

    // Radial Start Line back to initial start point
    path.lineTo(pStartRadOuter.x, pStartRadOuter.y)
    path.close()

    drawPath(path, color)
}
