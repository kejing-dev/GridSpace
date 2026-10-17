package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid

/** The brush size (R8): W × H × D cells, 1..16 each, clamped to the grid; 1 × 1 × 1 is a single cell. */
data class BrushSize(val x: Int, val y: Int, val z: Int) {
    val isUnit: Boolean
        get() = x == 1 && y == 1 && z == 1

    val count: Int
        get() = x * y * z

    operator fun get(axis: Axis): Int =
        when (axis) {
            Axis.X -> x
            Axis.Y -> y
            Axis.Z -> z
        }

    fun with(axis: Axis, value: Int): BrushSize =
        when (axis) {
            Axis.X -> copy(x = value)
            Axis.Y -> copy(y = value)
            Axis.Z -> copy(z = value)
        }

    /** Every extent limited to [MIN]..min([MAX], [gridSize]). */
    fun clamped(gridSize: Int): BrushSize {
        val hi = MAX.coerceAtMost(gridSize)
        return BrushSize(x.coerceIn(MIN, hi), y.coerceIn(MIN, hi), z.coerceIn(MIN, hi))
    }

    companion object {
        const val MIN = 1
        const val MAX = 16
        val UNIT = BrushSize(1, 1, 1)
    }
}

/**
 * The one brush of R8 (replaces the Voxel / Bar / Box brushes): a W × H × D block of cells that
 * extends from the target cell along +X / +Y / +Z. Per axis, if that leaves the grid the block
 * is flipped to the − direction; if neither direction fits, the side with more cells inside the
 * grid wins (ties go to +) and is clipped at the edge, so the result always contains the target
 * cell. Occupied cells are skipped when adding; erasing only clears existing cubes. One history
 * step either way; a tap places at once, a pinch-hold previews.
 */
object SizedBrush {
    /** Inclusive extent of one axis: [coordinate] plus [extent] − 1 more cells, flipped / clipped to 0..gridSize − 1. */
    fun span(coordinate: Int, extent: Int, gridSize: Int): IntRange {
        val n = extent.coerceIn(BrushSize.MIN, BrushSize.MAX.coerceAtMost(gridSize))
        val last = gridSize - 1
        if (coordinate + n - 1 <= last) return coordinate..(coordinate + n - 1)
        if (coordinate - n + 1 >= 0) return (coordinate - n + 1)..coordinate
        val roomPlus = last - coordinate + 1
        val roomMinus = coordinate + 1
        return if (roomPlus >= roomMinus) coordinate..last else 0..coordinate
    }

    /** The block's cells as an inclusive axis-aligned region (never empty while [anchor] is in the grid). */
    fun region(anchor: Cell, size: BrushSize, gridSize: Int): BoxRegion {
        val xs = span(anchor.x, size.x, gridSize)
        val ys = span(anchor.y, size.y, gridSize)
        val zs = span(anchor.z, size.z, gridSize)
        return BoxRegion(Cell(xs.first, ys.first, zs.first), Cell(xs.last, ys.last, zs.last))
    }

    /** Cells the block covers, in grid order. */
    fun cells(anchor: Cell, size: BrushSize, gridSize: Int): List<Cell> = region(anchor, size, gridSize).cells().toList()

    fun fill(grid: VoxelGrid, anchor: Cell, size: BrushSize, color: Int): EditCommand.CellsChange =
        BoxFill.fill(grid, region(anchor, size, grid.size), color)

    fun erase(grid: VoxelGrid, anchor: Cell, size: BrushSize): EditCommand.CellsChange =
        BoxFill.erase(grid, region(anchor, size, grid.size))
}
