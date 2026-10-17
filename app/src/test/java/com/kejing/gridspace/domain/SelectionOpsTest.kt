package com.kejing.gridspace.domain

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.SelectionOps
import com.kejing.gridspace.domain.usecase.SelectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionOpsTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    private fun row(y: Int = 0, z: Int = 5, xs: IntRange = 3..7): Set<Cell> = xs.map { Cell(it, y, z) }.toSet()

    @Test
    fun rotateTurnsARowAlongXIntoARowAlongZAroundItsCentre() {
        val selection = row()
        val grid = VoxelGrid(16, selection.associateWith { red })
        val result = SelectionOps.rotate90(grid, selection) as SelectionResult.Done
        val rotated = result.command.apply(grid)
        assertEquals((3..7).map { Cell(5, 0, it) }.toSet(), result.selection)
        assertEquals(result.selection, rotated.cells.keys)
        assertEquals(Cell(5, 0, 5), SelectionOps.center(result.selection))
        assertEquals(grid, result.command.undo(rotated))
    }

    @Test
    fun rotateOutOfGridIsRefused() {
        val selection = row(z = 1, xs = 0..4)
        val grid = VoxelGrid(16, selection.associateWith { red })
        assertEquals(SelectionResult.OutOfBounds, SelectionOps.rotate90(grid, selection))
    }

    @Test
    fun rotateOverwritesCubesOutsideTheSelection() {
        val selection = row()
        val cells = selection.associateWith { red } + (Cell(5, 0, 3) to blue)
        val grid = VoxelGrid(16, cells)
        val result = SelectionOps.rotate90(grid, selection) as SelectionResult.Done
        val rotated = result.command.apply(grid)
        assertEquals(red, rotated[Cell(5, 0, 3)])
        assertEquals(5, rotated.count)
    }

    @Test
    fun duplicateGoesOneColumnRightAndBecomesTheSelection() {
        val selection = setOf(Cell(2, 0, 2), Cell(2, 1, 2))
        val grid = VoxelGrid(16, selection.associateWith { red })
        val result = SelectionOps.duplicate(grid, selection) as SelectionResult.Done
        val copied = result.command.apply(grid)
        assertEquals(setOf(Cell(3, 0, 2), Cell(3, 1, 2)), result.selection)
        assertEquals(4, copied.count)
    }

    @Test
    fun duplicateFallsBackToSelectionWidthThenReportsNoRoom() {
        // A gapped selection (width 3): the +1 column is blocked at (3,0,2), the +W column is free.
        val selection = setOf(Cell(2, 0, 2), Cell(4, 0, 2))
        val blocked = VoxelGrid(16, selection.associateWith { red } + (Cell(3, 0, 2) to blue))
        val result = SelectionOps.duplicate(blocked, selection) as SelectionResult.Done
        assertEquals(setOf(Cell(5, 0, 2), Cell(7, 0, 2)), result.selection)
        val fullyBlocked = VoxelGrid(16, blocked.cells + (Cell(5, 0, 2) to blue))
        assertEquals(SelectionResult.NoRoom, SelectionOps.duplicate(fullyBlocked, selection))
    }

    @Test
    fun deleteClearsCubesAndSelection() {
        val selection = row()
        val grid = VoxelGrid(16, selection.associateWith { red } + (Cell(0, 0, 0) to blue))
        val result = SelectionOps.delete(grid, selection) as SelectionResult.Done
        assertEquals(1, result.command.apply(grid).count)
        assertTrue(result.selection.isEmpty())
    }

    @Test
    fun translateMovesWholeSelectionAndStopsAtTheEdge() {
        val selection = setOf(Cell(14, 0, 0), Cell(15, 0, 0))
        val grid = VoxelGrid(16, selection.associateWith { red })
        val moved = SelectionOps.translate(grid, selection, Cell(0, 1, 0)) as SelectionResult.Done
        assertEquals(setOf(Cell(14, 1, 0), Cell(15, 1, 0)), moved.selection)
        assertEquals(SelectionResult.OutOfBounds, SelectionOps.translate(grid, selection, Cell(1, 0, 0)))
    }
}

/** Spec R4 O.2.4 / K4.24: drag-move commit semantics. */
class SelectionMoveTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun translateOverwritesNonSelectedCubesInTheSameStep() {
        val selection = setOf(Cell(3, 0, 5), Cell(4, 0, 5))
        val grid = VoxelGrid(16, selection.associateWith { red } + (Cell(5, 0, 5) to blue))
        val delta = Cell(1, 0, 0)
        assertEquals(setOf(Cell(5, 0, 5)), SelectionOps.overwritten(grid, selection, SelectionOps.targets(selection, delta)))
        val result = SelectionOps.translate(grid, selection, delta) as SelectionResult.Done
        val moved = result.command.apply(grid)
        assertEquals(red, moved[Cell(5, 0, 5)])
        assertEquals(2, moved.count)
        assertEquals(setOf(Cell(4, 0, 5), Cell(5, 0, 5)), result.selection)
        // One undo restores the blue cube and the originals.
        assertEquals(grid, result.command.undo(moved))
    }

    @Test
    fun zeroDeltaProducesNoHistoryStep() {
        val selection = setOf(Cell(3, 0, 5))
        val grid = VoxelGrid(16, selection.associateWith { red })
        val result = SelectionOps.translate(grid, selection, Cell.ZERO) as SelectionResult.Done
        assertTrue(result.command.isEmpty)
    }

    @Test
    fun clampKeepsTheBoundingBoxInsideTheGrid() {
        val selection = setOf(Cell(14, 0, 0), Cell(15, 2, 0))
        assertEquals(Cell(0, -0, 0), SelectionOps.clampDelta(selection, Cell(3, 0, 0), 16))
        assertEquals(Cell(-14, 13, 15), SelectionOps.clampDelta(selection, Cell(-20, 20, 20), 16))
    }
}
