package com.sileo.island.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sileo.island.Sileo
import com.sileo.island.SileoSettings
import com.sileo.island.ToastData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_EXPAND_DELAY_MS = 480L
private const val COLLAPSE_SETTLE_MS = 820L
private const val EXIT_MS = 460L

/**
 * Drop this once near the top of your UI. It observes [Sileo.toasts] and renders
 * the stack at top-center, each toast driving its own expand/collapse/dismiss
 * timeline — the "autopilot" behaviour from the web component.
 */
@Composable
fun SileoHost(modifier: Modifier = Modifier) {
    Box(
        // Sizing is supplied by the caller: fillMaxSize() for the in-app host (island
        // centered over full-screen UI), wrap for the overlay window (so it hugs the
        // island and stays touch-transparent elsewhere). statusBarsPadding keeps it
        // clear of the cutout; the user's vertical-offset setting nudges it up/down.
        modifier = modifier
            .statusBarsPadding()
            .padding(top = SileoSettings.verticalOffsetDp.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val toasts = Sileo.toasts
        val peekPx = with(LocalDensity.current) { 9.dp.toPx() }
        toasts.forEachIndexed { index, toast ->
            // Stack them as a deck instead of a long vertical list: 0 = newest (front,
            // full size), higher = older, tucked slightly down + scaled + faded behind.
            val depth = toasts.lastIndex - index
            key(toast.id) {
                Box(
                    Modifier
                        .zIndex(-depth.toFloat())
                        .graphicsLayer {
                            translationY = depth * peekPx
                            val s = (1f - depth * 0.06f).coerceAtLeast(0.7f)
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            alpha = if (depth >= 3) 0f else 1f - depth * 0.18f
                        },
                ) {
                    ToastSlot(toast, front = depth == 0)
                }
            }
        }
    }
}

/** Fire a notification's PendingIntent. Returns true if it was sent. */
private fun sendIntent(pi: android.app.PendingIntent?): Boolean {
    if (pi == null) return false
    return runCatching { pi.send() }.isSuccess
}

@Composable
private fun ToastSlot(data: ToastData, front: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    // Only the front (newest) toast actually expands; ones tucked behind in the deck
    // stay collapsed as pills so the stack stays compact.
    var expandedRaw by remember { mutableStateOf(false) }
    val expanded = expandedRaw && front
    // Flips the moment the user taps or swipes the island away. Both the auto
    // timeline and the manual-dismiss effect check it so only ONE of them runs
    // the exit (and so a leftover blob can't linger).
    var dismissed by remember { mutableStateOf(false) }
    // Vertical drag offset (px, <= 0 — only travels upward).
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // Past this upward travel a release counts as "swipe to remove".
    val dismissThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    fun dismiss() { dismissed = true }

    // Manual dismissal: fade/scale the WHOLE island out in one go, then drop it.
    LaunchedEffect(dismissed) {
        if (!dismissed) return@LaunchedEffect
        visible = false
        delay(EXIT_MS)
        Sileo.dismiss(data.id)
    }

    // Auto timeline (bails if the user already dismissed it). The reveal-speed setting
    // scales the wait before the gooey body opens (faster setting → shorter delay).
    LaunchedEffect(data.id) {
        val expandDelay = (AUTO_EXPAND_DELAY_MS / SileoSettings.gooeySpeed).toLong()
        visible = true
        delay(expandDelay)
        if (dismissed) return@LaunchedEffect
        expandedRaw = true

        val hold = (data.durationMs - expandDelay - COLLAPSE_SETTLE_MS).coerceAtLeast(1200L)
        delay(hold)
        if (dismissed) return@LaunchedEffect

        expandedRaw = false
        delay(COLLAPSE_SETTLE_MS)
        if (dismissed) return@LaunchedEffect
        visible = false
        delay(EXIT_MS)
        Sileo.dismiss(data.id)
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.85f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        ) + fadeIn(spring(stiffness = 600f)),
        // Auto-dismiss (and tap/swipe) exit: slide straight up and fade out — the
        // island leaves the way it came in, never sideways.
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(EXIT_MS.toInt()),
        ) + fadeOut(tween(EXIT_MS.toInt())),
    ) {
        SileoToast(
            data = data,
            expanded = expanded,
            // Pill tap → collapse / toggle the gooey body (no app launch).
            onClick = { expandedRaw = !expandedRaw },
            // Gooey-body tap → launch the source app (in-app demos have no intent,
            // so they fall back to toggling).
            onBodyClick = {
                if (sendIntent(data.contentIntent)) dismiss() else expandedRaw = !expandedRaw
            },
            // Action chip ("Done"/"Reply"/…): fire the notification's action, then go.
            onAction = {
                if (sendIntent(data.actionIntent) || sendIntent(data.contentIntent)) dismiss()
            },
            modifier = Modifier
                .graphicsLayer {
                    translationY = dragY.value
                    // Fade as it's lifted, fully gone by the threshold.
                    alpha = (1f - (-dragY.value / dismissThresholdPx)).coerceIn(0f, 1f)
                }
                .pointerInput(data.id) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            // Clamp to upward-only travel.
                            val next = (dragY.value + delta).coerceAtMost(0f)
                            scope.launch { dragY.snapTo(next) }
                        },
                        onDragEnd = {
                            if (-dragY.value >= dismissThresholdPx) dismiss()
                            else scope.launch { dragY.animateTo(0f, spring()) }
                        },
                    )
                },
        )
    }
}
