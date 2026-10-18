package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Vec3

/**
 * Geometry of the two axis lines on the plate (spec R4 T.9, R12), in model units (cells; the
 * origin is the centre of the plate's top face, like [CameraMath.cellCenter]). Pure data so it
 * can be unit-tested; [SceneParts.axisLine] turns each into one instanced entity.
 *
 * Standard editor convention, flat on the grid itself:
 *  - the X line runs along the grid's z = 0 boundary (model z = -N/2, the back edge) from grid
 *    x = 0 to x = N (model -N/2..+N/2), muted red;
 *  - the Z line runs along the x = 0 boundary (model x = -N/2, the left edge) from z = 0 to N,
 *    muted blue;
 *  - both are [THICKNESS] wide (grid lines are ~0.02 cell), [HEIGHT] tall and lifted [LIFT]
 *    above the plate top so they are not blended into its glass; no Y line, no letters (red = X,
 *    blue = Z is the convention). Cubes may occlude them naturally.
 * R11's gizmo (bars + arrowheads + letter chips outside the plate) was rejected by the user; the
 * attempts also showed that the Volume clips scene content farther than ≈ 830–890 dp from the
 * model centre, which the lines (≤ 848 dp at the plate corners) stay inside.
 */
object AxisLines {
    /** Line width in the plate plane. */
    const val THICKNESS = 0.06f
    /** Line height; flat. */
    const val HEIGHT = 0.02f
    /** Bottom of the line above the plate top. */
    const val LIFT = 0.01f

    /** One axis-aligned box: centre and size in cells. */
    data class Box(val center: Vec3, val size: Vec3)

    data class Line(val axis: Axis, val box: Box)

    /** The X and Z lines (no Y, see the class comment). */
    fun lines(gridSize: Int): List<Line> = listOf(line(Axis.X, gridSize), line(Axis.Z, gridSize))

    fun line(axis: Axis, gridSize: Int): Line {
        val half = gridSize / 2f
        val y = LIFT + HEIGHT / 2f
        val box =
            when (axis) {
                Axis.X -> Box(Vec3(0f, y, -half), Vec3(gridSize.toFloat(), HEIGHT, THICKNESS))
                Axis.Z -> Box(Vec3(-half, y, 0f), Vec3(THICKNESS, HEIGHT, gridSize.toFloat()))
                Axis.Y -> error("no Y line")
            }
        return Line(axis, box)
    }
}
