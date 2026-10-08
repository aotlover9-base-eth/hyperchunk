package com.hyperchunk.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = HyperPrimary,
    onPrimary = HyperBgDark,
    secondary = HyperEmerald,
    onSecondary = HyperBgDark,
    tertiary = HyperViolet,
    background = HyperBgDark,
    onBackground = TextPrimary,
    surface = HyperSurface,
    onSurface = TextPrimary,
    surfaceVariant = HyperSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = HyperBorder
)

val HyperShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HyperTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = HyperShapes,
        content = content
    )
}
