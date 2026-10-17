package com.kejing.gridspace.domain.model

/** One editable model with its three quick colour slots. */
data class Project(
    val id: String,
    val name: String,
    val gridSize: Int,
    val voxels: VoxelGrid,
    val slots: List<Int> = Palette.DEFAULT_SLOTS,
    val activeSlot: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val activeColor: Int
        get() = slots[activeSlot.coerceIn(0, slots.lastIndex)]
}

/** Row of the recent-projects list; read from the index without loading the voxels. */
data class ProjectSummary(
    val id: String,
    val name: String,
    val gridSize: Int,
    val voxelCount: Int,
    /** Most frequent colour, or null for an empty model. */
    val dominantColor: Int?,
    val updatedAt: Long,
    /** True when the project file could not be parsed; the row stays so the user can delete it. */
    val corrupt: Boolean = false,
)

enum class EditMode { ADD, ERASE, PAINT, SELECT }

/** VOXEL / BAR / BOX are offered in Add and Erase; FACE is reserved for v1.1 and never shown. */
/** How the last save ended; shown as the coloured dot in the title augment. */
enum class SaveState { SAVING, SAVED, FAILED }
