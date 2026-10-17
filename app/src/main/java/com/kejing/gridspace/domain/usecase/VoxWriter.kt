package com.kejing.gridspace.domain.usecase

import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.VoxelGrid
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MagicaVoxel .vox encoder (design spec H.1): 'VOX ' + version 150, a MAIN chunk holding SIZE,
 * XYZI and RGBA. MagicaVoxel is Z-up, so app (x, y_up, z) becomes vox (x, z, y_up). Palette indices
 * are 1-based; a model with more than 255 colours maps extras to the nearest already-used colour.
 */
object VoxWriter {
    const val MAX_COLORS = 255

    fun write(grid: VoxelGrid): ByteArray {
        val palette = buildPalette(grid)
        val voxels = grid.cells.entries.sortedWith(compareBy({ it.key.y }, { it.key.z }, { it.key.x }))

        val size = chunk("SIZE", 12) { it.putInt(grid.size).putInt(grid.size).putInt(grid.size) }
        val xyzi =
            chunk("XYZI", 4 + voxels.size * 4) { buf ->
                buf.putInt(voxels.size)
                for ((cell, color) in voxels) {
                    buf.put(cell.x.toByte()).put(cell.z.toByte()).put(cell.y.toByte()).put(palette.indexOf(color).toByte())
                }
            }
        val rgba =
            chunk("RGBA", 256 * 4) { buf ->
                for (i in 0 until 256) {
                    val color = palette.colorAt(i + 1)
                    if (color == null) {
                        buf.put(0).put(0).put(0).put(0)
                    } else {
                        buf.put(ColorMath.red(color).toByte()).put(ColorMath.green(color).toByte())
                            .put(ColorMath.blue(color).toByte()).put(0xFF.toByte())
                    }
                }
            }
        val children = size + xyzi + rgba
        val out = ByteArrayOutputStream(20 + children.size)
        out.write("VOX ".toByteArray(Charsets.US_ASCII))
        out.write(le(150))
        out.write("MAIN".toByteArray(Charsets.US_ASCII))
        out.write(le(0))
        out.write(le(children.size))
        out.write(children)
        return out.toByteArray()
    }

    /** True when the export has to merge colours. */
    fun exceedsPalette(grid: VoxelGrid): Boolean = grid.distinctColors().size > MAX_COLORS

    class VoxPalette(private val colors: List<Int>, private val mapping: Map<Int, Int>) {
        /** 1-based palette index for a model colour. */
        fun indexOf(color: Int): Int = mapping.getValue(color)

        /** Colour stored at a 1-based palette index, or null when unused. */
        fun colorAt(index: Int): Int? = colors.getOrNull(index - 1)

        val size: Int
            get() = colors.size
    }

    fun buildPalette(grid: VoxelGrid): VoxPalette {
        // Colours take indices in order of first appearance (sorted traversal keeps it deterministic).
        val ordered = LinkedHashSet<Int>()
        for (entry in grid.cells.entries.sortedWith(compareBy({ it.key.y }, { it.key.z }, { it.key.x }))) {
            ordered.add(entry.value)
        }
        val colors = ordered.take(MAX_COLORS)
        val mapping = HashMap<Int, Int>()
        colors.forEachIndexed { i, c -> mapping[c] = i + 1 }
        for (extra in ordered.drop(MAX_COLORS)) {
            val nearest = colors.minBy { ColorMath.distanceSquared(it, extra) }
            mapping[extra] = mapping.getValue(nearest)
        }
        return VoxPalette(colors, mapping)
    }

    private fun chunk(id: String, contentSize: Int, fill: (ByteBuffer) -> Unit): ByteArray {
        val buf = ByteBuffer.allocate(12 + contentSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(id.toByteArray(Charsets.US_ASCII)).putInt(contentSize).putInt(0)
        fill(buf)
        return buf.array()
    }

    private fun le(value: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
