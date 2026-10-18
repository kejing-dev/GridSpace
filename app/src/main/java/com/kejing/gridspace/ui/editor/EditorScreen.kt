package com.kejing.gridspace.ui.editor

import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kejing.gridspace.R
import com.kejing.gridspace.data.AppGraph
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.usecase.BoxFill
import com.kejing.gridspace.domain.usecase.SelectionOps
import com.kejing.gridspace.domain.usecase.VoxWriter
import com.kejing.gridspace.ui.common.settleRedraw
import com.kejing.gridspace.ui.common.signed
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.ui.editor.components.AxisChip
import com.kejing.gridspace.ui.editor.components.BrushRow
import com.kejing.gridspace.ui.editor.components.BrushRowState
import com.kejing.gridspace.ui.editor.components.ColorRail
import com.kejing.gridspace.ui.editor.components.ColorRailState
import com.kejing.gridspace.ui.editor.components.ColorEditCard
import com.kejing.gridspace.ui.editor.components.ColorEditState
import com.kejing.gridspace.ui.editor.components.EyedropperBar
import com.kejing.gridspace.ui.editor.components.EDITOR_UI_SCALE
import com.kejing.gridspace.ui.editor.components.EditorScaled
import com.kejing.gridspace.ui.editor.components.EditorToolbarContent
import com.kejing.gridspace.ui.editor.components.FRONT_PLANE_SCALE
import com.kejing.gridspace.ui.editor.components.LABEL_UI_SCALE
import com.kejing.gridspace.ui.editor.components.ExportSheet
import com.kejing.gridspace.ui.editor.components.LabelPlacement
import com.kejing.gridspace.ui.editor.components.labelRes
import com.kejing.gridspace.ui.editor.components.ProjectSheet
import com.kejing.gridspace.ui.editor.components.SelectionPanel
import com.kejing.gridspace.ui.editor.components.SelectionPanelState
import com.kejing.gridspace.ui.editor.components.TargetLabel
import com.kejing.gridspace.ui.editor.components.VoxelSceneView
import com.kejing.gridspace.ui.navigation.DemoMode
import com.kejing.gridspace.ui.navigation.WindowIds
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.appString
import com.kejing.gridspace.ui.theme.str
import com.kejing.gridspace.ui.theme.untitledPattern
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.LocalSnackbarHostState
import com.pico.spatial.ui.design.windows.SnackbarHost
import com.pico.spatial.ui.design.windows.Toolbar
import com.pico.spatial.ui.foundation.geometry.DpOffset3D
import com.pico.spatial.ui.foundation.geometry.NormalizedPoint3D
import com.pico.spatial.ui.foundation.window.Augment
import com.pico.spatial.ui.foundation.window.AugmentContentAlignment
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

private const val TAG = "GridSpaceEditor"
/** Spec F.1: the target label fades out within 200 ms of release. */
private const val LABEL_FADE_MS = 200
/** Extra air between the label anchor and the card's near edge. */
private const val LABEL_GAP_DP = 12

/** An Augment placement that a `gridspace_*` global setting can override on the emulator. */
private data class Placement(val anchor: NormalizedPoint3D, val alignment: AugmentContentAlignment, val offset: DpOffset3D) {
    companion object {
        fun fromOverride(values: List<Float>?, default: Placement): Placement {
            if (values == null) return default
            val alignment =
                when (values.getOrNull(6)?.toInt()) {
                    0 -> AugmentContentAlignment.BottomCenter
                    1 -> AugmentContentAlignment.CenterLeft
                    2 -> AugmentContentAlignment.CenterRight
                    3 -> AugmentContentAlignment.TopCenter
                    else -> default.alignment
                }
            return Placement(NormalizedPoint3D(values[0], values[1], values[2]), alignment, DpOffset3D(values[3].dp, values[4].dp, values[5].dp))
        }
    }
}

/**
 * Where the brush-row Augment sits (spec R8 T.3): on the bottom face 3 % behind the front face,
 * rendered unscaled (the front plane is ≈ 1.7 px/Volume-dp on the emulator). R2–R7 had it at
 * z 0.75 / −96 dp, where the plate's near corner glass covered the row's right half (user
 * feedback on the R7 captures). The spec's −24 dp offset puts the row ≈ 400 px below the
 * emulator capture (the bottom face at z 0.97 projects at ≈ 2580 px, frame 2158); −320 dp is
 * the T.3 "raise the offset" fallback that keeps the whole row in frame (y 1900..2063 px) and
 * still in front of the plate (`docs/screenshots/r8/probe/brush-0.5,1,0.97,0,-320,0,0.png`;
 * z 1.0 / −340 looked the same). Overridable with `gridspace_brush`.
 */
private val BrushRowPlacement = Placement(NormalizedPoint3D(0.5f, 1f, 0.97f), AugmentContentAlignment.BottomCenter, DpOffset3D(0.dp, (-320).dp, 0.dp))

/**
 * Spec R4 S.3 puts the side cards at z = 0.5 with a 32 dp offset. With the R5 EditorScaled content
 * (rail 645 x 1557 Volume-dp) that projects the rail's right third past the emulator capture from
 * the post-restart viewpoint (`docs/screenshots/r5/probe/`): z 0.5 → cut at the second swatch
 * column, z 0.35 / offset 16 → grid ends at the frame edge, z 0.3 / 16 → glass corner cut, z 0.25 / 32
 * → whole card in view but swatches 85–91 px, z 0 → behind the model. z = 0.28 with an 8 dp offset
 * keeps the card 20 px inside the frame with swatches 90–94 px (>= 92 target), so both side cards
 * use it. Overridable with `gridspace_rail` / `gridspace_selpanel` (re-check 0.5 / 32 on a headset).
 */
private const val SIDE_CARD_Z = 0.28f
private const val SIDE_CARD_OFFSET_DP = 8
private val ColorRailPlacement = Placement(NormalizedPoint3D(1f, 0.5f, SIDE_CARD_Z), AugmentContentAlignment.CenterLeft, DpOffset3D(SIDE_CARD_OFFSET_DP.dp, 0.dp, 0.dp))
private val SelectionPlacement = Placement(NormalizedPoint3D(0f, 0.5f, SIDE_CARD_Z), AugmentContentAlignment.CenterRight, DpOffset3D((-SIDE_CARD_OFFSET_DP).dp, 0.dp, 0.dp))

/**
 * R6/R7: the colour-edit card and the eyedropper bar are Augments on a plane near the Volume's
 * front (z 0.90, rendered unscaled since R7: the front plane is about 1.7 px/Volume-dp on the
 * emulator), in front of every cube (the sample's rotated near corner reaches z ≈ 0.86) and
 * above the base plate (card y 0.13..0.71, plate at 0.725), so the model never pokes through
 * them as it did through the R4/R5 SpatialPopup at the rail's depth. The spec's "beside the
 * rail at (1, 0.5, 0.95) CenterLeft" is off the emulator capture entirely (the front plane
 * projects at ≈ 1.7 px/Volume-dp, the right face at ≈ 2440 px, `docs/screenshots/r6/probe/`);
 * x 0.88 CenterRight is the right-most placement that keeps the 840-dp card in the frame
 * (0.94 / 0.92 put its right edge at ≈ 2134 / 2186 px, probes p2 and the first R6 run).
 * Overridable with `gridspace_popup`.
 */
private val ColorEditPlacement = Placement(NormalizedPoint3D(0.88f, 0.42f, 0.90f), AugmentContentAlignment.CenterRight, DpOffset3D(0.dp, 0.dp, 0.dp))

/** W2: the editor Volume with its window-level fittings. */
@Composable
fun EditorScreen(projectId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: EditorViewModel =
        viewModel(
            key = "editor_$projectId",
            factory =
                EditorViewModel.Factory(
                    projectId = projectId,
                    untitledName = { context.appString(R.string.untitled, it) },
                    untitledPattern = context.untitledPattern(),
                ),
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalSpatialNavigator.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // One stable callback for the whole screen, so fittings never recompose because of it.
    val onEvent: (EditorEvent) -> Unit = remember(viewModel) { { event -> viewModel.onEvent(event) } }

    // Closing the Volume saves at once (the 1 s debounce may still be pending) and tells the
    // session that no editor is open any more, whether the close came from us or the system
    // button. Branch A (spec R4 N.6.A): when this was the last editor, the entry window is
    // reopened from the navigator captured at composition; a switch to another project has
    // already recorded the new id, so the compare-and-set leaves the session set and nothing reopens.
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onEvent(EditorEvent.Closed)
            AppGraph.editorSession.closed(projectId)
            if (AppGraph.editorSession.openProjectId.value == null) {
                runCatching { navigator.openWindowContainer(WindowIds.ENTRY_WINDOW, null, Bundle()) }
                    .onSuccess { Log.i(TAG, "last editor closed; entry window reopen requested") }
                    .onFailure { Log.w(TAG, "entry window reopen failed", it) }
            }
        }
    }

    // Going to the background flushes the debounce too (spec G.3: save on onPause).
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) viewModel.onEvent(EditorEvent.Paused)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Switching project from the Projects sheet: open the new Volume first, then close this one.
    LaunchedEffect(state.switchRequest) {
        val request = state.switchRequest ?: return@LaunchedEffect
        AppGraph.editorSession.opened(request.open)
        navigator.openWindowContainer(
            WindowIds.EDITOR_VOLUME,
            tag = WindowIds.editorTag(request.open),
            bundle = Bundle().apply { putString(WindowIds.ARG_PROJECT_ID, request.open) },
        )
        viewModel.onEvent(EditorEvent.SwitchHandled)
        navigator.closeWindowContainer(WindowIds.EDITOR_VOLUME, WindowIds.editorTag(request.close))
    }

    // Demo recording only; returns at once unless the emulator setting names a script.
    LaunchedEffect(state.project != null) {
        if (state.project == null) return@LaunchedEffect
        val closeAfter = DemoMode.runEditorScript(context, projectId, { viewModel.state.value }, onEvent)
        if (closeAfter) navigator.closeWindowContainer(WindowIds.EDITOR_VOLUME, WindowIds.editorTag(projectId))
    }

    SnackbarHost { EditorContent(state = state, onEvent = onEvent, modifier = modifier) }
}

/** What the target label showed last; kept so the card can fade out after the preview is gone. */
private data class LabelContent(val title: String, val subtitle: String?, val icon: Int?, val placement: LabelPlacement)

@Composable
internal fun EditorContent(state: EditorUiState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    EditorNotices(state.notice, onEvent)
    val project = state.project
    Box(modifier = modifier.fillMaxSize()) {
        if (project == null) {
            if (state.loadFailed) {
                Text(str(R.string.corrupt_snack), style = GridType.body, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.align(Alignment.Center))
            }
            return@Box
        }
        val context = LocalContext.current
        var labelAnchor by remember { mutableStateOf<LabelPlacement?>(null) }
        var selectionAnchor by remember { mutableStateOf<Vec3?>(null) }
        var axisTips by remember { mutableStateOf<Map<Axis, Vec3>>(emptyMap()) }
        val selectionCenter = if (state.selection.isEmpty()) null else SelectionOps.center(state.selection)
        val language = AppGraph.language.collectAsStateWithLifecycle()

        VoxelSceneView(
            grid = project.voxels,
            mode = state.mode,
            lastCommitted = state.lastCommitted,
            preview = state.preview,
            activeColor = project.activeColor,
            selection = state.selection,
            resetToken = state.resetToken,
            locked = state.sceneLocked,
            // R6: no floating card during a selection drag-move (its text lives on the panel).
            labelCell = state.preview?.takeUnless { it is Preview.MoveSelection }?.anchorCell,
            selectionCenter = selectionCenter,
            onEvent = onEvent,
            onAnchors = { label, sel, tips ->
                labelAnchor = label
                selectionAnchor = sel
                axisTips = tips
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Window-level fittings: positioned by the system around the Volume, never inside it.
        // The system Toolbar is the single main bar (spec R4 R.1 / S.4): modes, history, reset;
        // its content is not wrapped in EditorScaled (spec S.1, the system scales it [H]).
        Toolbar { EditorToolbarContent(state = state, onEvent = onEvent) }

        // Every Augment gets fixed-size content and a single State, so its content lambda never
        // changes and the system never rebuilds the spatial window (AGENTS: black-card rebuilds).
        // Every content is wrapped in EditorScaled (spec S.1): the Volume and its attachments do
        // not scale with distance, so the design dp are rendered at 1.3–1.5 x; anchors and
        // offsets stay outside, in Volume-dp.
        val brushState = rememberUpdatedState(BrushRowState(state.brushSize, project.gridSize))
        val brushOverride = remember { DemoMode.placement(context, DemoMode.BRUSH_SETTING) }
        // Emulator diagnostics: an override whose first value is negative hides the Augment.
        if (state.showBrushRow && (brushOverride == null || brushOverride[0] >= 0f)) {
            val p = Placement.fromOverride(brushOverride, BrushRowPlacement)
            Augment(anchor = p.anchor, alignment = p.alignment, offset = p.offset) {
                // R8: unscaled on the front plane (spec T.3); settleRedraw keeps the glass nudge.
                Box(modifier = Modifier.settleRedraw()) { BrushRow(state = brushState, onEvent = onEvent) }
            }
        }

        val railState =
            rememberUpdatedState(
                ColorRailState(
                    projectName = project.name,
                    saveState = state.saveState,
                    slots = project.slots,
                    activeSlot = project.activeSlot,
                    preset = state.lastPreset,
                    picker = state.picker,
                    language = language.value,
                )
            )
        val railOverride = remember { DemoMode.placement(context, DemoMode.RAIL_SETTING) }
        // R10: the rail is a toolbar-toggled panel (closed by default, spec T.8).
        if (state.showColorRail && (railOverride == null || railOverride[0] >= 0f)) {
            val p = Placement.fromOverride(railOverride, ColorRailPlacement)
            Augment(anchor = p.anchor, alignment = p.alignment, offset = p.offset) {
                EditorScaled(EDITOR_UI_SCALE) { ColorRail(state = railState, onEvent = onEvent) }
            }
        }

        val moveHint = (state.preview as? Preview.MoveSelection)?.let { moveTexts(it).let { (delta, second) -> "$delta · $second" } }
        val panelState =
            rememberUpdatedState(
                SelectionPanelState(
                    count = state.selection.size,
                    translation = state.translation + state.dragDelta,
                    blockedStep = state.blockedStep,
                    dragging = state.dragging,
                    liveHint = moveHint,
                )
            )
        val panelOverride = remember { DemoMode.placement(context, DemoMode.SELPANEL_SETTING) }
        if (state.showSelectionPanel && (panelOverride == null || panelOverride[0] >= 0f)) {
            val yN = (selectionAnchor?.y ?: 0.5f).coerceIn(0.15f, 0.85f)
            val base = Placement.fromOverride(panelOverride, SelectionPlacement)
            val anchor = if (panelOverride == null) NormalizedPoint3D(base.anchor.x, yN, base.anchor.z) else base.anchor
            Augment(anchor = anchor, alignment = base.alignment, offset = base.offset) {
                EditorScaled(EDITOR_UI_SCALE) { SelectionPanel(state = panelState, onEvent = onEvent) }
            }
        }
        TargetLabelAugment(preview = state.preview, anchor = labelAnchor, gridSize = project.gridSize, largeFill = state.largeFill)

        // R11 axis letters (spec T.9): 56 x 56 design-dp chips on the front plane at the projected
        // arrow tips, following the orbit like the target label. Fixed content, anchor-only updates.
        for ((axis, tip) in axisTips) {
            Augment(
                anchor = NormalizedPoint3D(tip.x, tip.y, tip.z),
                alignment = AugmentContentAlignment.Center,
                offset = DpOffset3D(0.dp, 0.dp, 0.dp),
            ) {
                EditorScaled(FRONT_PLANE_SCALE) { AxisChip(letter = str(axis.labelRes())) }
            }
        }

        // R6: colour-edit card / eyedropper bar as Augments on the front plane beside the rail.
        val picker = state.picker
        val editState = rememberUpdatedState(picker?.let { ColorEditState(it, project.activeSlot, project.activeColor) })
        val popupOverride = remember { DemoMode.placement(context, DemoMode.POPUP_SETTING) }
        if (picker != null && (popupOverride == null || popupOverride[0] >= 0f)) {
            val p = Placement.fromOverride(popupOverride, ColorEditPlacement)
            if (picker.eyedropper) {
                Augment(anchor = p.anchor, alignment = p.alignment, offset = p.offset) {
                    EditorScaled(FRONT_PLANE_SCALE) { EyedropperBar(onCancel = { onEvent(EditorEvent.EyedropperCancelled) }) }
                }
            } else {
                Augment(anchor = p.anchor, alignment = p.alignment, offset = p.offset) {
                    EditorScaled(FRONT_PLANE_SCALE) { ColorEditCard(state = editStateOrLast(editState), onEvent = onEvent) }
                }
            }
        }
        state.exportSheet?.let { ExportSheet(export = it, tooManyColours = VoxWriter.exceedsPalette(project.voxels), onEvent = onEvent) }
        state.projectSheet?.let { ProjectSheet(sheet = it, currentId = project.id, recent = state.recent, onEvent = onEvent) }
    }
}

/**
 * The target label Augment. While a preview is live the card follows [anchor]; once the preview
 * ends the last content stays for a 200 ms fade-out, then the Augment is removed (spec F.1).
 * R6: the card hangs above the target on the Volume's front plane (`LabelAnchor`), growing
 * upward from the anchor (BottomCenter); a selection drag-move shows no card at all.
 */
@Composable
private fun TargetLabelAugment(preview: Preview?, anchor: LabelPlacement?, gridSize: Int, largeFill: Boolean) {
    val current =
        if (preview != null && preview !is Preview.MoveSelection && anchor != null) {
            val (title, subtitle, icon) = labelTexts(preview, gridSize, largeFill)
            LabelContent(title, subtitle, icon, anchor)
        } else {
            null
        }
    // Plain holder (not snapshot state): remembering the last content must not trigger recomposition.
    val last = remember { arrayOfNulls<LabelContent>(1) }
    if (current != null) last[0] = current
    val shown = current ?: last[0] ?: return
    val transition = updateTransition(targetState = current != null, label = "targetLabel")
    if (!transition.currentState && !transition.targetState) return
    val a = shown.placement.anchor
    Augment(
        anchor = NormalizedPoint3D(a.x.coerceIn(0f, 1f), a.y.coerceIn(0f, 1f), a.z.coerceIn(0f, 1f)),
        alignment = AugmentContentAlignment.BottomCenter,
        offset = DpOffset3D(0.dp, 0.dp, 0.dp),
    ) {
        EditorScaled(LABEL_UI_SCALE) {
            transition.AnimatedVisibility(
                visible = { it },
                enter = fadeIn(tween(LABEL_FADE_MS)),
                exit = fadeOut(tween(LABEL_FADE_MS)),
            ) {
                TargetLabel(title = shown.title, subtitle = shown.subtitle, icon = shown.icon)
            }
        }
    }
}

/**
 * The colour-edit Augment content must read one State; the picker becomes null one frame before
 * the Augment is removed, so the last non-null value is kept for that frame.
 */
@Composable
private fun editStateOrLast(state: State<ColorEditState?>): State<ColorEditState> {
    val last = remember { arrayOfNulls<ColorEditState>(1) }
    state.value?.let { last[0] = it }
    return remember { derivedStateOf { state.value ?: last[0]!! } }
}

/** Drag-move texts (spec R4 O.2.3): "ΔX +2 · ΔY 0 · ΔZ −1" and the second line. */
@Composable
private fun moveTexts(preview: Preview.MoveSelection): Pair<String, String> {
    val d = preview.delta
    val second =
        when {
            !preview.moving -> str(R.string.label_move_hint)
            preview.overwritten.isEmpty() -> str(R.string.label_move_release, preview.origin.size)
            else -> str(R.string.label_move_overwrite, preview.overwritten.size)
        }
    return str(R.string.label_move_delta, signed(d.x), signed(d.y), signed(d.z)) to second
}

/** Title, subtitle and icon of the target label for each preview state (design spec F.2, R4 O.2.3). */
@Composable
private fun labelTexts(preview: Preview, gridSize: Int, largeFill: Boolean): Triple<String, String?, Int?> =
    when (preview) {
        is Preview.Ghost ->
            if (preview.occupied) Triple(str(R.string.label_occupied), str(R.string.label_occupied_sub), R.drawable.ic_exclamation_mark_fill)
            else Triple(str(R.string.label_aligned), str(R.string.label_cell, preview.cell.x, preview.cell.y, preview.cell.z), null)
        is Preview.Outside -> Triple(str(R.string.label_outside), str(R.string.label_grid_dims, gridSize), null)
        is Preview.EraseCell -> Triple(str(R.string.label_release_erase), null, R.drawable.ic_eraser)
        is Preview.PaintCell -> Triple(str(R.string.label_release_paint), null, R.drawable.ic_paint)
        is Preview.SelectCell -> Triple(str(R.string.mode_select), null, R.drawable.ic_pointer)
        is Preview.BlockAdd -> {
            val r = preview.region
            if (preview.blocked) {
                Triple(str(R.string.label_occupied), str(R.string.label_occupied_sub), R.drawable.ic_exclamation_mark_fill)
            } else {
                // Spec T.2 step 8: a large block appends "large, please wait" to the release line.
                val release = str(R.string.label_release_place)
                val second = if (r.cellCount > BoxFill.LARGE_THRESHOLD || largeFill) "$release · ${str(R.string.label_large)}" else release
                Triple(str(R.string.label_size_dims, r.width, r.height, r.depth), second, null)
            }
        }
        is Preview.BlockErase -> {
            val r = preview.region
            Triple(str(R.string.label_size_dims, r.width, r.height, r.depth), str(R.string.label_release_erase), R.drawable.ic_eraser)
        }
        is Preview.MoveSelection -> moveTexts(preview).let { (delta, second) -> Triple(delta, second, R.drawable.ic_pointer) }
    }

@Composable
private fun EditorNotices(notice: EditorNotice?, onEvent: (EditorEvent) -> Unit) {
    val snackbar = LocalSnackbarHostState.current
    val message =
        when (notice) {
            EditorNotice.SaveFailed -> str(R.string.save_failed_snack)
            EditorNotice.RotateOutOfGrid -> str(R.string.rotate_out_of_grid)
            EditorNotice.NoRoom -> str(R.string.no_room)
            is EditorNotice.Exported -> str(R.string.export_snack, notice.displayName)
            is EditorNotice.Deleted -> str(R.string.deleted_snack, notice.name)
            null -> null
        }
    val retry = str(R.string.retry)
    LaunchedEffect(notice) {
        if (notice == null || message == null) return@LaunchedEffect
        onEvent(EditorEvent.NoticeShown)
        if (notice == EditorNotice.SaveFailed) {
            val result =
                snackbar.show(
                    message = message,
                    leadingIcon = { Icon(painterResource(R.drawable.ic_exclamation_mark_fill), contentDescription = null, tint = GridColors.Coral) },
                    action = retry,
                )
            if (result is com.pico.spatial.ui.design.windows.SnackbarResult.ActionPerformed) onEvent(EditorEvent.RetrySave)
        } else {
            snackbar.show(message = message)
        }
    }
}
