package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid
import kotlin.math.roundToInt

/** Outcome of a selection operation: the step to commit and the selection afterwards. */
sealed class SelectionResult {
    data class Done(val command: EditCommand.CellsChange, val selection: Set<Cell>) : SelectionResult()

    /** The operation would leave the grid or has no room; nothing changes. */
    data object OutOfBounds : SelectionResult()

    data object NoRoom : SelectionResult()
}

/**
 * Whole-selection edits. All of them are computed around the centre of the selection's bounding
 * box, rounded to the nearest cell centre, so a rotated or moved block stays where the user sees it.
 */
object SelectionOps {
    /** Bounding-box centre in cell units, rounded to the nearest cell. */
    fun center(selection: Set<Cell>): Cell {
        val xs = selection.map { it.x }
        val ys = selection.map { it.y }
        val zs = selection.map { it.z }
        return Cell(
            ((xs.min() + xs.max()) / 2f).roundToInt(),
            ((ys.min() + ys.max()) / 2f).roundToInt(),
            ((zs.min() + zs.max()) / 2f).roundToInt(),
        )
    }

    /** 90 degrees counter-clockwise seen from above, about the vertical axis through the centre. */
    fun rotate90(grid: VoxelGrid, selection: Set<Cell>): SelectionResult {
        if (selection.isEmpty()) return SelectionResult.OutOfBounds
        val c = center(selection)
        val mapping = selection.associateWith { Cell(c.x + (it.z - c.z), it.y, c.z - (it.x - c.x)) }
        return move(grid, selection, mapping)
    }

    fun translate(grid: VoxelGrid, selection: Set<Cell>, delta: Cell): SelectionResult {
        if (selection.isEmpty()) return SelectionResult.OutOfBounds
        return move(grid, selection, selection.associateWith { it + delta })
    }

    /**
     * Clamps a whole-selection offset so the selection's bounding box stays inside 0..size-1 on
     * every axis (spec R4 O.2.2 step 6). The view applies this before previewing; the ViewModel
     * applies it again for scripted deltas.
     */
    fun clampDelta(selection: Set<Cell>, delta: Cell, size: Int): Cell {
        if (selection.isEmpty()) return Cell.ZERO
        val last = size - 1
        fun clamp(d: Int, min: Int, max: Int): Int = d.coerceIn(-min, last - max)
        return Cell(
            clamp(delta.x, selection.minOf { it.x }, selection.maxOf { it.x }),
            clamp(delta.y, selection.minOf { it.y }, selection.maxOf { it.y }),
            clamp(delta.z, selection.minOf { it.z }, selection.maxOf { it.z }),
        )
    }

    /** Cells the selection would occupy after [delta]. */
    fun targets(selection: Set<Cell>, delta: Cell): Set<Cell> = selection.mapTo(HashSet()) { it + delta }

    /** Non-selected cubes that [targets] would replace (spec R4 O.2.3 coral wireframes). */
    fun overwritten(grid: VoxelGrid, selection: Set<Cell>, targets: Set<Cell>): Set<Cell> =
        targets.filterTo(HashSet()) { it in grid && it !in selection }

    /** Removes every selected cube; the selection becomes empty. */
    fun delete(grid: VoxelGrid, selection: Set<Cell>): SelectionResult {
        val command = EditCommand.change(grid, selection.associateWith { null })
        return SelectionResult.Done(command, emptySet())
    }

    /**
     * Copies the selection one column along +X. When that column has any cube outside the selection
     * or leaves the grid, the copy goes a full selection width further instead. The copy becomes the
     * new selection.
     */
    fun duplicate(grid: VoxelGrid, selection: Set<Cell>): SelectionResult {
        if (selection.isEmpty()) return SelectionResult.NoRoom
        val width = selection.maxOf { it.x } - selection.minOf { it.x } + 1
        for (offset in listOf(1, width)) {
            val delta = Cell(offset, 0, 0)
            val targets = selection.map { it + delta }
            val fits = targets.all { grid.inBounds(it) && (it !in grid || it in selection) }
            if (!fits) continue
            val changes = HashMap<Cell, Int?>()
            for (cell in selection) changes[cell + delta] = grid[cell]
            return SelectionResult.Done(EditCommand.change(grid, changes), targets.toSet())
        }
        return SelectionResult.NoRoom
    }

    /** Shared by rotate and translate: vacate the old cells, occupy the new ones (overwriting). */
    private fun move(grid: VoxelGrid, selection: Set<Cell>, mapping: Map<Cell, Cell>): SelectionResult {
        if (mapping.values.any { !grid.inBounds(it) }) return SelectionResult.OutOfBounds
        val changes = HashMap<Cell, Int?>()
        for (cell in selection) changes[cell] = null
        for ((from, to) in mapping) changes[to] = grid[from]
        return SelectionResult.Done(EditCommand.change(grid, changes), mapping.values.toSet())
    }
}
