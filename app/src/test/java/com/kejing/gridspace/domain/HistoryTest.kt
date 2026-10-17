package com.kejing.gridspace.domain

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.EditCommand
import com.kejing.gridspace.domain.usecase.History
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun addEraseAndPaintRoundTripThroughUndoRedo() {
        val history = History()
        var grid = VoxelGrid(16)
        val cell = Cell(1, 2, 3)
        grid = history.commit(grid, EditCommand.add(grid, cell, red)!!)
        assertEquals(red, grid[cell])
        grid = history.commit(grid, EditCommand.paint(grid, cell, blue)!!)
        assertEquals(blue, grid[cell])
        grid = history.commit(grid, EditCommand.erase(grid, cell)!!)
        assertFalse(cell in grid)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        grid = history.undo(grid)
        assertEquals(blue, grid[cell])
        grid = history.undo(grid)
        assertEquals(red, grid[cell])
        grid = history.undo(grid)
        assertTrue(grid.isEmpty())
        assertFalse(history.canUndo)

        grid = history.redo(grid)
        assertEquals(red, grid[cell])
        grid = history.redo(grid)
        grid = history.redo(grid)
        assertTrue(grid.isEmpty())
        assertFalse(history.canRedo)
    }

    @Test
    fun noOpCommandsAreNotRecorded() {
        val grid = VoxelGrid(16, mapOf(Cell(0, 0, 0) to red))
        assertNull(EditCommand.add(grid, Cell(0, 0, 0), blue))
        assertNull(EditCommand.paint(grid, Cell(0, 0, 0), red))
        assertNull(EditCommand.erase(grid, Cell(5, 5, 5)))
        assertNull(EditCommand.add(grid, Cell(16, 0, 0), red))
        val history = History()
        history.commit(grid, EditCommand.change(grid, mapOf(Cell(0, 0, 0) to red)))
        assertFalse(history.canUndo)
    }

    @Test
    fun newCommitAfterUndoClearsRedo() {
        val history = History()
        var grid = VoxelGrid(16)
        grid = history.commit(grid, EditCommand.add(grid, Cell(0, 0, 0), red)!!)
        grid = history.undo(grid)
        assertTrue(history.canRedo)
        grid = history.commit(grid, EditCommand.add(grid, Cell(1, 0, 0), red)!!)
        assertFalse(history.canRedo)
        assertEquals(1, grid.count)
    }

    @Test
    fun depthIsCappedAtOneHundred() {
        val history = History()
        var grid = VoxelGrid(48)
        for (i in 0 until 120) grid = history.commit(grid, EditCommand.add(grid, Cell(i % 48, i / 48, 0), red)!!)
        assertEquals(History.DEFAULT_CAPACITY, history.undoDepth)
        repeat(200) { grid = history.undo(grid) }
        // The 20 oldest steps are gone for good, so 20 cubes survive every undo.
        assertEquals(20, grid.count)
    }
}
