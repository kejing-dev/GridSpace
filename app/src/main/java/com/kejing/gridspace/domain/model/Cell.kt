package com.kejing.gridspace.domain.model

/** Integer grid coordinate. y is up; 0 <= x, y, z < N inside a grid of size N. */
data class Cell(val x: Int, val y: Int, val z: Int) {
    operator fun plus(other: Cell): Cell = Cell(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Cell): Cell = Cell(x - other.x, y - other.y, z - other.z)

    fun inBounds(size: Int): Boolean = x in 0 until size && y in 0 until size && z in 0 until size

    companion object {
        val UNIT_X = Cell(1, 0, 0)
        val UNIT_Y = Cell(0, 1, 0)
        val UNIT_Z = Cell(0, 0, 1)
        val ZERO = Cell(0, 0, 0)
    }
}

/** One coloured cube. [color] is ARGB with alpha always 0xFF. */
data class Voxel(val cell: Cell, val color: Int)

/** Which axis a selection is nudged along. */
enum class Axis { X, Y, Z;

    fun unit(step: Int): Cell =
        when (this) {
            X -> Cell(step, 0, 0)
            Y -> Cell(0, step, 0)
            Z -> Cell(0, 0, step)
        }
}

/** Float triple for scene maths; kept free of SDK types so the use cases stay unit-testable. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val l = length()
        return if (l < 1e-9f) this else this * (1f / l)
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}
