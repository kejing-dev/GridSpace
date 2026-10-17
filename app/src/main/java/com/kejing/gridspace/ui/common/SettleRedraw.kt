package com.kejing.gridspace.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * Emulator quirk (R5): a freshly created spatial window (Augment, SpatialPopup, and the entry
 * window on a cold start) is composited with its system glass and content texture lagging
 * behind the window's final size until the content is drawn again (the R4 popup's black band
 * under Done, QA N4-03; the scaled rail's lower rows for the first ~40 s; the entry window's
 * lower rows missing at 40 s). This modifier invalidates the node's draw at 0.5 / 1.5 / 3.5 /
 * 7.5 / 15.5 / 31.5 s after composition; each nudge is an empty draw, so it is harmless on a
 * headset.
 */
@Composable
fun Modifier.settleRedraw(): Modifier {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        for (ms in SETTLE_REDRAW_MS) {
            delay(ms)
            tick++
        }
    }
    return drawBehind { if (tick < 0) drawRect(Color.Transparent) }
}

/** Delays between the redraw nudges (cumulative 0.5 / 1.5 / 3.5 / 7.5 / 15.5 / 31.5 s). */
private val SETTLE_REDRAW_MS = listOf(500L, 1_000L, 2_000L, 4_000L, 8_000L, 16_000L)
