package com.hyperchunk.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.alarm.AlarmScheduler
import com.hyperchunk.app.data.model.ChunkCategory
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.ui.components.WheelTimePicker
import com.hyperchunk.app.ui.theme.HyperAmber
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperCoral
import com.hyperchunk.app.ui.theme.HyperEmerald
import com.hyperchunk.app.ui.theme.HyperPrimary
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.HyperSurfaceVariant
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChunkSheet(
    chunkToEdit: TimeChunk?,
    dayOfWeek: Int,
    defaultStartMinute: Int = 9 * 60,
    onDismiss: () -> Unit,
    onSave: (TimeChunk) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(chunkToEdit?.title ?: "") }
    var category by remember { mutableStateOf(chunkToEdit?.category ?: ChunkCategory.STUDY) }
    var notes by remember { mutableStateOf(chunkToEdit?.notes ?: "") }
    var hasAlarm by remember { mutableStateOf(chunkToEdit?.hasAlarm ?: true) }

    val initialStartMin = chunkToEdit?.startMinuteOfDay ?: defaultStartMinute
    val initialEndMin = chunkToEdit?.endMinuteOfDay ?: ((initialStartMin + 60) % 1440)

    var startHour by remember(chunkToEdit, defaultStartMinute) { mutableIntStateOf(initialStartMin / 60) }
    var startMinute by remember(chunkToEdit, defaultStartMinute) { mutableIntStateOf(initialStartMin % 60) }

    var endHour by remember(chunkToEdit, defaultStartMinute) { mutableIntStateOf(initialEndMin / 60) }
    var endMinute by remember(chunkToEdit, defaultStartMinute) { mutableIntStateOf(initialEndMin % 60) }

    // Selected duration preset in minutes: 30, 60, 120, 180, or null (custom)
    var selectedDurationPreset by remember(chunkToEdit, defaultStartMinute) {
        val diff = if (initialEndMin >= initialStartMin) (initialEndMin - initialStartMin) else (1440 - initialStartMin + initialEndMin)
        mutableStateOf<Int?>(if (diff in listOf(30, 60, 120, 180)) diff else if (chunkToEdit == null) 60 else null)
    }

    // 0 = Edit Start Time, 1 = Edit End Time
    var activeTimeTab by remember { mutableIntStateOf(0) }

    fun formatTime12h(h24: Int, m: Int): String {
        val hour12 = when (val h = h24 % 12) {
            0 -> 12
            else -> h
        }
        val amPm = if (h24 < 12) "AM" else "PM"
        return String.format("%d:%02d %s", hour12, m, amPm)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color(0xFF13161C),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2C3240))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (chunkToEdit == null) "New Time Chunk" else "Edit Time Chunk",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What are you doing?") },
                placeholder = { Text("e.g. College Lecture, Deep Work, Gym") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HyperPrimary,
                    unfocusedBorderColor = HyperBorder,
                    focusedContainerColor = HyperSurface,
                    unfocusedContainerColor = HyperSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Category Chips
            Text("CATEGORY", fontSize = 11.sp, color = TextTertiary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChunkCategory.values()) { cat ->
                    val isSelected = (cat == category)
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(cat.defaultColor))
                    } catch (e: Exception) {
                        HyperPrimary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) catColor else HyperSurface)
                            .clickable { category = cat }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0C0E12) else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Time Selector Segmented Buttons (Start vs End)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(HyperSurface)
                    .padding(4.dp)
            ) {
                val startFormatted = formatTime12h(startHour, startMinute)
                val endFormatted = formatTime12h(endHour, endMinute)

                // Start Time Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeTimeTab == 0) HyperSurfaceElevated else Color.Transparent)
                        .clickable { activeTimeTab = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STARTS AT", fontSize = 10.sp, color = TextTertiary)
                        Text(
                            text = startFormatted,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTimeTab == 0) HyperPrimary else TextPrimary
                        )
                    }
                }

                // End Time Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeTimeTab == 1) HyperSurfaceElevated else Color.Transparent)
                        .clickable { activeTimeTab = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ENDS AT", fontSize = 10.sp, color = TextTertiary)
                        Text(
                            text = endFormatted,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTimeTab == 1) HyperPrimary else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Duration Preset Buttons: 30m, 1 hr, 2 hr, 3 hr, Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val presets = listOf(
                    30 to "30m",
                    60 to "1 hr",
                    120 to "2 hr",
                    180 to "3 hr"
                )

                presets.forEach { (durationMins, label) ->
                    val isSelected = (selectedDurationPreset == durationMins)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) HyperPrimary else HyperSurface)
                            .clickable {
                                selectedDurationPreset = durationMins
                                val newEndTotal = (startHour * 60 + startMinute + durationMins) % 1440
                                endHour = newEndTotal / 60
                                endMinute = newEndTotal % 60
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }

                // "Edit End" button to manually scroll the end clock
                val isCustom = (selectedDurationPreset == null)
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCustom && activeTimeTab == 1) HyperSurfaceElevated else HyperSurface)
                        .clickable {
                            selectedDurationPreset = null
                            activeTimeTab = 1
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit End",
                        fontSize = 12.sp,
                        fontWeight = if (isCustom && activeTimeTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCustom && activeTimeTab == 1) HyperPrimary else TextSecondary
                    )
                }
            }

            // Real-Time Dynamic Timing Preview
            val startTotalMinutes = startHour * 60 + startMinute
            val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(dayOfWeek, startTotalMinutes)
            val diffMs = triggerMillis - System.currentTimeMillis()
            val diffMins = (diffMs / 60000).coerceAtLeast(0)
            val timingText = when {
                diffMins < 1 -> "Starts in less than 1 minute"
                diffMins < 60 -> "Starts in ${diffMins}m"
                diffMins < 1440 -> "Starts in ${diffMins / 60}h ${diffMins % 60}m"
                else -> "Starts in ${diffMins / 1440}d ${diffMins / 60 % 24}h"
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HyperSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(HyperEmerald, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timingText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HyperEmerald
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Buttery Smooth Wheel Time Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HyperSurface)
                    .padding(vertical = 8.dp)
            ) {
                if (activeTimeTab == 0) {
                    WheelTimePicker(
                        initialHour = startHour,
                        initialMinute = startMinute,
                        onTimeChanged = { h, m ->
                            startHour = h
                            startMinute = m
                            selectedDurationPreset?.let { durationMins ->
                                val newEndTotal = (h * 60 + m + durationMins) % 1440
                                endHour = newEndTotal / 60
                                endMinute = newEndTotal % 60
                            }
                        }
                    )
                } else {
                    WheelTimePicker(
                        initialHour = endHour,
                        initialMinute = endMinute,
                        onTimeChanged = { h, m ->
                            endHour = h
                            endMinute = m
                            val curDuration = (h * 60 + m) - (startHour * 60 + startMinute)
                            selectedDurationPreset = if (curDuration in listOf(30, 60, 120, 180)) curDuration else null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Alarm Toggle Switch Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(HyperSurface)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Alarm",
                        tint = if (hasAlarm) HyperAmber else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sound Alarm on Start",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Heads-up sound alert when chunk begins",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = hasAlarm,
                    onCheckedChange = { hasAlarm = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HyperPrimary,
                        checkedTrackColor = HyperSurfaceElevated,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = HyperSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & Subtasks (optional)") },
                placeholder = { Text("Bring textbook, revise formulas, hydrate...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HyperPrimary,
                    unfocusedBorderColor = HyperBorder,
                    focusedContainerColor = HyperSurface,
                    unfocusedContainerColor = HyperSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    val sMin = startHour * 60 + startMinute
                    val eMin = endHour * 60 + endMinute
                    val adjustedEnd = if (eMin <= sMin) (eMin + 1440) else eMin

                    val updated = TimeChunk(
                        id = chunkToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                        dayOfWeek = dayOfWeek,
                        title = if (title.isBlank()) category.displayName else title.trim(),
                        startMinuteOfDay = sMin,
                        endMinuteOfDay = if (adjustedEnd > 1440) 1440 else adjustedEnd,
                        category = category,
                        colorHex = category.defaultColor,
                        hasAlarm = hasAlarm,
                        notes = notes.trim()
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HyperPrimary)
            ) {
                Text(
                    text = if (chunkToEdit == null) "Add to Schedule" else "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0C0E12)
                )
            }

            if (chunkToEdit != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onDelete(chunkToEdit.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HyperSurfaceElevated)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = HyperCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Chunk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HyperCoral
                    )
                }
            }
        }
    }
}
