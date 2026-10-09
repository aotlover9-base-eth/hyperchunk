package com.hyperchunk.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.ChunkCategory
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.toMonochromeColor
import com.hyperchunk.app.data.storage.PresetBlock
import com.hyperchunk.app.data.storage.RoutinePreset
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperRose
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.LocalAppSettings
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import com.hyperchunk.app.ui.theme.parseHexColor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPresetSheet(
    presetToEdit: RoutinePreset?,
    onDismiss: () -> Unit,
    onSave: (RoutinePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appColors = LocalAppThemeColors.current
    val settings = LocalAppSettings.current
    val isMonochrome = settings.paletteMode == PaletteMode.MONOCHROME

    var name by remember { mutableStateOf(presetToEdit?.name ?: "") }
    var description by remember { mutableStateOf(presetToEdit?.description ?: "") }
    var selectedEmoji by remember { mutableStateOf(presetToEdit?.iconEmoji ?: "⚡") }

    val emojiCatalog = listOf("⚡", "⚖️", "🎓", "💼", "🏋️", "🎯", "🌙", "🧘", "🚀", "💻", "🎨", "📚", "☕", "🏃", "🔋", "🛠️")

    val blocks = remember {
        mutableStateListOf<PresetBlock>().apply {
            if (presetToEdit != null) {
                addAll(presetToEdit.blocks)
            } else {
                // Sensible default starter blocks
                addAll(
                    listOf(
                        PresetBlock("Sleep & Rest", 0, 480, ChunkCategory.SLEEP, hasAlarm = true),
                        PresetBlock("Morning Routine", 480, 540, ChunkCategory.ROUTINE),
                        PresetBlock("Deep Work Sprint", 540, 780, ChunkCategory.WORK, hasAlarm = true),
                        PresetBlock("Lunch & Recharge", 780, 840, ChunkCategory.MEAL),
                        PresetBlock("Afternoon Focus", 840, 1080, ChunkCategory.STUDY),
                        PresetBlock("Workout / Life", 1080, 1200, ChunkCategory.FITNESS, hasAlarm = true),
                        PresetBlock("Evening Wind-down", 1200, 1440, ChunkCategory.REST)
                    )
                )
            }
        }
    }

    val totalMins = blocks.sumOf {
        if (it.endMinute >= it.startMinute) it.endMinute - it.startMinute else (1440 - it.startMinute) + it.endMinute
    }
    val totalHours = totalMins / 60f
    val freeHours = (1440 - totalMins).coerceAtLeast(0) / 60f

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
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (presetToEdit == null) "Create Routine Preset" else "Edit Routine Preset",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Build a reusable 24-hour routine archetype",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Preset Details Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(HyperSurface)
                            .border(1.dp, HyperBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            // Emoji Selector
                            Text(
                                text = "ROUTINE ICON",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                emojiCatalog.forEach { emoji ->
                                    val isSelected = emoji == selectedEmoji
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) appColors.primary.copy(alpha = 0.25f) else HyperSurfaceElevated)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) appColors.primary else HyperBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedEmoji = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Name Input
                            Text(
                                text = "ROUTINE TITLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HyperSurfaceElevated)
                                    .border(1.dp, HyperBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    cursorBrush = SolidColor(appColors.primary),
                                    decorationBox = { innerTextField ->
                                        if (name.isEmpty()) {
                                            Text(
                                                text = "e.g. Focus Sprint, Exam Marathon, Sunday Reset",
                                                color = TextTertiary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Description Input
                            Text(
                                text = "DESCRIPTION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HyperSurfaceElevated)
                                    .border(1.dp, HyperBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    textStyle = TextStyle(
                                        color = TextSecondary,
                                        fontSize = 12.5.sp
                                    ),
                                    cursorBrush = SolidColor(appColors.primary),
                                    decorationBox = { innerTextField ->
                                        if (description.isEmpty()) {
                                            Text(
                                                text = "Short summary of focus, hours, or archetype",
                                                color = TextTertiary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 24H Allocation Balance Bar
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(HyperSurface)
                            .border(1.dp, HyperBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "24-HOUR BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = String.format("%.1fh active • %.1fh free", totalHours, freeHours),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalMins > 1440) HyperRose else appColors.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Timeline preview bar
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                var xCursor = 0f
                                val totalW = size.width
                                blocks.forEach { b ->
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
                                if (xCursor < totalW) {
                                    drawRect(
                                        color = Color(0xFF1B202E),
                                        topLeft = Offset(xCursor, 0f),
                                        size = Size(totalW - xCursor, size.height)
                                    )
                                }
                            }
                        }
                    }
                }

                // Blocks Header & Add Block Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIME BLOCKS (${blocks.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.2.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.surfaceElevated)
                                .clickable {
                                    val lastEnd = blocks.lastOrNull()?.endMinute ?: 540
                                    val newEnd = (lastEnd + 120) % 1440
                                    blocks.add(
                                        PresetBlock(
                                            title = "Focus Block",
                                            startMinute = lastEnd,
                                            endMinute = if (newEnd == 0) 1440 else newEnd,
                                            category = ChunkCategory.WORK,
                                            hasAlarm = false
                                        )
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Block", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = appColors.primary)
                            }
                        }
                    }
                }

                // Blocks List
                itemsIndexed(blocks) { index, block ->
                    PresetBlockItemCard(
                        block = block,
                        isMonochrome = isMonochrome,
                        onUpdate = { updated ->
                            blocks[index] = updated
                        },
                        onDelete = {
                            if (blocks.size > 1) {
                                blocks.removeAt(index)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save Action Button
            Button(
                onClick = {
                    val finalName = name.ifBlank { "Custom Routine" }
                    val finalPreset = RoutinePreset(
                        id = presetToEdit?.id ?: UUID.randomUUID().toString(),
                        name = finalName,
                        description = description.ifBlank { "${blocks.size} blocks scheduled" },
                        iconEmoji = selectedEmoji,
                        blocks = blocks.toList(),
                        isCustom = true
                    )
                    onSave(finalPreset)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.primary,
                    contentColor = appColors.onPrimary
                )
            ) {
                Text(
                    text = if (presetToEdit == null) "Save Routine Preset" else "Update Routine Preset",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PresetBlockItemCard(
    block: PresetBlock,
    isMonochrome: Boolean,
    onUpdate: (PresetBlock) -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalAppThemeColors.current
    val categoryColor = if (isMonochrome) {
        block.category.toMonochromeColor()
    } else {
        parseHexColor(block.category.defaultColor, appColors.primary)
    }

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
            .clip(RoundedCornerShape(18.dp))
            .background(HyperSurface)
            .border(1.dp, HyperBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = block.title,
                        onValueChange = { onUpdate(block.copy(title = it)) },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(appColors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Alarm Toggle
                    IconButton(
                        onClick = { onUpdate(block.copy(hasAlarm = !block.hasAlarm)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (block.hasAlarm) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                            contentDescription = "Toggle Alarm",
                            tint = if (block.hasAlarm) appColors.primary else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete Block
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = HyperRose.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timing Adjusters & Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Start Time Controls
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HyperSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatMinute12h(block.startMinute),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Text("→", fontSize = 11.sp, color = TextTertiary)

                    // End Time Controls
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HyperSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatMinute12h(block.endMinute),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Quick Step +/- 30m buttons
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HyperSurfaceElevated)
                            .clickable {
                                val newEnd = (block.endMinute + 30).coerceAtMost(1440)
                                onUpdate(block.copy(endMinute = newEnd))
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("+30m", fontSize = 10.sp, color = appColors.primary, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HyperSurfaceElevated)
                            .clickable {
                                val minEnd = block.startMinute + 15
                                val newEnd = (block.endMinute - 30).coerceAtLeast(minEnd)
                                onUpdate(block.copy(endMinute = newEnd))
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("-30m", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                // Category Selector Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.2f))
                        .clickable {
                            val allCats = ChunkCategory.values()
                            val nextCat = allCats[(block.category.ordinal + 1) % allCats.size]
                            onUpdate(block.copy(category = nextCat))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = block.category.displayName,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }
            }
        }
    }
}
