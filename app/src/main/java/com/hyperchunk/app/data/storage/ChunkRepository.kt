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
import java.util.UUID

data class RoutinePreset(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val blocks: List<PresetBlock>
)

data class PresetBlock(
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val category: ChunkCategory,
    val hasAlarm: Boolean = false
)

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
        val isClean = prefs.getBoolean(KEY_VERSION, false)
        if (!isClean) {
            prefs.edit().remove(KEY_CHUNKS).putBoolean(KEY_VERSION, true).apply()
            _chunks.value = emptyList()
            return
        }

        val json = prefs.getString(KEY_CHUNKS, null)
        if (json.isNullOrBlank()) {
            _chunks.value = emptyList()
        } else {
            val type = object : TypeToken<List<TimeChunk>>() {}.type
            val list: List<TimeChunk> = try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            _chunks.value = list
        }
    }

    private fun saveChunksToStorage(list: List<TimeChunk>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_CHUNKS, json).apply()
        try {
            com.hyperchunk.app.widget.HyperChunkWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
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

    fun clearDay(dayOfWeek: Int) {
        val updated = _chunks.value.filterNot { it.dayOfWeek == dayOfWeek }
        saveChunksToStorage(updated)
        _chunks.value = updated
    }

    /**
     * Replaces the target days' schedules with the source day's schedule.
     */
    fun syncDayToTargets(sourceDay: Int, targetDays: List<Int>) {
        val sourceChunks = getChunksForDay(sourceDay)
        val filtered = _chunks.value.filterNot { targetDays.contains(it.dayOfWeek) }.toMutableList()

        targetDays.forEach { targetDay ->
            val duplicated = sourceChunks.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    dayOfWeek = targetDay
                )
            }
            filtered.addAll(duplicated)
        }

        saveChunksToStorage(filtered)
        _chunks.value = filtered
    }

    /**
     * Applies a structured productivity preset template to the target day.
     */
    fun applyPreset(targetDay: Int, preset: RoutinePreset) {
        val filtered = _chunks.value.filterNot { it.dayOfWeek == targetDay }.toMutableList()
        val newChunks = preset.blocks.map { block ->
            TimeChunk(
                id = UUID.randomUUID().toString(),
                dayOfWeek = targetDay,
                title = block.title,
                startMinuteOfDay = block.startMinute,
                endMinuteOfDay = block.endMinute,
                category = block.category,
                colorHex = block.category.defaultColor,
                hasAlarm = block.hasAlarm
            )
        }
        filtered.addAll(newChunks)
        saveChunksToStorage(filtered)
        _chunks.value = filtered
    }

    fun getCurrentAndNextChunk(dayOfWeek: Int, currentMinute: Int): Pair<TimeChunk?, TimeChunk?> {
        val dayChunks = getChunksForDay(dayOfWeek).sortedBy { it.startMinuteOfDay }
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

    fun clearAllChunks() {
        saveChunksToStorage(emptyList())
        _chunks.value = emptyList()
    }

    /**
     * Copies all schedule chunks from a source day to a target day.
     */
    fun copySchedule(fromDay: Int, toDay: Int) {
        val source = getChunksForDay(fromDay)
        val filtered = _chunks.value.filterNot { it.dayOfWeek == toDay }.toMutableList()
        val duplicated = source.map {
            it.copy(
                id = UUID.randomUUID().toString(),
                dayOfWeek = toDay
            )
        }
        filtered.addAll(duplicated)
        saveChunksToStorage(filtered)
        _chunks.value = filtered
    }

    /**
     * Automatically identifies unscheduled intervals between 00:00 and 24:00 (1440 mins)
     * and fills each gap >= 15 minutes with a rest/free time chunk.
     */
    fun fillUnscheduledGaps(
        dayOfWeek: Int,
        defaultTitle: String = "Free Time & Rest",
        category: ChunkCategory = ChunkCategory.REST
    ) {
        val dayChunks = getChunksForDay(dayOfWeek).sortedBy { it.startMinuteOfDay }
        val newGaps = mutableListOf<TimeChunk>()

        var cursor = 0
        for (chunk in dayChunks) {
            val chunkStart = chunk.startMinuteOfDay
            val chunkEnd = chunk.endMinuteOfDay

            if (chunkEnd >= chunkStart) {
                if (chunkStart > cursor && (chunkStart - cursor) >= 15) {
                    newGaps.add(
                        TimeChunk(
                            id = UUID.randomUUID().toString(),
                            dayOfWeek = dayOfWeek,
                            title = defaultTitle,
                            startMinuteOfDay = cursor,
                            endMinuteOfDay = chunkStart,
                            category = category,
                            colorHex = category.defaultColor
                        )
                    )
                }
                cursor = maxOf(cursor, chunkEnd)
            } else {
                // Overnight chunk: spans chunkStart -> 1440
                if (chunkStart > cursor && (chunkStart - cursor) >= 15) {
                    newGaps.add(
                        TimeChunk(
                            id = UUID.randomUUID().toString(),
                            dayOfWeek = dayOfWeek,
                            title = defaultTitle,
                            startMinuteOfDay = cursor,
                            endMinuteOfDay = chunkStart,
                            category = category,
                            colorHex = category.defaultColor
                        )
                    )
                }
                cursor = 1440
            }
        }

        if (cursor < 1440 && (1440 - cursor) >= 15) {
            newGaps.add(
                TimeChunk(
                    id = UUID.randomUUID().toString(),
                    dayOfWeek = dayOfWeek,
                    title = defaultTitle,
                    startMinuteOfDay = cursor,
                    endMinuteOfDay = 1440,
                    category = category,
                    colorHex = category.defaultColor
                )
            )
        }

        if (newGaps.isNotEmpty()) {
            val updated = _chunks.value.toMutableList()
            updated.addAll(newGaps)
            saveChunksToStorage(updated)
            _chunks.value = updated
        }
    }

    /**
     * Computes the scheduled duration breakdown by category for a given day in minutes.
     */
    fun getCategoryBreakdownForDay(dayOfWeek: Int): Map<ChunkCategory, Int> {
        val dayChunks = getChunksForDay(dayOfWeek)
        val result = mutableMapOf<ChunkCategory, Int>()
        for (chunk in dayChunks) {
            result[chunk.category] = (result[chunk.category] ?: 0) + chunk.durationMinutes
        }
        return result
    }

    /**
     * Exports all schedule chunks as JSON string.
     */
    fun exportScheduleJson(): String {
        return gson.toJson(_chunks.value)
    }

    /**
     * Imports schedule chunks from JSON string.
     */
    fun importScheduleJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<TimeChunk>>() {}.type
            val list: List<TimeChunk> = gson.fromJson(json, type) ?: return false
            saveChunksToStorage(list)
            _chunks.value = list
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_CHUNKS = "saved_time_chunks_json"
        private const val KEY_VERSION = "data_version_clean_v2"

        val BUILT_IN_PRESETS = listOf(
            RoutinePreset(
                id = "preset_888",
                name = "The 8-8-8 Rule",
                description = "8h Sleep, 8h Focused Work/College, 8h Health & Life",
                iconEmoji = "⚖️",
                blocks = listOf(
                    PresetBlock("Sleep & Deep Rest", 0, 480, ChunkCategory.SLEEP, hasAlarm = true),
                    PresetBlock("Morning Routine & Breakfast", 480, 540, ChunkCategory.ROUTINE),
                    PresetBlock("Deep Work: Focus Sprint 1", 540, 780, ChunkCategory.WORK, hasAlarm = true),
                    PresetBlock("Lunch & Midday Recharge", 780, 840, ChunkCategory.MEAL),
                    PresetBlock("Execution & Projects", 840, 1080, ChunkCategory.STUDY, hasAlarm = true),
                    PresetBlock("Workout & Physical Training", 1080, 1170, ChunkCategory.FITNESS, hasAlarm = true),
                    PresetBlock("Dinner & Social Connection", 1170, 1260, ChunkCategory.MEAL),
                    PresetBlock("Wind Down & Reading", 1260, 1440, ChunkCategory.REST)
                )
            ),
            RoutinePreset(
                id = "preset_student",
                name = "Student / College Grind",
                description = "Morning lectures, afternoon revision slots, evening gym",
                iconEmoji = "🎓",
                blocks = listOf(
                    PresetBlock("Sleep & Recovery", 0, 390, ChunkCategory.SLEEP, hasAlarm = true),
                    PresetBlock("Shower & Campus Breakfast", 390, 480, ChunkCategory.ROUTINE),
                    PresetBlock("College Lecture Block 1", 480, 720, ChunkCategory.STUDY, hasAlarm = true),
                    PresetBlock("Campus Lunch & Rest", 720, 810, ChunkCategory.MEAL),
                    PresetBlock("Lab Work & Revision Slot", 810, 990, ChunkCategory.STUDY),
                    PresetBlock("Gym / Sports Training", 990, 1080, ChunkCategory.FITNESS, hasAlarm = true),
                    PresetBlock("Dinner & Free Time", 1080, 1200, ChunkCategory.REST),
                    PresetBlock("Tomorrow Prep & Sleep Flow", 1200, 1440, ChunkCategory.REST)
                )
            ),
            RoutinePreset(
                id = "preset_deepwork",
                name = "Deep Work & Maker Sprint",
                description = "Unbroken creative flow blocks, zero noise, intense output",
                iconEmoji = "⚡",
                blocks = listOf(
                    PresetBlock("Sleep & Restoration", 0, 420, ChunkCategory.SLEEP, hasAlarm = true),
                    PresetBlock("Morning Walk & Focus Fuel", 420, 510, ChunkCategory.ROUTINE),
                    PresetBlock("Deep Work Block: Build", 510, 750, ChunkCategory.WORK, hasAlarm = true),
                    PresetBlock("Clean Fuel & Walk", 750, 810, ChunkCategory.MEAL),
                    PresetBlock("Secondary Build Sprint", 810, 1020, ChunkCategory.WORK, hasAlarm = true),
                    PresetBlock("High Intensity Workout", 1020, 1110, ChunkCategory.FITNESS, hasAlarm = true),
                    PresetBlock("Dinner & Leisure", 1110, 1230, ChunkCategory.MEAL),
                    PresetBlock("Unplug & Night Routine", 1230, 1440, ChunkCategory.SLEEP)
                )
            )
        )
    }
}
