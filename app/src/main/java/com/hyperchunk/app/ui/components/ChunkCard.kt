package com.hyperchunk.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.ui.theme.HyperAmber
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperEmerald
import com.hyperchunk.app.ui.theme.HyperPrimary
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary

@Composable
fun ChunkCard(
    chunk: TimeChunk,
    currentMinute: Int,
    onCardClick: () -> Unit,
    onAlarmToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = chunk.isCurrentlyActive(currentMinute)

    val categoryColor = try {
        Color(android.graphics.Color.parseColor(chunk.colorHex))
    } catch (e: Exception) {
        HyperPrimary
    }

    val borderColor by animateColorAsState(
        targetValue = if (isActive) categoryColor.copy(alpha = 0.8f) else HyperBorder,
        label = "cardBorder"
    )

    // Pulsing dot animation for active task
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    HyperCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = borderColor,
        borderWidth = if (isActive) 1.5.dp else 1.dp,
        backgroundColor = if (isActive) Color(0xFF141822) else HyperSurface,
        onClick = onCardClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Time, Category Badge, Alarm Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Color Pill
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(categoryColor)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${chunk.startTimeFormatted} – ${chunk.endTimeFormatted}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) TextPrimary else TextSecondary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "(${chunk.durationFormatted})",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        // "LIVE" Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(categoryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(pulseAlpha)
                                    .background(categoryColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Alarm Toggle Button
                    IconButton(
                        onClick = onAlarmToggle,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (chunk.hasAlarm) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Toggle Alarm",
                            tint = if (chunk.hasAlarm) HyperAmber else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Notes
            Text(
                text = chunk.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (chunk.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chunk.notes,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            // If Active: Live Progress Bar & Countdown
            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                val progress = chunk.progress(currentMinute)
                val remaining = chunk.remainingMinutes(currentMinute)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = categoryColor,
                    trackColor = Color(0xFF1E2533)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% elapsed",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = if (remaining >= 60) "${remaining / 60}h ${remaining % 60}m remaining" else "${remaining}m remaining",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HyperEmerald
                    )
                }
            }
        }
    }
}
