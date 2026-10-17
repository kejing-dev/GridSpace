package com.kejing.gridspace.domain

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.SampleProject
import com.kejing.gridspace.domain.usecase.VoxWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoxWriterTest {
    private fun ascii(bytes: ByteArray, at: Int) = String(bytes, at, 4, Charsets.US_ASCII)

    private fun int32(bytes: ByteArray, at: Int) = ByteBuffer.wrap(bytes, at, 4).order(ByteOrder.LITTLE_ENDIAN).int

    @Test
    fun headerAndChunkLayoutMatchTheSpec() {
        val grid = VoxelGrid(16, mapOf(Cell(1, 2, 3) to ColorMath.parseHex("#f3bc9c")))
        val bytes = VoxWriter.write(grid)
        assertEquals("VOX ", ascii(bytes, 0))
        assertEquals(150, int32(bytes, 4))
        assertEquals("MAIN", ascii(bytes, 8))
        assertEquals(0, int32(bytes, 12))
        assertEquals(bytes.size - 20, int32(bytes, 16))

        var at = 20
        assertEquals("SIZE", ascii(bytes, at))
        assertEquals(12, int32(bytes, at + 4))
        assertEquals(16, int32(bytes, at + 12))
        assertEquals(16, int32(bytes, at + 16))
        assertEquals(16, int32(bytes, at + 20))
        at += 12 + 12

        assertEquals("XYZI", ascii(bytes, at))
        assertEquals(1, int32(bytes, at + 12))
        // app (x, y_up, z) -> vox (x, z, y_up), palette index 1-based.
        assertEquals(1, bytes[at + 16].toInt())
        assertEquals(3, bytes[at + 17].toInt())
        assertEquals(2, bytes[at + 18].toInt())
        assertEquals(1, bytes[at + 19].toInt())
        at += 12 + 4 + 4

        assertEquals("RGBA", ascii(bytes, at))
        assertEquals(1024, int32(bytes, at + 4))
        assertEquals(0xF3, bytes[at + 12].toInt() and 0xFF)
        assertEquals(0xBC, bytes[at + 13].toInt() and 0xFF)
        assertEquals(0x9C, bytes[at + 14].toInt() and 0xFF)
        assertEquals(0xFF, bytes[at + 15].toInt() and 0xFF)
        assertEquals(0, bytes[at + 12 + 255 * 4].toInt())
        assertEquals(bytes.size, at + 12 + 1024)
    }

    @Test
    fun emptyModelIsAllowed() {
        val bytes = VoxWriter.write(VoxelGrid(24))
        val xyzi = 20 + 24
        assertEquals("XYZI", ascii(bytes, xyzi))
        assertEquals(0, int32(bytes, xyzi + 12))
    }

    @Test
    fun sampleModelKeepsItsCountAndThreeColours() {
        val grid = SampleProject.grid()
        val bytes = VoxWriter.write(grid)
        assertEquals(grid.count, int32(bytes, 20 + 24 + 12))
        assertEquals(3, VoxWriter.buildPalette(grid).size)
        assertFalse(VoxWriter.exceedsPalette(grid))
    }

    @Test
    fun moreThan255ColoursMergeToTheNearestPaletteEntry() {
        val cells = HashMap<Cell, Int>()
        var i = 0
        for (x in 0 until 16) for (z in 0 until 16) for (y in 0 until 2) {
            if (i >= 300) break
            cells[Cell(x, y, z)] = ColorMath.rgb(i % 256, (i * 7) % 256, (i * 13) % 256)
            i++
        }
        val grid = VoxelGrid(16, cells)
        assertTrue(VoxWriter.exceedsPalette(grid))
        val palette = VoxWriter.buildPalette(grid)
        assertEquals(255, palette.size)
        for (color in grid.distinctColors()) assertTrue(palette.indexOf(color) in 1..255)
        val bytes = VoxWriter.write(grid)
        val xyzi = 20 + 24
        val n = int32(bytes, xyzi + 12)
        for (k in 0 until n) {
            val index = bytes[xyzi + 16 + k * 4 + 3].toInt() and 0xFF
            assertTrue(index in 1..255)
        }
    }
}
