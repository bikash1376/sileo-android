package com.sileo.island.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sileo.island.ToastData
import kotlin.math.max
import kotlin.math.roundToInt

private val ISLAND = Color(0xFF1B1B1D)
private val TITLE = Color(0xFFF4F4F5)
private val SUBTLE = Color(0xFFAEAEB4)

// Slow, liquid spring so the gooey morph is actually visible (~0.9s).
private fun <T> morphSpring() = spring<T>(dampingRatio = 0.72f, stiffness = 150f)

@Composable
fun SileoToast(
    data: ToastData,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // val pillHeightDp = 48.dp
    val pillHeightDp = 52.dp

    val pillHeightPx = with(density) { pillHeightDp.toPx() }
    // val baseBlurPx = with(density) { 12.dp.toPx() }
    // val peakBlurPx = with(density) { 10.dp.toPx() }
    // val baseBlurPx = with(density) { 14.dp.toPx() }
    // val peakBlurPx = with(density) { 10.dp.toPx() }
    // 14dp blur eroded the body's bottom edge; 10/7 keeps the concave neck without it.
    val baseBlurPx = with(density) { 10.dp.toPx() }
    val peakBlurPx = with(density) { 7.dp.toPx() }
    // val overlapPx = with(density) { 22.dp.toPx() }
    // val overlapPx = with(density) { 42.dp.toPx() }
    val overlapPx = with(density) { 22.dp.toPx() }
    // val bodyCornerPx = with(density) { 26.dp.toPx() }
    val bodyCornerPx = with(density) { 30.dp.toPx() }
    // Extends the drawn blob slightly beyond the measured text so the text isn't flush to the edge.
    val blobPadding = with(density) { 12.dp.toPx() }

    val hasBody = data.description != null || data.actionLabel != null

    // Measured sizes (px). Width is NOT fed back as a layout constraint, so there's
    // no collapse loop; the container just wraps its content.
    var headerWPx by remember { mutableIntStateOf(0) }
    var bodyWPx by remember { mutableIntStateOf(0) }
    var bodyHFullPx by remember { mutableIntStateOf(0) }

    // Reveal fraction (0..1) drives the morph; multiplied by the measured full
    // height so the goo and the clip always agree — no measurement mismatch.
    val reveal by animateFloatAsState(
        targetValue = if (expanded && hasBody) 1f else 0f,
        animationSpec = morphSpring(),
        label = "reveal",
    )
    val bodyAlpha by animateFloatAsState(
        targetValue = if (expanded && hasBody) 1f else 0f,
        animationSpec = tween(if (expanded) 280 else 140),
        label = "bodyAlpha",
    )
    val bodyVisPx = bodyHFullPx * reveal
    // Goo pill width springs (smooths the promise content swap underneath the text).
    // val headerW by animateFloatAsState(headerWPx.toFloat(), morphSpring(), label = "headerW")
    val extraInsetPx = with(density) { 28.dp.toPx() }

// Header should nearly match the body width
// val targetHeaderWidth =
//     max(headerWPx.toFloat(), bodyWPx.toFloat() - extraInsetPx)
// Pill hugs its own content (narrow); the wider body forms the concave goo neck.
val targetHeaderWidth = headerWPx.toFloat()

val headerW by animateFloatAsState(
    targetValue = targetHeaderWidth,
    animationSpec = morphSpring(),
    label = "headerW"
)

    // Dynamic "motion blur": blob gets gooier mid-morph, then settles crisp.
    val extraBlur = remember { Animatable(0f) }
    var primed by remember { mutableIntStateOf(0) }
    LaunchedEffect(expanded) {
        if (primed == 0) { primed = 1; return@LaunchedEffect }
        extraBlur.animateTo(peakBlurPx, tween(140, easing = LinearOutSlowInEasing))
        extraBlur.animateTo(0f, tween(560, easing = FastOutSlowInEasing))
    }
    val goo = gooeyRenderEffect(baseBlurPx + extraBlur.value)

    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        // ---- GOO LAYER (behind; matches the content's wrapped size) ----
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { renderEffect = goo },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val cx = size.width / 2f
                // drawRoundRect(
                //     color = ISLAND,
                //     topLeft = Offset(cx - headerW / 2f, 0f),
                //     size = Size(headerW, pillHeightPx),
                //     cornerRadius = CornerRadius(pillHeightPx / 2f),
                // )
                drawRoundRect(
                    color = ISLAND,
                    topLeft = Offset(cx - (headerW + blobPadding) / 2f, 0f),
                    size = Size(headerW + blobPadding, pillHeightPx),
                    cornerRadius = CornerRadius(pillHeightPx / 2f),
                )
                if (bodyVisPx > 1f) {
                    val top = pillHeightPx - overlapPx
                    // val bw = bodyWPx.toFloat()
                    val bodyWidth = bodyWPx.toFloat() + blobPadding
                    // Two rounded rects + the gooey blur = real Sileo: a wide body below
                    // the narrow pill, with the blur forming the concave neck between them.
                    drawRoundRect(
                        color = ISLAND,
                        topLeft = Offset(cx - bodyWidth / 2f, top),
                        size = Size(bodyWidth, bodyVisPx + overlapPx),
                        cornerRadius = CornerRadius(bodyCornerPx),
                    )
                }
            }
        }

        // ---- CONTENT LAYER (sharp; drives the wrapped size) ----
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .widthIn(min = 234.dp, max = 380.dp)
                    .height(pillHeightDp)
                    .padding(horizontal = 40.dp)
                    .onSizeChanged { headerWPx = it.width },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Crossfade(targetState = data.variant to data.loading, label = "badge") { (v, l) ->
                    Badge(variant = v, loading = l, modifier = Modifier.size(22.dp), appIcon = data.appIcon)
                }
                Text(
                    text = data.title,
                    color = TITLE,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Body: clipped to the animated height; content measured with width
            // bounded (so text wraps) but height unbounded (so we know its full size).
            // Reveal via a custom layout: the content is always measured at its full
            // size; only the OUTPUT height is scaled by `reveal` (and clipped). So the
            // height we measure is exactly the height we draw the goo to.
            Box(
                Modifier
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        // Capture the full size from the SAME placeable we draw, so the
                        // goo height always matches the content (onSizeChanged drifted).
                        bodyWPx = p.width
                        bodyHFullPx = p.height
                        val h = (p.height * reveal).roundToInt().coerceAtLeast(0)
                        layout(p.width, h) { p.place(0, 0) }
                    },
            ) {
                Column(
                    Modifier
                        // Fixed width so the measured height matches what's drawn.
                        // .width(316.dp)
                        // .width(300.dp)
                        .width(344.dp)
                        .padding(horizontal = 26.dp)
                        // .padding(top = 10.dp, bottom = 22.dp)
                        // .padding(top = 14.dp, bottom = 22.dp)
                        .padding(top = 14.dp, bottom = 30.dp)
                        .graphicsLayer { alpha = bodyAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    data.description?.let {
                        Text(
                            text = it,
                            color = SUBTLE,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    data.actionLabel?.let {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .clickable(onClick = onClick)
                                .background(
                                    color = data.variant.accent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50),
                                )
                                .heightIn(min = 28.dp)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = it,
                                color = data.variant.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
