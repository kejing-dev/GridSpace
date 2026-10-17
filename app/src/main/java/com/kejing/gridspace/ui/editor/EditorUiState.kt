package com.kejing.gridspace.ui.editor

import com.kejing.gridspace.data.ExportResult
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.domain.model.PalettePreset
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.domain.model.SaveState
import com.kejing.gridspace.domain.model.Vec3
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.BoxRegion
import com.kejing.gridspace.domain.usecase.BrushSize
import com.kejing.gridspace.ui.theme.AppLanguage

/** What the 3D view hit, in model-local cell units. Resolved into a target cell by the ViewModel. */
sealed class SceneHit {
    /** A cube was hit; [point] is the hit point when the view could derive one. */
    data class Voxel(val cell: Cell, val point: Vec3?) : SceneHit()

    /** The base plate was hit at [point] (y is ignored). */
    data class Base(val point: Vec3) : SceneHit()

    /** A ray through the grid (pinch-hold movement): marched with 3D-DDA. */
    data class Ray(val origin: Vec3, val direction: Vec3) : SceneHit()

    /** An already-resolved cell (demo script, or the drag-offset fallback). */
    data class Target(val cell: Cell) : SceneHit()

    data object None : SceneHit()
}

/** Transient 3D feedback while a pinch is held (design spec F.2, R4 O.1 / O.2). */
sealed class Preview {
    /** Add: ghost cube at [cell]; coral wireframe when the cell is occupied. */
    data class Ghost(val cell: Cell, val occupied: Boolean) : Preview()

    /** Add: target left the grid; the label stays on the last valid cell. */
    data class Outside(val anchor: Cell?) : Preview()

    data class EraseCell(val cell: Cell) : Preview()

    data class PaintCell(val cell: Cell, val color: Int) : Preview()

    data class SelectCell(val cell: Cell) : Preview()

    /**
     * Add with a brush larger than 1 x 1 x 1 (R8 sized brush, spec T.2): the whole block as a mint
     * ghost, or a coral wireframe when [blocked] (the target cell is occupied, nothing to add).
     */
    data class BlockAdd(val region: BoxRegion, val color: Int, val blocked: Boolean = false) : Preview()

    /** Erase with a brush larger than 1 x 1 x 1: the block as a coral wireframe. */
    data class BlockErase(val region: BoxRegion) : Preview()

    /**
     * Select mode, dragging a selected cube (spec R4 O.2): the whole [origin] selection moves by
     * [delta] (already clamped to the grid) onto [targets]; [overwritten] are the non-selected cubes
     * the move would replace.
     */
    data class MoveSelection(val origin: Set<Cell>, val delta: Cell, val targets: Set<Cell>, val overwritten: Set<Cell>) : Preview() {
        val moving: Boolean
            get() = delta != Cell.ZERO
    }

    /** Cell the label is attached to. */
    val anchorCell: Cell?
        get() =
            when (this) {
                is Ghost -> cell
                is Outside -> anchor
                is EraseCell -> cell
                is PaintCell -> cell
                is SelectCell -> cell
                is BlockAdd -> region.max
                is BlockErase -> region.max
                is MoveSelection -> bboxMax(if (moving) targets else origin)
            }

    companion object {
        /** The max corner of a cell set's bounding box (label anchor for multi-cell previews). */
        fun bboxMax(cells: Set<Cell>): Cell? =
            if (cells.isEmpty()) null else Cell(cells.maxOf { it.x }, cells.maxOf { it.y }, cells.maxOf { it.z })
    }
}

/**
 * The colour-edit popup (spec R4 N.3.3); null when closed. The slot being edited is always the
 * project's active slot (R4 dropped the separate editing slot).
 */
data class PickerState(
    val hsl: ColorMath.Hsl,
    val eyedropper: Boolean = false,
)

enum class ExportPhase { IDLE, EXPORTING, DONE, FAILED }

data class ExportState(
    val fileName: String,
    val phase: ExportPhase = ExportPhase.IDLE,
    val result: ExportResult? = null,
    val error: String? = null,
)

/** The "Projects" Sheet (spec R4 N.6.A); null when closed. */
data class ProjectSheetState(
    val gridSize: Int = VoxelGrid.DEFAULT_SIZE,
    /** Project whose delete confirmation is showing. */
    val confirmDelete: ProjectSummary? = null,
)

/** Asks the screen to open another project's Volume and then close this one (open before close). */
data class SwitchRequest(val open: String, val close: String)

/** A stepper button (axis and direction) that is briefly disabled after an out-of-bounds step. */
data class StepKey(val axis: Axis, val delta: Int)

sealed class EditorNotice {
    data object SaveFailed : EditorNotice()

    data object RotateOutOfGrid : EditorNotice()

    data object NoRoom : EditorNotice()

    data class Exported(val displayName: String) : EditorNotice()

    data class Deleted(val name: String) : EditorNotice()
}

data class EditorUiState(
    val project: Project? = null,
    val loadFailed: Boolean = false,
    val mode: EditMode = EditMode.ADD,
    /** R8: the one brush's W x H x D, 1..16 clamped to the grid; remembered per editor session, not saved. */
    val brushSize: BrushSize = BrushSize.UNIT,
    val selection: Set<Cell> = emptySet(),
    /** Accumulated nudge of the selection this session, shown in the steppers. */
    val translation: Cell = Cell.ZERO,
    /** Stepper disabled for 100 ms after an out-of-bounds step (spec E.4). */
    val blockedStep: StepKey? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saveState: SaveState = SaveState.SAVED,
    val preview: Preview? = null,
    /** True while a large box fill is still being built into the scene. */
    val largeFill: Boolean = false,
    val picker: PickerState? = null,
    /** Preset page shown by the colour rail (remembered across sessions of the popup). */
    val lastPreset: PalettePreset = PalettePreset.SOFT,
    val exportSheet: ExportState? = null,
    val projectSheet: ProjectSheetState? = null,
    /** Recent projects for the Projects sheet (from the shared repository). */
    val recent: List<ProjectSummary> = emptyList(),
    val switchRequest: SwitchRequest? = null,
    val notice: EditorNotice? = null,
    /** Incremented to ask the view to ease the camera back to the start pose. */
    val resetToken: Int = 0,
    /** Incremented on every commit so the view can pop in the new cubes. */
    val commitToken: Int = 0,
    val lastCommitted: Set<Cell> = emptySet(),
) {
    /** The size the current mode acts with: PAINT / SELECT always work on one cell. */
    val effectiveBrushSize: BrushSize
        get() = if (mode == EditMode.ADD || mode == EditMode.ERASE) brushSize else BrushSize.UNIT

    val showBrushRow: Boolean
        get() = mode == EditMode.ADD || mode == EditMode.ERASE

    val showSelectionPanel: Boolean
        get() = mode == EditMode.SELECT && selection.isNotEmpty()

    /** The export and project sheets are modal: the Volume ignores input while one is open. */
    val sceneLocked: Boolean
        get() = exportSheet != null || projectSheet != null

    val modesEnabled: Boolean
        get() = picker?.eyedropper != true

    val eyedropperActive: Boolean
        get() = picker?.eyedropper == true

    /** Live drag-move offset (spec R4 O.2.3): the steppers show translation + this while dragging. */
    val dragDelta: Cell
        get() = (preview as? Preview.MoveSelection)?.delta ?: Cell.ZERO

    val dragging: Boolean
        get() = preview is Preview.MoveSelection
}

sealed class EditorEvent {
    data class ModeSelected(val mode: EditMode) : EditorEvent()

    /** Brush row stepper (R8): one axis of the brush size by +1 / -1, clamped to 1..min(16, grid). */
    data class SizeStep(val axis: Axis, val delta: Int) : EditorEvent()

    /** Colour rail: the slot becomes current (and the one the popup edits). */
    data class SlotTapped(val index: Int) : EditorEvent()

    data class PresetSelected(val preset: PalettePreset) : EditorEvent()

    /** Colour rail swatch: written straight into the current slot, never into the history. */
    data class SwatchPicked(val color: Int) : EditorEvent()

    data class HslChanged(val hsl: ColorMath.Hsl) : EditorEvent()

    /** "Edit colour" on the rail opens the popup for the current slot. */
    data object ColorEditOpened : EditorEvent()

    data object EyedropperStarted : EditorEvent()

    data object EyedropperCancelled : EditorEvent()

    data object PickerDismissed : EditorEvent()

    data class SceneTapped(val hit: SceneHit) : EditorEvent()

    /** Pinch held: preview starts at [hit]. */
    data class PreviewStarted(val hit: SceneHit) : EditorEvent()

    data class PreviewMoved(val hit: SceneHit) : EditorEvent()

    /** Pinch released: commit whatever the preview shows. */
    data object Committed : EditorEvent()

    /** Tracking lost: drop the preview without committing. */
    data object PreviewCancelled : EditorEvent()

    /** Select mode: a drag started on the selected [cell] (spec R4 O.2.1). */
    data class SelectionDragStarted(val cell: Cell) : EditorEvent()

    /** The drag now maps to a whole-cell [delta] (already clamped by the view). */
    data class SelectionDragMoved(val delta: Cell) : EditorEvent()

    /** Release: move the selection by the previewed delta as one history step. */
    data object SelectionDragEnded : EditorEvent()

    data object SelectionDragCancelled : EditorEvent()

    data object Undo : EditorEvent()

    data object Redo : EditorEvent()

    data object ResetView : EditorEvent()

    data object SelectionRotate : EditorEvent()

    data object SelectionDuplicate : EditorEvent()

    data object SelectionDelete : EditorEvent()

    data class SelectionStep(val axis: Axis, val delta: Int) : EditorEvent()

    data object SelectionClear : EditorEvent()

    data object ExportOpened : EditorEvent()

    data class ExportNameChanged(val name: String) : EditorEvent()

    data object ExportConfirmed : EditorEvent()

    data object ExportDismissed : EditorEvent()

    data object ProjectSheetOpened : EditorEvent()

    data object ProjectSheetDismissed : EditorEvent()

    data class ProjectSheetGridSize(val size: Int) : EditorEvent()

    /** Create "Untitled k" with the sheet's grid size and switch to it. */
    data object ProjectSheetNew : EditorEvent()

    data class ProjectSheetOpen(val summary: ProjectSummary) : EditorEvent()

    data class ProjectDeleteRequested(val summary: ProjectSummary) : EditorEvent()

    data object ProjectDeleteCancelled : EditorEvent()

    data object ProjectDeleteConfirmed : EditorEvent()

    /** The screen opened the new Volume and closed this one. */
    data object SwitchHandled : EditorEvent()

    /** 中 / EN segment in the title bar. */
    data class LanguageSelected(val language: AppLanguage) : EditorEvent()

    data object NoticeShown : EditorEvent()

    data object RetrySave : EditorEvent()

    /** The window went to the background: flush any pending debounced save. */
    data object Paused : EditorEvent()

    /** The Volume is closing: save at once. */
    data object Closed : EditorEvent()
}
