package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid

/**
 * One undoable step. Every edit, from a single cube to a rotated selection, is recorded as the set
 * of cells it touched with their colour before and after (null = empty), so apply and undo are the
 * same operation in opposite directions and nothing else needs to know the edit's shape.
 */
sealed class EditCommand {
    abstract fun apply(grid: VoxelGrid): VoxelGrid

    abstract fun undo(grid: VoxelGrid): VoxelGrid

    /** True when applying would change nothing; such commands never enter the history. */
    abstract val isEmpty: Boolean

    data class CellsChange(val before: Map<Cell, Int?>, val after: Map<Cell, Int?>) : EditCommand() {
        override fun apply(grid: VoxelGrid): VoxelGrid = grid.with(after)

        override fun undo(grid: VoxelGrid): VoxelGrid = grid.with(before)

        override val isEmpty: Boolean
            get() = after.isEmpty()

        val changedCells: Set<Cell>
            get() = after.keys
    }

    companion object {
        /** Builds a diff from the desired end state, dropping cells whose colour does not change. */
        fun change(grid: VoxelGrid, target: Map<Cell, Int?>): CellsChange {
            val before = HashMap<Cell, Int?>()
            val after = HashMap<Cell, Int?>()
            for ((cell, color) in target) {
                if (!grid.inBounds(cell)) continue
                val current = grid[cell]
                if (current == color) continue
                before[cell] = current
                after[cell] = color
            }
            return CellsChange(before, after)
        }

        fun add(grid: VoxelGrid, cell: Cell, color: Int): CellsChange? =
            if (!grid.inBounds(cell) || cell in grid) null else change(grid, mapOf(cell to color))

        fun erase(grid: VoxelGrid, cell: Cell): CellsChange? =
            if (cell !in grid) null else change(grid, mapOf(cell to null))

        fun paint(grid: VoxelGrid, cell: Cell, color: Int): CellsChange? {
            val current = grid[cell] ?: return null
            return if (current == color) null else change(grid, mapOf(cell to color))
        }
    }
}

/** Undo / redo stacks with a fixed depth; the oldest step is dropped when the cap is exceeded. */
class History(private val capacity: Int = DEFAULT_CAPACITY) {
    private val undoStack = ArrayDeque<EditCommand>()
    private val redoStack = ArrayDeque<EditCommand>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val undoDepth: Int
        get() = undoStack.size

    /** Applies [command] and records it. A new step after an undo discards the redo stack. */
    fun commit(grid: VoxelGrid, command: EditCommand): VoxelGrid {
        if (command.isEmpty) return grid
        redoStack.clear()
        undoStack.addLast(command)
        while (undoStack.size > capacity) undoStack.removeFirst()
        return command.apply(grid)
    }

    fun undo(grid: VoxelGrid): VoxelGrid {
        val command = undoStack.removeLastOrNull() ?: return grid
        redoStack.addLast(command)
        return command.undo(grid)
    }

    fun redo(grid: VoxelGrid): VoxelGrid {
        val command = redoStack.removeLastOrNull() ?: return grid
        undoStack.addLast(command)
        return command.apply(grid)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    companion object {
        const val DEFAULT_CAPACITY = 100
    }
}
