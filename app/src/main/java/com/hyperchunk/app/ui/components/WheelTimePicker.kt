package com.hyperchunk.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPositionInLayout
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.HapticMode
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.LocalAppSettings
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val view = LocalView.current
    val feedbackManager = remember { SmoothFeedbackManager(context) }
    val settings = LocalAppSettings.current
    val isHapticsEnabled = settings.hapticMode != HapticMode.OFF
    val primaryColor = LocalAppThemeColors.current.primary

    DisposableEffect(feedbackManager) {
        onDispose {
            feedbackManager.release()
        }
    }

    val isAm = initialHour < 12
    val hour12 = when (val h = initialHour % 12) {
        0 -> 12
        else -> h
    }

    val currentOnTimeChanged by rememberUpdatedState(onTimeChanged)

    val rowHeight = if (compact) 138.dp else 180.dp
    val colWidth = if (compact) 68.dp else 78.dp
    val capsuleWidth = if (compact) 54.dp else 62.dp
    val capsuleHeight = if (compact) 88.dp else 104.dp
    val separatorSize = if (compact) 24.sp else 30.sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 12-Hour Hours Wheel (01..12)
        WheelColumn(
            count = 12,
            initialValue = hour12 - 1,
            labelFormatter = { String.format("%02d", it + 1) },
            feedbackManager = feedbackManager,
            isHapticsEnabled = isHapticsEnabled,
            compact = compact,
            onValueSelected = { newHourIndex ->
                val newHour12 = newHourIndex + 1
                val newH24 = if (isAm) {
                    if (newHour12 == 12) 0 else newHour12
                } else {
                    if (newHour12 == 12) 12 else newHour12 + 12
                }
                currentOnTimeChanged(newH24, initialMinute)
            },
            modifier = Modifier.width(colWidth)
        )

        // Colon separator
        Text(
            text = ":",
            fontSize = separatorSize,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 8.dp)
        )

        // Minutes Wheel (00..59)
        WheelColumn(
            count = 60,
            initialValue = initialMinute,
            labelFormatter = { String.format("%02d", it) },
            feedbackManager = feedbackManager,
            isHapticsEnabled = isHapticsEnabled,
            compact = compact,
            onValueSelected = { newMinute ->
                currentOnTimeChanged(initialHour, newMinute)
            },
            modifier = Modifier.width(colWidth)
        )

        Spacer(modifier = Modifier.width(if (compact) 10.dp else 14.dp))

        // AM / PM Segmented Capsule Column
        Column(
            modifier = Modifier
                .width(capsuleWidth)
                .height(capsuleHeight)
                .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
                .background(HyperSurfaceElevated)
                .padding(3.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // AM Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(if (compact) 12.dp else 15.dp))
                    .background(if (isAm) primaryColor else Color.Transparent)
                    .clickable {
                        if (!isAm) {
                            if (isHapticsEnabled) feedbackManager.playTick(view)
                            val newH24 = if (initialHour >= 12) initialHour - 12 else initialHour
                            currentOnTimeChanged(newH24, initialMinute)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = if (isAm) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isAm) Color.White else TextSecondary
                )
            }

            // PM Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(if (compact) 12.dp else 15.dp))
                    .background(if (!isAm) primaryColor else Color.Transparent)
                    .clickable {
                        if (isAm) {
                            if (isHapticsEnabled) feedbackManager.playTick(view)
                            val newH24 = if (initialHour < 12) initialHour + 12 else initialHour
                            currentOnTimeChanged(newH24, initialMinute)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = if (!isAm) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (!isAm) Color.White else TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    count: Int,
    initialValue: Int,
    feedbackManager: SmoothFeedbackManager,
    isHapticsEnabled: Boolean,
    compact: Boolean,
    onValueSelected: (Int) -> Unit,
    labelFormatter: (Int) -> String = { String.format("%02d", it) },
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val itemHeight: Dp = if (compact) 36.dp else 44.dp
    val totalHeight: Dp = if (compact) 138.dp else 180.dp
    val verticalPadding: Dp = (totalHeight - itemHeight) / 2
    val baseFontSize = if (compact) 20.sp else 24.sp
    val primaryColor = LocalAppThemeColors.current.primary

    val totalRepeat = 100
    val startIndex = (totalRepeat / 2) * count + initialValue

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val currentOnValueSelected by rememberUpdatedState(onValueSelected)

    LaunchedEffect(initialValue) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }
                val currentSelected = (closest?.index ?: listState.firstVisibleItemIndex) % count
                if (currentSelected != initialValue) {
                    val currentLoop = (listState.firstVisibleItemIndex / count).coerceAtLeast(1)
                    listState.scrollToItem(currentLoop * count + initialValue)
                }
            }
        }
    }

    val snapLayoutInfoProvider = remember(listState) {
        SnapLayoutInfoProvider(
            lazyListState = listState,
            positionInLayout = SnapPositionInLayout.CenterToCenter
        )
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = snapLayoutInfoProvider)

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
                    if (isHapticsEnabled) {
                        feedbackManager.playTick(view)
                    }
                }
                previousRawIndex = currentRawIndex
                currentOnValueSelected(currentRawIndex % count)
            }
    }

    Box(
        modifier = modifier.height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        // Center Highlight Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(HyperSurfaceElevated, RoundedCornerShape(if (compact) 12.dp else 16.dp))
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
                        text = labelFormatter(itemValue),
                        fontSize = baseFontSize,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) primaryColor else TextSecondary,
                        modifier = Modifier
                            .scale(scale)
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}
