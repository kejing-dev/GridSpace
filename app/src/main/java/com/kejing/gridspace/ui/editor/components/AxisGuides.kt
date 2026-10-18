package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Vec3

/**
 * Geometry of the three axis guides on the base plate (spec R4 T.9, R11), in model units (cells;
 * the origin is the centre of the plate's top face, like [CameraMath.cellCenter]). Pure data so it
 * can be unit-tested; [SceneParts.axisGuides] turns it into one instanced entity.
 *
 * The plate (1200 dp) is larger than the inscribed circle of the 1600-dp Volume (its corners sit
 * 848 dp from the centre), so anything at a corner or beyond an edge end is clipped by the Volume
 * at some yaw. The guides therefore stay within [MAX_RADIUS] cells of the centre:
 *  - +X bar along the near edge (z = +N/2 + [GAP]; from the start camera, yaw 0.60, the +z edge is
 *    the one facing the viewer and the −z edge is behind the model), centred on the edge midpoint,
 *    x within ±[HALF_LENGTH];
 *  - +Z bar along the left edge (x = −N/2 − GAP), centred, z within ±HALF_LENGTH;
 *  - each ends with a tapered arrowhead (three shrinking boxes) at the + end, and the letter sits
 *    [LABEL_GAP] beyond the arrow tip.
 *  - No Y guide: a vertical bar and its letter stood in front of the model (user feedback, R11).
 *
 * The bars float [LIFT] above the plate top, GAP outside its edge, and never cross the plate or a
 * cube at (0, y, z) / (x, y, 0).
 */
object AxisGuides {
    const val THICKNESS = 0.08f
    /** Bars float this much above the plate top so they are not blended into its glass. */
    const val LIFT = 0.04f
    const val GAP = 0.35f
    /** Half the length of the X / Z bars; with the arrowhead and letter the far end stays within MAX_RADIUS. */
    const val HALF_LENGTH = 4.7f
    /** Everything stays inside this radius (16 grid: 10.3 cells = 770 dp, under the 800-dp half-Volume). */
    const val MAX_RADIUS = 10.4f
    val ARROW_STEPS = floatArrayOf(0.35f, 0.25f, 0.15f)
    const val LABEL_GAP = 0.5f

    /** One axis-aligned box: centre and size in cells. */
    data class Box(val center: Vec3, val size: Vec3)

    /** Boxes of one guide (bar + arrowhead) and where its letter goes. */
    data class Guide(val axis: Axis, val boxes: List<Box>, val tip: Vec3, val label: Vec3)

    /** The two plate guides, X and Z (no Y, see the class comment). */
    fun guides(gridSize: Int): List<Guide> = listOf(guide(Axis.X, gridSize), guide(Axis.Z, gridSize))

    fun guide(axis: Axis, gridSize: Int): Guide {
        val half = gridSize / 2f
        // The fixed coordinate of the bar: the near (+z) edge for X and Y, the left (−x) edge for Z.
        val edge = if (axis == Axis.Z) -half - GAP else half + GAP
        require(axis != Axis.Y) { "no Y guide" }
        // Bar start and end along the axis; the other coordinates come from point().
        val (start, end) = -HALF_LENGTH to HALF_LENGTH
        val boxes = ArrayList<Box>(1 + ARROW_STEPS.size)
        boxes.add(Box(point(axis, (start + end) / 2f, edge, LIFT + THICKNESS / 2f), extent(axis, end - start, THICKNESS)))
        var cursor = end
        for (step in ARROW_STEPS) {
            boxes.add(Box(point(axis, cursor + step / 2f, edge, LIFT + step / 2f), Vec3(step, step, step)))
            cursor += step
        }
        val tip = point(axis, cursor, edge, LIFT + THICKNESS / 2f)
        val label = point(axis, cursor + LABEL_GAP, edge, LIFT + THICKNESS / 2f)
        return Guide(axis, boxes, tip, label)
    }

    /** A point [along] the axis; the other two coordinates are the corner [edge] (horizontal) and [lift] (vertical). */
    private fun point(axis: Axis, along: Float, edge: Float, lift: Float): Vec3 =
        when (axis) {
            Axis.X -> Vec3(along, lift, edge)
            Axis.Z -> Vec3(edge, lift, along)
            Axis.Y -> error("no Y guide")
        }

    private fun extent(axis: Axis, length: Float, thickness: Float): Vec3 =
        when (axis) {
            Axis.X -> Vec3(length, thickness, thickness)
            Axis.Z -> Vec3(thickness, thickness, length)
            Axis.Y -> error("no Y guide")
        }
}
