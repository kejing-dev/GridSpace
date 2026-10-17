package com.kejing.gridspace.ui.editor.components

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.TargetCellResolver
import com.kejing.gridspace.ui.editor.SceneHit
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ViewCoordinateSpace
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.gesture.data.InputDevicePose
import com.pico.spatial.ui.geometry.Offset3D

/**
 * Converts gesture data (view-local pixels, device poses) into [SceneHit]s in grid cell space.
 *
 * Neither path can be exercised on the emulator (no spatial input), so each is defensive:
 * - tap positions are assumed to be view-local pixels with the origin at the top-left and y down;
 *   a point that does not land near the hit cube falls back to its top face in the resolver;
 * - the device-pose ray is only trusted while a 3D-DDA march through the grid reproduces the entity
 *   the SDK says was hit (spec L.1 step 4); otherwise the pinch's screen displacement slides the
 *   start cell along the grid axes facing the viewer.
 */
internal class ViewMapper(
    private val scene: VoxelScene,
    private val size: Int,
    private val cellMeters: Float,
    private val widthPx: Float,
    private val heightPx: Float,
    private val density: Float,
    private val dpToMeters: (Float) -> Float,
) {
    private var rootPos = Vec3.ZERO
    private var yaw = 0f
    private var pitch = 0f
    private var zoom = 1f

    fun pose(rootPos: Vec3, yaw: Float, pitch: Float, zoom: Float) {
        this.rootPos = rootPos
        this.yaw = yaw
        this.pitch = pitch
        this.zoom = zoom
    }

    private fun pxToMeters(px: Float): Float = dpToMeters(px / density)

    /** View-local pixels -> grid cell space. */
    private fun pixelToCellSpace(p: Offset3D): Vec3 {
        val view = Vec3(pxToMeters(p.x - widthPx / 2f), pxToMeters(heightPx / 2f - p.y), pxToMeters(p.z))
        return CameraMath.localToCellSpace(CameraMath.viewToLocal(view, rootPos, yaw, pitch, cellMeters * zoom), size)
    }

    fun tapHit(entity: Entity?, position: Offset3D?): SceneHit {
        val point = position?.let { runCatching { pixelToCellSpace(it) }.getOrNull() }
        val cell = scene.cellOf(entity)
        return when {
            cell != null -> SceneHit.Voxel(cell, point)
            scene.isPlate(entity) -> point?.let { SceneHit.Base(it) } ?: SceneHit.None
            else -> SceneHit.None
        }
    }

    /** Ray from the hand / controller pose in grid cell space, or null when conversion fails. */
    fun rayHit(pose: InputDevicePose): SceneHit.Ray? =
        runCatching {
            val content = scene.content ?: return null
            val local = content.localSpatialCoordinateSpace
            val rotation = content.convertRotation(pose.rawRotation.toQuaternion(), ViewCoordinateSpace.Global, local)
            val forward = rotation.rotateVector(Vector3(0f, 0f, -1f))
            val raw = pose.rawPosition
            val origin = content.convertPosition(Vector3(pxToMeters(raw.x), pxToMeters(raw.y), pxToMeters(raw.z)), ViewCoordinateSpace.Global, local)
            val originCell = CameraMath.localToCellSpace(CameraMath.viewToLocal(Vec3(origin.x, origin.y, origin.z), rootPos, yaw, pitch, cellMeters * zoom), size)
            val dirCell = CameraMath.viewDirToLocal(Vec3(forward.x, forward.y, forward.z), yaw, pitch).normalized()
            if (!originCell.x.isFinite() || !dirCell.x.isFinite()) null else SceneHit.Ray(originCell, dirCell)
        }.getOrNull()

    /** The ray is trusted only when marching it reproduces what the SDK reported as hit. */
    fun validates(ray: SceneHit.Ray, cell: Cell?, plate: Boolean, grid: VoxelGrid): Boolean {
        val target = TargetCellResolver.march(ray.origin, ray.direction, grid)
        return when {
            cell != null -> target.hit == cell
            plate -> target.hit == null && target.before != null
            else -> false
        }
    }

    /** Start cell for the offset fallback: above the hit cube when adding, the cube itself otherwise. */
    fun fallbackAnchor(cell: Cell?, mode: EditMode): Cell =
        when {
            cell == null -> Cell(size / 2, 0, size / 2)
            mode == EditMode.ADD -> cell + Cell.UNIT_Y
            else -> cell
        }

    fun offsetTarget(anchor: Cell, drag: Offset3D, cellDp: Float): Cell =
        CameraMath.offsetTarget(anchor, drag.x, drag.y, cellDp * density * zoom, yaw, pitch)
}
