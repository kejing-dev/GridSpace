package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid
import kotlin.math.max
import kotlin.math.min

/** Axis-aligned box between two corner cells, inclusive, clipped to the grid. */
data class BoxRegion(val min: Cell, val max: Cell) {
    val width: Int
        get() = max.x - min.x + 1

    val height: Int
        get() = max.y - min.y + 1

    val depth: Int
        get() = max.z - min.z + 1

    val cellCount: Int
        get() = width * height * depth

    fun cells(): Sequence<Cell> = sequence {
        for (x in min.x..max.x) for (y in min.y..max.y) for (z in min.z..max.z) yield(Cell(x, y, z))
    }

    companion object {
        fun between(a: Cell, b: Cell, gridSize: Int): BoxRegion {
            val lo = Cell(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
            val hi = Cell(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
            val last = gridSize - 1
            return BoxRegion(
                Cell(lo.x.coerceIn(0, last), lo.y.coerceIn(0, last), lo.z.coerceIn(0, last)),
                Cell(hi.x.coerceIn(0, last), hi.y.coerceIn(0, last), hi.z.coerceIn(0, last)),
            )
        }
    }
}

/** Fills every empty cell or clears every cube inside a region, as one history step (the sized brush's block, R8). */
object BoxFill {
    /** Above this many cells the preview label adds "large, please wait" (spec T.2 step 8). */
    const val LARGE_THRESHOLD = 2_000

    fun fill(grid: VoxelGrid, region: BoxRegion, color: Int): EditCommand.CellsChange {
        val target = HashMap<Cell, Int?>()
        for (cell in region.cells()) {
            // Occupied cells are skipped silently: the spec treats them as "already there".
            if (cell !in grid) target[cell] = color
        }
        return EditCommand.change(grid, target)
    }

    fun erase(grid: VoxelGrid, region: BoxRegion): EditCommand.CellsChange {
        val target = HashMap<Cell, Int?>()
        for (cell in region.cells()) {
            if (cell in grid) target[cell] = null
        }
        return EditCommand.change(grid, target)
    }
}
