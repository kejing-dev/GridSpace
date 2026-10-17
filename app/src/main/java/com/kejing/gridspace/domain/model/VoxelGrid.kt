package com.kejing.gridspace.domain.model

/**
 * Immutable N x N x N voxel grid: a map from occupied cell to ARGB colour.
 *
 * Every edit produces a new grid. Copying the map is O(n), which is fine even for the 48^3 cap
 * (about 110k entries) because edits are user-paced and never per frame.
 */
class VoxelGrid(val size: Int, val cells: Map<Cell, Int> = emptyMap()) {
    val count: Int
        get() = cells.size

    operator fun get(cell: Cell): Int? = cells[cell]

    operator fun contains(cell: Cell): Boolean = cells.containsKey(cell)

    fun inBounds(cell: Cell): Boolean = cell.inBounds(size)

    /** Applies [changes]; a null value clears the cell. */
    fun with(changes: Map<Cell, Int?>): VoxelGrid {
        if (changes.isEmpty()) return this
        val next = HashMap(cells)
        for ((cell, color) in changes) {
            if (color == null) next.remove(cell) else next[cell] = color
        }
        return VoxelGrid(size, next)
    }

    /** True when at least one face of [cell] is not covered by a neighbour, so it can be seen. */
    fun isVisible(cell: Cell): Boolean =
        NEIGHBOURS.any { !cells.containsKey(cell + it) }

    fun visibleCells(): List<Cell> = cells.keys.filter { isVisible(it) }

    /** Colour used most often, or null for an empty grid. */
    fun dominantColor(): Int? =
        cells.values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

    fun distinctColors(): Set<Int> = cells.values.toHashSet()

    fun isEmpty(): Boolean = cells.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is VoxelGrid && other.size == size && other.cells == cells

    override fun hashCode(): Int = 31 * size + cells.hashCode()

    companion object {
        val SIZES = listOf(16, 24, 32, 48)
        const val DEFAULT_SIZE = 16
        const val MAX_SIZE = 48

        private val NEIGHBOURS =
            listOf(Cell(1, 0, 0), Cell(-1, 0, 0), Cell(0, 1, 0), Cell(0, -1, 0), Cell(0, 0, 1), Cell(0, 0, -1))

        fun of(size: Int, voxels: Iterable<Voxel>): VoxelGrid =
            VoxelGrid(size, voxels.associate { it.cell to it.color })
    }
}
