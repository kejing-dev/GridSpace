package com.kejing.gridspace.ui.editor.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.common.ToolbarIcon
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.EditorUiState
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleButton
import com.pico.spatial.ui.design.ToggleButtonDefaults
import com.pico.spatial.ui.design.ToggleIconButton
import com.pico.spatial.ui.design.ToggleIconButtonDefaults
import com.pico.spatial.ui.design.VerticalDivider

@StringRes
fun EditMode.labelRes(): Int =
    when (this) {
        EditMode.ADD -> R.string.mode_add
        EditMode.ERASE -> R.string.mode_erase
        EditMode.PAINT -> R.string.mode_paint
        EditMode.SELECT -> R.string.mode_select
    }

@DrawableRes
fun EditMode.iconRes(): Int =
    when (this) {
        EditMode.ADD -> R.drawable.ic_plus
        EditMode.ERASE -> R.drawable.ic_eraser
        EditMode.PAINT -> R.drawable.ic_paint
        EditMode.SELECT -> R.drawable.ic_pointer
    }

/**
 * Spec R4 S.4 + R10 T.8: mode toggles 80 high / 160 min wide, 12 apart; dividers 16 + 1 x 40 + 16;
 * 676 + 33 + 152 + 33 + 72 + 33 + 152 + 33 + 152 + 40 = 1376 wide in all.
 */
object ToolbarSizes {
    val ModeHeight = 80.dp
    val ModeMinWidth = 160.dp
    val ModeGap = 12.dp
    val IconGap = 12.dp
    val DividerPad = 16.dp
    val DividerHeight = 40.dp
}

/**
 * Content of the window-level Toolbar, the single main bar (spec R4 R.1 / S.4 / T.8): four modes,
 * undo / redo, reset view, the 尺寸 / 颜色 panel toggles (R10: the size row and the colour rail are
 * opened from here, closed by default) and projects / export. Only the 中/EN segment stays on the
 * colour rail. Everything on it is one tap away (UX rule U2); no colour slots live here.
 */
@Composable
fun RowScope.EditorToolbarContent(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    if (state.project == null) return
    EditMode.entries.forEachIndexed { index, mode ->
        if (index > 0) Spacer(Modifier.width(ToolbarSizes.ModeGap))
        ModeToggle(mode, selected = state.mode == mode, enabled = state.modesEnabled) { onEvent(EditorEvent.ModeSelected(mode)) }
    }
    ToolbarDivider()
    ToolbarIcon(R.drawable.ic_undo, str(R.string.undo), enabled = state.canUndo) { onEvent(EditorEvent.Undo) }
    Spacer(Modifier.width(ToolbarSizes.IconGap))
    ToolbarIcon(R.drawable.ic_redo, str(R.string.redo), enabled = state.canRedo) { onEvent(EditorEvent.Redo) }
    ToolbarDivider()
    ToolbarIcon(R.drawable.ic_locate, str(R.string.reset_view)) { onEvent(EditorEvent.ResetView) }
    ToolbarDivider()
    PanelToggle(R.drawable.ic_cube, str(R.string.toggle_size), checked = state.showSizeRow) { onEvent(EditorEvent.ToggleSizeRow) }
    Spacer(Modifier.width(ToolbarSizes.IconGap))
    PanelToggle(R.drawable.ic_palette, str(R.string.toggle_colour), checked = state.showColorRail) { onEvent(EditorEvent.ToggleColorRail) }
    ToolbarDivider()
    ToolbarIcon(R.drawable.ic_folder, str(R.string.projects)) { onEvent(EditorEvent.ProjectSheetOpened) }
    Spacer(Modifier.width(ToolbarSizes.IconGap))
    ToolbarIcon(R.drawable.ic_upload, str(R.string.export)) { onEvent(EditorEvent.ExportOpened) }
}

/** 72 dp ToggleIconButton for a panel (R10): checked while the panel is shown; [label] is the content description. */
@Composable
private fun PanelToggle(icon: Int, label: String, checked: Boolean, onToggle: () -> Unit) {
    ToggleIconButton(
        checked = checked,
        onCheckedChange = { onToggle() },
        size = ToggleIconButtonDefaults.toggleIconButtonSize(GridSizes.Control),
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(GridSizes.Icon))
    }
}

@Composable
private fun ToolbarDivider() {
    Spacer(Modifier.width(ToolbarSizes.DividerPad))
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = ToolbarSizes.DividerHeight))
    Spacer(Modifier.width(ToolbarSizes.DividerPad))
}

/** Capsule mode toggle: 80 high, 28 dp icon, 20 sp label, Mint when current (spec R4 M.3). */
@Composable
private fun ModeToggle(mode: EditMode, selected: Boolean, enabled: Boolean, onSelected: () -> Unit) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { if (!selected) onSelected() },
        enabled = enabled,
        size =
            ToggleButtonDefaults.toggleButtonSize(
                minWidth = ToolbarSizes.ModeMinWidth,
                height = ToolbarSizes.ModeHeight,
                minHeight = ToolbarSizes.ModeHeight,
                maxHeight = ToolbarSizes.ModeHeight,
            ),
        colors =
            ToggleButtonDefaults.toggleButtonColors(
                checkedContainerColor = GridColors.Mint,
                checkedContentColor = GridColors.OnMint,
                uncheckedContainerColor = Color.Transparent,
                uncheckedContentColor = PicoTheme.colorScheme.labelPrimary,
            ),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        gap = 10.dp,
        leadingIcon = { Icon(painterResource(mode.iconRes()), contentDescription = null, modifier = Modifier.size(GridSizes.Icon)) },
    ) {
        Text(str(mode.labelRes()), style = GridType.label, maxLines = 1)
    }
}
