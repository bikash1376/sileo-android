package com.sileo.island.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The in-app UI palette (the onboarding / picker / settings screens — NOT the island,
 * which has its own user-tunable colors). Two fixed sets; [appColors] picks the one
 * matching the system theme so the app itself is dark in dark mode, light otherwise.
 */
data class AppColors(
    val pageBg: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textBody: Color,
    val accent: Color,
    val success: Color,
    val successText: Color,
    val track: Color,
    val filledBg: Color,
    val filledOn: Color,
    val dark: Boolean,
)

private val LightAppColors = AppColors(
    pageBg = Color(0xFFF4F4F5),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF18181B),
    textSecondary = Color(0xFF71717A),
    textBody = Color(0xFF52525B),
    accent = Color(0xFF0A84FF),
    success = Color(0xFF34C759),
    successText = Color(0xFF15803D),
    track = Color(0xFFE4E4E7),
    filledBg = Color(0xFF18181B),
    filledOn = Color(0xFFFFFFFF),
    dark = false,
)

private val DarkAppColors = AppColors(
    pageBg = Color(0xFF0B0B0C),
    surface = Color(0xFF1B1B1D),
    textPrimary = Color(0xFFF4F4F5),
    textSecondary = Color(0xFF8E8E93),
    textBody = Color(0xFFAEAEB4),
    accent = Color(0xFF0A84FF),
    success = Color(0xFF34C759),
    successText = Color(0xFF4ADE80),
    track = Color(0xFF3A3A3E),
    filledBg = Color(0xFFF4F4F5),
    filledOn = Color(0xFF09090B),
    dark = true,
)

@Composable
fun appColors(): AppColors = if (isSystemInDarkTheme()) DarkAppColors else LightAppColors
