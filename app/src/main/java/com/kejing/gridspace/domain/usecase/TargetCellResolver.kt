package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.model.VoxelGrid
import kotlin.math.abs
import kotlin.math.floor

/** Where a ray through the grid ended up: the first cube hit and the empty cell just before it. */
data class RayTarget(val hit: Cell?, val before: Cell?)

/**
 * Turns points and rays in model-local cell coordinates into grid cells (design spec L.1).
 *
 * Model-local coordinates: cell (x, y, z) occupies [x, x+1) x [y, y+1) x [z, z+1); the base plate's
 * top face is y = 0. The SDK gives no surface normal, so the face a tap landed on is derived from
 * where the hit point lies relative to the cube's centre.
 */
object TargetCellResolver {
    /** The cube containing a local point, or null outside the grid. */
    fun cellAt(point: Vec3, size: Int): Cell? {
        val cell = Cell(floor(point.x).toInt(), floor(point.y).toInt(), floor(point.z).toInt())
        return if (cell.inBounds(size)) cell else null
    }

    /**
     * Cell adjacent to [hit] on the side the local [point] touched: the dominant component of the
     * offset from the cube centre picks the face. If the point is clearly not on this cube (the
     * coordinate mapping is unverified on the emulator) the top face is assumed, so a tap always
     * does something sensible.
     */
    fun adjacentCell(hit: Cell, point: Vec3?): Cell {
        if (point == null) return hit + Cell.UNIT_Y
        val d = point - Vec3(hit.x + 0.5f, hit.y + 0.5f, hit.z + 0.5f)
        if (abs(d.x) > 1.5f || abs(d.y) > 1.5f || abs(d.z) > 1.5f) return hit + Cell.UNIT_Y
        return when {
            abs(d.x) >= abs(d.y) && abs(d.x) >= abs(d.z) -> hit + Cell(if (d.x >= 0) 1 else -1, 0, 0)
            abs(d.y) >= abs(d.z) -> hit + Cell(0, if (d.y >= 0) 1 else -1, 0)
            else -> hit + Cell(0, 0, if (d.z >= 0) 1 else -1)
        }
    }

    /** The ground cell under a point on the base plate (y ignored). */
    fun baseCell(point: Vec3, size: Int): Cell? {
        val cell = Cell(floor(point.x).toInt(), 0, floor(point.z).toInt())
        return if (cell.inBounds(size)) cell else null
    }

    /**
     * 3D-DDA march through the grid along a ray in local cell units. Returns the first occupied cell
     * and the empty cell visited just before it. Crossing the base plate (y = 0 going down) counts
     * as a hit on the plate, with the cell above it as the target.
     */
    fun march(origin: Vec3, direction: Vec3, grid: VoxelGrid, maxSteps: Int = 4 * VoxelGrid.MAX_SIZE): RayTarget {
        val dir = direction.normalized()
        if (dir.length() < 1e-6f) return RayTarget(null, null)
        val size = grid.size
        // Move the origin onto the grid's bounding box first so marching never starts far away.
        val entry = clipToBounds(origin, dir, size) ?: return RayTarget(null, null)
        var x = floor(entry.x).toInt()
        var y = floor(entry.y).toInt()
        var z = floor(entry.z).toInt()
        val stepX = if (dir.x >= 0) 1 else -1
        val stepY = if (dir.y >= 0) 1 else -1
        val stepZ = if (dir.z >= 0) 1 else -1
        var tMaxX = boundaryDistance(entry.x, dir.x)
        var tMaxY = boundaryDistance(entry.y, dir.y)
        var tMaxZ = boundaryDistance(entry.z, dir.z)
        val tDeltaX = if (dir.x == 0f) Float.MAX_VALUE else abs(1f / dir.x)
        val tDeltaY = if (dir.y == 0f) Float.MAX_VALUE else abs(1f / dir.y)
        val tDeltaZ = if (dir.z == 0f) Float.MAX_VALUE else abs(1f / dir.z)
        var previous: Cell? = null
        repeat(maxSteps) {
            val cell = Cell(x, y, z)
            if (!cell.inBounds(size)) {
                // Falling through the floor: the plate is solid, so the last cell above it is the target.
                return if (y < 0 && previous != null) RayTarget(null, previous) else RayTarget(null, null)
            }
            if (cell in grid) return RayTarget(cell, previous)
            previous = cell
            when {
                tMaxX < tMaxY && tMaxX < tMaxZ -> { x += stepX; tMaxX += tDeltaX }
                tMaxY < tMaxZ -> { y += stepY; tMaxY += tDeltaY }
                else -> { z += stepZ; tMaxZ += tDeltaZ }
            }
        }
        return RayTarget(null, previous)
    }

    private fun boundaryDistance(p: Float, d: Float): Float =
        when {
            d > 0f -> (floor(p) + 1f - p) / d
            d < 0f -> (p - floor(p)) / -d
            else -> Float.MAX_VALUE
        }

    /** Slab test against [0, size]^3; returns the entry point or the origin when already inside. */
    private fun clipToBounds(origin: Vec3, dir: Vec3, size: Int): Vec3? {
        var tMin = 0f
        var tMax = Float.MAX_VALUE
        val o = floatArrayOf(origin.x, origin.y, origin.z)
        val d = floatArrayOf(dir.x, dir.y, dir.z)
        for (i in 0..2) {
            if (abs(d[i]) < 1e-7f) {
                if (o[i] < 0f || o[i] > size) return null
            } else {
                var t1 = (0f - o[i]) / d[i]
                var t2 = (size - o[i]) / d[i]
                if (t1 > t2) { val t = t1; t1 = t2; t2 = t }
                tMin = maxOf(tMin, t1)
                tMax = minOf(tMax, t2)
                if (tMin > tMax) return null
            }
        }
        // Nudge inside so floor() lands in the first cell rather than on its boundary.
        return origin + dir * (tMin + 1e-4f)
    }
}
