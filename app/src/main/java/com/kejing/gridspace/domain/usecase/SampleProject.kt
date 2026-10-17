package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.Palette
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.VoxelGrid

/**
 * The "garden pavilion" demo model (design spec L.4): a 10 x 10 lime floor, four coral pillars,
 * five lime beams across the top and a 3 x 2 x 3 mint block inside. About 128 visible cubes in 16^3.
 */
object SampleProject {
    const val ID = "sample-pavilion"

    fun build(name: String, now: Long): Project = Project(
        id = ID,
        name = name,
        gridSize = 16,
        voxels = grid(),
        slots = Palette.SAMPLE_SLOTS,
        activeSlot = 0,
        createdAt = now,
        updatedAt = now,
    )

    fun grid(): VoxelGrid {
        val cells = HashMap<Cell, Int>()
        fun box(x0: Int, y0: Int, z0: Int, w: Int, h: Int, d: Int, color: Int) {
            for (x in x0 until x0 + w) for (y in y0 until y0 + h) for (z in z0 until z0 + d) cells[Cell(x, y, z)] = color
        }
        box(3, 0, 3, 10, 1, 10, Palette.LIME)
        for ((px, pz) in listOf(3 to 3, 12 to 3, 3 to 12, 12 to 12)) box(px, 1, pz, 1, 4, 1, Palette.CORAL)
        for (i in 0 until 5) box(3, 5, 3 + i * 2, 10, 1, 1, Palette.LIME)
        box(6, 1, 6, 3, 2, 3, Palette.MINT)
        return VoxelGrid(16, cells)
    }
}
