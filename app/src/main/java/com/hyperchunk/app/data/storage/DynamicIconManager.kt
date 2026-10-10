package com.hyperchunk.app.data.storage

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.hyperchunk.app.data.model.AccentTheme
import com.hyperchunk.app.data.model.PaletteMode

object DynamicIconManager {

    val ALIASES = listOf(
        "com.hyperchunk.app.MainActivityRoseCrimson",
        "com.hyperchunk.app.MainActivityPixelBlue",
        "com.hyperchunk.app.MainActivityCyberCyan",
        "com.hyperchunk.app.MainActivityElectricLime",
        "com.hyperchunk.app.MainActivityAmethystViolet",
        "com.hyperchunk.app.MainActivitySunsetAmber",
        "com.hyperchunk.app.MainActivityMonochrome"
    )

    fun updateAppIcon(context: Context, accentTheme: AccentTheme, paletteMode: PaletteMode) {
        val targetAlias = if (paletteMode == PaletteMode.MONOCHROME) {
            "com.hyperchunk.app.MainActivityMonochrome"
        } else {
            when (accentTheme) {
                AccentTheme.ROSE_CRIMSON -> "com.hyperchunk.app.MainActivityRoseCrimson"
                AccentTheme.PIXEL_BLUE -> "com.hyperchunk.app.MainActivityPixelBlue"
                AccentTheme.CYBER_CYAN -> "com.hyperchunk.app.MainActivityCyberCyan"
                AccentTheme.ELECTRIC_LIME -> "com.hyperchunk.app.MainActivityElectricLime"
                AccentTheme.AMETHYST_VIOLET -> "com.hyperchunk.app.MainActivityAmethystViolet"
                AccentTheme.SUNSET_AMBER -> "com.hyperchunk.app.MainActivitySunsetAmber"
            }
        }

        try {
            val pm = context.packageManager
            val targetComponent = ComponentName(context.packageName, targetAlias)
            val currentState = pm.getComponentEnabledSetting(targetComponent)

            if (currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return // Already the active launcher icon
            }

            // 1. Enable new target first so the launcher never loses the shortcut
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other aliases
            for (alias in ALIASES) {
                if (alias != targetAlias) {
                    val comp = ComponentName(context.packageName, alias)
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DynamicIconManager", "Failed to update dynamic app icon", e)
        }
    }
}
