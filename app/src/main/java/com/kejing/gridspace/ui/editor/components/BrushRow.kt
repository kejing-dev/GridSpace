package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.usecase.BrushSize
import com.kejing.gridspace.ui.common.ActionIcon
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.VerticalDivider

/** What the brush row shows; passed as one State so the Augment content lambda stays stable. */
data class BrushRowState(val size: BrushSize, val gridSize: Int)

/**
 * Spec R8 T.1 as revised in R9 (T.7; design dp, rendered unscaled on the front plane): three
 * clearly separated groups "宽 X [−] n [+]" | "高 Y …" | "深 Z …", each 300 wide (label 88 · 12 ·
 * [−] 72 · value 56 · [+] 72), divided by 24 + VerticalDivider 1 x 48 + 24, padding 32 each side:
 * 32 + 3 x 300 + 2 x 49 + 32 = 1062 x 80.
 */
object BrushRowSizes {
    val Width = 1062.dp
    val Height = 80.dp
    val SidePad = 32.dp
    val VerticalPad = 4.dp
    val LabelWidth = 88.dp
    val Gap = 12.dp
    val ValueWidth = 56.dp
    val DividerPad = 24.dp
    val DividerHeight = 48.dp
}

/**
 * The one brush of R8: W x H x D steppers, each 1..min(16, grid), always visible in ADD / ERASE
 * (the row is hidden in PAINT / SELECT). A step is one tap (U2), no long press (U1); the buttons
 * at a limit are disabled with the system alpha.
 */
@Composable
fun BrushRow(state: State<BrushRowState>, onEvent: (EditorEvent) -> Unit) {
    val s = state.value
    val max = BrushSize.MAX.coerceAtMost(s.gridSize)
    val label = str(R.string.brush_size)
    Row(
        modifier =
            Modifier.width(BrushRowSizes.Width)
                .height(BrushRowSizes.Height)
                .padding(horizontal = BrushRowSizes.SidePad, vertical = BrushRowSizes.VerticalPad)
                .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Axis.X to R.string.size_axis_x, Axis.Y to R.string.size_axis_y, Axis.Z to R.string.size_axis_z).forEachIndexed { index, (axis, axisLabel) ->
            if (index > 0) {
                Spacer(Modifier.width(BrushRowSizes.DividerPad))
                VerticalDivider(modifier = Modifier.size(width = 1.dp, height = BrushRowSizes.DividerHeight))
                Spacer(Modifier.width(BrushRowSizes.DividerPad))
            }
            SizeStepper(axis = str(axisLabel), value = s.size[axis], max = max, onStep = { onEvent(EditorEvent.SizeStep(axis, it)) })
        }
    }
}

@Composable
private fun SizeStepper(axis: String, value: Int, max: Int, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(axis, style = GridType.label, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1, modifier = Modifier.width(BrushRowSizes.LabelWidth))
        Spacer(Modifier.width(BrushRowSizes.Gap))
        ActionIcon(R.drawable.ic_minus, str(R.string.step_minus), enabled = value > BrushSize.MIN, onClick = { onStep(-1) })
        Box(modifier = Modifier.width(BrushRowSizes.ValueWidth), contentAlignment = Alignment.Center) {
            Text(value.toString(), style = GridType.label, color = PicoTheme.colorScheme.labelPrimary, textAlign = TextAlign.Center, maxLines = 1)
        }
        ActionIcon(R.drawable.ic_plus, str(R.string.step_plus), enabled = value < max, onClick = { onStep(1) })
    }
}
