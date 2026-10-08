package com.hyperchunk.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hyperchunk.app.data.model.ChunkCategory
import com.hyperchunk.app.data.model.TimeChunk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class ChunkRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hyperchunk_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _chunks = MutableStateFlow<List<TimeChunk>>(emptyList())
    val chunks: StateFlow<List<TimeChunk>> = _chunks.asStateFlow()

    init {
        loadChunks()
    }

    private fun loadChunks() {
        val json = prefs.getString(KEY_CHUNKS, null)
        if (json.isNullOrBlank()) {
            val defaultList = generateDefaultSchedule()
            saveChunksToStorage(defaultList)
            _chunks.value = defaultList
        } else {
            val type = object : TypeToken<List<TimeChunk>>() {}.type
            val list: List<TimeChunk> = try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            if (list.isEmpty()) {
                val defaultList = generateDefaultSchedule()
                saveChunksToStorage(defaultList)
                _chunks.value = defaultList
            } else {
                _chunks.value = list
            }
        }
    }

    private fun saveChunksToStorage(list: List<TimeChunk>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_CHUNKS, json).apply()
    }

    fun getChunksForDay(dayOfWeek: Int): List<TimeChunk> {
        return _chunks.value
            .filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { it.startMinuteOfDay }
    }

    fun addChunk(chunk: TimeChunk) {
        val updated = _chunks.value.toMutableList()
        updated.add(chunk)
        saveChunksToStorage(updated)
        _chunks.value = updated
    }

    fun updateChunk(chunk: TimeChunk) {
        val updated = _chunks.value.map {
            if (it.id == chunk.id) chunk else it
        }
        saveChunksToStorage(updated)
        _chunks.value = updated
    }

    fun deleteChunk(id: String) {
        val updated = _chunks.value.filterNot { it.id == id }
        saveChunksToStorage(updated)
        _chunks.value = updated
    }

    fun toggleAlarm(id: String) {
        val updated = _chunks.value.map {
            if (it.id == id) it.copy(hasAlarm = !it.hasAlarm) else it
        }
        saveChunksToStorage(updated)
        _chunks.value = updated
    }

    fun copySchedule(fromDay: Int, toDay: Int) {
        val fromChunks = getChunksForDay(fromDay)
        val withoutToDay = _chunks.value.filterNot { it.dayOfWeek == toDay }.toMutableList()
        val copied = fromChunks.map {
            it.copy(
                id = java.util.UUID.randomUUID().toString(),
                dayOfWeek = toDay
            )
        }
        withoutToDay.addAll(copied)
        saveChunksToStorage(withoutToDay)
        _chunks.value = withoutToDay
    }

    fun getCurrentAndNextChunk(dayOfWeek: Int, currentMinute: Int): Pair<TimeChunk?, TimeChunk?> {
        val dayChunks = getChunksForDay(dayOfWeek)
        if (dayChunks.isEmpty()) return Pair(null, null)

        val active = dayChunks.firstOrNull { it.isCurrentlyActive(currentMinute) }
        val next = if (active != null) {
            dayChunks.firstOrNull { it.startMinuteOfDay >= active.endMinuteOfDay }
                ?: dayChunks.firstOrNull { it.id != active.id }
        } else {
            dayChunks.firstOrNull { it.startMinuteOfDay > currentMinute }
                ?: dayChunks.firstOrNull()
        }
        return Pair(active, next)
    }

    private fun generateDefaultSchedule(): List<TimeChunk> {
        val list = mutableListOf<TimeChunk>()
        // Generate for all 7 days of the week (1..7)
        for (day in 1..7) {
            val isWeekend = (day == 6 || day == 7) // Sat, Sun

            // 00:00 - 06:00: Sleep (Alarm at 06:00)
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Deep Sleep & Recovery",
                    startMinuteOfDay = 0,
                    endMinuteOfDay = 360,
                    category = ChunkCategory.SLEEP,
                    hasAlarm = true,
                    notes = "Restful sleep, zero screen time."
                )
            )

            // 06:00 - 07:00: Morning Routine
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Morning Routine & Bathing",
                    startMinuteOfDay = 360,
                    endMinuteOfDay = 420,
                    category = ChunkCategory.ROUTINE,
                    hasAlarm = false,
                    notes = "Bathing, brushing, cleaning & hydration."
                )
            )

            // 07:00 - 07:30: Breakfast
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Healthy Breakfast",
                    startMinuteOfDay = 420,
                    endMinuteOfDay = 450,
                    category = ChunkCategory.MEAL,
                    hasAlarm = false,
                    notes = "High protein breakfast and morning tea."
                )
            )

            if (!isWeekend) {
                // 07:30 - 10:00: First Lecture of College
                list.add(
                    TimeChunk(
                        dayOfWeek = day,
                        title = "College Lecture 1",
                        startMinuteOfDay = 450,
                        endMinuteOfDay = 600,
                        category = ChunkCategory.STUDY,
                        hasAlarm = true,
                        notes = "Attend first major lecture & take active notes."
                    )
                )

                // 10:00 - 13:00: Study & Revision / Free Slot
                list.add(
                    TimeChunk(
                        dayOfWeek = day,
                        title = "Free Slot & Self Study",
                        startMinuteOfDay = 600,
                        endMinuteOfDay = 780,
                        category = ChunkCategory.REST,
                        hasAlarm = false,
                        notes = "Resting period, review lecture notes, revise topics."
                    )
                )
            } else {
                // Weekend Project / Creative Slot
                list.add(
                    TimeChunk(
                        dayOfWeek = day,
                        title = "Creative Projects & Building",
                        startMinuteOfDay = 450,
                        endMinuteOfDay = 780,
                        category = ChunkCategory.WORK,
                        hasAlarm = false,
                        notes = "Building side-projects and deep learning."
                    )
                )
            }

            // 13:00 - 14:00: Lunch
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Lunch & Relaxation",
                    startMinuteOfDay = 780,
                    endMinuteOfDay = 840,
                    category = ChunkCategory.MEAL,
                    hasAlarm = false,
                    notes = "Nutritious lunch and mental break."
                )
            )

            // 14:00 - 17:00: Deep Work / Lab
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = if (isWeekend) "Reading & Upskilling" else "Afternoon Lab & Projects",
                    startMinuteOfDay = 840,
                    endMinuteOfDay = 1020,
                    category = ChunkCategory.WORK,
                    hasAlarm = true,
                    notes = "Deep focused work session."
                )
            )

            // 17:00 - 18:00: Evening Tea & Rest
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Evening Rest & Walk",
                    startMinuteOfDay = 1020,
                    endMinuteOfDay = 1080,
                    category = ChunkCategory.REST,
                    hasAlarm = false,
                    notes = "Outdoor walk, fresh air, casual chatting."
                )
            )

            // 18:00 - 19:30: Gym / Fitness
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Gym & Workout Session",
                    startMinuteOfDay = 1080,
                    endMinuteOfDay = 1170,
                    category = ChunkCategory.FITNESS,
                    hasAlarm = true,
                    notes = "Weight lifting, cardio, stretching."
                )
            )

            // 19:30 - 20:30: Dinner & Wind Down
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Dinner & Family Time",
                    startMinuteOfDay = 1170,
                    endMinuteOfDay = 1230,
                    category = ChunkCategory.MEAL,
                    hasAlarm = false,
                    notes = "Relaxed dinner and conversation."
                )
            )

            // 20:30 - 23:00: Deep Coding & Review
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Coding & Day Review",
                    startMinuteOfDay = 1230,
                    endMinuteOfDay = 1380,
                    category = ChunkCategory.WORK,
                    hasAlarm = true,
                    notes = "100 Days of Code challenge & plan tomorrow."
                )
            )

            // 23:00 - 24:00: Night Routine & Sleep Prep
            list.add(
                TimeChunk(
                    dayOfWeek = day,
                    title = "Wind Down & Night Routine",
                    startMinuteOfDay = 1380,
                    endMinuteOfDay = 1440,
                    category = ChunkCategory.ROUTINE,
                    hasAlarm = false,
                    notes = "Hydration, dim lights, prepare for sleep."
                )
            )
        }
        return list
    }

    companion object {
        private const val KEY_CHUNKS = "saved_time_chunks_json"
    }
}
