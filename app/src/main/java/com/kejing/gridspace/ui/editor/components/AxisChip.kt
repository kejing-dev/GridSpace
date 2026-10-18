package com.kejing.gridspace.ui.editor.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.pico.spatial.ui.design.Text

/** The axis letters reuse the stepper / panel axis strings (spec T.9). */
@StringRes
fun Axis.labelRes(): Int =
    when (this) {
        Axis.X -> R.string.axis_x
        Axis.Y -> R.string.axis_y
        Axis.Z -> R.string.axis_z
    }

/** R11 axis letter chip: a 56 dp disc in the guide colour with the letter in `label` 20 Medium. */
@Composable
fun AxisChip(letter: String) {
    Box(
        modifier = Modifier.size(AXIS_CHIP_DP).background(GridColors.AxisGuide.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = GridType.label, color = GridColors.BaseInk, maxLines = 1)
    }
}

val AXIS_CHIP_DP = 56.dp
