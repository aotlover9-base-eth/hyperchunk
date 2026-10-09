package com.hyperchunk.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.model.toMonochromeColor
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.data.storage.SettingsRepository
import com.hyperchunk.app.ui.components.Chunk24hChronograph
import com.hyperchunk.app.ui.components.ChunkCard
import com.hyperchunk.app.ui.components.DaySelector
import com.hyperchunk.app.ui.components.RoutineHubSheet
import com.hyperchunk.app.ui.components.SettingsSheet
import com.hyperchunk.app.ui.theme.HyperAmber
import com.hyperchunk.app.ui.theme.HyperBorder
import com.hyperchunk.app.ui.theme.HyperCyan
import com.hyperchunk.app.ui.theme.HyperEmerald
import com.hyperchunk.app.ui.theme.HyperPrimary
import com.hyperchunk.app.ui.theme.HyperSurface
import com.hyperchunk.app.ui.theme.HyperSurfaceElevated
import com.hyperchunk.app.ui.theme.HyperSurfaceVariant
import com.hyperchunk.app.ui.theme.LocalAppSettings
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import com.hyperchunk.app.ui.theme.parseHexColor
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Geometric Chrono Logo Icon.
 * Features an orbital outer dial with rounded notch, concentric inner arc, and center bead.
 */
@Composable
fun HyperChronoLogo(
    modifier: Modifier = Modifier,
    color: Color = HyperPrimary
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = size.minDimension / 2f - 2.5.dp.toPx()
        val innerR = outerR * 0.58f

        // Outer Chrono ring with an open angular gap
        drawArc(
            color = color,
            startAngle = -70f,
            sweepAngle = 310f,
            useCenter = false,
            topLeft = Offset(center.x - outerR, center.y - outerR),
            size = Size(outerR * 2f, outerR * 2f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner orbit arc
        drawArc(
            color = color.copy(alpha = 0.45f),
            startAngle = 60f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = Offset(center.x - innerR, center.y - innerR),
            size = Size(innerR * 2f, innerR * 2f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Center jewel bead
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = center
        )

        // Orbit live bead at top notch
        val notchRad = Math.toRadians(-70.0)
        val notchOffset = Offset(
            center.x + outerR * cos(notchRad).toFloat(),
            center.y + outerR * sin(notchRad).toFloat()
        )
        drawCircle(
            color = Color.White,
            radius = 2.4.dp.toPx(),
            center = notchOffset
        )
    }
}

@Composable
fun HomeScreen(
    repository: ChunkRepository,
    settingsRepository: SettingsRepository,
    onScheduleAlarm: (TimeChunk) -> Unit,
    onCancelAlarm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    var currentMinuteOfDay by remember {
        val cal = Calendar.getInstance()
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
    }

    fun getDayOfWeek(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    var todayDayOfWeek by remember {
        mutableIntStateOf(getDayOfWeek(Calendar.getInstance()))
    }

    var selectedDay by remember { mutableIntStateOf(todayDayOfWeek) }

    // Live clock ticker & auto midnight rollover
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val freshDay = getDayOfWeek(cal)
            if (freshDay != todayDayOfWeek) {
                todayDayOfWeek = freshDay
            }
            try {
                com.hyperchunk.app.widget.HyperChunkWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {}
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
    var showSettingsSheet by remember { mutableStateOf(false) }

    val appColors = LocalAppThemeColors.current
    val settings by settingsRepository.settings.collectAsState()
    val isMonochrome = settings.paletteMode == PaletteMode.MONOCHROME

    val (activeChunk, nextChunk) = remember(dayChunks, currentMinuteOfDay, selectedDay) {
        repository.getCurrentAndNextChunk(selectedDay, currentMinuteOfDay)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = appColors.bgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    chunkToEdit = null
                    showEditSheet = true
                },
                containerColor = appColors.primary,
                contentColor = appColors.onPrimary,
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
            // Premium Redesigned Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom Chrono Logo Box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(appColors.surfaceElevated)
                            .border(1.dp, appColors.border, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        HyperChronoLogo(
                            modifier = Modifier.size(28.dp),
                            color = appColors.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HYPERCHUNK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = appColors.primary,
                                letterSpacing = 1.4.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(appColors.surfaceElevated)
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "24H",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        Text(
                            text = "24-Hour Time Planner",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    }
                }

                // Header Action Buttons (Routines & Settings)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Routines Hub Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(appColors.surface)
                            .border(1.dp, appColors.border, RoundedCornerShape(16.dp))
                            .clickable { showRoutineHubSheet = true }
                            .padding(horizontal = 11.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Routines",
                                tint = appColors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Routines",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(appColors.surface)
                            .border(1.dp, appColors.border, RoundedCornerShape(16.dp))
                            .clickable { showSettingsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = appColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Day Selector Pill Row
            DaySelector(
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Redesigned 24-Hour Polar Chronograph Dial
                item {
                    Chunk24hChronograph(
                        chunks = dayChunks,
                        currentMinute = currentMinuteOfDay,
                        activeChunk = if (selectedDay == todayDayOfWeek) activeChunk else null,
                        nextChunk = nextChunk,
                        isToday = (selectedDay == todayDayOfWeek),
                        dayName = dayNames[selectedDay - 1],
                        compactMode = settings.compactMode
                    )
                }

                // 2. Day Overview & Category Analytics Card (if chunks exist)
                if (dayChunks.isNotEmpty()) {
                    item {
                        val totalMins = dayChunks.sumOf { it.durationMinutes }
                        val scheduledH = totalMins / 60f
                        val freeH = (1440 - totalMins).coerceAtLeast(0) / 60f
                        val breakdown = remember(dayChunks) { repository.getCategoryBreakdownForDay(selectedDay) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(appColors.surface)
                                .border(1.dp, appColors.border, RoundedCornerShape(22.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DAY BREAKDOWN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 1.1.sp
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format("%.1fh active", scheduledH),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appColors.primary
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = TextTertiary
                                        )
                                        Text(
                                            text = String.format("%.1fh free", freeH),
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Multi-segment category progress line
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    var xCursor = 0f
                                    val totalW = size.width
                                    breakdown.forEach { (cat, mins) ->
                                        val ratio = mins / 1440f
                                        val barW = ratio * totalW
                                        val color = if (isMonochrome) cat.toMonochromeColor() else parseHexColor(cat.defaultColor, appColors.primary)
                                        drawRoundRect(
                                            color = color,
                                            topLeft = Offset(xCursor, 0f),
                                            size = Size(barW.coerceAtLeast(1.5f), size.height),
                                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                        )
                                        xCursor += barW
                                    }
                                    if (xCursor < totalW) {
                                        drawRoundRect(
                                            color = Color(0xFF1B202E),
                                            topLeft = Offset(xCursor, 0f),
                                            size = Size(totalW - xCursor, size.height),
                                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Category chips summary
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(breakdown.entries.toList().sortedByDescending { it.value }) { (cat, mins) ->
                                        val h = mins / 60
                                        val m = mins % 60
                                        val timeStr = if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"
                                        val chipColor = if (isMonochrome) cat.toMonochromeColor() else parseHexColor(cat.defaultColor, appColors.primary)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(appColors.surfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(chipColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "${cat.displayName}: $timeStr",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Timeline Section Header with Fill Gaps Action
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (dayChunks.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(appColors.surfaceElevated)
                                        .clickable {
                                            repository.fillUnscheduledGaps(selectedDay)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "⚡ Fill Free Slots",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = appColors.primary
                                    )
                                }
                            }

                            if (selectedDay != todayDayOfWeek) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(appColors.surfaceElevated)
                                        .clickable {
                                            selectedDay = todayDayOfWeek
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Jump to Today",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "${dayChunks.size} Blocks",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 4. Empty / Unedited Day Quick Setup Card
                if (dayChunks.isEmpty()) {
                    item {
                        val todayChunks = repository.getChunksForDay(todayDayOfWeek)
                        val yesterday = if (selectedDay == 1) 7 else selectedDay - 1
                        val yesterdayChunks = repository.getChunksForDay(yesterday)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(HyperSurface)
                                .border(1.dp, HyperBorder, RoundedCornerShape(26.dp))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(HyperSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = appColors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "No Schedule for ${dayNames[selectedDay - 1]}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start fresh or 1-tap copy an existing routine",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Quick Copy Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (selectedDay != todayDayOfWeek && todayChunks.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(appColors.surfaceElevated)
                                                .clickable {
                                                    repository.copySchedule(todayDayOfWeek, selectedDay)
                                                    repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                        onScheduleAlarm(it)
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "📋 Copy Today",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appColors.primary
                                            )
                                        }
                                    }

                                    if (yesterdayChunks.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(appColors.surfaceElevated)
                                                .clickable {
                                                    repository.copySchedule(yesterday, selectedDay)
                                                    repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                        onScheduleAlarm(it)
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "↩️ Copy Yesterday",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appColors.isMonochrome) Color.White else HyperCyan
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Productivity Presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(HyperSurfaceElevated)
                                            .clickable {
                                                repository.applyPreset(selectedDay, ChunkRepository.BUILT_IN_PRESETS[0])
                                                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                    onScheduleAlarm(it)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚖️ 8-8-8 Rule",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(HyperSurfaceElevated)
                                            .clickable {
                                                repository.applyPreset(selectedDay, ChunkRepository.BUILT_IN_PRESETS[1])
                                                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                    onScheduleAlarm(it)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🎓 Student Grind",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (appColors.isMonochrome) Color.White else HyperEmerald
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(HyperSurfaceElevated)
                                            .clickable {
                                                repository.applyPreset(selectedDay, ChunkRepository.BUILT_IN_PRESETS[2])
                                                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                                                    onScheduleAlarm(it)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚡ Deep Work",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (appColors.isMonochrome) Color.White else HyperAmber
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
    // If empty/unedited day, default to morning (08:00 AM) instead of late night!
    val continuousStartMinute = remember(dayChunks, lastAddedEndMinute, currentMinuteOfDay, selectedDay, todayDayOfWeek) {
        lastAddedEndMinute?.let { if (it in 0..1439) it else null } ?: run {
            if (dayChunks.isNotEmpty()) {
                val maxEnd = dayChunks.maxOf { it.endMinuteOfDay }
                if (maxEnd in 1..1439) {
                    maxEnd
                } else {
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
                if (selectedDay == todayDayOfWeek) {
                    ((currentMinuteOfDay / 30 + 1) * 30).coerceIn(0, 23 * 60)
                } else {
                    8 * 60 // 08:00 AM sensible morning start for future/unedited days
                }
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
                try {
                    com.hyperchunk.app.widget.HyperChunkWidgetProvider.updateAllWidgets(context)
                } catch (_: Exception) {}
                showEditSheet = false
            },
            onDelete = { idToDelete ->
                onCancelAlarm(idToDelete)
                repository.deleteChunk(idToDelete)
                try {
                    com.hyperchunk.app.widget.HyperChunkWidgetProvider.updateAllWidgets(context)
                } catch (_: Exception) {}
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
                val targets = listOf(1, 2, 3, 4, 5)
                targets.forEach { targetDay ->
                    repository.getChunksForDay(targetDay).filter { it.hasAlarm }.forEach { onCancelAlarm(it.id) }
                }
                repository.syncDayToTargets(selectedDay, targets)
                targets.forEach { targetDay ->
                    repository.getChunksForDay(targetDay).filter { it.hasAlarm }.forEach { onScheduleAlarm(it) }
                }
                showRoutineHubSheet = false
            },
            onSyncToWeekend = {
                val targets = listOf(6, 7)
                targets.forEach { targetDay ->
                    repository.getChunksForDay(targetDay).filter { it.hasAlarm }.forEach { onCancelAlarm(it.id) }
                }
                repository.syncDayToTargets(selectedDay, targets)
                targets.forEach { targetDay ->
                    repository.getChunksForDay(targetDay).filter { it.hasAlarm }.forEach { onScheduleAlarm(it) }
                }
                showRoutineHubSheet = false
            },
            onApplyPreset = { preset ->
                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach { onCancelAlarm(it.id) }
                repository.applyPreset(selectedDay, preset)
                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach {
                    onScheduleAlarm(it)
                }
                showRoutineHubSheet = false
            },
            onClearDay = {
                repository.getChunksForDay(selectedDay).filter { it.hasAlarm }.forEach { onCancelAlarm(it.id) }
                repository.clearDay(selectedDay)
                showRoutineHubSheet = false
            }
        )
    }

    // Appearance & Settings Sheet
    if (showSettingsSheet) {
        SettingsSheet(
            settings = settings,
            onFontScaleChanged = { settingsRepository.updateFontScale(it) },
            onCompactModeChanged = { settingsRepository.updateCompactMode(it) },
            onPaletteModeChanged = { settingsRepository.updatePaletteMode(it) },
            onAccentThemeChanged = { settingsRepository.updateAccentTheme(it) },
            onBackgroundThemeChanged = { settingsRepository.updateBackgroundTheme(it) },
            onHapticModeChanged = { settingsRepository.updateHapticMode(it) },
            on24HourFormatChanged = { settingsRepository.update24HourFormat(it) },
            onDefaultAlarmChanged = { settingsRepository.updateDefaultSoundAlarm(it) },
            onResetDefaults = { settingsRepository.resetToDefaults() },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
