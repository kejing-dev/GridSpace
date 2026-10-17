package com.kejing.gridspace.ui

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.ui.editor.components.CameraMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec R4 O.2.2 / K4.23: screen displacement of a drag-move -> whole-cell selection offset. */
class CameraMathDragTest {
    private val cellPx = 100f
    private val size = 16
    private val axes = CameraMath.dragAxes(CameraMath.START_YAW, CameraMath.START_PITCH)
    private val selection = setOf(Cell(5, 0, 5), Cell(6, 0, 5))

    private fun step(u: Float, v: Float, plane: CameraMath.DragPlane? = null, sel: Set<Cell> = selection) =
        // Screen y grows downward, so "up" is a negative dy.
        CameraMath.dragToDelta(u * cellPx, -v * cellPx, cellPx, axes, plane, sel, size)

    @Test
    fun insideTheDeadZoneNothingMovesAndNoPlaneIsLocked() {
        val s = step(0.49f, 0.49f)
        assertEquals(Cell.ZERO, s.delta)
        assertNull(s.plane)
    }

    @Test
    fun atTheStartYawScreenRightIsTheGridXAxis() {
        // yaw 0.60: screen-right projects mostly onto +X, screen-up (away from the viewer) onto -Z.
        assertEquals(Axis.X, axes.h1)
        assertEquals(1, axes.h1Sign)
        assertEquals(Axis.Z, axes.h2)
        assertEquals(-1, axes.h2Sign)
        assertEquals(Cell(2, 0, 0), step(2.0f, 0.2f).delta)
    }

    @Test
    fun aMostlyVerticalDragLocksLiftAndMovesY() {
        val s = step(0.3f, 2.0f)
        assertEquals(CameraMath.DragPlane.LIFT, s.plane)
        assertEquals(Cell(0, 2, 0), s.delta)
    }

    @Test
    fun aDiagonalDragLocksGroundAndMovesBothHorizontalAxes() {
        val s = step(1.0f, 1.0f)
        assertEquals(CameraMath.DragPlane.GROUND, s.plane)
        assertEquals(Cell(1, 0, -1), s.delta)
    }

    @Test
    fun theLockedPlaneDoesNotChangeWithLaterRatios() {
        val lifted = step(0.0f, 1.0f)
        assertEquals(CameraMath.DragPlane.LIFT, lifted.plane)
        // Now a strongly horizontal movement: the plane stays LIFT and v still drives Y.
        val later = step(3.0f, 1.0f, lifted.plane)
        assertEquals(CameraMath.DragPlane.LIFT, later.plane)
        assertEquals(Cell(3, 1, 0), later.delta)
    }

    @Test
    fun theDeltaIsClampedSoTheSelectionStaysInsideTheGrid() {
        val edge = setOf(Cell(14, 0, 5), Cell(15, 0, 5))
        assertEquals(Cell(0, 0, 0), step(2.0f, 0.0f, sel = edge).delta)
        assertEquals(Cell(-3, 0, 0), step(-3.0f, 0.0f, sel = edge).delta)
    }
}
