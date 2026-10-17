package com.kejing.gridspace.ui

import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.ui.editor.components.LabelAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** R6: the target label card sits above the target on the front plane and never leaves the face. */
class LabelAnchorTest {
    private val cell = 75f / 1600f

    @Test
    fun centredCellOnTheFrontPlaneGoesStraightUp() {
        val a = LabelAnchor.place(Vec3(0.5f, 0.5f, LabelAnchor.Z), cell)
        assertEquals(0.5f, a.x, 1e-5f)
        assertEquals(0.5f - 2f * cell, a.y, 1e-5f)
        assertEquals(LabelAnchor.Z, a.z, 1e-6f)
    }

    @Test
    fun deeperCellsAreProjectedTowardsTheCentre() {
        // x 0.55 stays under the X_MAX clamp (0.58) so the projection itself is observable.
        val front = LabelAnchor.place(Vec3(0.55f, 0.6f, LabelAnchor.Z), cell)
        val back = LabelAnchor.place(Vec3(0.55f, 0.6f, 0.3f), cell)
        assertTrue(back.x > 0.5f && back.x < front.x)
        assertTrue(back.y > 0.5f - 2f * cell && back.y < front.y)
        // Viewer 0.5 depths in front: a cell at z 0.3 keeps 0.58 / 1.2 of its offset from the centre.
        assertEquals(0.5f + 0.05f * (0.58f / 1.2f), back.x, 1e-4f)
    }

    @Test
    fun cardStaysInsideTheFace() {
        // Cells on the front plane at the corners: the anchor is clamped so the whole card fits.
        val topRight = LabelAnchor.place(Vec3(0.99f, 0.02f, LabelAnchor.Z), cell)
        assertEquals(LabelAnchor.X_MAX, topRight.x, 1e-5f)
        assertEquals(LabelAnchor.Y_MIN, topRight.y, 1e-5f)
        val bottomLeft = LabelAnchor.place(Vec3(0.01f, 1.2f, LabelAnchor.Z), cell)
        assertEquals(LabelAnchor.X_MIN, bottomLeft.x, 1e-5f)
        assertEquals(LabelAnchor.Y_MAX, bottomLeft.y, 1e-5f)
        // The limits leave room for the 360 x 96 card (half-width 0.1125, height 0.06); the right
        // limit 0.60 keeps the card left of the rail on the emulator capture (R7 report).
        assertTrue(LabelAnchor.X_MIN >= LabelAnchor.CARD_WIDTH_N / 2f && LabelAnchor.X_MAX <= 1f - LabelAnchor.CARD_WIDTH_N / 2f)
        assertTrue(LabelAnchor.Y_MIN >= LabelAnchor.CARD_HEIGHT_N)
    }
}
