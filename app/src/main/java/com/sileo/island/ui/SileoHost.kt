package com.sileo.island.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.sileo.island.Sileo
import com.sileo.island.ToastData
import kotlinx.coroutines.delay

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
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Sileo.toasts.forEach { toast ->
                key(toast.id) { ToastSlot(toast) }
            }
        }
    }
}

@Composable
private fun ToastSlot(data: ToastData) {
    var visible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(data.id) {
        visible = true
        delay(AUTO_EXPAND_DELAY_MS)
        expanded = true

        val hold = (data.durationMs - AUTO_EXPAND_DELAY_MS - COLLAPSE_SETTLE_MS).coerceAtLeast(1200L)
        delay(hold)

        expanded = false
        delay(COLLAPSE_SETTLE_MS)
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
        exit = scaleOut(
            targetScale = 0.9f,
            transformOrigin = TransformOrigin(0.5f, 0f),
        ) + fadeOut(),
    ) {
        SileoToast(
            data = data,
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
    }
}
