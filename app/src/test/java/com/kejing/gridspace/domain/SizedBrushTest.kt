package com.kejing.gridspace.domain

import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.BoxRegion
import com.kejing.gridspace.domain.usecase.BrushSize
import com.kejing.gridspace.domain.usecase.SizedBrush
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** R8 sized brush (spec T): anchor, per-axis flip, clamp, skip on add, remove on erase. */
class SizedBrushTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun unitSizeIsTheTargetCell() {
        assertEquals(listOf(Cell(7, 3, 7)), SizedBrush.cells(Cell(7, 3, 7), BrushSize.UNIT, 16))
        assertTrue(BrushSize.UNIT.isUnit)
        assertEquals(6, BrushSize(3, 2, 1).count)
    }

    @Test
    fun blockExtendsAlongPlusAxesFromTheAnchor() {
        val region = SizedBrush.region(Cell(2, 0, 5), BrushSize(3, 2, 1), 16)
        assertEquals(BoxRegion(Cell(2, 0, 5), Cell(4, 1, 5)), region)
        assertEquals(6, region.cellCount)
    }

    @Test
    fun eachAxisFlipsOnItsOwnWhenPlusLeavesTheGrid() {
        // x flips (14 + 3 > 15), y does not (0 + 1 fits), z flips (15 + 1 > 15).
        val region = SizedBrush.region(Cell(14, 0, 15), BrushSize(4, 2, 2), 16)
        assertEquals(BoxRegion(Cell(11, 0, 14), Cell(14, 1, 15)), region)
        assertEquals(13..15, SizedBrush.span(15, 3, 16))
    }

    @Test
    fun whenNeitherDirectionFitsTheLongerSideIsKeptAndClipped() {
        // Grid 4, extent 5 from 1: + has 3 cells (1..3), - has 2 (0..1) -> plus side, clipped.
        assertEquals(1..3, SizedBrush.span(1, 5, 4))
        // From 2: + has 2 (2..3), - has 3 (0..2) -> minus side.
        assertEquals(0..2, SizedBrush.span(2, 5, 4))
        // Always contains the anchor.
        assertTrue(Cell(2, 2, 2) in SizedBrush.cells(Cell(2, 2, 2), BrushSize(5, 5, 5), 4))
    }

    @Test
    fun sizeIsClampedToTheGridAndTheLimits() {
        assertEquals(BrushSize(8, 1, 8), BrushSize(20, 0, 9).clamped(8))
        assertEquals(BrushSize(16, 16, 1), BrushSize(16, 99, 1).clamped(32))
        assertEquals(BrushSize(3, 2, 1), BrushSize.UNIT.with(Axis.X, 3).with(Axis.Y, 2))
        assertEquals(2, BrushSize(3, 2, 1)[Axis.Y])
    }

    @Test
    fun addingSkipsOccupiedCellsAndAFullBlockIsEmpty() {
        val grid = VoxelGrid(16, mapOf(Cell(1, 0, 1) to blue))
        val command = SizedBrush.fill(grid, Cell(0, 0, 0), BrushSize(3, 1, 3), red)
        assertEquals(8, command.changedCells.size)
        assertTrue(Cell(1, 0, 1) !in command.changedCells)
        assertEquals(blue, command.apply(grid)[Cell(1, 0, 1)])
        val full = VoxelGrid(16, (0..2).associate { Cell(0, it, 0) to red })
        assertTrue(SizedBrush.fill(full, Cell(0, 0, 0), BrushSize(1, 3, 1), red).isEmpty)
    }

    @Test
    fun erasingOnlyTouchesExistingCubesAsOneStep() {
        val grid = VoxelGrid(16, mapOf(Cell(5, 0, 1) to red, Cell(5, 0, 3) to red, Cell(9, 0, 9) to blue))
        val command = SizedBrush.erase(grid, Cell(5, 0, 1), BrushSize(1, 1, 3))
        assertEquals(setOf(Cell(5, 0, 1), Cell(5, 0, 3)), command.changedCells)
        assertEquals(1, command.apply(grid).count)
    }

    @Test
    fun regionIsNormalisedAndClipped() {
        val region = BoxRegion.between(Cell(5, 3, -2), Cell(1, 20, 4), gridSize = 16)
        assertEquals(Cell(1, 3, 0), region.min)
        assertEquals(Cell(5, 15, 4), region.max)
        assertEquals(5 * 13 * 5, region.cellCount)
    }
}
