package com.hyperchunk.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.ui.theme.HyperCyan
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextTertiary

@Composable
fun DaySelector(
    selectedDay: Int, // 1..7 (Mon..Sun)
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HyperSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNumber = index + 1
            val isSelected = (selectedDay == dayNumber)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) HyperCyan else Color.Transparent,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "dayBg"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF0C0E12) else TextTertiary,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "dayText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .clickable { onDaySelected(dayNumber) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = textColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
