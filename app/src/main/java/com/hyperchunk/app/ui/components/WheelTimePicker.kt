package com.hyperchunk.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperCyan
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hours Wheel
        WheelColumn(
            count = 24,
            initialValue = initialHour,
            onValueSelected = { newHour ->
                onTimeChanged(newHour, initialMinute)
            },
            modifier = Modifier.width(90.dp)
        )

        // Colon separator
        Text(
            text = ":",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = HyperCyan,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Minutes Wheel
        WheelColumn(
            count = 60,
            initialValue = initialMinute,
            onValueSelected = { newMinute ->
                onTimeChanged(initialHour, newMinute)
            },
            modifier = Modifier.width(90.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    count: Int,
    initialValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 44.dp
    val totalRepeat = 100
    val startIndex = (totalRepeat / 2) * count + initialValue

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex - 1)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Center item index
    val selectedIndex by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val center = if (offset > 22) firstVisible + 2 else firstVisible + 1
            center % count
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { selectedIndex }
            .distinctUntilChanged()
            .collect { value ->
                onValueSelected(value)
            }
    }

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Highlight Pill (HyperOS Rounded Card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(HyperSurfaceElevated, RoundedCornerShape(16.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.height(180.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count * totalRepeat) { index ->
                val itemValue = index % count
                val isSelected = (itemValue == selectedIndex)

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.25f else 0.85f,
                    label = "scale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.35f,
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", itemValue),
                        fontSize = 24.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) HyperCyan else TextSecondary,
                        modifier = Modifier
                            .scale(scale)
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}
