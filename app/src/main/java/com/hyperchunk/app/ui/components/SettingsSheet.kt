package com.hyperchunk.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperchunk.app.data.model.AccentTheme
import com.hyperchunk.app.data.model.AppSettings
import com.hyperchunk.app.data.model.BackgroundTheme
import com.hyperchunk.app.data.model.HapticMode
import com.hyperchunk.app.ui.theme.LocalAppThemeColors
import com.hyperchunk.app.ui.theme.TextPrimary
import com.hyperchunk.app.ui.theme.TextSecondary
import com.hyperchunk.app.ui.theme.TextTertiary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onFontScaleChanged: (Float) -> Unit,
    onCompactModeChanged: (Boolean) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onBackgroundThemeChanged: (BackgroundTheme) -> Unit,
    onHapticModeChanged: (HapticMode) -> Unit,
    on24HourFormatChanged: (Boolean) -> Unit,
    onDefaultAlarmChanged: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appColors = LocalAppThemeColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = appColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2C3240))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(appColors.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PREFERENCES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = appColors.primary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Appearance & Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Font & UI Scale Slider
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.FormatSize, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(18.dp)) },
                title = "UI & FONT SCALING"
            )

            val currentScale = settings.fontScale
            val scaleLabel = when {
                currentScale <= 0.85f -> "Compact (Pixel 9a)"
                currentScale <= 0.95f -> "Small"
                currentScale <= 1.05f -> "Default (1.00x)"
                currentScale <= 1.15f -> "Large"
                else -> "Extra Large"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.surfaceElevated)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Text Scale: $scaleLabel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(currentScale * 100).roundToInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = currentScale,
                        onValueChange = onFontScaleChanged,
                        valueRange = 0.80f..1.25f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = appColors.primary,
                            activeTrackColor = appColors.primary,
                            inactiveTrackColor = appColors.surfaceVariant
                        )
                    )

                    // Live preview mock card
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(appColors.surface)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Preview: Physics Lecture",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "10:30 AM – 12:00 PM • 45m remaining",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(appColors.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Compact Screen Optimization Toggle (Pixel 9a)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.surfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (settings.compactMode) appColors.primary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Compact Screen Optimization",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Optimizes dial height & spacing for Pixel 9a",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = settings.compactMode,
                        onCheckedChange = onCompactModeChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = appColors.primary,
                            checkedTrackColor = appColors.surface,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = appColors.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Accent Color Palette
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(18.dp)) },
                title = "ACCENT COLOR THEME"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentTheme.values().forEach { theme ->
                    val isSelected = (settings.accentTheme == theme)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(theme.composeColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { onAccentThemeChanged(theme) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (theme == AccentTheme.ELECTRIC_LIME || theme == AccentTheme.CYBER_CYAN) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Background Style (Hyper Slate vs OLED Black)
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.DarkMode, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(18.dp)) },
                title = "BACKGROUND CONTRAST"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BackgroundTheme.values().forEach { bgTheme ->
                    val isSelected = (settings.backgroundTheme == bgTheme)
                    val cardBg by animateColorAsState(
                        targetValue = if (isSelected) appColors.surfaceElevated else appColors.surfaceVariant,
                        label = "cardBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) appColors.primary else Color.Transparent,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onBackgroundThemeChanged(bgTheme) }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(bgTheme.bgDark)
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = bgTheme.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Haptic Mode Selector
            SettingsSectionHeader(
                icon = { Icon(Icons.Default.Vibration, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(18.dp)) },
                title = "HAPTIC FEEDBACK INTENSITY"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(appColors.surfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HapticMode.values().forEach { mode ->
                    val isSelected = (settings.hapticMode == mode)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) appColors.primary else Color.Transparent)
                            .clickable { onHapticModeChanged(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Time & Alarm Preferences
            SettingsSectionHeader(
                icon = null,
                title = "DEFAULTS & CLOCK"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.surfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    // 24 Hour Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("24-Hour Time Format", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("e.g. 14:00 vs 2:00 PM", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = settings.use24HourFormat,
                            onCheckedChange = on24HourFormatChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = appColors.primary,
                                checkedTrackColor = appColors.surface,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = appColors.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Default Alarm Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Enable Alarms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Turn on alerts for newly created chunks", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = settings.defaultSoundAlarm,
                            onCheckedChange = onDefaultAlarmChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = appColors.primary,
                                checkedTrackColor = appColors.surface,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = appColors.surfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Defaults Button
            Button(
                onClick = onResetDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = appColors.surfaceVariant)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset All Settings to Defaults",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: (@Composable () -> Unit)?,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextTertiary,
            letterSpacing = 1.sp
        )
    }
}
