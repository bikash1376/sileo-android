package com.sileo.island.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * What the system overlay window renders. It's the same [SileoHost] used in the
 * demo, just placed for an overlay context. The small top inset keeps the island
 * clear of the status bar / camera cutout (tunable per device later).
 */
@Composable
fun SileoOverlay() {
    Box(Modifier.fillMaxSize()) {
        // Sit up near the status bar / cutout (tunable per device).
        SileoHost(modifier = Modifier.padding(top = 4.dp))
    }
}
