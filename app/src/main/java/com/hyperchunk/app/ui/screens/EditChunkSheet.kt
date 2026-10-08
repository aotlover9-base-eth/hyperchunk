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
import com.hyperchunk.app.data.model.ChunkCategory
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.ui.components.WheelTimePicker
import com.hyperchunk.app.ui.theme.HyperAmber
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperCoral
import com.hyperchunk.app.ui.theme.HyperCyan
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
    onDismiss: () -> Unit,
    onSave: (TimeChunk) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(chunkToEdit?.title ?: "") }
    var category by remember { mutableStateOf(chunkToEdit?.category ?: ChunkCategory.STUDY) }
    var notes by remember { mutableStateOf(chunkToEdit?.notes ?: "") }
    var hasAlarm by remember { mutableStateOf(chunkToEdit?.hasAlarm ?: true) }

    val initialStartMin = chunkToEdit?.startMinuteOfDay ?: (9 * 60)
    val initialEndMin = chunkToEdit?.endMinuteOfDay ?: (11 * 60)

    var startHour by remember { mutableIntStateOf(initialStartMin / 60) }
    var startMinute by remember { mutableIntStateOf(initialStartMin % 60) }

    var endHour by remember { mutableIntStateOf(initialEndMin / 60) }
    var endMinute by remember { mutableIntStateOf(initialEndMin % 60) }

    // 0 = Edit Start Time, 1 = Edit End Time
    var activeTimeTab by remember { mutableIntStateOf(0) }

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
                    focusedBorderColor = HyperCyan,
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
                        HyperCyan
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
                val startFormatted = String.format("%02d:%02d", startHour, startMinute)
                val endFormatted = String.format("%02d:%02d", endHour, endMinute)

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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTimeTab == 0) HyperCyan else TextPrimary
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTimeTab == 1) HyperCyan else TextPrimary
                        )
                    }
                }
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
                        }
                    )
                } else {
                    WheelTimePicker(
                        initialHour = endHour,
                        initialMinute = endMinute,
                        onTimeChanged = { h, m ->
                            endHour = h
                            endMinute = m
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
                        checkedThumbColor = HyperCyan,
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
                    focusedBorderColor = HyperCyan,
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
                colors = ButtonDefaults.buttonColors(containerColor = HyperCyan)
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
