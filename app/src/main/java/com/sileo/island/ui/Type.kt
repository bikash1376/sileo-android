package com.sileo.island.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.sileo.island.R

// Inter, bundled as a single variable TTF (res/font/inter.ttf). Each entry pins a
// weight on the font's weight axis so SemiBold/Bold render as real weights, not faux.
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    resId = R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** App-UI typeface. Applied to the in-app screens only — the island keeps its own. */
val InterFamily = FontFamily(
    inter(400),
    inter(500),
    inter(600),
    inter(700),
)
