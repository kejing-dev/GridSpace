package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.ui.common.ActionIcon
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.common.signed
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.StepKey
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/** What the selection panel shows; one State keeps the Augment content lambda stable. */
data class SelectionPanelState(
    val count: Int,
    /** Accumulated nudge plus the live drag-move delta (spec R4 O.2.3). */
    val translation: Cell,
    val blockedStep: StepKey?,
    /** Steppers are inert while a drag-move is in progress (spec R4 O.2.4). */
    val dragging: Boolean,
    /** R6: the live drag-move text ("ΔX +2 · ΔY 0 · ΔZ −1 · …") shown on the last line instead of the hint. */
    val liveHint: String? = null,
)

/**
 * Spec R4 S.3 (design dp, rendered through EditorScaled 1.5): 400 wide, padding 24; steppers 84;
 * 24 + 72 + 12 + 88 + 20 + 22 + 12 + 3 x 84 + 2 x 12 + 12 + 22 + 24 = 584 (600 x 876 Volume-dp).
 */
object SelectionSizes {
    val Width = 400.dp
    val Padding = 24.dp
    val HeaderHeight = 72.dp
    val TileWidth = 112.dp
    val TileHeight = 88.dp
    val TileGap = 8.dp
    val AxisWidth = 32.dp
    val StepButton = 84.dp
    val ValueWidth = 84.dp
    val StepGap = 12.dp
    val CancelMinWidth = 96.dp
}

/**
 * Selection Augment (design spec E.4, R4 R.2): count, rotate / duplicate / delete, X Y Z steppers
 * whose values follow a drag-move live, cancel, and the drag hint on the last line.
 */
@Composable
fun SelectionPanel(state: State<SelectionPanelState>, onEvent: (EditorEvent) -> Unit) {
    val s = state.value
    Column(modifier = Modifier.width(SelectionSizes.Width).padding(SelectionSizes.Padding)) {
        Row(modifier = Modifier.fillMaxWidth().height(SelectionSizes.HeaderHeight), verticalAlignment = Alignment.CenterVertically) {
            Text(str(R.string.selected_count, s.count), style = GridType.label, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onEvent(EditorEvent.SelectionClear) },
                size = ButtonDefaults.buttonSize(minWidth = SelectionSizes.CancelMinWidth, height = GridSizes.Control, minHeight = GridSizes.Control, maxHeight = GridSizes.Control),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = PicoTheme.colorScheme.labelSecondary),
                contentPadding = GridSizes.TightButtonPadding,
            ) {
                Text(str(R.string.cancel), style = GridType.label, maxLines = 1)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(SelectionSizes.TileGap)) {
            ActionTile(R.drawable.ic_rotate, str(R.string.rotate_90), enabled = !s.dragging) { onEvent(EditorEvent.SelectionRotate) }
            ActionTile(R.drawable.ic_copy, str(R.string.duplicate), enabled = !s.dragging) { onEvent(EditorEvent.SelectionDuplicate) }
            // Delete is the one destructive action: coral icon and text on the ordinary container.
            ActionTile(R.drawable.ic_trash, str(R.string.delete), enabled = !s.dragging, tint = GridColors.Coral) { onEvent(EditorEvent.SelectionDelete) }
        }
        Spacer(Modifier.height(20.dp))
        Text(str(R.string.grid_move), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(SelectionSizes.StepGap)) {
            StepperRow(str(R.string.axis_x), s.translation.x, s.blockedStep?.takeIf { it.axis == Axis.X }?.delta, s.dragging) { onEvent(EditorEvent.SelectionStep(Axis.X, it)) }
            StepperRow(str(R.string.axis_y), s.translation.y, s.blockedStep?.takeIf { it.axis == Axis.Y }?.delta, s.dragging) { onEvent(EditorEvent.SelectionStep(Axis.Y, it)) }
            StepperRow(str(R.string.axis_z), s.translation.z, s.blockedStep?.takeIf { it.axis == Axis.Z }?.delta, s.dragging) { onEvent(EditorEvent.SelectionStep(Axis.Z, it)) }
        }
        Spacer(Modifier.height(24.dp))
        // Fixed single line: the drag hint, or the live Δ text while a drag-move is in progress (R6).
        Text(
            s.liveHint ?: str(R.string.selection_drag_hint),
            style = GridType.caption,
            color = if (s.liveHint != null) PicoTheme.colorScheme.labelPrimary else PicoTheme.colorScheme.labelSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActionTile(icon: Int, label: String, enabled: Boolean, tint: Color? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        size =
            ButtonDefaults.buttonSize(
                width = SelectionSizes.TileWidth, height = SelectionSizes.TileHeight,
                minWidth = SelectionSizes.TileWidth, maxWidth = SelectionSizes.TileWidth,
                minHeight = SelectionSizes.TileHeight, maxHeight = SelectionSizes.TileHeight,
            ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tint != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(GridSizes.Icon), tint = tint)
            else Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(GridSizes.Icon))
            if (tint != null) Text(label, style = GridType.captionMedium, color = tint, maxLines = 1)
            else Text(label, style = GridType.captionMedium, maxLines = 1)
        }
    }
}

/**
 * The SDK NumberField is a text field with a keyboard; a pair of 84 dp IconButtons around the
 * value is the step-only control the spec allows instead (spec S.2).
 */
@Composable
private fun StepperRow(axis: String, value: Int, blockedDelta: Int?, dragging: Boolean, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SelectionSizes.StepGap)) {
        Text(axis, style = GridType.bodyMedium, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.width(SelectionSizes.AxisWidth))
        // An out-of-bounds step disables that button for 100 ms (spec E.4).
        ActionIcon(R.drawable.ic_minus, str(R.string.step_minus), enabled = !dragging && blockedDelta != -1, size = SelectionSizes.StepButton, onClick = { onStep(-1) })
        Box(modifier = Modifier.width(SelectionSizes.ValueWidth), contentAlignment = Alignment.Center) {
            // U+2212 minus via signed(), as on the drag-move label; no "+" on an accumulated value (spec S.8, QA N4-07).
            Text(signed(value, plus = false), style = GridType.body, color = PicoTheme.colorScheme.labelPrimary, textAlign = TextAlign.Center, maxLines = 1)
        }
        ActionIcon(R.drawable.ic_plus, str(R.string.step_plus), enabled = !dragging && blockedDelta != 1, size = SelectionSizes.StepButton, onClick = { onStep(1) })
    }
}
