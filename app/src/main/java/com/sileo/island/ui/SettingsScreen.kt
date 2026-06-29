package com.sileo.island.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sileo.island.IslandTheme
import com.sileo.island.Sileo
import com.sileo.island.SileoSettings
import kotlin.math.roundToInt

// Curated swatches for the color pickers — surfaces, neutrals and accents that read
// well in the island. Users tap one to set the field.
private val SWATCHES = listOf(
    0xFFFFFFFF, 0xFFF4F4F5, 0xFFAEAEB4, 0xFF8E8E93, 0xFF52525B,
    0xFF2A2A2E, 0xFF1B1B1D, 0xFF09090B, 0xFF000000,
    0xFF0A84FF, 0xFF34C759, 0xFFFF453A, 0xFFFFB020, 0xFF5E5CE6,
    0xFFFF2D55, 0xFF30D158, 0xFFBF5AF2, 0xFF64D2FF, 0xFFFFD60A,
).map { Color(it) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val c = appColors()

    // Which mode's colors we're editing (defaults to the live system mode).
    var editDark by remember { mutableStateOf(c.dark) }
    val theme = if (editDark) SileoSettings.dark else SileoSettings.light
    fun update(next: IslandTheme) {
        if (editDark) SileoSettings.setDark(context, next) else SileoSettings.setLight(context, next)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.pageBg),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 32.dp),
        ) {
            if (onBack != null) {
                Text(
                    "‹ Back",
                    color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            Text("Customize the island", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Tune how your island looks and moves. Changes apply instantly — tap Preview to see them.",
                color = c.textBody, fontSize = 14.sp, lineHeight = 20.sp,
            )

            Spacer(Modifier.height(20.dp))
            SectionCard(c) {
                SettingLabel("Vertical position", c)
                Caption("Nudge the island up or down from the top of the screen.", c)
                SliderRow(
                    value = SileoSettings.verticalOffsetDp,
                    range = SileoSettings.MIN_OFFSET_DP..SileoSettings.MAX_OFFSET_DP,
                    valueText = "${SileoSettings.verticalOffsetDp.roundToInt()} dp",
                    accent = c.accent,
                    onChange = { SileoSettings.setVerticalOffset(context, it) },
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(c) {
                SettingLabel("Gooey reveal speed", c)
                Caption("How fast the gooey body opens after the pill appears.", c)
                SliderRow(
                    value = SileoSettings.gooeySpeed,
                    range = SileoSettings.MIN_SPEED..SileoSettings.MAX_SPEED,
                    valueText = "${(SileoSettings.gooeySpeed * 100).roundToInt() / 100f}×",
                    accent = c.accent,
                    onChange = { SileoSettings.setGooeySpeed(context, it) },
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(c) {
                SettingLabel("Font", c)
                Caption("Applies to the island's title and text.", c)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SileoFont.values().forEach { f ->
                        val selected = SileoSettings.font == f
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) c.accent else c.pageBg)
                                .clickable { SileoSettings.setFont(context, f) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        ) {
                            Text(
                                f.label,
                                color = if (selected) Color.White else c.textPrimary,
                                fontFamily = f.family,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(c) {
                SettingLabel("Colors", c)
                Caption("Set colors separately for light and dark mode.", c)
                Spacer(Modifier.height(10.dp))
                ModeToggle(editDark = editDark, accent = c.accent, track = c.track, onPrimary = c.textPrimary) {
                    editDark = it
                }

                Spacer(Modifier.height(16.dp))
                ColorField("Background", theme.bg, c) { update(theme.copy(bg = it)) }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gradient background", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Switch(checked = theme.gradient, onCheckedChange = { update(theme.copy(gradient = it)) })
                }
                if (theme.gradient) {
                    Spacer(Modifier.height(10.dp))
                    GradientPreview(theme.bg, theme.bgEnd)
                    Spacer(Modifier.height(10.dp))
                    ColorField("Gradient end", theme.bgEnd, c) { update(theme.copy(bgEnd = it)) }
                }

                Spacer(Modifier.height(14.dp))
                ColorField("Title text", theme.title, c) { update(theme.copy(title = it)) }
                Spacer(Modifier.height(14.dp))
                ColorField("Body text", theme.subtitle, c) { update(theme.copy(subtitle = it)) }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.filledBg)
                    .clickable { Sileo.info("Looking good", "This is how your island looks right now.") }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Preview the island", color = c.filledOn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, c.track, RoundedCornerShape(14.dp))
                    .clickable {
                        SileoSettings.setLight(context, SileoSettings.DEFAULT_LIGHT)
                        SileoSettings.setDark(context, SileoSettings.DEFAULT_DARK)
                        SileoSettings.setVerticalOffset(context, SileoSettings.DEFAULT_OFFSET_DP)
                        SileoSettings.setGooeySpeed(context, SileoSettings.DEFAULT_SPEED)
                        SileoSettings.setFont(context, SileoFont.INTER)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Reset to defaults", color = c.textBody, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(c: AppColors, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun SettingLabel(text: String, c: AppColors) {
    Text(text, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Caption(text: String, c: AppColors) {
    Spacer(Modifier.height(2.dp))
    Text(text, color = c.textSecondary, fontSize = 12.5.sp, lineHeight = 17.sp)
}

@Composable
private fun SliderRow(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    accent: Color,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(12.dp))
        Text(valueText, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModeToggle(
    editDark: Boolean,
    accent: Color,
    track: Color,
    onPrimary: Color,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(track)
            .padding(3.dp),
    ) {
        listOf(false to "Light", true to "Dark").forEach { (isDark, label) ->
            val selected = editDark == isDark
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) accent else Color.Transparent)
                    .clickable { onChange(isDark) }
                    .padding(horizontal = 22.dp, vertical = 7.dp),
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else onPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorField(label: String, current: Color, c: AppColors, onPick: (Color) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(current)
                .border(1.dp, c.track, RoundedCornerShape(7.dp)),
        )
    }
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SWATCHES.forEach { sw ->
            val selected = sw.value == current.value
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(sw)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) c.accent else c.track,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onPick(sw) },
            )
        }
    }
}

@Composable
private fun GradientPreview(start: Color, end: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(start, end))),
    )
}
