package com.hyperchunk.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.ui.components.Chunk24hBar
import com.hyperchunk.app.ui.components.ChunkCard
import com.hyperchunk.app.ui.components.DaySelector
import com.hyperchunk.app.ui.components.HyperCard
import com.hyperchunk.app.ui.theme.HyperBgDark
import com.hyperchunk.app.ui.theme.HyperCoral
import com.hyperchunk.app.ui.theme.HyperCyan
import com.hyperchunk.app.ui.theme.HyperLime
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HomeScreen(
    repository: ChunkRepository,
    onScheduleAlarm: (TimeChunk) -> Unit,
    onCancelAlarm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current day & time tracker
    var currentMinuteOfDay by remember {
        val cal = Calendar.getInstance()
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
    }

    val todayDayOfWeek = remember {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    var selectedDay by remember { mutableIntStateOf(todayDayOfWeek) }

    // Ticking minute loop
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            delay(15000) // check every 15s
        }
    }

    val allChunks by repository.chunks.collectAsState()
    val dayChunks = remember(allChunks, selectedDay) {
        repository.getChunksForDay(selectedDay)
    }

    var chunkToEdit by remember { mutableStateOf<TimeChunk?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }

    val activeChunk = remember(dayChunks, currentMinuteOfDay, selectedDay, todayDayOfWeek) {
        if (selectedDay == todayDayOfWeek) {
            dayChunks.firstOrNull { it.isCurrentlyActive(currentMinuteOfDay) }
        } else null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HyperBgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    chunkToEdit = null
                    showEditSheet = true
                },
                containerColor = HyperCyan,
                contentColor = Color(0xFF0C0E12),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Chunk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(HyperCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HYPERCHUNK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HyperCyan,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "24-Hour Time Planner",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = { showCopyDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HyperSurface)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Schedule",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Day Selector Pill Row
            DaySelector(
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Hero Active Chunk Card (if viewing today & a chunk is active)
                if (activeChunk != null) {
                    item {
                        HyperCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF141A24),
                            borderColor = HyperCyan,
                            borderWidth = 1.5.dp
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CURRENT ACTIVITY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HyperCyan,
                                        letterSpacing = 1.sp
                                    )
                                    val remaining = activeChunk.remainingMinutes(currentMinuteOfDay)
                                    Text(
                                        text = if (remaining >= 60) "${remaining / 60}h ${remaining % 60}m left" else "${remaining}m left",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HyperLime
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = activeChunk.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )

                                Text(
                                    text = "${activeChunk.startTimeFormatted} – ${activeChunk.endTimeFormatted} (${activeChunk.durationFormatted})",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )

                                if (activeChunk.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activeChunk.notes,
                                        fontSize = 12.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. 24-Hour Visual Segment Ribbon
                item {
                    Chunk24hBar(
                        chunks = dayChunks,
                        currentMinute = if (selectedDay == todayDayOfWeek) currentMinuteOfDay else 0
                    )
                }

                // 3. Daily Stats Bar
                item {
                    val totalChunksCount = dayChunks.size
                    val totalStudyHours = dayChunks
                        .filter { it.category == ChunkCategory.STUDY || it.category == ChunkCategory.WORK }
                        .sumOf { it.durationMinutes } / 60f
                    val alarmsCount = dayChunks.count { it.hasAlarm }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(HyperSurface)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BLOCKS", fontSize = 9.sp, color = TextTertiary, letterSpacing = 0.5.sp)
                            Text("$totalChunksCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FOCUS", fontSize = 9.sp, color = TextTertiary, letterSpacing = 0.5.sp)
                            Text(String.format("%.1fh", totalStudyHours), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HyperCyan)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ALARMS", fontSize = 9.sp, color = TextTertiary, letterSpacing = 0.5.sp)
                            Text("$alarmsCount Active", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HyperLime)
                        }
                    }
                }

                // 4. Chunk List
                if (dayChunks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No Chunks Configured",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ Add Chunk' below to segment this day",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = dayChunks,
                        key = { it.id }
                    ) { chunk ->
                        ChunkCard(
                            chunk = chunk,
                            currentMinute = if (selectedDay == todayDayOfWeek) currentMinuteOfDay else -1,
                            onCardClick = {
                                chunkToEdit = chunk
                                showEditSheet = true
                            },
                            onAlarmToggle = {
                                repository.toggleAlarm(chunk.id)
                                val updated = chunk.copy(hasAlarm = !chunk.hasAlarm)
                                if (updated.hasAlarm) {
                                    onScheduleAlarm(updated)
                                } else {
                                    onCancelAlarm(chunk.id)
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Modal Edit Sheet
    if (showEditSheet) {
        EditChunkSheet(
            chunkToEdit = chunkToEdit,
            dayOfWeek = selectedDay,
            onDismiss = { showEditSheet = false },
            onSave = { savedChunk ->
                if (chunkToEdit == null) {
                    repository.addChunk(savedChunk)
                } else {
                    repository.updateChunk(savedChunk)
                }
                if (savedChunk.hasAlarm) {
                    onScheduleAlarm(savedChunk)
                } else {
                    onCancelAlarm(savedChunk.id)
                }
                showEditSheet = false
            },
            onDelete = { idToDelete ->
                onCancelAlarm(idToDelete)
                repository.deleteChunk(idToDelete)
                showEditSheet = false
            }
        )
    }

    // Copy Schedule Dialog
    if (showCopyDialog) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var targetDay by remember { mutableIntStateOf(if (selectedDay == 7) 1 else selectedDay + 1) }

        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = {
                Text(
                    "Duplicate Day Schedule",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Copy all chunks from ${days[selectedDay - 1]} to:",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    days.forEachIndexed { idx, name ->
                        val dNum = idx + 1
                        if (dNum != selectedDay) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (targetDay == dNum) HyperSurfaceElevated else Color.Transparent)
                                    .clickable { targetDay = dNum }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, color = if (targetDay == dNum) HyperCyan else TextPrimary)
                                if (targetDay == dNum) {
                                    Text("Selected", color = HyperCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            dayChunks.forEach { repository.deleteChunk(it.id) }
                            showCopyDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HyperSurfaceElevated),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Clear ${days[selectedDay - 1]} Chunks",
                            color = HyperCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.copySchedule(selectedDay, targetDay)
                        showCopyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Copy Schedule", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF181B22),
            shape = RoundedCornerShape(26.dp)
        )
    }
}
