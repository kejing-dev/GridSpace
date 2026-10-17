package com.kejing.gridspace.ui.editor

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.TargetCellResolver

/** A [SceneHit] after the mode has been applied (design spec E.1). */
sealed class Resolved {
    /** An existing cube (erase / paint / select / eyedropper target). */
    data class Cube(val cell: Cell) : Resolved()

    /** An empty cell inside the grid (add target). */
    data class Empty(val cell: Cell) : Resolved()

    /** Add target that is already occupied. */
    data class Occupied(val cell: Cell) : Resolved()

    /** Add target outside 0..N-1. */
    data object Outside : Resolved()

    /** Nothing useful was hit. */
    data object Nothing : Resolved()

    val cellOrNull: Cell?
        get() =
            when (this) {
                is Cube -> cell
                is Empty -> cell
                is Occupied -> cell
                else -> null
            }
}

/** Pure mapping from what the view hit to what the current mode acts on. */
object HitResolver {
    fun resolve(hit: SceneHit, mode: EditMode, grid: VoxelGrid): Resolved =
        if (mode == EditMode.ADD) resolveAdd(hit, grid) else resolveCube(hit, grid)

    private fun resolveAdd(hit: SceneHit, grid: VoxelGrid): Resolved =
        when (hit) {
            is SceneHit.Voxel -> classifyAdd(TargetCellResolver.adjacentCell(hit.cell, hit.point), grid)
            is SceneHit.Base -> TargetCellResolver.baseCell(hit.point, grid.size)?.let { classifyAdd(it, grid) } ?: Resolved.Outside
            is SceneHit.Ray -> {
                val target = TargetCellResolver.march(hit.origin, hit.direction, grid)
                when {
                    target.before != null -> classifyAdd(target.before, grid)
                    target.hit != null -> Resolved.Occupied(target.hit)
                    else -> Resolved.Outside
                }
            }
            is SceneHit.Target -> classifyAdd(hit.cell, grid)
            SceneHit.None -> Resolved.Nothing
        }

    private fun classifyAdd(cell: Cell, grid: VoxelGrid): Resolved =
        when {
            !grid.inBounds(cell) -> Resolved.Outside
            cell in grid -> Resolved.Occupied(cell)
            else -> Resolved.Empty(cell)
        }

    /** Erase, paint and select act on the cube itself; the plate and empty space are nothing. */
    private fun resolveCube(hit: SceneHit, grid: VoxelGrid): Resolved =
        when (hit) {
            is SceneHit.Voxel -> if (hit.cell in grid) Resolved.Cube(hit.cell) else Resolved.Nothing
            is SceneHit.Ray -> TargetCellResolver.march(hit.origin, hit.direction, grid).hit?.let { Resolved.Cube(it) } ?: Resolved.Nothing
            is SceneHit.Target -> if (hit.cell in grid) Resolved.Cube(hit.cell) else Resolved.Nothing
            is SceneHit.Base, SceneHit.None -> Resolved.Nothing
        }
}
