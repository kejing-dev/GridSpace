package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.ui.common.ActionIcon
import com.kejing.gridspace.ui.common.PrimaryButton
import com.kejing.gridspace.ui.common.SecondaryButton
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.PickerState
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/**
 * Spec R4 S.5 (design dp, rendered through EditorScaled 1.5): 560 wide, padding 32, rows 24 apart;
 * 32 + 72 + 24 + 88 + 24 + 248 + 24 + 80 + 32 = 624 high (840 x 936 Volume-dp).
 */
object EditSizes {
    val Width = 560.dp
    val Height = 624.dp
    val Padding = 32.dp
    val RowGap = 24.dp
    val TitleRow = 72.dp
    val PreviewDisc = 88.dp
    val HexWidth = 112.dp
    val PickWidth = 220.dp
    val BarHeight = 88.dp
}

/** What the colour-edit card shows; one State keeps the Augment content lambda stable. */
data class ColorEditState(val picker: PickerState, val slotIndex: Int, val color: Int)

/**
 * The colour-edit card (R6): title, preview disc + hex, HSL sliders writing live into the current
 * slot, "Pick from cube" and Done. R4/R5 rendered it as a `SpatialPopup` anchored on the rail's
 * "Edit colour" button; the SDK popup has no depth parameter (`SpatialPopup(onDismissRequest,
 * modifier, positionProvider, cornerRadius, defaultMinWidth, defaultMinHeight, …)`, javap
 * design-6.1.9) and sat at the rail's depth where the model's near cubes poked through it, so
 * the card is now a window-level `Augment` on the Volume's front plane (`EditorScreen`), fixed
 * 560 x 624 design dp so the window is never rebuilt. While the eyedropper is active the card is
 * replaced by [EyedropperBar] (its own Augment).
 */
@Composable
fun ColorEditCard(state: State<ColorEditState>, onEvent: (EditorEvent) -> Unit) {
    val s = state.value
    Column(
        modifier = Modifier.width(EditSizes.Width).height(EditSizes.Height).padding(EditSizes.Padding),
        verticalArrangement = Arrangement.spacedBy(EditSizes.RowGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(EditSizes.TitleRow), verticalAlignment = Alignment.CenterVertically) {
            Text(str(R.string.edit_colour_slot, s.slotIndex + 1), style = GridType.title, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
            Spacer(Modifier.weight(1f))
            ActionIcon(R.drawable.ic_close, str(R.string.close), onClick = { onEvent(EditorEvent.PickerDismissed) }, transparent = true)
        }
        Row(modifier = Modifier.height(EditSizes.PreviewDisc), verticalAlignment = Alignment.CenterVertically) {
            ColorDisc(color = s.color, ring = false, diameter = EditSizes.PreviewDisc, ringWidth = 0.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                ColorMath.toHex(s.color).uppercase(),
                style = GridType.caption,
                color = PicoTheme.colorScheme.labelSecondary,
                maxLines = 1,
                modifier = Modifier.width(EditSizes.HexWidth),
            )
            Spacer(Modifier.width(16.dp))
            SecondaryButton(
                label = str(R.string.pick_from_cube),
                icon = R.drawable.ic_eyedropper,
                onClick = { onEvent(EditorEvent.EyedropperStarted) },
                modifier = Modifier.width(EditSizes.PickWidth),
                contentPadding = com.kejing.gridspace.ui.common.GridSizes.TightButtonPadding,
            )
        }
        HslSliders(hsl = s.picker.hsl, onChange = { onEvent(EditorEvent.HslChanged(it)) })
        PrimaryButton(label = str(R.string.done), onClick = { onEvent(EditorEvent.PickerDismissed) }, modifier = Modifier.fillMaxWidth())
    }
}

/** The 88 dp hint bar shown instead of the card while the eyedropper is active (spec N.3.3). */
@Composable
fun EyedropperBar(onCancel: () -> Unit) {
    Row(
        modifier = Modifier.height(EditSizes.BarHeight).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(str(R.string.eyedropper_hint), style = GridType.body, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        SecondaryButton(label = str(R.string.cancel), onClick = onCancel)
    }
}
