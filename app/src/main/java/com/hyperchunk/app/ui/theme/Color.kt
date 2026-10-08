package com.hyperchunk.app.ui.theme

import androidx.compose.ui.graphics.Color

// HyperOS 3 Obsidian & Graphite Backgrounds
val HyperBgDark = Color(0xFF090A0E)
val HyperSurface = Color(0xFF13161F)
val HyperSurfaceVariant = Color(0xFF1B202B)
val HyperSurfaceElevated = Color(0xFF242A38)
val HyperBorder = Color(0xFF232936)
val HyperBorderGlow = Color(0xFF3B82F6)

// HyperOS 3 Sophisticated Vibrant Accents
val HyperPrimary = Color(0xFF3B82F6) // Electric Cobalt
val HyperCobalt = Color(0xFF3B82F6)
val HyperEmerald = Color(0xFF10B981) // Fresh Mint / Emerald
val HyperAmber = Color(0xFFF59E0B) // Warm Amber Gold
val HyperRose = Color(0xFFF43F5E) // Energetic Crimson
val HyperViolet = Color(0xFF8B5CF6) // Soft Lavender
val HyperCyan = Color(0xFF06B6D4) // Refined Deep Cyan
val HyperLime = Color(0xFF10B981)
val HyperCoral = Color(0xFFF43F5E)
val HyperBlue = Color(0xFF3B82F6)

// Typography Palette
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

/**
 * Pure Kotlin hex parser that safely converts hex strings (#RRGGBB, #AARRGGBB, #RGB)
 * to Compose Color without relying on android.graphics.Color (making it 100% JVM test friendly).
 */
fun parseHexColor(hex: String?, fallback: Color = HyperPrimary): Color {
    if (hex.isNullOrBlank()) return fallback
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> {
                val rgb = clean.toLong(16)
                Color(0xFF000000 or rgb)
            }
            8 -> {
                val argb = clean.toLong(16)
                Color(argb)
            }
            3 -> {
                val r = clean[0].digitToInt(16)
                val g = clean[1].digitToInt(16)
                val b = clean[2].digitToInt(16)
                Color(0xFF000000 or ((r * 17L) shl 16) or ((g * 17L) shl 8) or (b * 17L))
            }
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
