package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.usecase.SelectionOps
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure maths for the model root pose (design spec C.1 / E.5) and for turning view-space points into
 * cell coordinates. The model root is rotated by Rx(pitch) * Ry(yaw) and uniformly scaled by the
 * cell size, so the inverse is Ry(-yaw) * Rx(-pitch) and a division.
 */
object CameraMath {
    const val START_YAW = 0.60f
    const val START_PITCH = 0.40f
    const val RADIANS_PER_PIXEL = 0.006f
    const val MIN_PITCH = -0.2f
    const val MAX_PITCH = 1.4f
    const val MIN_ZOOM = 0.5f
    const val MAX_ZOOM = 2.5f
    const val RESET_MS = 300L

    /** The model's usable region inside the Volume and how far below centre the plate sits. */
    const val MODEL_REGION_DP = 1200f
    const val PLATE_DROP_DP = 360f

    fun rotateX(v: Vec3, a: Float): Vec3 {
        val c = cos(a)
        val s = sin(a)
        return Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c)
    }

    fun rotateY(v: Vec3, a: Float): Vec3 {
        val c = cos(a)
        val s = sin(a)
        return Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
    }

    /** Model-root local units (cells, origin at the plate centre) from a view-space point in metres. */
    fun viewToLocal(point: Vec3, rootPos: Vec3, yaw: Float, pitch: Float, scale: Float): Vec3 =
        rotateY(rotateX(point - rootPos, -pitch), -yaw) * (1f / scale)

    fun localToView(local: Vec3, rootPos: Vec3, yaw: Float, pitch: Float, scale: Float): Vec3 =
        rotateX(rotateY(local * scale, yaw), pitch) + rootPos

    /** A direction (no translation, no scale) from view space into model-root space. */
    fun viewDirToLocal(dir: Vec3, yaw: Float, pitch: Float): Vec3 = rotateY(rotateX(dir, -pitch), -yaw)

    /** Model-root units -> grid cell space, where cell (x, y, z) spans [x, x+1) etc. */
    fun localToCellSpace(local: Vec3, size: Int): Vec3 = Vec3(local.x + size / 2f, local.y, local.z + size / 2f)

    fun cellSpaceToLocal(p: Vec3, size: Int): Vec3 = Vec3(p.x - size / 2f, p.y, p.z - size / 2f)

    /** Centre of a cell in model-root units. */
    fun cellCenter(cell: Cell, size: Int): Vec3 =
        Vec3(cell.x - size / 2f + 0.5f, cell.y + 0.5f, cell.z - size / 2f + 0.5f)

    /** The grid axis most aligned with a model-space direction, with its sign. */
    fun dominantAxis(v: Vec3): Pair<Axis, Int> =
        when {
            abs(v.x) >= abs(v.y) && abs(v.x) >= abs(v.z) -> Axis.X to (if (v.x >= 0) 1 else -1)
            abs(v.y) >= abs(v.z) -> Axis.Y to (if (v.y >= 0) 1 else -1)
            else -> Axis.Z to (if (v.z >= 0) 1 else -1)
        }

    /**
     * Fallback preview movement when the device ray is unusable (design spec L.1 step 4): the
     * pinch's screen displacement, in cells, slides the start cell along the grid axes that
     * currently face screen-right and screen-up.
     */
    fun offsetTarget(start: Cell, dragDxPx: Float, dragDyPx: Float, cellPx: Float, yaw: Float, pitch: Float): Cell {
        if (cellPx <= 0f) return start
        val (rightAxis, rightSign) = dominantAxis(viewDirToLocal(Vec3(1f, 0f, 0f), yaw, pitch))
        val (upAxis, upSign) = dominantAxis(viewDirToLocal(Vec3(0f, 1f, 0f), yaw, pitch))
        val stepsRight = (dragDxPx / cellPx).roundToInt()
        // Screen y grows downward; the grid's y grows upward.
        val stepsUp = (-dragDyPx / cellPx).roundToInt()
        return start + rightAxis.unit(stepsRight * rightSign) + upAxis.unit(stepsUp * upSign)
    }

    // ---- selection drag-move (spec R4 O.2.2) ---------------------------------------------------

    /** Which plane a drag-move is locked to once it leaves the dead zone. */
    enum class DragPlane { LIFT, GROUND }

    /**
     * The grid axes a drag-move uses, fixed at pinch-down: [h1] is the horizontal axis (X or Z)
     * facing screen-right, [h2] the other horizontal axis with the sign of screen-up (away from
     * the viewer); screen-up maps to Y instead when the plane is LIFT.
     */
    data class DragAxes(val h1: Axis, val h1Sign: Int, val h2: Axis, val h2Sign: Int)

    data class DragStep(val delta: Cell, val plane: DragPlane?)

    /** Dead zone: no step until the drag passes half a cell on an axis. */
    const val DRAG_DEAD_ZONE = 0.5f
    /** tan(60 deg): mostly-vertical drags lift the selection instead of sliding it. */
    const val LIFT_RATIO = 1.73f

    fun dragAxes(yaw: Float, pitch: Float): DragAxes {
        val right = viewDirToLocal(Vec3(1f, 0f, 0f), yaw, pitch)
        val up = viewDirToLocal(Vec3(0f, 1f, 0f), yaw, pitch)
        val (h1, h1Sign) = dominantAxis(Vec3(right.x, 0f, right.z))
        val h2 = if (h1 == Axis.X) Axis.Z else Axis.X
        val upOnH2 = if (h2 == Axis.X) up.x else up.z
        return DragAxes(h1, h1Sign, h2, if (upOnH2 >= 0f) 1 else -1)
    }

    /**
     * Screen displacement (pixels; y down) of a drag that started on a selected cube -> whole-cell
     * offset of the selection. Steps 2-6 of spec R4 O.2.2: pixels to cells, 0.5-cell dead zone,
     * one-time plane lock (LIFT when the drag is mostly vertical, GROUND otherwise), round to
     * whole cells, clamp so the selection's bounding box stays inside the grid.
     */
    fun dragToDelta(
        dragDxPx: Float,
        dragDyPx: Float,
        cellPx: Float,
        axes: DragAxes,
        plane: DragPlane?,
        selection: Set<Cell>,
        size: Int,
    ): DragStep {
        if (cellPx <= 0f) return DragStep(Cell.ZERO, plane)
        val u = dragDxPx / cellPx
        val v = -dragDyPx / cellPx
        if (plane == null && abs(u) < DRAG_DEAD_ZONE && abs(v) < DRAG_DEAD_ZONE) return DragStep(Cell.ZERO, null)
        val locked = plane ?: if (abs(v) >= LIFT_RATIO * abs(u)) DragPlane.LIFT else DragPlane.GROUND
        val stepsRight = u.roundToInt()
        val stepsUp = v.roundToInt()
        val raw =
            axes.h1.unit(stepsRight * axes.h1Sign) +
                if (locked == DragPlane.LIFT) Axis.Y.unit(stepsUp) else axes.h2.unit(stepsUp * axes.h2Sign)
        return DragStep(SelectionOps.clampDelta(selection, raw, size), locked)
    }

    /** Ease-out used by the reset animation. */
    fun easeOut(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return 1f - (1f - x) * (1f - x)
    }
}
