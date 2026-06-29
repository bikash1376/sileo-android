package com.sileo.island.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.sileo.island.R

// Each face is bundled as a single variable TTF (res/font/*.ttf). Each entry pins a
// weight on the font's weight axis so SemiBold/Bold render as real weights, not faux.
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun family(resId: Int) = FontFamily(
    variable(resId, 400),
    variable(resId, 500),
    variable(resId, 600),
    variable(resId, 700),
)

/** Inter — app-UI typeface and the default island face. */
val InterFamily = family(R.font.inter)
private val NunitoFamily = family(R.font.nunito)
private val LoraFamily = family(R.font.lora)
private val JetBrainsMonoFamily = family(R.font.jetbrains_mono)

/** The user-selectable island typefaces, exposed in the settings screen. */
enum class SileoFont(val label: String, val family: FontFamily) {
    INTER("Inter", InterFamily),
    NUNITO("Nunito", NunitoFamily),
    LORA("Lora", LoraFamily),
    JETBRAINS_MONO("JetBrains Mono", JetBrainsMonoFamily),
}
