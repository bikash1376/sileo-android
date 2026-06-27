package com.sileo.island.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import com.sileo.island.SileoVariant

/**
 * The colored circular badge with a variant glyph, matching the web component's
 * 24px badge. For PROMISE/loading it renders a spinning arc instead of a glyph.
 */
@Composable
fun Badge(
    variant: SileoVariant,
    loading: Boolean,
    modifier: Modifier = Modifier,
    appIcon: ImageBitmap? = null,
) {
    // Real notifications: show the posting app's icon instead of a glyph.
    if (appIcon != null && !loading) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = modifier.clip(CircleShape).fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        return
    }
    // Always running but trivially cheap; only read in the loading branch.
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)

        if (loading) {
            val ringColor = Color(0xFF8E8E93)
            drawCircle(ringColor.copy(alpha = 0.25f), radius = r, center = c, style = Stroke(width = r * 0.28f))
            rotate(spin, pivot = c) {
                drawArc(
                    color = ringColor,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = r * 0.28f, cap = StrokeCap.Round),
                )
            }
            return@Canvas
        }

        // Solid colored disc + white glyph on top.
        drawCircle(variant.accent, radius = r, center = c)
        val g = Color.White
        val sw = r * 0.30f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        when (variant) {
            SileoVariant.SUCCESS -> {
                val p = Path().apply {
                    moveTo(c.x - r * 0.45f, c.y + r * 0.02f)
                    lineTo(c.x - r * 0.08f, c.y + r * 0.40f)
                    lineTo(c.x + r * 0.50f, c.y - r * 0.40f)
                }
                drawPath(p, g, style = stroke)
            }
            SileoVariant.ERROR -> {
                drawLine(g, Offset(c.x - r * 0.4f, c.y - r * 0.4f), Offset(c.x + r * 0.4f, c.y + r * 0.4f), sw, StrokeCap.Round)
                drawLine(g, Offset(c.x + r * 0.4f, c.y - r * 0.4f), Offset(c.x - r * 0.4f, c.y + r * 0.4f), sw, StrokeCap.Round)
            }
            SileoVariant.WARNING -> {
                drawLine(g, Offset(c.x, c.y - r * 0.45f), Offset(c.x, c.y + r * 0.15f), sw, StrokeCap.Round)
                drawCircle(g, radius = sw * 0.55f, center = Offset(c.x, c.y + r * 0.45f))
            }
            SileoVariant.INFO -> {
                drawCircle(g, radius = sw * 0.55f, center = Offset(c.x, c.y - r * 0.45f))
                drawLine(g, Offset(c.x, c.y - r * 0.12f), Offset(c.x, c.y + r * 0.45f), sw, StrokeCap.Round)
            }
            SileoVariant.ACTION -> {
                drawLine(g, Offset(c.x - r * 0.45f, c.y), Offset(c.x + r * 0.4f, c.y), sw, StrokeCap.Round)
                drawLine(g, Offset(c.x + r * 0.05f, c.y - r * 0.35f), Offset(c.x + r * 0.45f, c.y), sw, StrokeCap.Round)
                drawLine(g, Offset(c.x + r * 0.05f, c.y + r * 0.35f), Offset(c.x + r * 0.45f, c.y), sw, StrokeCap.Round)
            }
            SileoVariant.PROMISE -> { /* handled by loading branch above */ }
        }
    }
}
