package com.hyperchunk.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.hyperchunk.app.data.model.AccentTheme
import com.hyperchunk.app.data.model.AppSettings
import com.hyperchunk.app.data.model.BackgroundTheme
import com.hyperchunk.app.data.model.HapticMode
import com.hyperchunk.app.data.model.PaletteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.80f, 1.25f)
        val compactMode = prefs.getBoolean(KEY_COMPACT_MODE, false)
        val paletteId = prefs.getString(KEY_PALETTE_MODE, PaletteMode.MONOCHROME.id) ?: PaletteMode.MONOCHROME.id
        val accentId = prefs.getString(KEY_ACCENT_THEME, AccentTheme.PIXEL_BLUE.id) ?: AccentTheme.PIXEL_BLUE.id
        val bgId = prefs.getString(KEY_BG_THEME, BackgroundTheme.HYPER_GRAPHITE.id) ?: BackgroundTheme.HYPER_GRAPHITE.id
        val hapticId = prefs.getString(KEY_HAPTIC_MODE, HapticMode.SMOOTH.id) ?: HapticMode.SMOOTH.id
        val use24h = prefs.getBoolean(KEY_24H_FORMAT, false)
        val soundAlarm = prefs.getBoolean(KEY_DEFAULT_ALARM, true)
        val duration = prefs.getInt(KEY_DEFAULT_DURATION, 60)

        return AppSettings(
            fontScale = fontScale,
            compactMode = compactMode,
            paletteMode = PaletteMode.fromId(paletteId),
            accentTheme = AccentTheme.fromId(accentId),
            backgroundTheme = BackgroundTheme.fromId(bgId),
            hapticMode = HapticMode.fromId(hapticId),
            use24HourFormat = use24h,
            defaultSoundAlarm = soundAlarm,
            defaultDurationMinutes = duration
        )
    }

    private fun saveAndEmit(newSettings: AppSettings) {
        prefs.edit()
            .putFloat(KEY_FONT_SCALE, newSettings.fontScale)
            .putBoolean(KEY_COMPACT_MODE, newSettings.compactMode)
            .putString(KEY_PALETTE_MODE, newSettings.paletteMode.id)
            .putString(KEY_ACCENT_THEME, newSettings.accentTheme.id)
            .putString(KEY_BG_THEME, newSettings.backgroundTheme.id)
            .putString(KEY_HAPTIC_MODE, newSettings.hapticMode.id)
            .putBoolean(KEY_24H_FORMAT, newSettings.use24HourFormat)
            .putBoolean(KEY_DEFAULT_ALARM, newSettings.defaultSoundAlarm)
            .putInt(KEY_DEFAULT_DURATION, newSettings.defaultDurationMinutes)
            .apply()
        _settings.value = newSettings
    }

    fun updateFontScale(scale: Float) {
        saveAndEmit(_settings.value.copy(fontScale = scale.coerceIn(0.80f, 1.25f)))
    }

    fun updateCompactMode(enabled: Boolean) {
        saveAndEmit(_settings.value.copy(compactMode = enabled))
    }

    fun updatePaletteMode(mode: PaletteMode) {
        saveAndEmit(_settings.value.copy(paletteMode = mode))
    }

    fun updateAccentTheme(theme: AccentTheme) {
        saveAndEmit(_settings.value.copy(accentTheme = theme))
    }

    fun updateBackgroundTheme(theme: BackgroundTheme) {
        saveAndEmit(_settings.value.copy(backgroundTheme = theme))
    }

    fun updateHapticMode(mode: HapticMode) {
        saveAndEmit(_settings.value.copy(hapticMode = mode))
    }

    fun update24HourFormat(enabled: Boolean) {
        saveAndEmit(_settings.value.copy(use24HourFormat = enabled))
    }

    fun updateDefaultSoundAlarm(enabled: Boolean) {
        saveAndEmit(_settings.value.copy(defaultSoundAlarm = enabled))
    }

    fun resetToDefaults() {
        saveAndEmit(AppSettings())
    }

    companion object {
        private const val PREFS_NAME = "hyperchunk_settings"
        private const val KEY_FONT_SCALE = "pref_font_scale"
        private const val KEY_COMPACT_MODE = "pref_compact_mode"
        private const val KEY_PALETTE_MODE = "pref_palette_mode"
        private const val KEY_ACCENT_THEME = "pref_accent_theme"
        private const val KEY_BG_THEME = "pref_bg_theme"
        private const val KEY_HAPTIC_MODE = "pref_haptic_mode"
        private const val KEY_24H_FORMAT = "pref_24h_format"
        private const val KEY_DEFAULT_ALARM = "pref_default_alarm"
        private const val KEY_DEFAULT_DURATION = "pref_default_duration"
    }
}
