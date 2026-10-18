package com.kejing.gridspace.ui

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.ui.editor.components.AxisLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec T.9 (R12): two flat lines on the plate's z = 0 (X) and x = 0 (Z) boundaries. */
class AxisLinesTest {
    private val n = 16
    private val half = n / 2f

    private fun minOf(b: AxisLines.Box, pick: (Vec3) -> Float) = pick(b.center) - pick(b.size) / 2f
    private fun maxOf(b: AxisLines.Box, pick: (Vec3) -> Float) = pick(b.center) + pick(b.size) / 2f

    @Test
    fun xLineRunsAlongTheBackBoundaryFromZeroToN() {
        val b = AxisLines.line(Axis.X, n).box
        assertEquals(-half, minOf(b) { it.x }, 1e-5f)
        assertEquals(half, maxOf(b) { it.x }, 1e-5f)
        assertEquals(-half, b.center.z, 1e-6f)
        assertEquals(AxisLines.THICKNESS, b.size.z, 1e-6f)
    }

    @Test
    fun zLineRunsAlongTheLeftBoundaryFromZeroToN() {
        val b = AxisLines.line(Axis.Z, n).box
        assertEquals(-half, minOf(b) { it.z }, 1e-5f)
        assertEquals(half, maxOf(b) { it.z }, 1e-5f)
        assertEquals(-half, b.center.x, 1e-6f)
        assertEquals(AxisLines.THICKNESS, b.size.x, 1e-6f)
    }

    @Test
    fun linesAreFlatAndLiftedJustAboveThePlate() {
        for (l in AxisLines.lines(n)) {
            assertEquals(l.axis.name, AxisLines.LIFT, minOf(l.box) { it.y }, 1e-6f)
            assertEquals(l.axis.name, AxisLines.HEIGHT, l.box.size.y, 1e-6f)
            assertTrue(l.axis.name, l.box.size.y < AxisLines.THICKNESS)
            // Centred on the boundary line, so at most half the width hangs over the plate edge;
            // nothing else outside the grid (and well inside the Volume's clip radius).
            val slack = AxisLines.THICKNESS / 2f + 1e-5f
            assertTrue(l.axis.name, minOf(l.box) { it.x } >= -half - slack && maxOf(l.box) { it.x } <= half + slack)
            assertTrue(l.axis.name, minOf(l.box) { it.z } >= -half - slack && maxOf(l.box) { it.z } <= half + slack)
        }
    }

    @Test
    fun thereIsNoYLine() {
        assertEquals(listOf(Axis.X, Axis.Z), AxisLines.lines(n).map { it.axis })
    }
}
