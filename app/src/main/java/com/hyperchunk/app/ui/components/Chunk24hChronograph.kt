package com.hyperchunk.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperEmerald
import com.hyperchunk.app.ui.theme.HyperPrimary
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceVariant
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luxury 24-Hour Polar Chronograph Dial.
 * Visualizes the 24-hour day as an interactive 360-degree radial ring with category arcs,
 * a pulsing live sun sweep needle, and a center active block complication.
 */
@Composable
fun Chunk24hChronograph(
    chunks: List<TimeChunk>,
    currentMinute: Int,
    activeChunk: TimeChunk?,
    modifier: Modifier = Modifier
) {
    val currentH = currentMinute / 60
    val currentM = currentMinute % 60
    val hour12 = if (currentH % 12 == 0) 12 else (currentH % 12)
    val amPm = if (currentH < 12) "AM" else "PM"
    val formattedNow = String.format("%d:%02d %s", hour12, currentM, amPm)

    // Pulsing aura animation for current time needle
    val infiniteTransition = rememberInfiniteTransition(label = "needlePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(HyperSurface)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(HyperPrimary, CircleShape)
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
                color = HyperEmerald
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Circular 24-Hour Chronograph Dial
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 16.dp.toPx()
                val radius = (size.minDimension - strokePx) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                // 1. Dark Frosted Background Ring
                drawArc(
                    color = Color(0xFF181C26),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // 2. Cardinal Hour Markers (00h, 06h, 12h, 18h)
                for (hour in 0 until 24 step 3) {
                    val angleDeg = (hour / 24f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val tickInnerRadius = radius - strokePx / 2f - 4.dp.toPx()
                    val tickOuterRadius = radius - strokePx / 2f
                    val p1 = Offset(
                        (centerOffset.x + tickInnerRadius * cos(angleRad)).toFloat(),
                        (centerOffset.y + tickInnerRadius * sin(angleRad)).toFloat()
                    )
                    val p2 = Offset(
                        (centerOffset.x + tickOuterRadius * cos(angleRad)).toFloat(),
                        (centerOffset.y + tickOuterRadius * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF2C3345),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. Category Arcs
                chunks.forEach { chunk ->
                    val startAngle = (chunk.startMinuteOfDay / 1440f) * 360f - 90f
                    val sweepAngle = ((chunk.durationMinutes / 1440f) * 360f).coerceAtLeast(3f)

                    val chunkColor = try {
                        Color(android.graphics.Color.parseColor(chunk.colorHex))
                    } catch (_: Exception) {
                        HyperPrimary
                    }

                    drawArc(
                        color = chunkColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx - 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. Current Time Sweep Indicator Needle
                val currentAngleDeg = (currentMinute / 1440f) * 360f - 90f
                val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())
                val needlePos = Offset(
                    (centerOffset.x + radius * cos(currentAngleRad)).toFloat(),
                    (centerOffset.y + radius * sin(currentAngleRad)).toFloat()
                )

                // Glowing ambient aura
                drawCircle(
                    color = HyperPrimary.copy(alpha = 0.28f),
                    radius = 14.dp.toPx() * pulseScale,
                    center = needlePos
                )
                // Crisp white center dot
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
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
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (activeChunk != null) {
                    val remaining = activeChunk.remainingMinutes(currentMinute)
                    val chunkColor = try {
                        Color(android.graphics.Color.parseColor(activeChunk.colorHex))
                    } catch (_: Exception) {
                        HyperPrimary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chunkColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${activeChunk.title} • ${remaining}m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = chunkColor,
                            maxLines = 1
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(HyperSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Free Slot",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // Cardinal Legends (00:00 Top, 06:00 Right, 12:00 Bottom, 18:00 Left)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("12:00 AM Night", fontSize = 10.sp, color = TextTertiary)
            Text("12:00 PM Midday", fontSize = 10.sp, color = TextTertiary)
            Text("12:00 AM End", fontSize = 10.sp, color = TextTertiary)
        }
    }
}
