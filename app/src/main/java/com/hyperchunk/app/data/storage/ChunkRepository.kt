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
        // Automatically purge any previously cached dummy schedule
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

    fun clearAllChunks() {
        saveChunksToStorage(emptyList())
        _chunks.value = emptyList()
    }

    companion object {
        private const val KEY_CHUNKS = "saved_time_chunks_json"
        private const val KEY_VERSION = "data_version_clean_v2"
    }
}
