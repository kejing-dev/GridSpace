package com.kejing.gridspace.domain

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.TargetCellResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetCellResolverTest {
    private val red = 0xFFFF0000.toInt()

    @Test
    fun adjacentCellFollowsTheDominantAxisOfTheHitPoint() {
        val hit = Cell(4, 2, 4)
        assertEquals(Cell(4, 3, 4), TargetCellResolver.adjacentCell(hit, Vec3(4.5f, 3.0f, 4.5f)))
        assertEquals(Cell(4, 1, 4), TargetCellResolver.adjacentCell(hit, Vec3(4.5f, 2.0f, 4.5f)))
        assertEquals(Cell(5, 2, 4), TargetCellResolver.adjacentCell(hit, Vec3(5.0f, 2.5f, 4.5f)))
        assertEquals(Cell(3, 2, 4), TargetCellResolver.adjacentCell(hit, Vec3(4.0f, 2.5f, 4.5f)))
        assertEquals(Cell(4, 2, 5), TargetCellResolver.adjacentCell(hit, Vec3(4.5f, 2.5f, 5.0f)))
        assertEquals(Cell(4, 2, 3), TargetCellResolver.adjacentCell(hit, Vec3(4.5f, 2.5f, 4.0f)))
    }

    @Test
    fun adjacentCellDefaultsToTopWhenThePointIsUnusable() {
        assertEquals(Cell(4, 3, 4), TargetCellResolver.adjacentCell(Cell(4, 2, 4), null))
        assertEquals(Cell(4, 3, 4), TargetCellResolver.adjacentCell(Cell(4, 2, 4), Vec3(40f, 2f, 4f)))
    }

    @Test
    fun baseCellFloorsToTheGroundRow() {
        assertEquals(Cell(3, 0, 7), TargetCellResolver.baseCell(Vec3(3.9f, 0f, 7.1f), 16))
        assertNull(TargetCellResolver.baseCell(Vec3(-0.5f, 0f, 7.1f), 16))
        assertNull(TargetCellResolver.baseCell(Vec3(16f, 0f, 7.1f), 16))
    }

    @Test
    fun marchFindsTheFirstCubeAndTheCellBeforeIt() {
        val grid = VoxelGrid(16, mapOf(Cell(8, 0, 8) to red))
        // Straight down onto the cube from above.
        val down = TargetCellResolver.march(Vec3(8.5f, 20f, 8.5f), Vec3(0f, -1f, 0f), grid)
        assertEquals(Cell(8, 0, 8), down.hit)
        assertEquals(Cell(8, 1, 8), down.before)
        // Along x from the side.
        val side = TargetCellResolver.march(Vec3(-3f, 0.5f, 8.5f), Vec3(1f, 0f, 0f), grid)
        assertEquals(Cell(8, 0, 8), side.hit)
        assertEquals(Cell(7, 0, 8), side.before)
    }

    @Test
    fun marchThroughEmptySpaceOntoThePlateReturnsTheGroundCell() {
        val grid = VoxelGrid(16)
        val result = TargetCellResolver.march(Vec3(2.5f, 10f, 2.5f), Vec3(0f, -1f, 0f), grid)
        assertNull(result.hit)
        assertEquals(Cell(2, 0, 2), result.before)
    }

    @Test
    fun marchMissingTheGridReturnsNothing() {
        val grid = VoxelGrid(16, mapOf(Cell(8, 0, 8) to red))
        val miss = TargetCellResolver.march(Vec3(-5f, 30f, -5f), Vec3(0f, 0f, 1f), grid)
        assertNull(miss.hit)
        assertNull(miss.before)
    }

    @Test
    fun cellAtRejectsPointsOutsideTheGrid() {
        assertEquals(Cell(15, 15, 15), TargetCellResolver.cellAt(Vec3(15.9f, 15.9f, 15.9f), 16))
        assertNull(TargetCellResolver.cellAt(Vec3(16f, 0f, 0f), 16))
    }
}
