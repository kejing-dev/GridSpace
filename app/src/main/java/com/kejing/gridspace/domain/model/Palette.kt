package com.kejing.gridspace.domain.model

/** The three preset swatches of the colour picker (design spec D.3), 24 colours each, row-major. */
enum class PalettePreset(val colors: List<Int>) {
    SOFT(
        hex(
            "#f3bc9c", "#f7d3b5", "#f5e3c3", "#f3f5ed", "#dfddd0", "#cfd6c2", "#afd8bb", "#a5edcc",
            "#8fd4c6", "#9fd0e2", "#b7c5ea", "#c9b8e8", "#e0b4d6", "#f0b3c0", "#e5a6a0", "#ce9278",
            "#af755e", "#8a6a5a", "#7dad95", "#568776", "#3a5653", "#688681", "#aab9b8", "#2d383b",
        )
    ),
    VIVID(
        hex(
            "#ff3b30", "#ff6b35", "#ff9500", "#ffcc00", "#d4e157", "#7ed321", "#34c759", "#00c7a0",
            "#00bcd4", "#2aa7ff", "#007aff", "#4f5bd5", "#7c4dff", "#a855f7", "#d946ef", "#ff2d92",
            "#ff6b81", "#ffa07a", "#ffd166", "#8bd3dd", "#5e8bff", "#9b6bff", "#c8a2ff", "#ffffff",
        )
    ),
    EARTH(
        hex(
            "#f5efe6", "#e8dcc8", "#d9c5a0", "#c9ad7c", "#b8905a", "#a67643", "#8c5a2b", "#6e4423",
            "#4f3019", "#c7b299", "#a89a7b", "#8b8b6c", "#6b7351", "#4f5a3a", "#8fa88a", "#5f7f6b",
            "#3f5a4e", "#b56a4e", "#9c4f3a", "#d8a47f", "#e7c6a5", "#7e6a5a", "#5a4a40", "#3a3330",
        )
    ),
}

object Palette {
    const val SLOT_COUNT = 3
    /** Swatch grid of the colour rail (spec R4 R.2): 4 columns x 6 rows, row-major. */
    const val PALETTE_COLUMNS = 4
    const val PALETTE_ROWS = 6

    /** New projects start with white (current), mint and light coral (spec R4 R.3). */
    val DEFAULT_SLOTS: List<Int> = hex("#ffffff", "#a5edcc", "#f3bc9c")

    /** The sample pavilion keeps its v1 slots: light coral, mint, lime-white. */
    val SAMPLE_SLOTS: List<Int> = hex("#f3bc9c", "#a5edcc", "#f3f5ed")

    /** Reference colours used by the sample model and the 3D feedback. */
    const val CORAL = 0xFFF3BC9C.toInt()
    const val MINT = 0xFFA5EDCC.toInt()
    const val LIME = 0xFFF3F5ED.toInt()
}

private fun hex(vararg values: String): List<Int> = values.map { ColorMath.parseHex(it) }
