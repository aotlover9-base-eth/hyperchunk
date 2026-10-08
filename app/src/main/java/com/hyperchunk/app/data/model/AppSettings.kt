package com.hyperchunk.app.data.model

import androidx.compose.ui.graphics.Color

enum class AccentTheme(
    val id: String,
    val displayName: String,
    val colorHex: String,
    val composeColor: Color
) {
    PIXEL_BLUE("pixel_blue", "Pixel Blue", "#3B82F6", Color(0xFF3B82F6)),
    CYBER_CYAN("cyber_cyan", "Cyber Cyan", "#00F2FE", Color(0xFF00F2FE)),
    ELECTRIC_LIME("electric_lime", "Electric Lime", "#A8FF78", Color(0xFFA8FF78)),
    AMETHYST_VIOLET("amethyst_violet", "Amethyst", "#A855F7", Color(0xFFA855F7)),
    SUNSET_AMBER("sunset_amber", "Sunset Amber", "#F59E0B", Color(0xFFF59E0B)),
    ROSE_CRIMSON("rose_crimson", "Rose Crimson", "#F43F5E", Color(0xFFF43F5E));

    companion object {
        fun fromId(id: String): AccentTheme =
            values().firstOrNull { it.id == id } ?: PIXEL_BLUE
    }
}

enum class BackgroundTheme(
    val id: String,
    val displayName: String,
    val bgDark: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color
) {
    HYPER_GRAPHITE(
        id = "hyper_graphite",
        displayName = "Hyper Slate",
        bgDark = Color(0xFF090A0E),
        surface = Color(0xFF13161F),
        surfaceVariant = Color(0xFF1B202B),
        surfaceElevated = Color(0xFF242A38),
        border = Color(0xFF232936)
    ),
    OLED_BLACK(
        id = "oled_black",
        displayName = "OLED Pure Black",
        bgDark = Color(0xFF000000),
        surface = Color(0xFF0D0E12),
        surfaceVariant = Color(0xFF14161C),
        surfaceElevated = Color(0xFF1C1E26),
        border = Color(0xFF20232E)
    );

    companion object {
        fun fromId(id: String): BackgroundTheme =
            values().firstOrNull { it.id == id } ?: HYPER_GRAPHITE
    }
}

enum class PaletteMode(val id: String, val displayName: String) {
    COLORFUL("colorful", "Colourful"),
    MONOCHROME("monochrome", "Monochrome");

    companion object {
        fun fromId(id: String): PaletteMode =
            values().firstOrNull { it.id == id } ?: MONOCHROME
    }
}

enum class HapticMode(val id: String, val displayName: String) {
    OFF("off", "Off"),
    LIGHT("light", "Light"),
    SMOOTH("smooth", "Smooth"),
    RATCHET("ratchet", "Ratchet");

    companion object {
        fun fromId(id: String): HapticMode =
            values().firstOrNull { it.id == id } ?: SMOOTH
    }
}

data class AppSettings(
    val fontScale: Float = 1.0f,
    val compactMode: Boolean = false,
    val paletteMode: PaletteMode = PaletteMode.MONOCHROME,
    val accentTheme: AccentTheme = AccentTheme.PIXEL_BLUE,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.HYPER_GRAPHITE,
    val hapticMode: HapticMode = HapticMode.SMOOTH,
    val use24HourFormat: Boolean = false,
    val defaultSoundAlarm: Boolean = true,
    val defaultDurationMinutes: Int = 60
)
