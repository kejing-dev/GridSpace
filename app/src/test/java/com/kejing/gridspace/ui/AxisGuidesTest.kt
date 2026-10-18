package com.kejing.gridspace.ui

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.ui.editor.components.AxisGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec T.9 (R11 final): a corner gizmo outside the plate's right corner; nothing inside the grid. */
class AxisGuidesTest {
    private val n = 16
    private val half = n / 2f

    private fun minOf(b: AxisGuides.Box, pick: (Vec3) -> Float) = pick(b.center) - pick(b.size) / 2f
    private fun maxOf(b: AxisGuides.Box, pick: (Vec3) -> Float) = pick(b.center) + pick(b.size) / 2f

    @Test
    fun originIsOutsideTheNearEdgeMidpoint() {
        val o = AxisGuides.origin(n)
        assertEquals(0f, o.x, 1e-6f)
        assertEquals(half + AxisGuides.EDGE_GAP, o.z, 1e-6f)
        assertEquals(AxisGuides.LIFT, o.y, 1e-6f)
    }

    @Test
    fun everyBoxLiesOutsideTheGridAndAboveThePlate() {
        for (g in AxisGuides.guides(n)) {
            assertTrue(g.axis.name, g.boxes.all { minOf(it) { v -> v.z } >= half })
            // Inside the renderable radius (the Volume clips beyond ~830 dp = 11.1 cells of a 16 grid).
            assertTrue(g.axis.name, kotlin.math.sqrt(g.tip.x * g.tip.x + g.tip.z * g.tip.z) <= 10.6f)
            assertTrue(g.axis.name, g.boxes.all { minOf(it) { v -> v.y } >= AxisGuides.LIFT - 1e-5f })
        }
    }

    @Test
    fun barsStartAtTheOriginAndTheTipsAreBeyondTheArrowheads() {
        val o = AxisGuides.origin(n)
        val x = AxisGuides.guide(Axis.X, n)
        assertEquals(o.x, minOf(x.boxes.first()) { v -> v.x }, 1e-5f)
        assertEquals(o.x + AxisGuides.LENGTH, maxOf(x.boxes.first()) { v -> v.x }, 1e-5f)
        assertEquals(o.x + AxisGuides.LENGTH + AxisGuides.ARROW_STEPS.sum(), x.tip.x, 1e-5f)
        assertEquals(x.tip.x + AxisGuides.LABEL_GAP, x.label.x, 1e-5f)
        assertEquals(o.z, x.label.z, 1e-6f)
        val z = AxisGuides.guide(Axis.Z, n)
        assertEquals(o.z + AxisGuides.LENGTH + AxisGuides.ARROW_STEPS.sum(), z.tip.z, 1e-5f)
        assertEquals(o.x, z.label.x, 1e-6f)
        assertEquals(4, z.boxes.size)
    }

    @Test
    fun arrowheadShrinksTowardsTheTip() {
        val sizes = AxisGuides.guide(Axis.Z, n).boxes.drop(1).map { it.size.x }
        assertEquals(listOf(0.35f, 0.25f, 0.15f), sizes)
    }

    @Test
    fun thereIsNoYGuide() {
        assertEquals(listOf(Axis.X, Axis.Z), AxisGuides.guides(n).map { it.axis })
    }
}
