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
import java.util.UUID

data class RoutinePreset(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val blocks: List<PresetBlock>,
    val isCustom: Boolean = false
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

    private val _customPresets = MutableStateFlow<List<RoutinePreset>>(emptyList())
    val customPresets: StateFlow<List<RoutinePreset>> = _customPresets.asStateFlow()

    init {
        loadChunks()
        loadCustomPresets()
    }

    private fun loadCustomPresets() {
        val json = prefs.getString(KEY_CUSTOM_PRESETS, null)
        if (json.isNullOrBlank()) {
            _customPresets.value = emptyList()
        } else {
            val type = object : TypeToken<List<RoutinePreset>>() {}.type
            val list: List<RoutinePreset> = try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            _customPresets.value = list
        }
    }

    private fun saveCustomPresetsToStorage(list: List<RoutinePreset>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_CUSTOM_PRESETS, json).apply()
    }

    private fun getCalendarDayOfWeek(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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

    private fun loadChunks() {
        val isClean = prefs.getBoolean(KEY_VERSION, false)
        if (!isClean) {
            prefs.edit().remove(KEY_CHUNKS).putBoolean(KEY_VERSION, true).apply()
            val todayDay = getCalendarDayOfWeek()
            val starter = createDefaultStarterSchedule(todayDay)
            saveChunksToStorage(starter)
            _chunks.value = starter
            return
        }

        val json = prefs.getString(KEY_CHUNKS, null)
        if (json.isNullOrBlank()) {
            val todayDay = getCalendarDayOfWeek()
            val starter = createDefaultStarterSchedule(todayDay)
            saveChunksToStorage(starter)
            _chunks.value = starter
        } else {
            val type = object : TypeToken<List<TimeChunk>>() {}.type
            val list: List<TimeChunk> = try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            if (list.isEmpty()) {
                val todayDay = getCalendarDayOfWeek()
                val starter = createDefaultStarterSchedule(todayDay)
                saveChunksToStorage(starter)
                _chunks.value = starter
            } else {
                _chunks.value = list
            }
        }
    }

    fun createDefaultStarterSchedule(dayOfWeek: Int): List<TimeChunk> = Companion.createDefaultStarterSchedule(dayOfWeek)

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
        try {
            com.hyperchunk.app.alarm.AlarmScheduler.cancelChunkAlarm(context, id)
        } catch (_: Exception) {}
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
        _chunks.value.filter { it.dayOfWeek == dayOfWeek && it.hasAlarm }.forEach { chunk ->
            try {
                com.hyperchunk.app.alarm.AlarmScheduler.cancelChunkAlarm(context, chunk.id)
            } catch (_: Exception) {}
        }
        val updated = _chunks.value.filterNot { it.dayOfWeek == dayOfWeek }
        saveChunksToStorage(updated)
        _chunks.value = updated
    }

    private fun intervalsOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
        val spans1 = if (end1 >= start1) listOf(Pair(start1, end1)) else listOf(Pair(start1, 1440), Pair(0, end1))
        val spans2 = if (end2 >= start2) listOf(Pair(start2, end2)) else listOf(Pair(start2, 1440), Pair(0, end2))

        for ((s1, e1) in spans1) {
            for ((s2, e2) in spans2) {
                if (maxOf(s1, s2) < minOf(e1, e2)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Synchronizes schedule chunks from sourceDay to targetDays.
     * If mergeOnly is true, only non-overlapping blocks are added into empty slots.
     */
    fun syncDayToTargets(sourceDay: Int, targetDays: List<Int>, mergeOnly: Boolean = false) {
        val sourceChunks = getChunksForDay(sourceDay)
        if (!mergeOnly) {
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
        } else {
            val updated = _chunks.value.toMutableList()
            targetDays.forEach { targetDay ->
                val existing = getChunksForDay(targetDay)
                sourceChunks.forEach { source ->
                    val hasOverlap = existing.any { ex ->
                        intervalsOverlap(source.startMinuteOfDay, source.endMinuteOfDay, ex.startMinuteOfDay, ex.endMinuteOfDay)
                    }
                    if (!hasOverlap) {
                        updated.add(
                            source.copy(
                                id = UUID.randomUUID().toString(),
                                dayOfWeek = targetDay
                            )
                        )
                    }
                }
            }
            saveChunksToStorage(updated)
            _chunks.value = updated
        }
    }

    /**
     * Applies a structured productivity preset template to the target day.
     * If mergeOnly is true, existing chunks are preserved and only non-overlapping blocks are filled.
     */
    fun applyPreset(targetDay: Int, preset: RoutinePreset, mergeOnly: Boolean = false) {
        if (!mergeOnly) {
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
        } else {
            val existingChunks = getChunksForDay(targetDay)
            val updated = _chunks.value.toMutableList()
            preset.blocks.forEach { block ->
                val hasOverlap = existingChunks.any { existing ->
                    intervalsOverlap(block.startMinute, block.endMinute, existing.startMinuteOfDay, existing.endMinuteOfDay)
                }
                if (!hasOverlap) {
                    val newChunk = TimeChunk(
                        id = UUID.randomUUID().toString(),
                        dayOfWeek = targetDay,
                        title = block.title,
                        startMinuteOfDay = block.startMinute,
                        endMinuteOfDay = block.endMinute,
                        category = block.category,
                        colorHex = block.category.defaultColor,
                        hasAlarm = block.hasAlarm
                    )
                    updated.add(newChunk)
                }
            }
            saveChunksToStorage(updated)
            _chunks.value = updated
        }
    }

    /**
     * Saves a user-created or updated custom routine preset.
     */
    fun saveCustomPreset(preset: RoutinePreset) {
        val updated = _customPresets.value.filterNot { it.id == preset.id }.toMutableList()
        updated.add(preset.copy(isCustom = true))
        saveCustomPresetsToStorage(updated)
        _customPresets.value = updated
    }

    /**
     * Deletes a user-created custom routine preset.
     */
    fun deleteCustomPreset(id: String) {
        val updated = _customPresets.value.filterNot { it.id == id }
        saveCustomPresetsToStorage(updated)
        _customPresets.value = updated
    }

    /**
     * Captures all chunks from a given day and saves them as a new custom RoutinePreset.
     */
    fun saveDayAsPreset(
        dayOfWeek: Int,
        name: String,
        description: String,
        iconEmoji: String
    ): RoutinePreset {
        val dayChunks = getChunksForDay(dayOfWeek).sortedBy { it.startMinuteOfDay }
        val blocks = dayChunks.map { chunk ->
            PresetBlock(
                title = chunk.title,
                startMinute = chunk.startMinuteOfDay,
                endMinute = chunk.endMinuteOfDay,
                category = chunk.category,
                hasAlarm = chunk.hasAlarm
            )
        }
        val preset = RoutinePreset(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Custom Routine" },
            description = description.ifBlank { "${blocks.size} blocks scheduled" },
            iconEmoji = iconEmoji.ifBlank { "⭐" },
            blocks = blocks,
            isCustom = true
        )
        saveCustomPreset(preset)
        return preset
    }

    /**
     * Returns all presets (both built-in curated templates and user custom routines).
     */
    fun getAllPresets(): List<RoutinePreset> {
        return BUILT_IN_PRESETS + _customPresets.value
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
        private const val KEY_CUSTOM_PRESETS = "custom_routine_presets_json"
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

        fun createDefaultStarterSchedule(dayOfWeek: Int): List<TimeChunk> {
            return listOf(
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Sleep & Restoration",
                    startMinuteOfDay = 0,
                    endMinuteOfDay = 420,
                    category = ChunkCategory.SLEEP,
                    colorHex = ChunkCategory.SLEEP.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Morning Fuel & Routine",
                    startMinuteOfDay = 420,
                    endMinuteOfDay = 510,
                    category = ChunkCategory.ROUTINE,
                    colorHex = ChunkCategory.ROUTINE.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Deep Focus: Build",
                    startMinuteOfDay = 510,
                    endMinuteOfDay = 750,
                    category = ChunkCategory.WORK,
                    colorHex = ChunkCategory.WORK.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Clean Lunch & Recharge",
                    startMinuteOfDay = 750,
                    endMinuteOfDay = 810,
                    category = ChunkCategory.MEAL,
                    colorHex = ChunkCategory.MEAL.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Sprint & Projects",
                    startMinuteOfDay = 810,
                    endMinuteOfDay = 1020,
                    category = ChunkCategory.STUDY,
                    colorHex = ChunkCategory.STUDY.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Workout & Training",
                    startMinuteOfDay = 1020,
                    endMinuteOfDay = 1110,
                    category = ChunkCategory.FITNESS,
                    colorHex = ChunkCategory.FITNESS.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Dinner & Social",
                    startMinuteOfDay = 1110,
                    endMinuteOfDay = 1230,
                    category = ChunkCategory.MEAL,
                    colorHex = ChunkCategory.MEAL.defaultColor,
                    hasAlarm = false
                ),
                TimeChunk(
                    dayOfWeek = dayOfWeek,
                    title = "Unplug & Wind Down",
                    startMinuteOfDay = 1230,
                    endMinuteOfDay = 1440,
                    category = ChunkCategory.REST,
                    colorHex = ChunkCategory.REST.defaultColor,
                    hasAlarm = false
                )
            )
        }
    }
}
