package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Vec3

/**
 * R6 target-label placement: the card hangs **above** the target on a plane near the Volume's
 * front face (z = [Z]), so it is never behind the model, and grows upward (BottomCenter).
 *
 * All coordinates are normalised Volume coordinates (0..1, y down, z = 1 front). A point on the
 * cell's depth would shift on screen when moved to the front plane (perspective, AGENTS R3), so
 * the anchor's x / y are the cell-top point projected onto the front plane along the line to a
 * viewer [VIEWER_DISTANCE] Volume depths in front of the face: offsets from the centre scale by
 * (D + (1 − Z)) / (D + (1 − z_cell)). The anchor is then clamped to x ∈ [[X_MIN], [X_MAX]],
 * y ∈ [[Y_MIN], [Y_MAX]] (R7). The x limits are set for the emulator capture, where the front
 * plane maps to x_px ≈ 1092 + (x − 0.5) × 2560 (≈ 1.6 px/Volume-dp): the 360-wide card
 * (half-width 0.1125) must end left of the colour rail's projection (x_px 1676: anchor 0.60 still
 * touched it by 1 px, 0.58 leaves ≈ 50 px)
 * and start inside the frame (anchor ≥ 0.20); the spec's 0.14 / 0.80 assumed the face fills the
 * frame and put the box label over the rail (`docs/screenshots/r7/probe/`).
 */
object LabelAnchor {
    const val Z = 0.92f
    /**
     * Assumed viewer distance in front of the Volume's front face, in Volume depths. Fitted to
     * the emulator captures (rail at z 0.28 → 0.73 px/Volume-dp, brush row at z 0.75 → 1.2):
     * px/dp ∝ 1 / (D + 1 − z) gives D ≈ 0.48. A headset user sitting further sees the card a
     * little outward of the target; the clamp keeps it inside the face either way.
     */
    const val VIEWER_DISTANCE = 0.5f
    /** Air between the cell's top and the card's bottom edge, in cells. */
    const val UP_CELLS = 1.5f
    const val CARD_WIDTH_N = 360f * LABEL_UI_SCALE / 1600f
    const val CARD_HEIGHT_N = 96f * LABEL_UI_SCALE / 1600f
    const val X_MIN = 0.20f
    const val X_MAX = 0.58f
    const val Y_MIN = 0.08f
    const val Y_MAX = 0.97f

    /** [centre]: the cell centre (normalised); [cellN]: one cell in normalised units. */
    fun place(centre: Vec3, cellN: Float): Vec3 {
        val s = (VIEWER_DISTANCE + (1f - Z)) / (VIEWER_DISTANCE + (1f - centre.z))
        val x = 0.5f + (centre.x - 0.5f) * s
        val y = 0.5f + (centre.y - (0.5f + UP_CELLS) * cellN - 0.5f) * s
        return Vec3(x.coerceIn(X_MIN, X_MAX), y.coerceIn(Y_MIN, Y_MAX), Z)
    }
}
