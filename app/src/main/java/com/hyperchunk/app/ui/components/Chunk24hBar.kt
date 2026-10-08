package com.hyperchunk.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.ui.theme.HyperCyan
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.TextTertiary

@Composable
fun Chunk24hBar(
    chunks: List<TimeChunk>,
    currentMinute: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HyperSurface)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("24-HOUR TIMELINE", fontSize = 11.sp, color = TextTertiary, letterSpacing = 1.sp)
            val currentH = currentMinute / 60
            val currentM = currentMinute % 60
            Text(
                text = String.format("NOW %02d:%02d", currentH, currentM),
                fontSize = 11.sp,
                color = HyperCyan,
                letterSpacing = 1.sp
            )
        }

        // Multi-segment 24h bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            val totalMinutes = 1440f
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Base track
            drawRoundRect(
                color = Color(0xFF1E222C),
                size = size,
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Draw each chunk block
            chunks.forEach { chunk ->
                val startRatio = chunk.startMinuteOfDay / totalMinutes
                val durationRatio = chunk.durationMinutes / totalMinutes

                val x = startRatio * canvasWidth
                val width = durationRatio * canvasWidth

                val color = try {
                    Color(android.graphics.Color.parseColor(chunk.colorHex))
                } catch (e: Exception) {
                    HyperCyan
                }

                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(x, 0f),
                    size = Size(width.coerceAtLeast(3f), canvasHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Draw moving current-time needle indicator
            val needleRatio = (currentMinute % 1440) / totalMinutes
            val needleX = needleRatio * canvasWidth

            // Glowing indicator
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = Offset(needleX, canvasHeight / 2)
            )
            drawCircle(
                color = HyperCyan.copy(alpha = 0.5f),
                radius = 12f,
                center = Offset(needleX, canvasHeight / 2)
            )
        }

        // Time ticks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00", fontSize = 10.sp, color = TextTertiary)
            Text("06:00", fontSize = 10.sp, color = TextTertiary)
            Text("12:00", fontSize = 10.sp, color = TextTertiary)
            Text("18:00", fontSize = 10.sp, color = TextTertiary)
            Text("24:00", fontSize = 10.sp, color = TextTertiary)
        }
    }
}
