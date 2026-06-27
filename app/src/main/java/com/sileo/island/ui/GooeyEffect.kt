package com.sileo.island.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

/**
 * Reproduces Sileo's "gooey" SVG filter on Android.
 *
 * The web version chains:
 *   feGaussianBlur(stdDeviation = blur)  ->  feColorMatrix(alpha 20 / -10)
 *
 * We do the identical thing with the platform [RenderEffect] graph:
 *   1. blur the layer so neighbouring shapes' alpha bleeds together
 *   2. push alpha through a steep ramp (multiply, then bias) so the soft
 *      blur collapses back into a hard, merged silhouette — the "metaball".
 *
 * RGB is left as identity, so the pill keeps its solid fill; only alpha is
 * thresholded. Returns null below API 31, where callers should simply draw
 * the shapes without merging (a clean spring morph, no goo).
 *
 * @param blurPx blur radius in pixels (≈ corner radius * 0.5, like the original)
 */
fun gooeyRenderEffect(blurPx: Float): ComposeRenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL)

    // Alpha ramp: A' = 18*A - 9  (in 0..1 terms). In Android's 0..255 colour
    // space the bias must be scaled by 255 -> -9 * 255 ≈ -2295.
    val matrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 18f, -2295f,
        )
    )
    val threshold = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))

    // createChainEffect(outer, inner): inner runs first. blur -> threshold.
    return RenderEffect.createChainEffect(threshold, blur).asComposeRenderEffect()
}
