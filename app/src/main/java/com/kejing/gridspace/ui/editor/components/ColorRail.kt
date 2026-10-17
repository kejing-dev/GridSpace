package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.Palette
import com.kejing.gridspace.domain.model.PalettePreset
import com.kejing.gridspace.domain.model.SaveState
import com.kejing.gridspace.ui.common.ActionIcon
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.common.LanguageSegment
import com.kejing.gridspace.ui.common.SecondaryButton
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.PickerState
import com.kejing.gridspace.ui.theme.AppLanguage
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.LocalDisableAlpha
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect

/** Everything the colour rail shows; one State keeps the Augment content lambda stable. */
data class ColorRailState(
    val projectName: String,
    val saveState: SaveState,
    val slots: List<Int>,
    val activeSlot: Int,
    val preset: PalettePreset,
    val picker: PickerState?,
    val language: AppLanguage,
) {
    val eyedropper: Boolean
        get() = picker?.eyedropper == true
}

/**
 * Spec R4 S.3 (design dp, rendered through EditorScaled 1.5): 430 wide, padding 24, content 382;
 * 24 + 48 + 12 + 72 + 12 + 88 + 12 + 72 + 12 + 578 + 12 + 72 + 24 = 1038 high (1557 Volume-dp).
 */
object RailSizes {
    val Width = 430.dp
    val Padding = 24.dp
    val ContentWidth = 382.dp
    val RowGap = 12.dp
    val HeaderHeight = 48.dp
    val NameWidth = 200.dp
    val StatusWidth = 120.dp
    val Dot = 10.dp
    val ActionGap = 12.dp
    val LanguageWidth = 144.dp
    val LanguageItemWidth = 64.dp
    val SlotHit = 88.dp
    val SlotDisc = 72.dp
    val SlotRing = 3.dp
    val SlotGap = 12.dp
    val PresetItemWidth = 122.dp
    val Swatch = 88.dp
    val SwatchRadius = 20.dp
    val SwatchGap = 10.dp
    val SwatchRing = 3.dp
}

/**
 * The persistent colour rail on the Volume's right (spec R4 N.3 / S.3): project name + save dot,
 * the action row (projects / export icon buttons and the 中/EN segment, moved here from the
 * system Toolbar in R5), three slots (tap = current slot, no long press), the preset tabs, the
 * 4 x 6 swatch grid that writes straight into the current slot, and "Edit colour". While the eyedropper is active the slots, tabs and swatches are dimmed
 * and inert.
 */
@Composable
fun ColorRail(state: State<ColorRailState>, onEvent: (EditorEvent) -> Unit) {
    val s = state.value
    val current = s.slots[s.activeSlot.coerceIn(0, s.slots.lastIndex)]
    Column(
        modifier = Modifier.width(RailSizes.Width).padding(RailSizes.Padding),
        verticalArrangement = Arrangement.spacedBy(RailSizes.RowGap),
    ) {
        RailHeader(name = s.projectName, saveState = s.saveState)
        Row(
            modifier = Modifier.width(RailSizes.ContentWidth).height(GridSizes.Control),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RailSizes.ActionGap),
        ) {
            ActionIcon(R.drawable.ic_folder, str(R.string.projects), onClick = { onEvent(EditorEvent.ProjectSheetOpened) })
            ActionIcon(R.drawable.ic_upload, str(R.string.export), onClick = { onEvent(EditorEvent.ExportOpened) })
            LanguageSegment(
                language = s.language,
                onSelect = { onEvent(EditorEvent.LanguageSelected(it)) },
                width = RailSizes.LanguageWidth,
                itemWidth = RailSizes.LanguageItemWidth,
            )
        }
        Dimmable(enabled = !s.eyedropper) {
            Row(horizontalArrangement = Arrangement.spacedBy(RailSizes.SlotGap)) {
                s.slots.forEachIndexed { index, color ->
                    ColorSlot(
                        color = color,
                        current = index == s.activeSlot,
                        enabled = !s.eyedropper,
                        label = str(R.string.slot_n, index + 1),
                        onTap = { onEvent(EditorEvent.SlotTapped(index)) },
                    )
                }
            }
        }
        Dimmable(enabled = !s.eyedropper) {
            SegmentControl(
                modifier = Modifier.width(RailSizes.ContentWidth).height(GridSizes.SegmentHeight),
                itemSpace = GridSizes.SegmentGap,
                contentPadding = GridSizes.SegmentPadding,
            ) {
                PalettePreset.entries.forEach { preset ->
                    SegmentItem(
                        selected = s.preset == preset,
                        onClick = { if (!s.eyedropper) onEvent(EditorEvent.PresetSelected(preset)) },
                        modifier = Modifier.width(RailSizes.PresetItemWidth).height(GridSizes.SegmentItemHeight),
                        title = { Text(str(preset.labelRes()), style = GridType.captionMedium, maxLines = 1) },
                    )
                }
            }
        }
        Dimmable(enabled = !s.eyedropper) {
            SwatchGrid(colors = s.preset.colors, current = current, enabled = !s.eyedropper, onPick = { onEvent(EditorEvent.SwatchPicked(it)) })
        }
        // R6: the colour-edit card is a window-level Augment on the front plane (EditorScreen),
        // no longer a popup anchored here.
        SecondaryButton(
            label = str(R.string.edit_colour),
            icon = R.drawable.ic_sliders,
            onClick = { onEvent(EditorEvent.ColorEditOpened) },
            modifier = Modifier.width(RailSizes.ContentWidth),
        )
    }
}

/** Row 0 of the rail: project name (fixed 200) and the save-status dot + text (fixed 120). */
@Composable
private fun RailHeader(name: String, saveState: SaveState) {
    val (dot, label) =
        when (saveState) {
            SaveState.SAVING -> GridColors.SavingGrey to R.string.save_saving
            SaveState.SAVED -> GridColors.Mint to R.string.save_saved
            SaveState.FAILED -> GridColors.Coral to R.string.save_failed
        }
    Row(modifier = Modifier.width(RailSizes.ContentWidth).height(RailSizes.HeaderHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = GridType.label, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(RailSizes.NameWidth))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.size(RailSizes.Dot).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(str(label), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1, modifier = Modifier.width(RailSizes.StatusWidth))
    }
}

/** System disabled look (LocalDisableAlpha) for the rail parts that are inert during the eyedropper. */
@Composable
private fun Dimmable(enabled: Boolean, content: @Composable () -> Unit) {
    val alpha = if (enabled) 1f else LocalDisableAlpha.current
    Box(modifier = Modifier.alpha(alpha)) {
        if (enabled) content() else CompositionLocalProvider(LocalDisableAlpha provides 1f) { content() }
    }
}

fun PalettePreset.labelRes(): Int =
    when (this) {
        PalettePreset.SOFT -> R.string.preset_soft
        PalettePreset.VIVID -> R.string.preset_vivid
        PalettePreset.EARTH -> R.string.preset_earth
    }

/** A colour slot: 88 dp hit area, 72 dp disc, 3 dp white ring when current (spec S.3); tap only, with hover. */
@Composable
fun ColorSlot(color: Int, current: Boolean, enabled: Boolean, label: String, onTap: () -> Unit) {
    Box(
        modifier =
            Modifier.size(RailSizes.SlotHit)
                .spatialHoverEffect()
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onTap)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        ColorDisc(color = color, ring = current, diameter = RailSizes.SlotDisc, ringWidth = RailSizes.SlotRing)
    }
}

/** Filled disc with an optional white ring outside it and a faint inner edge for near-white colours. */
@Composable
fun ColorDisc(color: Int, ring: Boolean, diameter: Dp, ringWidth: Dp) {
    val fill = Color(color)
    val outer = if (ring) diameter + ringWidth * 2 else diameter
    Box(
        modifier =
            Modifier.size(outer)
                .then(if (ring) Modifier.border(ringWidth, GridColors.SelectionWhite, CircleShape) else Modifier)
                .padding(if (ring) ringWidth else 0.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (ColorMath.isNearWhite(color)) Modifier.border(1.dp, GridColors.BaseInk.copy(alpha = 0.3f), CircleShape) else Modifier)
    )
}

/**
 * 4 x 6 swatches, 88 dp with 10 dp gaps (spec R4 S.3). The swatch equal to the current colour
 * gets a 3 dp ring: white, or BaseInk at 60 % on near-white swatches where white would vanish.
 */
@Composable
private fun SwatchGrid(colors: List<Int>, current: Int, enabled: Boolean, onPick: (Int) -> Unit) {
    val shape = RoundedCornerShape(RailSizes.SwatchRadius)
    Column(verticalArrangement = Arrangement.spacedBy(RailSizes.SwatchGap)) {
        for (row in 0 until Palette.PALETTE_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(RailSizes.SwatchGap)) {
                for (col in 0 until Palette.PALETTE_COLUMNS) {
                    val color = colors[row * Palette.PALETTE_COLUMNS + col]
                    val selected = color == current
                    val nearWhite = ColorMath.isNearWhite(color)
                    val ring = if (nearWhite) GridColors.BaseInk.copy(alpha = 0.6f) else GridColors.SelectionWhite
                    Box(
                        modifier =
                            Modifier.size(RailSizes.Swatch)
                                .spatialHoverEffect()
                                .clip(shape)
                                .background(Color(color))
                                .then(
                                    when {
                                        selected -> Modifier.border(RailSizes.SwatchRing, ring, shape)
                                        nearWhite -> Modifier.border(1.dp, GridColors.BaseInk.copy(alpha = 0.3f), shape)
                                        else -> Modifier
                                    }
                                )
                                .clickable(enabled = enabled) { onPick(color) }
                    )
                }
            }
        }
    }
}
