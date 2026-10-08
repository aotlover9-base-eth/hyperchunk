package com.hyperchunk.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hyperchunk.app.data.model.AppSettings
import com.hyperchunk.app.data.model.PaletteMode

data class AppThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val isMonochrome: Boolean,
    val bgDark: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color
)

val LocalAppThemeColors = staticCompositionLocalOf {
    AppThemeColors(
        primary = HyperPrimary,
        onPrimary = HyperBgDark,
        isMonochrome = false,
        bgDark = HyperBgDark,
        surface = HyperSurface,
        surfaceVariant = HyperSurfaceVariant,
        surfaceElevated = HyperSurfaceElevated,
        border = HyperBorder
    )
}

val LocalAppSettings = staticCompositionLocalOf {
    AppSettings()
}

val HyperShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HyperTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val isMonochrome = (settings.paletteMode == PaletteMode.MONOCHROME)
    val accentColor = if (isMonochrome) Color.White else settings.accentTheme.composeColor
    val onPrimaryColor = if (isMonochrome) Color(0xFF0C0E12) else settings.backgroundTheme.bgDark
    val bgTheme = settings.backgroundTheme

    val appColors = remember(settings.accentTheme, settings.backgroundTheme, settings.paletteMode) {
        AppThemeColors(
            primary = accentColor,
            onPrimary = onPrimaryColor,
            isMonochrome = isMonochrome,
            bgDark = bgTheme.bgDark,
            surface = bgTheme.surface,
            surfaceVariant = bgTheme.surfaceVariant,
            surfaceElevated = bgTheme.surfaceElevated,
            border = if (isMonochrome) Color(0xFF283040) else bgTheme.border
        )
    }

    val dynamicColorScheme = remember(appColors) {
        darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            secondary = if (appColors.isMonochrome) Color(0xFFE2E8F0) else HyperEmerald,
            onSecondary = appColors.onPrimary,
            tertiary = if (appColors.isMonochrome) Color(0xFF94A3B8) else HyperViolet,
            background = appColors.bgDark,
            onBackground = TextPrimary,
            surface = appColors.surface,
            onSurface = TextPrimary,
            surfaceVariant = appColors.surfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = appColors.border
        )
    }

    val currentDensity = LocalDensity.current
    val dynamicDensity = remember(currentDensity, settings.fontScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * settings.fontScale
        )
    }

    CompositionLocalProvider(
        LocalAppThemeColors provides appColors,
        LocalAppSettings provides settings,
        LocalDensity provides dynamicDensity
    ) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            shapes = HyperShapes,
            content = content
        )
    }
}
