package com.sileo.island.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sileo.island.Sileo

/**
 * What the system overlay window renders. It's the same [SileoHost] used in the
 * demo, just placed for an overlay context. The small top inset keeps the island
 * clear of the status bar / camera cutout (tunable per device later).
 */
@Composable
fun SileoOverlay() {
    // While Sileo's own activity is on screen, its in-app host draws the island;
    // suppress the overlay copy so the two don't stack into a doubled gooey.
    if (Sileo.appInForeground) return
    // Fill width (the overlay window is full-width, WRAP_CONTENT height) so the island
    // centers itself and never drifts sideways. Vertical placement (clearing the cutout
    // + the user's up/down nudge) is handled inside SileoHost via SileoSettings.
    SileoHost(modifier = Modifier.fillMaxWidth())
}
