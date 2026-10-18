package com.kejing.gridspace.ui

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.ui.editor.components.AxisGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec T.9 (R11): the axis guides sit just outside the plate and never cross it or the edge cubes. */
class AxisGuidesTest {
    private val n = 16
    private val half = n / 2f

    private fun minOf(b: AxisGuides.Box, pick: (com.kejing.gridspace.domain.model.Vec3) -> Float) = pick(b.center) - pick(b.size) / 2f
    private fun maxOf(b: AxisGuides.Box, pick: (com.kejing.gridspace.domain.model.Vec3) -> Float) = pick(b.center) + pick(b.size) / 2f

    @Test
    fun barsStayOutsideThePlateFootprint() {
        val x = AxisGuides.guide(Axis.X, n)
        val z = AxisGuides.guide(Axis.Z, n)
        // +X guide: entirely outside the near (+z) edge; +Z guide: left of the left (−x) edge.
        assertTrue(x.boxes.all { minOf(it) { v -> v.z } >= half })
        assertTrue(z.boxes.all { maxOf(it) { v -> v.x } <= -half })
        // Nothing dips below the plate top; the bars float LIFT above it.
        assertTrue((x.boxes + z.boxes).all { minOf(it) { v -> v.y } >= 0f })
        assertEquals(AxisGuides.LIFT, minOf(x.boxes.first()) { v -> v.y }, 1e-5f)
    }

    @Test
    fun barsAreCentredOnTheEdgeAndTheArrowTipsAreBeyondTheEnd() {
        val x = AxisGuides.guide(Axis.X, n)
        val bar = x.boxes.first()
        assertEquals(-AxisGuides.HALF_LENGTH, minOf(bar) { v -> v.x }, 1e-5f)
        assertEquals(AxisGuides.HALF_LENGTH, maxOf(bar) { v -> v.x }, 1e-5f)
        assertEquals(AxisGuides.HALF_LENGTH + AxisGuides.ARROW_STEPS.sum(), x.tip.x, 1e-5f)
        assertEquals(x.tip.x + AxisGuides.LABEL_GAP, x.label.x, 1e-5f)
        assertEquals(4, x.boxes.size)
    }

    @Test
    fun thereIsNoYGuide() {
        assertEquals(listOf(Axis.X, Axis.Z), AxisGuides.guides(n).map { it.axis })
    }

    @Test
    fun everythingStaysInsideTheVolumeRadius() {
        // Rotation about Y keeps the horizontal radius, so this bound holds at every yaw.
        fun r(x: Float, z: Float) = kotlin.math.sqrt(x * x + z * z)
        // A box's farthest point from the centre is the horizontal corner with the larger |x| and |z|.
        fun far(b: AxisGuides.Box) = r(kotlin.math.abs(b.center.x) + b.size.x / 2f, kotlin.math.abs(b.center.z) + b.size.z / 2f)
        for (g in AxisGuides.guides(n)) {
            assertTrue(g.axis.name, r(g.label.x, g.label.z) <= AxisGuides.MAX_RADIUS)
            assertTrue(g.axis.name, g.boxes.all { far(it) <= AxisGuides.MAX_RADIUS })
        }
    }

    @Test
    fun arrowheadShrinksTowardsTheTip() {
        val sizes = AxisGuides.guide(Axis.Z, n).boxes.drop(1).map { it.size.x }
        assertEquals(listOf(0.35f, 0.25f, 0.15f), sizes)
    }
}
