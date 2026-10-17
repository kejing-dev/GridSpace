package com.kejing.gridspace.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * App-owned colours from the design spec (I.1). Text, dividers and control states keep the native
 * SpatialUI colour roles; these tint only what the spec names.
 */
object GridColors {
    /** Current mode, primary action, success, placeable ghost. */
    val Mint = Color(0xFFA5EDCC)
    /** Text and icons on Mint. */
    val OnMint = Color(0xFF1C2A26)
    /** Error, delete, occupied cell, erase preview. */
    val Coral = Color(0xFFF3BC9C)
    /** Selection outlines, current-slot ring, matching swatch ring. */
    val SelectionWhite = Color(0xFFFFFFFF)
    /** Base plate fill (3D). */
    val BaseInk = Color(0xFF2D383B)
    /** Base plate grid lines (3D). */
    val GridLine = Color(0xFF688681)
    /** Placeholder tile colour for an empty project in the recent list. */
    val EmptyProject = Color(0xFF688681)
    /** Saving-in-progress dot in the title augment. */
    val SavingGrey = Color(0xFFAAB9B8)
}
