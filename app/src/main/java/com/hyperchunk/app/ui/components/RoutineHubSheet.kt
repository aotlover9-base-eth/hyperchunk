package com.hyperchunk.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.toMonochromeColor
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.data.storage.RoutinePreset
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperRose
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.HyperSurfaceVariant
import com.hyperchunk.app.ui.theme.LocalAppSettings
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import com.hyperchunk.app.ui.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutineHubSheet(
    selectedDay: Int,
    dayName: String,
    customPresets: List<RoutinePreset>,
    onDismiss: () -> Unit,
    onSyncToTargets: (List<Int>, Boolean) -> Unit,
    onApplyPreset: (RoutinePreset, Boolean) -> Unit,
    onSaveDayAsPreset: () -> Unit,
    onCreateCustomPreset: () -> Unit,
    onEditPreset: (RoutinePreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onClearDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appColors = LocalAppThemeColors.current
    val settings = LocalAppSettings.current
    val isMonochrome = settings.paletteMode == PaletteMode.MONOCHROME

    // Granular 7-day multi-selection state
    val selectedTargetDays = remember { mutableStateListOf<Int>() }
    var mergeOnlyMode by remember { mutableStateOf(false) }

    val dayLabels = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6, "Sun" to 7)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = appColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF262D3D))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Modular Routine Studio",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(appColors.surfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = appColors.primary
                            )
                        }
                    }
                    Text(
                        text = "Manage schedules & routine archetypes for $dayName",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Fast Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save Day as Preset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HyperSurface)
                        .border(1.dp, HyperBorder, RoundedCornerShape(16.dp))
                        .clickable { onSaveDayAsPreset() }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = appColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save $dayName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Build Custom Routine
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(appColors.surfaceElevated)
                        .border(1.dp, appColors.border, RoundedCornerShape(16.dp))
                        .clickable { onCreateCustomPreset() }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = appColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New Routine",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: GRANULAR 7-DAY SYNC MATRIX
            Text(
                text = "SCHEDULE REPLICATION MATRIX",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(HyperSurface)
                    .border(1.dp, HyperBorder, RoundedCornerShape(22.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Copy $dayName to:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

                        // Quick helper shortcuts
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Weekdays",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(appColors.surfaceElevated)
                                    .clickable {
                                        selectedTargetDays.clear()
                                        selectedTargetDays.addAll(listOf(1, 2, 3, 4, 5).filter { it != selectedDay })
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            Text(
                                text = "Weekend",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(appColors.surfaceElevated)
                                    .clickable {
                                        selectedTargetDays.clear()
                                        selectedTargetDays.addAll(listOf(6, 7).filter { it != selectedDay })
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            Text(
                                text = "All",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(appColors.surfaceElevated)
                                    .clickable {
                                        selectedTargetDays.clear()
                                        selectedTargetDays.addAll((1..7).filter { it != selectedDay })
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7-Day Pill Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLabels.forEach { (label, dayNum) ->
                            val isSource = (dayNum == selectedDay)
                            val isSelected = selectedTargetDays.contains(dayNum)

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSource -> appColors.primary.copy(alpha = 0.35f)
                                            isSelected -> appColors.primary
                                            else -> HyperSurfaceElevated
                                        }
                                    )
                                    .border(
                                        width = if (isSource || isSelected) 1.5.dp else 1.dp,
                                        color = if (isSource || isSelected) appColors.primary else HyperBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !isSource) {
                                        if (isSelected) {
                                            selectedTargetDays.remove(dayNum)
                                        } else {
                                            selectedTargetDays.add(dayNum)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isSource -> appColors.primary
                                        isSelected -> appColors.onPrimary
                                        else -> TextSecondary
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Toggle: Overwrite vs Smart Merge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HyperSurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (!mergeOnlyMode) appColors.primary else Color.Transparent)
                                .clickable { mergeOnlyMode = false }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Overwrite Target",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!mergeOnlyMode) appColors.onPrimary else TextTertiary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (mergeOnlyMode) appColors.primary else Color.Transparent)
                                .clickable { mergeOnlyMode = true }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Smart Merge (Keep Existing)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mergeOnlyMode) appColors.onPrimary else TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Execute Button
                    Button(
                        onClick = {
                            if (selectedTargetDays.isNotEmpty()) {
                                onSyncToTargets(selectedTargetDays.toList(), mergeOnlyMode)
                            }
                        },
                        enabled = selectedTargetDays.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.primary,
                            contentColor = appColors.onPrimary,
                            disabledContainerColor = HyperSurfaceElevated,
                            disabledContentColor = TextTertiary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedTargetDays.isEmpty()) {
                                    "Select target days above"
                                } else {
                                    "Replicate $dayName to ${selectedTargetDays.size} Days"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: CUSTOM USER ROUTINES (if any)
            if (customPresets.isNotEmpty()) {
                Text(
                    text = "CUSTOM ROUTINES (${customPresets.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    customPresets.forEach { preset ->
                        PresetCard(
                            preset = preset,
                            dayName = dayName,
                            isMonochrome = isMonochrome,
                            onApply = { onApplyPreset(preset, mergeOnlyMode) },
                            onEdit = { onEditPreset(preset) },
                            onDelete = { onDeletePreset(preset.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // SECTION 3: CURATED ARCHETYPES (Built-in)
            Text(
                text = "CURATED ARCHETYPES",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkRepository.BUILT_IN_PRESETS.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        dayName = dayName,
                        isMonochrome = isMonochrome,
                        onApply = { onApplyPreset(preset, mergeOnlyMode) },
                        onEdit = { onEditPreset(preset) },
                        onDelete = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: CLEAR DAY
            Button(
                onClick = onClearDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HyperSurfaceVariant)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (appColors.isMonochrome) Color(0xFFCBD5E1) else HyperRose,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clear All Chunks on $dayName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (appColors.isMonochrome) Color(0xFFCBD5E1) else HyperRose
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetCard(
    preset: RoutinePreset,
    dayName: String,
    isMonochrome: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    val appColors = LocalAppThemeColors.current

    val totalMins = preset.blocks.sumOf {
        if (it.endMinute >= it.startMinute) it.endMinute - it.startMinute else (1440 - it.startMinute) + it.endMinute
    }
    val totalHours = totalMins / 60f

    fun formatMinute12h(minute: Int): String {
        val normalized = minute % 1440
        val h = normalized / 60
        val m = normalized % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        val amPm = if (h < 12) "AM" else "PM"
        return String.format("%d:%02d %s", h12, m, amPm)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HyperSurface)
            .border(1.dp, HyperBorder, RoundedCornerShape(22.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = preset.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.name,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (preset.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(appColors.surfaceElevated)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.primary
                                    )
                                }
                            }
                        }
                        Text(
                            text = preset.description,
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp,
                            maxLines = if (expanded) 3 else 1
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Apply Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(appColors.surfaceElevated)
                            .clickable { onApply() }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Block Inspector & Timeline Preview
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Mini Timeline Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIMELINE PREVIEW",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = String.format("%.1fh active", totalHours),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        var xCursor = 0f
                        val totalW = size.width
                        preset.blocks.forEach { b ->
                            val dur = if (b.endMinute >= b.startMinute) b.endMinute - b.startMinute else (1440 - b.startMinute) + b.endMinute
                            val barW = (dur / 1440f) * totalW
                            val col = if (isMonochrome) b.category.toMonochromeColor() else parseHexColor(b.category.defaultColor, appColors.primary)
                            drawRect(
                                color = col,
                                topLeft = Offset(xCursor, 0f),
                                size = Size(barW.coerceAtLeast(1.5f), size.height)
                            )
                            xCursor += barW
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Block Chips List
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        preset.blocks.forEach { block ->
                            val catColor = if (isMonochrome) {
                                block.category.toMonochromeColor()
                            } else {
                                parseHexColor(block.category.defaultColor, appColors.primary)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HyperSurfaceElevated)
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(catColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${formatMinute12h(block.startMinute)} ${block.title}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Card Bottom Action Row (Edit, Clone, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDelete != null) {
                            Text(
                                text = "Delete",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = HyperRose,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDelete() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = if (preset.isCustom) "Edit Routine" else "Clone & Edit",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(appColors.surfaceElevated)
                                .clickable { onEdit() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
