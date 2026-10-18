package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Vec3

/**
 * Geometry of the axis gizmo (spec R4 T.9, R11 final), in model units (cells; the origin is the
 * centre of the plate's top face, like [CameraMath.cellCenter]). Pure data so it can be
 * unit-tested; [SceneParts.axisGuides] turns it into one instanced entity.
 *
 * A small gizmo just outside the plate's near (+z) edge, at that edge's midpoint (model
 * (0, 0.04, N/2 + [EDGE_GAP])):
 *  - +X bar from the gizmo origin [LENGTH] cells along +x, +Z bar likewise along +z;
 *  - each ends with a tapered arrowhead (three shrinking boxes) at the + end, and the letter sits
 *    [LABEL_GAP] beyond the arrow tip (Augment chips, see EditorScreen);
 *  - no Y bar (a vertical bar and its letter stood in front of the model, user feedback).
 * Everything lies outside the grid (z > N/2), so cubes can never occlude it and it never covers
 * the model. The Volume clips anything farther than ≈ 830–890 dp from the model centre (the plate
 * corner at 848 dp still shows, a gizmo beyond it at ≥ 896 dp did not), so the gizmo sits at an
 * edge midpoint (645 dp) and is short: its +Z tip stays at ≈ 790 dp. Earlier cuts along the plate
 * edges left the frame (near edges) or were hidden behind the model (back edge).
 */
object AxisGuides {
    const val THICKNESS = 0.08f
    /** Bars float this much above the plate top so they are not blended into its glass. */
    const val LIFT = 0.04f
    /** Gizmo origin: this far beyond the plate's +z edge. */
    const val EDGE_GAP = 0.6f
    const val LENGTH = 1.2f
    val ARROW_STEPS = floatArrayOf(0.35f, 0.25f, 0.15f)
    const val LABEL_GAP = 0.5f

    /** One axis-aligned box: centre and size in cells. */
    data class Box(val center: Vec3, val size: Vec3)

    /** Boxes of one guide (bar + arrowhead) and where its letter goes. */
    data class Guide(val axis: Axis, val boxes: List<Box>, val tip: Vec3, val label: Vec3)

    /** The gizmo origin for a grid of [gridSize] cells. */
    fun origin(gridSize: Int): Vec3 = Vec3(0f, LIFT, gridSize / 2f + EDGE_GAP)

    /** The two gizmo guides, X and Z (no Y, see the class comment). */
    fun guides(gridSize: Int): List<Guide> = listOf(guide(Axis.X, gridSize), guide(Axis.Z, gridSize))

    fun guide(axis: Axis, gridSize: Int): Guide {
        require(axis != Axis.Y) { "no Y guide" }
        val o = origin(gridSize)
        val boxes = ArrayList<Box>(1 + ARROW_STEPS.size)
        boxes.add(Box(point(axis, o, LENGTH / 2f, THICKNESS / 2f), extent(axis, LENGTH, THICKNESS)))
        var cursor = LENGTH
        for (step in ARROW_STEPS) {
            boxes.add(Box(point(axis, o, cursor + step / 2f, step / 2f), Vec3(step, step, step)))
            cursor += step
        }
        val tip = point(axis, o, cursor, THICKNESS / 2f)
        val label = point(axis, o, cursor + LABEL_GAP, THICKNESS / 2f)
        return Guide(axis, boxes, tip, label)
    }

    /** A point [along] the axis from the origin [o]; [lift] is the box's half-height above LIFT. */
    private fun point(axis: Axis, o: Vec3, along: Float, lift: Float): Vec3 =
        when (axis) {
            Axis.X -> Vec3(o.x + along, o.y + lift, o.z)
            Axis.Z -> Vec3(o.x, o.y + lift, o.z + along)
            Axis.Y -> error("no Y guide")
        }

    private fun extent(axis: Axis, length: Float, thickness: Float): Vec3 =
        when (axis) {
            Axis.X -> Vec3(length, thickness, thickness)
            Axis.Z -> Vec3(thickness, thickness, length)
            Axis.Y -> error("no Y guide")
        }
}
