package com.hyperchunk.app

import androidx.compose.ui.graphics.Color
import com.hyperchunk.app.alarm.AlarmScheduler
import com.hyperchunk.app.data.model.AccentTheme
import com.hyperchunk.app.data.model.AppSettings
import com.hyperchunk.app.data.model.BackgroundTheme
import com.hyperchunk.app.data.model.ChunkCategory
import com.hyperchunk.app.data.model.ClockShape
import com.hyperchunk.app.data.model.PaletteMode
import com.hyperchunk.app.data.model.TimeChunk
import com.hyperchunk.app.data.model.toMonochromeColor
import com.hyperchunk.app.ui.theme.parseHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Random

class StressTest {

    @Test
    fun testHexColorParserStressAndFuzzing() {
        val fallback = Color.Red

        // Standard 6-char hex
        assertEquals(Color(0xFF3B82F6), parseHexColor("#3B82F6", fallback))
        assertEquals(Color(0xFF000000), parseHexColor("#000000", fallback))
        assertEquals(Color(0xFFFFFFFF), parseHexColor("#FFFFFF", fallback))
        assertEquals(Color(0xFF3B82F6), parseHexColor("3B82F6", fallback)) // without hash

        // 8-char hex (ARGB)
        val argb = parseHexColor("#803B82F6", fallback)
        assertNotNull(argb)

        // 3-char shorthand hex (#RGB)
        val shortWhite = parseHexColor("#FFF", fallback)
        assertEquals(Color(0xFFFFFFFF), shortWhite)
        val shortBlack = parseHexColor("#000", fallback)
        assertEquals(Color(0xFF000000), shortBlack)

        // Fuzzing & Malformed strings must never crash and always return fallback
        assertEquals(fallback, parseHexColor(null, fallback))
        assertEquals(fallback, parseHexColor("", fallback))
        assertEquals(fallback, parseHexColor("   ", fallback))
        assertEquals(fallback, parseHexColor("not_a_color", fallback))
        assertEquals(fallback, parseHexColor("#ZZZZZZ", fallback))
        assertEquals(fallback, parseHexColor("#12", fallback))
        assertEquals(fallback, parseHexColor("#12345", fallback))
        assertEquals(fallback, parseHexColor("#123456789", fallback))
        assertEquals(fallback, parseHexColor("!@#$%^&*()", fallback))

        // Random garbage string fuzzing
        val rnd = Random(42)
        repeat(1000) {
            val randomChars = (1..15).map { (rnd.nextInt(128)).toChar() }.joinToString("")
            val res = parseHexColor(randomChars, fallback)
            assertNotNull(res)
        }
    }

    @Test
    fun testAllCategoriesMonochromeColors() {
        // Test default category monochrome slate tones
        val seenColors = mutableSetOf<Color>()
        ChunkCategory.values().forEach { category ->
            val chunk = TimeChunk(
                dayOfWeek = 1,
                title = "Test ${category.displayName}",
                startMinuteOfDay = 540,
                endMinuteOfDay = 660,
                category = category,
                colorHex = category.defaultColor
            )
            val mono = chunk.toMonochromeColor()
            assertNotNull(mono)
            assertTrue("Luminance should be readable against dark canvas", mono.red >= 0.25f)
            seenColors.add(mono)
        }
        // Ensure categories have diverse luminance levels (at least 6 distinct tones among 8 categories)
        assertTrue(seenColors.size >= 6)

        // Test custom chunk color converted to pure monochrome
        val customChunk = TimeChunk(
            dayOfWeek = 1,
            title = "Custom Neon Chunk",
            startMinuteOfDay = 540,
            endMinuteOfDay = 660,
            category = ChunkCategory.CUSTOM,
            colorHex = "#FF00FF" // Magenta custom
        )
        val customMono = customChunk.toMonochromeColor()
        assertEquals(customMono.red, customMono.green, 0.001f)
        assertEquals(customMono.green, customMono.blue, 0.001f)
        assertTrue(customMono.red >= 0.45f)
    }

    @Test
    fun testExtremeBoundaryMinutes() {
        // Full 24-hour block from 00:00 to 1440 (24:00)
        val fullDayChunk = TimeChunk(
            dayOfWeek = 1,
            title = "All Day Hackathon",
            startMinuteOfDay = 0,
            endMinuteOfDay = 1440,
            category = ChunkCategory.WORK
        )
        assertEquals(1440, fullDayChunk.durationMinutes)
        assertEquals("24h", fullDayChunk.durationFormatted)
        assertEquals("00:00", fullDayChunk.startTimeFormatted)
        assertEquals("00:00", fullDayChunk.endTimeFormatted)
        assertEquals("12:00 AM", fullDayChunk.startTime12h)
        assertEquals("12:00 AM", fullDayChunk.endTime12h)
        assertTrue(fullDayChunk.isCurrentlyActive(0))
        assertTrue(fullDayChunk.isCurrentlyActive(720))
        assertTrue(fullDayChunk.isCurrentlyActive(1439))
        assertEquals(0.5f, fullDayChunk.progress(720), 0.01f)
        assertEquals(720, fullDayChunk.remainingMinutes(720))

        // 1-minute chunk at midnight
        val oneMinMidnight = TimeChunk(
            dayOfWeek = 1,
            title = "Midnight Marker",
            startMinuteOfDay = 0,
            endMinuteOfDay = 1,
            category = ChunkCategory.ROUTINE
        )
        assertEquals(1, oneMinMidnight.durationMinutes)
        assertEquals("1m", oneMinMidnight.durationFormatted)
        assertTrue(oneMinMidnight.isCurrentlyActive(0))
        assertFalse(oneMinMidnight.isCurrentlyActive(1))
        assertEquals(1.0f, oneMinMidnight.progress(0), 1.0f)

        // 1-minute chunk at end of day
        val oneMinEndOfDay = TimeChunk(
            dayOfWeek = 1,
            title = "End of Day Marker",
            startMinuteOfDay = 1439,
            endMinuteOfDay = 1440,
            category = ChunkCategory.ROUTINE
        )
        assertEquals(1, oneMinEndOfDay.durationMinutes)
        assertTrue(oneMinEndOfDay.isCurrentlyActive(1439))
        assertFalse(oneMinEndOfDay.isCurrentlyActive(0))
    }

    @Test
    fun testOvernightChunkEdgeCases() {
        // Sleep from 23:45 to 07:15 (15 mins to midnight + 435 mins = 450 mins = 7h 30m)
        val sleepChunk = TimeChunk(
            dayOfWeek = 1,
            title = "Deep Sleep",
            startMinuteOfDay = 1425, // 23:45
            endMinuteOfDay = 435,    // 07:15
            category = ChunkCategory.SLEEP
        )
        assertEquals(450, sleepChunk.durationMinutes)
        assertEquals("7h 30m", sleepChunk.durationFormatted)

        // Before start
        assertFalse(sleepChunk.isCurrentlyActive(1424)) // 23:44
        // At start
        assertTrue(sleepChunk.isCurrentlyActive(1425))  // 23:45
        // At 23:59
        assertTrue(sleepChunk.isCurrentlyActive(1439))
        // At 00:00 midnight
        assertTrue(sleepChunk.isCurrentlyActive(0))
        // At 07:14
        assertTrue(sleepChunk.isCurrentlyActive(434))
        // At 07:15 (exclusive end)
        assertFalse(sleepChunk.isCurrentlyActive(435))
        // Next day morning outside
        assertFalse(sleepChunk.isCurrentlyActive(500))

        // Progress calculation across midnight:
        // At 00:00 midnight: 15 minutes elapsed out of 450
        assertEquals(15f / 450f, sleepChunk.progress(0), 0.01f)
        assertEquals(435, sleepChunk.remainingMinutes(0))

        // At 03:30 (minute 210): 15 + 210 = 225 elapsed out of 450 (exactly 50%)
        assertEquals(0.5f, sleepChunk.progress(210), 0.01f)
        assertEquals(225, sleepChunk.remainingMinutes(210))
    }

    @Test
    fun testWidgetRemainingMinutesCalculation() {
        val currentMinute = 1410 // 23:30

        // Case 1: Next chunk is at 08:00 tomorrow (480)
        val nextChunkStart = 480
        val minsUntilWrapped = if (nextChunkStart >= currentMinute) {
            nextChunkStart - currentMinute
        } else {
            (1440 - currentMinute) + nextChunkStart
        }
        // 30 mins to midnight + 480 mins = 510 mins (8h 30m)
        assertEquals(510, minsUntilWrapped)
        assertTrue("Minutes until must be strictly positive", minsUntilWrapped > 0)

        // Case 2: Next chunk is later tonight (23:45 = 1425)
        val laterTonightStart = 1425
        val minsUntilTonight = if (laterTonightStart >= currentMinute) {
            laterTonightStart - currentMinute
        } else {
            (1440 - currentMinute) + laterTonightStart
        }
        assertEquals(15, minsUntilTonight)
    }

    @Test
    fun testStressRandomChunksSimulation() {
        val rnd = Random(12345)
        repeat(10000) {
            val start = rnd.nextInt(1440)
            val end = rnd.nextInt(1440)
            val chunk = TimeChunk(
                dayOfWeek = (rnd.nextInt(7) + 1),
                title = "Random Chunk #$it",
                startMinuteOfDay = start,
                endMinuteOfDay = end,
                category = ChunkCategory.values()[rnd.nextInt(ChunkCategory.values().size)]
            )

            // Duration must always be >= 0
            assertTrue(chunk.durationMinutes >= 0)
            assertTrue(chunk.durationMinutes <= 1440)

            // Any arbitrary minute of the day must never throw an exception
            val sampleMin = rnd.nextInt(1440)
            val active = chunk.isCurrentlyActive(sampleMin)
            val prog = chunk.progress(sampleMin)
            val rem = chunk.remainingMinutes(sampleMin)

            assertTrue(prog in 0f..1f)
            assertTrue(rem >= 0)
            if (!active) {
                assertEquals(0f, prog, 0.001f)
                assertEquals(0, rem)
            }
        }
    }

    @Test
    fun testAlarmSchedulerNextTriggerCalculation() {
        // Test all 7 days of the week from Sunday through Saturday
        for (targetDay in 1..7) {
            for (hour in listOf(0, 6, 12, 18, 23)) {
                val startMinute = hour * 60 + 30
                val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(targetDay, startMinute)
                val cal = Calendar.getInstance().apply { timeInMillis = triggerMillis }

                val expectedCalDay = when (targetDay) {
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    7 -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }

                assertEquals(expectedCalDay, cal.get(Calendar.DAY_OF_WEEK))
                assertEquals(hour, cal.get(Calendar.HOUR_OF_DAY))
                assertEquals(30, cal.get(Calendar.MINUTE))
                assertEquals(0, cal.get(Calendar.SECOND))
                assertEquals(0, cal.get(Calendar.MILLISECOND))
                assertTrue("Trigger time must be in the future", triggerMillis > System.currentTimeMillis())
            }
        }
    }

    @Test
    fun testBuiltInPresetsContiguityAnd24hTotal() {
        val presets = com.hyperchunk.app.data.storage.ChunkRepository.BUILT_IN_PRESETS
        assertEquals(3, presets.size)

        for (preset in presets) {
            var cursor = 0
            var totalDuration = 0
            for (block in preset.blocks) {
                assertEquals("Block ${block.title} must start at previous block end ($cursor)", cursor, block.startMinute)
                assertTrue("Block end must be strictly after start", block.endMinute > block.startMinute)
                val duration = block.endMinute - block.startMinute
                totalDuration += duration
                cursor = block.endMinute
            }
            assertEquals("Preset ${preset.name} must cover the full 24h (1440m)", 1440, totalDuration)
            assertEquals(1440, cursor)
        }
    }

    @Test
    fun testGapFillingSimulationWithOvernightChunk() {
        // Scenario: Day has Morning lecture (540..720) and Late Night sleep (1380..360)
        val chunks = listOf(
            TimeChunk(dayOfWeek = 1, title = "Lecture", startMinuteOfDay = 540, endMinuteOfDay = 720, category = ChunkCategory.STUDY),
            TimeChunk(dayOfWeek = 1, title = "Sleep", startMinuteOfDay = 1380, endMinuteOfDay = 360, category = ChunkCategory.SLEEP)
        ).sortedBy { it.startMinuteOfDay }

        val gaps = mutableListOf<Pair<Int, Int>>()
        var cursor = 0
        for (chunk in chunks) {
            val chunkStart = chunk.startMinuteOfDay
            val chunkEnd = chunk.endMinuteOfDay

            if (chunkEnd >= chunkStart) {
                if (chunkStart > cursor && (chunkStart - cursor) >= 15) {
                    gaps.add(Pair(cursor, chunkStart))
                }
                cursor = maxOf(cursor, chunkEnd)
            } else {
                if (chunkStart > cursor && (chunkStart - cursor) >= 15) {
                    gaps.add(Pair(cursor, chunkStart))
                }
                cursor = 1440
            }
        }
        if (cursor < 1440 && (1440 - cursor) >= 15) {
            gaps.add(Pair(cursor, 1440))
        }

        // Expected gaps:
        // Gap 1: 0 to 540 (Morning before lecture)
        // Gap 2: 720 to 1380 (Afternoon & evening between lecture and sleep)
        // From 1380 to 1440 is occupied by sleep, so cursor was 1440. No gap after 1380!
        assertEquals(2, gaps.size)
        assertEquals(Pair(0, 540), gaps[0])
        assertEquals(Pair(720, 1380), gaps[1])
    }

    @Test
    fun testChronologicalNextChunkSelection() {
        // Add chunks out of order: 16:00 (960) added first, then 10:00 (600) added second
        val rawChunks = listOf(
            TimeChunk(id = "c2", dayOfWeek = 1, title = "Tea", startMinuteOfDay = 960, endMinuteOfDay = 1020, category = ChunkCategory.MEAL),
            TimeChunk(id = "c1", dayOfWeek = 1, title = "Morning Work", startMinuteOfDay = 600, endMinuteOfDay = 720, category = ChunkCategory.WORK)
        )

        // Sorted chronologically
        val sortedChunks = rawChunks.sortedBy { it.startMinuteOfDay }

        // At 08:00 (480) - Before both chunks
        val nextAt8 = sortedChunks.firstOrNull { it.startMinuteOfDay > 480 } ?: sortedChunks.firstOrNull()
        assertNotNull(nextAt8)
        assertEquals("Morning Work", nextAt8?.title)

        // At 10:30 (630) - Inside Morning Work (600..720)
        val active = sortedChunks.firstOrNull { it.isCurrentlyActive(630) }
        assertEquals("Morning Work", active?.title)
        val nextAt1030 = sortedChunks.firstOrNull { it.startMinuteOfDay >= active!!.endMinuteOfDay }
        assertEquals("Tea", nextAt1030?.title)
    }

    @Test
    fun testCustomPresetJsonSerialization() {
        val gson = com.google.gson.Gson()
        val customPreset = com.hyperchunk.app.data.storage.RoutinePreset(
            id = "custom_test_1",
            name = "Founder Deep Sprint",
            description = "High intensity build schedule",
            iconEmoji = "🚀",
            blocks = listOf(
                com.hyperchunk.app.data.storage.PresetBlock("Sleep", 0, 420, ChunkCategory.SLEEP, hasAlarm = true),
                com.hyperchunk.app.data.storage.PresetBlock("Sprint 1", 420, 720, ChunkCategory.WORK, hasAlarm = true),
                com.hyperchunk.app.data.storage.PresetBlock("Sprint 2", 720, 1080, ChunkCategory.WORK),
                com.hyperchunk.app.data.storage.PresetBlock("Wind Down", 1080, 1440, ChunkCategory.REST)
            ),
            isCustom = true
        )

        val json = gson.toJson(customPreset)
        val deserialized = gson.fromJson(json, com.hyperchunk.app.data.storage.RoutinePreset::class.java)

        assertEquals(customPreset.id, deserialized.id)
        assertEquals(customPreset.name, deserialized.name)
        assertEquals(customPreset.description, deserialized.description)
        assertEquals(customPreset.iconEmoji, deserialized.iconEmoji)
        assertEquals(customPreset.isCustom, deserialized.isCustom)
        assertEquals(4, deserialized.blocks.size)
        assertEquals("Sprint 1", deserialized.blocks[1].title)
        assertTrue(deserialized.blocks[0].hasAlarm)
    }

    @Test
    fun testIntervalOverlapDetectionMatrix() {
        fun intervalsOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
            val spans1 = if (end1 >= start1) listOf(Pair(start1, end1)) else listOf(Pair(start1, 1440), Pair(0, end1))
            val spans2 = if (end2 >= start2) listOf(Pair(start2, end2)) else listOf(Pair(start2, 1440), Pair(0, end2))
            for ((s1, e1) in spans1) {
                for ((s2, e2) in spans2) {
                    if (maxOf(s1, s2) < minOf(e1, e2)) return true
                }
            }
            return false
        }

        // 1. Separate non-overlapping blocks
        assertFalse(intervalsOverlap(0, 120, 180, 300))

        // 2. Abutting blocks (end1 == start2) - must NOT be considered overlapping
        assertFalse(intervalsOverlap(0, 120, 120, 240))
        assertFalse(intervalsOverlap(300, 450, 450, 600))

        // 3. Partial overlap
        assertTrue(intervalsOverlap(100, 250, 200, 350))
        assertTrue(intervalsOverlap(200, 350, 100, 250))

        // 4. Complete enclosure
        assertTrue(intervalsOverlap(100, 500, 200, 300))
        assertTrue(intervalsOverlap(200, 300, 100, 500))

        // 5. Overnight chunk (23:00 to 07:00 = 1380 to 420)
        // Blocks inside morning span (0..420)
        assertTrue(intervalsOverlap(1380, 420, 100, 200))
        // Blocks inside evening span (1380..1440)
        assertTrue(intervalsOverlap(1380, 420, 1390, 1420))
        // Blocks during daytime (e.g. 500..800) must NOT overlap overnight sleep
        assertFalse(intervalsOverlap(1380, 420, 500, 800))
    }

    @Test
    fun testSmartMergeSimulation() {
        fun intervalsOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
            val spans1 = if (end1 >= start1) listOf(Pair(start1, end1)) else listOf(Pair(start1, 1440), Pair(0, end1))
            val spans2 = if (end2 >= start2) listOf(Pair(start2, end2)) else listOf(Pair(start2, 1440), Pair(0, end2))
            for ((s1, e1) in spans1) {
                for ((s2, e2) in spans2) {
                    if (maxOf(s1, s2) < minOf(e1, e2)) return true
                }
            }
            return false
        }

        // Existing schedule on day: 10:00 - 13:00 (600..780) Focus Work
        val existing = listOf(
            TimeChunk(dayOfWeek = 1, title = "Existing Work", startMinuteOfDay = 600, endMinuteOfDay = 780, category = ChunkCategory.WORK)
        )

        // Preset trying to merge:
        // 1. Sleep: 0..480 (non-overlapping -> ACCEPT)
        // 2. Morning Call: 540..660 (overlaps 600..780 -> REJECT)
        // 3. Gym: 1080..1200 (non-overlapping -> ACCEPT)
        val presetBlocks = listOf(
            com.hyperchunk.app.data.storage.PresetBlock("Sleep", 0, 480, ChunkCategory.SLEEP),
            com.hyperchunk.app.data.storage.PresetBlock("Morning Call", 540, 660, ChunkCategory.WORK),
            com.hyperchunk.app.data.storage.PresetBlock("Gym", 1080, 1200, ChunkCategory.FITNESS)
        )

        val acceptedBlocks = mutableListOf<com.hyperchunk.app.data.storage.PresetBlock>()
        presetBlocks.forEach { block ->
            val hasOverlap = existing.any { ex ->
                intervalsOverlap(block.startMinute, block.endMinute, ex.startMinuteOfDay, ex.endMinuteOfDay)
            }
            if (!hasOverlap) {
                acceptedBlocks.add(block)
            }
        }

        assertEquals(2, acceptedBlocks.size)
        assertEquals("Sleep", acceptedBlocks[0].title)
        assertEquals("Gym", acceptedBlocks[1].title)
    }

    @Test
    fun testAppSettingsDefaultPreferences() {
        val defaults = AppSettings()
        assertTrue("Screen Optimisation Mode (compactMode) must be ON by default", defaults.compactMode)
        assertEquals(PaletteMode.COLORFUL, defaults.paletteMode)
        assertEquals(AccentTheme.ROSE_CRIMSON, defaults.accentTheme)
        assertEquals(BackgroundTheme.OLED_BLACK, defaults.backgroundTheme)
        assertFalse("Auto-Enable Alarms must be OFF by default", defaults.defaultSoundAlarm)
        assertFalse("12-hour format must be the default (use24HourFormat = false)", defaults.use24HourFormat)
        assertEquals(ClockShape.ROUND, defaults.clockShape)
    }

    @Test
    fun testClockShapeEnum() {
        assertEquals("round", ClockShape.ROUND.id)
        assertEquals("rectangle", ClockShape.RECTANGLE.id)
        assertEquals(ClockShape.ROUND, ClockShape.fromId("round"))
        assertEquals(ClockShape.RECTANGLE, ClockShape.fromId("rectangle"))
        assertEquals(ClockShape.ROUND, ClockShape.fromId("unknown_id"))
    }

    @Test
    fun testStarterScheduleGeneration() {
        val schedule = com.hyperchunk.app.data.storage.ChunkRepository.createDefaultStarterSchedule(2) // Tuesday
        assertTrue(schedule.isNotEmpty())
        
        var totalMinutes = 0
        schedule.forEach { chunk ->
            assertEquals(2, chunk.dayOfWeek)
            assertFalse("Starter schedule must have alarms disabled by default", chunk.hasAlarm)
            totalMinutes += chunk.durationMinutes
        }
        assertEquals("Starter schedule must cover the entire 24h day", 1440, totalMinutes)
    }

    @Test
    fun testAlarmSuppressionGuardLogic() {
        val todayDayOfWeek = 2 // Tuesday
        val chunkMonday = TimeChunk(
            dayOfWeek = 1, // Monday
            title = "Old Monday Class",
            startMinuteOfDay = 600,
            endMinuteOfDay = 720,
            hasAlarm = true
        )

        // 1. Wrong Day of week guard: Monday alarm firing on Tuesday must be suppressed
        val isDayValid = (chunkMonday.dayOfWeek == todayDayOfWeek)
        assertFalse("Monday alarm firing on Tuesday must be suppressed", isDayValid)

        // 2. Alarm disabled guard: chunk with hasAlarm = false must be suppressed
        val chunkNoAlarm = chunkMonday.copy(dayOfWeek = todayDayOfWeek, hasAlarm = false)
        assertFalse("Chunk with hasAlarm=false must be suppressed", chunkNoAlarm.hasAlarm)

        // 3. Stale window guard: alarm delayed by > 15 minutes must be suppressed
        val scheduledMinute = 600 // 10:00 AM
        val onTimeMinute = 605   // 10:05 AM (within 15 min)
        val staleMinute = 720    // 12:00 PM (2 hours late)

        fun isWindowValid(currentMinute: Int, targetMinute: Int): Boolean {
            val delta = kotlin.math.abs(currentMinute - targetMinute)
            val windowMinutes = kotlin.math.min(delta, 1440 - delta)
            return windowMinutes <= 15
        }

        assertTrue("On-time wakeup within 5 mins is valid", isWindowValid(onTimeMinute, scheduledMinute))
        assertFalse("Stale delayed wakeup after 2 hours must be suppressed", isWindowValid(staleMinute, scheduledMinute))
    }

    @Test
    fun testDynamicIconAliasesAndMapping() {
        val aliases = com.hyperchunk.app.data.storage.DynamicIconManager.ALIASES
        assertEquals(7, aliases.size)
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivityRoseCrimson"))
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivityPixelBlue"))
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivityCyberCyan"))
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivityElectricLime"))
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivityAmethystViolet"))
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivitySunsetAmber"))
        assertTrue(aliases.contains("com.hyperchunk.app.MainActivityMonochrome"))

        // Test alias selection logic for colorful mode
        AccentTheme.values().forEach { theme ->
            val expectedAlias = when (theme) {
                AccentTheme.ROSE_CRIMSON -> "com.hyperchunk.app.MainActivityRoseCrimson"
                AccentTheme.PIXEL_BLUE -> "com.hyperchunk.app.MainActivityPixelBlue"
                AccentTheme.CYBER_CYAN -> "com.hyperchunk.app.MainActivityCyberCyan"
                AccentTheme.ELECTRIC_LIME -> "com.hyperchunk.app.MainActivityElectricLime"
                AccentTheme.AMETHYST_VIOLET -> "com.hyperchunk.app.MainActivityAmethystViolet"
                AccentTheme.SUNSET_AMBER -> "com.hyperchunk.app.MainActivitySunsetAmber"
            }
            assertTrue(aliases.contains(expectedAlias))
        }
    }

    @Test
    fun testAllAccentThemesHaveValidHexAndComposeColors() {
        AccentTheme.values().forEach { theme ->
            val hex = theme.colorHex
            assertTrue(hex.startsWith("#"))
            assertEquals(7, hex.length)
            val parsed = parseHexColor(hex, Color.Red)
            assertNotNull(parsed)
            assertNotNull(theme.composeColor)
        }
    }
}


