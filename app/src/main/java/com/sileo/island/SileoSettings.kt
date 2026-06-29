package com.sileo.island

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sileo.island.ui.SileoFont

/**
 * One theme's worth of island appearance. [bg]/[bgEnd] form a vertical gradient when
 * [gradient] is on (otherwise only [bg] is used). All colors are forced opaque — the
 * gooey blur+threshold needs a solid fill or the app behind shows through.
 */
data class IslandTheme(
    val bg: Color,
    val bgEnd: Color,
    val gradient: Boolean,
    val title: Color,
    val subtitle: Color,
) {
    fun opaque() = copy(
        bg = bg.copy(alpha = 1f),
        bgEnd = bgEnd.copy(alpha = 1f),
        title = title.copy(alpha = 1f),
        subtitle = subtitle.copy(alpha = 1f),
    )
}

/**
 * User-tunable island appearance: vertical position, font, and per-mode colors
 * (separate light/dark sets). Backed by SharedPreferences and exposed as Compose
 * snapshot state so both the in-app host and the service overlay recompose live.
 * Same process as [AppPrefs], so a single in-memory holder serves both.
 */
object SileoSettings {
    private const val PREFS = "sileo_appearance"

    // Defaults reproduce the original hardcoded look (SileoToast constants).
    val DEFAULT_LIGHT = IslandTheme(
        bg = Color(0xFFFFFFFF),
        bgEnd = Color(0xFFF1F1F4),
        gradient = false,
        title = Color(0xFF09090B),
        subtitle = Color(0xFF52525B),
    )
    val DEFAULT_DARK = IslandTheme(
        bg = Color(0xFF1B1B1D),
        bgEnd = Color(0xFF2A2A2E),
        gradient = false,
        title = Color(0xFFF4F4F5),
        subtitle = Color(0xFFAEAEB4),
    )
    const val DEFAULT_OFFSET_DP = 12f
    const val MIN_OFFSET_DP = 0f
    const val MAX_OFFSET_DP = 120f

    // Reveal speed: a multiplier on how quickly the gooey body opens after the pill
    // appears (higher = snappier; lower = slow, liquid morph). 1.0 = original feel.
    const val DEFAULT_SPEED = 1f
    const val MIN_SPEED = 0.4f
    const val MAX_SPEED = 2.5f

    /** Extra top inset (dp) below the status bar — "move it up / down". */
    var verticalOffsetDp by mutableStateOf(DEFAULT_OFFSET_DP)
        private set
    var gooeySpeed by mutableStateOf(DEFAULT_SPEED)
        private set
    var font by mutableStateOf(SileoFont.INTER)
        private set
    var light by mutableStateOf(DEFAULT_LIGHT)
        private set
    var dark by mutableStateOf(DEFAULT_DARK)
        private set

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Load persisted values into state. Idempotent; call from activity + service. */
    fun load(ctx: Context) {
        val p = prefs(ctx)
        verticalOffsetDp = p.getFloat("offset", DEFAULT_OFFSET_DP)
            .coerceIn(MIN_OFFSET_DP, MAX_OFFSET_DP)
        gooeySpeed = p.getFloat("speed", DEFAULT_SPEED).coerceIn(MIN_SPEED, MAX_SPEED)
        font = runCatching { SileoFont.valueOf(p.getString("font", null) ?: "") }
            .getOrDefault(SileoFont.INTER)
        light = p.readTheme("light", DEFAULT_LIGHT)
        dark = p.readTheme("dark", DEFAULT_DARK)
    }

    fun setVerticalOffset(ctx: Context, dp: Float) {
        verticalOffsetDp = dp.coerceIn(MIN_OFFSET_DP, MAX_OFFSET_DP)
        prefs(ctx).edit().putFloat("offset", verticalOffsetDp).apply()
    }

    fun setGooeySpeed(ctx: Context, v: Float) {
        gooeySpeed = v.coerceIn(MIN_SPEED, MAX_SPEED)
        prefs(ctx).edit().putFloat("speed", gooeySpeed).apply()
    }

    fun setFont(ctx: Context, f: SileoFont) {
        font = f
        prefs(ctx).edit().putString("font", f.name).apply()
    }

    fun setLight(ctx: Context, theme: IslandTheme) = setTheme(ctx, "light", theme.opaque())
    fun setDark(ctx: Context, theme: IslandTheme) = setTheme(ctx, "dark", theme.opaque())

    private fun setTheme(ctx: Context, prefix: String, theme: IslandTheme) {
        if (prefix == "light") light = theme else dark = theme
        prefs(ctx).edit().writeTheme(prefix, theme).apply()
    }

    private fun SharedPreferences.readTheme(prefix: String, fallback: IslandTheme) = IslandTheme(
        bg = Color(getInt("${prefix}_bg", fallback.bg.toArgb())),
        bgEnd = Color(getInt("${prefix}_bgEnd", fallback.bgEnd.toArgb())),
        gradient = getBoolean("${prefix}_gradient", fallback.gradient),
        title = Color(getInt("${prefix}_title", fallback.title.toArgb())),
        subtitle = Color(getInt("${prefix}_subtitle", fallback.subtitle.toArgb())),
    ).opaque()

    private fun SharedPreferences.Editor.writeTheme(prefix: String, t: IslandTheme) = apply {
        putInt("${prefix}_bg", t.bg.toArgb())
        putInt("${prefix}_bgEnd", t.bgEnd.toArgb())
        putBoolean("${prefix}_gradient", t.gradient)
        putInt("${prefix}_title", t.title.toArgb())
        putInt("${prefix}_subtitle", t.subtitle.toArgb())
    }
}
