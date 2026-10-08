package com.hyperchunk.app.data

import com.hyperchunk.app.data.model.ChunkCategory
import com.hyperchunk.app.data.model.TimeChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeChunkTest {

    @Test
    fun testTimeFormattingAndDuration() {
        val chunk = TimeChunk(
            dayOfWeek = 1,
            title = "College Lecture 1",
            startMinuteOfDay = 450, // 07:30
            endMinuteOfDay = 600,   // 10:00
            category = ChunkCategory.STUDY
        )

        assertEquals("07:30", chunk.startTimeFormatted)
        assertEquals("10:00", chunk.endTimeFormatted)
        assertEquals(150, chunk.durationMinutes)
        assertEquals("2h 30m", chunk.durationFormatted)
    }

    @Test
    fun testIsCurrentlyActive() {
        val chunk = TimeChunk(
            dayOfWeek = 1,
            title = "Breakfast",
            startMinuteOfDay = 420, // 07:00
            endMinuteOfDay = 450,   // 07:30
            category = ChunkCategory.MEAL
        )

        assertFalse(chunk.isCurrentlyActive(419)) // 06:59
        assertTrue(chunk.isCurrentlyActive(420))  // 07:00
        assertTrue(chunk.isCurrentlyActive(435))  // 07:15
        assertFalse(chunk.isCurrentlyActive(450)) // 07:30 (exclusive end)
    }

    @Test
    fun testProgressAndRemainingMinutes() {
        val chunk = TimeChunk(
            dayOfWeek = 1,
            title = "Deep Work",
            startMinuteOfDay = 600, // 10:00
            endMinuteOfDay = 720,   // 12:00 (120 min)
            category = ChunkCategory.WORK
        )

        // At 11:00 (minute 660, 60 mins in)
        assertEquals(0.5f, chunk.progress(660), 0.01f)
        assertEquals(60, chunk.remainingMinutes(660))

        // Outside window
        assertEquals(0f, chunk.progress(500), 0.01f)
        assertEquals(0, chunk.remainingMinutes(500))
    }

    @Test
    fun testOvernightChunkWrapping() {
        val sleepChunk = TimeChunk(
            dayOfWeek = 1,
            title = "Overnight Sleep",
            startMinuteOfDay = 1380, // 23:00
            endMinuteOfDay = 360,    // 06:00
            category = ChunkCategory.SLEEP
        )

        assertEquals(420, sleepChunk.durationMinutes) // 7 hours (60 min + 360 min)
        assertTrue(sleepChunk.isCurrentlyActive(1400)) // 23:20
        assertTrue(sleepChunk.isCurrentlyActive(100))  // 01:40
        assertFalse(sleepChunk.isCurrentlyActive(400)) // 06:40
    }
}
