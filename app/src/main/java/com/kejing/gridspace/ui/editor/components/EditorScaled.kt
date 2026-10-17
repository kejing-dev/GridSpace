package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.kejing.gridspace.ui.common.settleRedraw

/**
 * Spec R4 S.1: the editor Volume and everything attached to it (Augments, SpatialPopup) keep
 * their physical size and never scale with distance, so one design dp rendered 0.70–0.80 px on
 * the emulator captures against 1.27 px/dp for the system Planar windows (QA N4-01). Every
 * Augment / popup content is wrapped in this helper, which multiplies the density so all spec
 * numbers stay design dp (sp follows, since Compose sp = density × fontScale). The anchor,
 * alignment and offset of the Augment stay outside, in Volume-dp.
 *
 * The wrapper also carries [settleRedraw], which nudges the freshly created spatial window's
 * glass into place on the emulator (see `ui/common/SettleRedraw.kt`).
 */
@Composable
fun EditorScaled(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    Box(modifier = Modifier.settleRedraw()) {
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale), content = content)
    }
}

/** Colour rail, selection panel and colour-edit popup (side cards at z = 0.28). */
const val EDITOR_UI_SCALE = 1.5f

/**
 * Target label card (R7): on the front plane (z 0.92) the emulator renders about 1.7 px per
 * Volume-dp, so the card is not scaled up at all (R5/R6 1.4 made it about 900 px wide).
 */
const val LABEL_UI_SCALE = 1.0f

/** Colour-edit card and eyedropper bar on the front plane (z 0.90), same reason (R7; R6 used 1.5). */
const val FRONT_PLANE_SCALE = 1.0f
