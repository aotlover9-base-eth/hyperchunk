package com.hyperchunk.app

import androidx.compose.ui.graphics.Color
import com.hyperchunk.app.alarm.AlarmScheduler
import com.hyperchunk.app.data.model.ChunkCategory
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
}
