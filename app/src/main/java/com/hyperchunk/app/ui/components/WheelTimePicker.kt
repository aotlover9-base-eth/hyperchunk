package com.hyperchunk.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPositionInLayout
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.ui.theme.HyperPrimary
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val feedbackManager = remember { SmoothFeedbackManager(context) }

    DisposableEffect(feedbackManager) {
        onDispose {
            feedbackManager.release()
        }
    }

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

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
            feedbackManager = feedbackManager,
            onValueSelected = { newHour ->
                selectedHour = newHour
                onTimeChanged(newHour, selectedMinute)
            },
            modifier = Modifier.width(90.dp)
        )

        // Colon separator
        Text(
            text = ":",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = HyperPrimary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Minutes Wheel
        WheelColumn(
            count = 60,
            initialValue = initialMinute,
            feedbackManager = feedbackManager,
            onValueSelected = { newMinute ->
                selectedMinute = newMinute
                onTimeChanged(selectedHour, newMinute)
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
    feedbackManager: SmoothFeedbackManager,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val itemHeight = 44.dp
    val totalHeight = 180.dp
    val verticalPadding = (totalHeight - itemHeight) / 2 // Exactly 68.dp to align with center highlight

    val totalRepeat = 100
    val startIndex = (totalRepeat / 2) * count + initialValue

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapLayoutInfoProvider = remember(listState) {
        SnapLayoutInfoProvider(
            lazyListState = listState,
            positionInLayout = SnapPositionInLayout.CenterToCenter
        )
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = snapLayoutInfoProvider)

    // Mathematically derive the item closest to the viewport center at all times
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf initialValue
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val closest = visibleItems.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }
            (closest?.index ?: startIndex) % count
        }
    }

    // Smooth subtle micro-tick and tactile haptic pulse trigger
    LaunchedEffect(listState) {
        var previousRawIndex: Int? = null

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) null
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { currentRawIndex ->
                if (previousRawIndex != null && previousRawIndex != currentRawIndex) {
                    feedbackManager.playTick(view)
                }
                previousRawIndex = currentRawIndex
                onValueSelected(currentRawIndex % count)
            }
    }

    Box(
        modifier = modifier.height(totalHeight),
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
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.height(totalHeight),
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
                        color = if (isSelected) HyperPrimary else TextSecondary,
                        modifier = Modifier
                            .scale(scale)
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}
