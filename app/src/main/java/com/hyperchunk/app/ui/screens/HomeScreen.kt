package com.hyperchunk.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.ui.components.Chunk24hChronograph
import com.hyperchunk.app.ui.components.ChunkCard
import com.hyperchunk.app.ui.components.DaySelector
import com.hyperchunk.app.ui.components.RoutineHubSheet
import com.hyperchunk.app.ui.theme.HyperBgDark
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperEmerald
import com.hyperchunk.app.ui.theme.HyperPrimary
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.HyperSurfaceVariant
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
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

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

    // Live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            delay(15000)
        }
    }

    val allChunks by repository.chunks.collectAsState()
    val dayChunks = remember(allChunks, selectedDay) {
        repository.getChunksForDay(selectedDay)
    }

    var chunkToEdit by remember { mutableStateOf<TimeChunk?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showRoutineHubSheet by remember { mutableStateOf(false) }

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
                containerColor = HyperPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(HyperPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HYPERCHUNK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HyperPrimary,
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

                // Smart Routine Hub Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(HyperSurface)
                        .border(1.dp, HyperBorder, RoundedCornerShape(16.dp))
                        .clickable { showRoutineHubSheet = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Routines",
                            tint = HyperPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Routines",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Circular 24-Hour Polar Chronograph Dial
                item {
                    Chunk24hChronograph(
                        chunks = dayChunks,
                        currentMinute = if (selectedDay == todayDayOfWeek) currentMinuteOfDay else 0,
                        activeChunk = activeChunk
                    )
                }

                // 2. Chunks Header & Summary
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${dayNames[selectedDay - 1].uppercase()} TIMELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "${dayChunks.size} Blocks",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // 3. Chunk Cards or Quick Fill Empty State
                if (dayChunks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(HyperSurface)
                                .border(1.dp, HyperBorder, RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No Chunks on ${dayNames[selectedDay - 1]}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start from scratch or load a productivity template",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(HyperSurfaceElevated)
                                            .clickable {
                                                repository.applyPreset(
                                                    selectedDay,
                                                    ChunkRepository.BUILT_IN_PRESETS[0]
                                                )
                                                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                    onScheduleAlarm(it)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "⚡ Load 8-8-8",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HyperPrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(HyperSurfaceElevated)
                                            .clickable {
                                                repository.applyPreset(
                                                    selectedDay,
                                                    ChunkRepository.BUILT_IN_PRESETS[1]
                                                )
                                                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                    onScheduleAlarm(it)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "🎓 Student Grind",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HyperEmerald
                                        )
                                    }
                                }
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
                            currentMinute = currentMinuteOfDay,
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

    var lastAddedEndMinute by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedDay) {
        lastAddedEndMinute = null
    }

    // Continuous scheduling: default start time to previous chunk's end time
    val continuousStartMinute = remember(dayChunks, lastAddedEndMinute, currentMinuteOfDay) {
        lastAddedEndMinute?.let { if (it in 0..1439) it else null } ?: run {
            if (dayChunks.isNotEmpty()) {
                val maxEnd = dayChunks.maxOf { it.endMinuteOfDay }
                if (maxEnd in 1..1439) {
                    maxEnd
                } else {
                    // If scheduled up to 24:00, look for any open gap in the day
                    val sorted = dayChunks.sortedBy { it.startMinuteOfDay }
                    var gapStart: Int? = null
                    if (sorted.first().startMinuteOfDay > 0) {
                        gapStart = 0
                    } else {
                        for (i in 0 until sorted.size - 1) {
                            if (sorted[i].endMinuteOfDay < sorted[i + 1].startMinuteOfDay) {
                                gapStart = sorted[i].endMinuteOfDay
                                break
                            }
                        }
                    }
                    gapStart ?: (9 * 60)
                }
            } else {
                ((currentMinuteOfDay / 30 + 1) * 30).coerceIn(0, 23 * 60)
            }
        }
    }

    // Modal Edit Sheet
    if (showEditSheet) {
        EditChunkSheet(
            chunkToEdit = chunkToEdit,
            dayOfWeek = selectedDay,
            defaultStartMinute = continuousStartMinute,
            onDismiss = { showEditSheet = false },
            onSave = { savedChunk ->
                if (chunkToEdit == null) {
                    repository.addChunk(savedChunk)
                    lastAddedEndMinute = savedChunk.endMinuteOfDay % 1440
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

    // Smart Routine Hub Sheet
    if (showRoutineHubSheet) {
        RoutineHubSheet(
            selectedDay = selectedDay,
            dayName = dayNames[selectedDay - 1],
            onDismiss = { showRoutineHubSheet = false },
            onSyncToWeekdays = {
                repository.syncDayToTargets(selectedDay, listOf(1, 2, 3, 4, 5))
                showRoutineHubSheet = false
            },
            onSyncToWeekend = {
                repository.syncDayToTargets(selectedDay, listOf(6, 7))
                showRoutineHubSheet = false
            },
            onApplyPreset = { preset ->
                repository.applyPreset(selectedDay, preset)
                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                    onScheduleAlarm(it)
                }
                showRoutineHubSheet = false
            },
            onClearDay = {
                repository.clearDay(selectedDay)
                showRoutineHubSheet = false
            }
        )
    }
}
