package com.kejing.gridspace.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kejing.gridspace.data.AppGraph
import com.kejing.gridspace.data.Exporter
import com.kejing.gridspace.data.ProjectCatalog
import com.kejing.gridspace.data.PanelMemory
import com.kejing.gridspace.data.ProjectStore
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.SaveState
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.BoxFill
import com.kejing.gridspace.domain.usecase.SizedBrush
import com.kejing.gridspace.domain.usecase.EditCommand
import com.kejing.gridspace.domain.usecase.History
import com.kejing.gridspace.domain.usecase.SelectionOps
import com.kejing.gridspace.domain.usecase.SelectionResult
import com.kejing.gridspace.ui.theme.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * All editing goes through [onEvent]; the demo script and the real gestures use the same entry.
 * The history holds the undo/redo stacks; the grid in [EditorUiState.project] is always the
 * current one. Colour handling lives in EditorViewModelColour.kt, export in EditorViewModelExport.kt
 * and the Projects sheet in EditorViewModelProjects.kt.
 */
class EditorViewModel(
    private val projectId: String,
    internal val repository: ProjectStore,
    internal val exporter: Exporter,
    /** Recent list for the Projects sheet; null in unit tests that never open it. */
    internal val catalog: ProjectCatalog? = null,
    /** 中 / EN switch sink (AppGraph in the app). */
    private val onLanguage: (AppLanguage) -> Unit = {},
    /** Produces "Untitled k" in the current language; injected so the ViewModel stays resource-free. */
    internal val untitledName: (Int) -> String = { "Untitled $it" },
    internal val untitledPattern: Regex = Regex("Untitled (\\d+)"),
    /** R10: session memory of the toolbar panel toggles (AppGraph.panels in the app). */
    private val panels: PanelMemory = PanelMemory(),
) : ViewModel() {

    internal val _state = MutableStateFlow(EditorUiState(showSizeRow = panels.showSizeRow, showColorRail = panels.showColorRail))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val history = History()
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val project = repository.load(projectId)
            _state.update { it.copy(project = project, loadFailed = project == null) }
        }
        catalog?.let { c -> viewModelScope.launch { c.summaries.collect { list -> _state.update { it.copy(recent = list) } } } }
    }

    fun onEvent(event: EditorEvent) {
        if (_state.value.project == null) return
        when (event) {
            is EditorEvent.ModeSelected -> selectMode(event.mode)
            is EditorEvent.SizeStep -> changeBrushSize(event)
            EditorEvent.ToggleSizeRow -> {
                val show = !_state.value.showSizeRow
                panels.showSizeRow = show
                _state.update { it.copy(showSizeRow = show) }
            }
            EditorEvent.ToggleColorRail -> {
                val show = !_state.value.showColorRail
                panels.showColorRail = show
                // Hiding the rail also closes the colour-edit card / eyedropper it hosts (spec T.8).
                _state.update { it.copy(showColorRail = show, picker = if (show) it.picker else null) }
            }
            is EditorEvent.SceneTapped -> if (!_state.value.sceneLocked) tapped(event.hit)
            is EditorEvent.PreviewStarted -> if (!_state.value.sceneLocked) previewStarted(event.hit)
            is EditorEvent.PreviewMoved -> if (!_state.value.sceneLocked) previewMoved(event.hit)
            EditorEvent.Committed -> commitPreview()
            EditorEvent.PreviewCancelled -> clearPreview()
            is EditorEvent.SelectionDragStarted -> if (!_state.value.sceneLocked) dragStarted(event.cell)
            is EditorEvent.SelectionDragMoved -> dragMoved(event.delta)
            EditorEvent.SelectionDragEnded -> dragEnded()
            EditorEvent.SelectionDragCancelled -> if (_state.value.dragging) clearPreview()
            EditorEvent.Undo -> undo()
            EditorEvent.Redo -> redo()
            EditorEvent.ResetView -> _state.update { it.copy(resetToken = it.resetToken + 1) }
            EditorEvent.SelectionRotate -> selectionOp { grid, sel -> SelectionOps.rotate90(grid, sel) }
            EditorEvent.SelectionDuplicate -> selectionOp { grid, sel -> SelectionOps.duplicate(grid, sel) }
            EditorEvent.SelectionDelete -> selectionOp { grid, sel -> SelectionOps.delete(grid, sel) }
            is EditorEvent.SelectionStep -> step(event)
            EditorEvent.SelectionClear -> _state.update { it.copy(selection = emptySet(), translation = Cell.ZERO) }
            is EditorEvent.LanguageSelected -> onLanguage(event.language)
            EditorEvent.NoticeShown -> _state.update { it.copy(notice = null) }
            EditorEvent.RetrySave -> saveNow()
            EditorEvent.Paused -> flushPendingSave()
            EditorEvent.Closed -> saveOnClose()
            is EditorEvent.SlotTapped, is EditorEvent.PresetSelected, is EditorEvent.SwatchPicked, is EditorEvent.HslChanged,
            EditorEvent.ColorEditOpened, EditorEvent.EyedropperStarted, EditorEvent.EyedropperCancelled, EditorEvent.PickerDismissed -> onColourEvent(event)
            EditorEvent.ExportOpened, is EditorEvent.ExportNameChanged, EditorEvent.ExportConfirmed,
            EditorEvent.ExportDismissed -> onExportEvent(event)
            EditorEvent.ProjectSheetOpened, EditorEvent.ProjectSheetDismissed, is EditorEvent.ProjectSheetGridSize,
            EditorEvent.ProjectSheetNew, is EditorEvent.ProjectSheetOpen, is EditorEvent.ProjectDeleteRequested,
            EditorEvent.ProjectDeleteCancelled, EditorEvent.ProjectDeleteConfirmed, EditorEvent.SwitchHandled -> onProjectEvent(event)
        }
    }

    // ---- modes and taps ------------------------------------------------------------------------

    private fun selectMode(mode: EditMode) {
        if (!_state.value.modesEnabled) return
        // Switching mode closes the colour popup and drops any half-finished preview.
        _state.update { it.copy(mode = mode, picker = null, preview = null) }
    }

    /** R8: one axis of the brush size, clamped to 1..min(16, grid size). */
    private fun changeBrushSize(event: EditorEvent.SizeStep) {
        _state.update { s ->
            val gridSize = s.project?.gridSize ?: VoxelGrid.DEFAULT_SIZE
            val value = s.brushSize[event.axis] + event.delta
            s.copy(brushSize = s.brushSize.with(event.axis, value).clamped(gridSize))
        }
    }

    private fun tapped(hit: SceneHit) {
        val s = _state.value
        val project = s.project ?: return
        val grid = project.voxels
        if (s.eyedropperActive) {
            if (hit is SceneHit.Voxel) grid[hit.cell]?.let { pickFromCube(it) }
            return
        }
        val size = s.effectiveBrushSize
        when (val target = HitResolver.resolve(hit, s.mode, grid)) {
            // Sized brush (R8): a tap commits the whole W x H x D block at once.
            is Resolved.Empty ->
                if (s.mode == EditMode.ADD) {
                    if (size.isUnit) commit(EditCommand.add(grid, target.cell, project.activeColor))
                    else commit(SizedBrush.fill(grid, target.cell, size, project.activeColor))
                }
            is Resolved.Occupied -> flashOccupied(target.cell)
            is Resolved.Cube ->
                when (s.mode) {
                    EditMode.ERASE ->
                        if (size.isUnit) commit(EditCommand.erase(grid, target.cell))
                        else commit(SizedBrush.erase(grid, target.cell, size))
                    EditMode.PAINT -> commit(EditCommand.paint(grid, target.cell, project.activeColor))
                    EditMode.SELECT -> toggleSelection(target.cell)
                    EditMode.ADD -> Unit
                }
            Resolved.Outside, Resolved.Nothing -> Unit
        }
    }

    private fun toggleSelection(cell: Cell) {
        _state.update {
            val next = if (cell in it.selection) it.selection - cell else it.selection + cell
            it.copy(selection = next, translation = if (next.isEmpty()) Cell.ZERO else it.translation)
        }
    }

    /** Release on an occupied cell: coral wireframe for 120 ms, no history step. */
    private fun flashOccupied(cell: Cell) {
        _state.update { it.copy(preview = Preview.Ghost(cell, occupied = true)) }
        viewModelScope.launch {
            delay(OCCUPIED_FLASH_MS)
            _state.update { if (it.preview == Preview.Ghost(cell, true)) it.copy(preview = null) else it }
        }
    }

    // ---- pinch-hold preview --------------------------------------------------------------------

    private fun previewStarted(hit: SceneHit) {
        val s = _state.value
        val grid = s.project?.voxels ?: return
        if (s.eyedropperActive) return
        val target = HitResolver.resolve(hit, s.mode, grid)
        _state.update { it.copy(preview = previewFor(target, s)) }
    }

    /** A pinch-drag in ADD / ERASE only moves the ghost block with the target (R8; the box drag is gone). */
    private fun previewMoved(hit: SceneHit) {
        val s = _state.value
        val grid = s.project?.voxels ?: return
        if (s.dragging) return
        if (s.preview == null) return
        val target = HitResolver.resolve(hit, s.mode, grid)
        val next = previewFor(target, s)
        // Keep the last valid cell as the label anchor when the target leaves the grid.
        val keepAnchor = if (next is Preview.Outside && next.anchor == null) Preview.Outside(s.preview?.anchorCell) else next
        _state.update { it.copy(preview = keepAnchor ?: it.preview) }
    }

    private fun previewFor(target: Resolved, s: EditorUiState): Preview? {
        val size = s.effectiveBrushSize
        val gridSize = s.project?.gridSize ?: VoxelGrid.DEFAULT_SIZE
        // Sized brush (R8): the block is recomputed from the target every frame; 1 x 1 x 1 keeps
        // the single-cell previews and their "aligned" label.
        return when (target) {
            is Resolved.Empty ->
                if (s.mode == EditMode.ADD && !size.isUnit) Preview.BlockAdd(SizedBrush.region(target.cell, size, gridSize), s.project?.activeColor ?: 0)
                else Preview.Ghost(target.cell, occupied = false)
            is Resolved.Occupied ->
                if (s.mode == EditMode.ADD && !size.isUnit) Preview.BlockAdd(SizedBrush.region(target.cell, size, gridSize), s.project?.activeColor ?: 0, blocked = true)
                else Preview.Ghost(target.cell, occupied = true)
            Resolved.Outside -> Preview.Outside(null)
            is Resolved.Cube ->
                when (s.mode) {
                    EditMode.ERASE ->
                        if (!size.isUnit) Preview.BlockErase(SizedBrush.region(target.cell, size, gridSize))
                        else Preview.EraseCell(target.cell)
                    EditMode.PAINT -> Preview.PaintCell(target.cell, s.project?.activeColor ?: 0)
                    EditMode.SELECT -> Preview.SelectCell(target.cell)
                    EditMode.ADD -> null
                }
            Resolved.Nothing -> null
        }
    }

    private fun commitPreview() {
        val s = _state.value
        val project = s.project ?: return
        val grid = project.voxels
        when (val p = s.preview) {
            is Preview.Ghost -> if (p.occupied) flashOccupied(p.cell) else commit(EditCommand.add(grid, p.cell, project.activeColor))
            is Preview.EraseCell -> commit(EditCommand.erase(grid, p.cell))
            is Preview.PaintCell -> commit(EditCommand.paint(grid, p.cell, project.activeColor))
            is Preview.SelectCell -> toggleSelection(p.cell)
            is Preview.BlockAdd ->
                if (p.blocked) flashOccupied(p.region.min)
                else commit(BoxFill.fill(grid, p.region, project.activeColor), large = p.region.cellCount > BoxFill.LARGE_THRESHOLD)
            is Preview.BlockErase -> commit(BoxFill.erase(grid, p.region))
            is Preview.MoveSelection -> {
                dragEnded()
                return
            }
            is Preview.Outside, null -> Unit
        }
        if (s.preview !is Preview.Ghost || !(s.preview as Preview.Ghost).occupied) clearPreview()
    }

    private fun clearPreview() {
        _state.update { it.copy(preview = null) }
    }

    // ---- selection drag-move (spec R4 O.2) -----------------------------------------------------

    /** A drag began on a selected cube: the whole selection previews a move of zero cells. */
    private fun dragStarted(cell: Cell) {
        val s = _state.value
        if (s.mode != EditMode.SELECT || cell !in s.selection || s.eyedropperActive) return
        _state.update { it.copy(preview = Preview.MoveSelection(it.selection, Cell.ZERO, it.selection, emptySet())) }
    }

    /** The view reports whole-cell deltas; they are clamped again here for scripted callers. */
    private fun dragMoved(delta: Cell) {
        val s = _state.value
        val grid = s.project?.voxels ?: return
        val current = s.preview as? Preview.MoveSelection ?: return
        val clamped = SelectionOps.clampDelta(current.origin, delta, grid.size)
        if (clamped == current.delta) return
        val targets = SelectionOps.targets(current.origin, clamped)
        _state.update { it.copy(preview = current.copy(delta = clamped, targets = targets, overwritten = SelectionOps.overwritten(grid, current.origin, targets))) }
    }

    /** Release: one history step `SelectionMoved(delta)`; the new cells become the selection. */
    private fun dragEnded() {
        val s = _state.value
        val grid = s.project?.voxels ?: return
        val current = s.preview as? Preview.MoveSelection ?: return
        clearPreview()
        if (!current.moving) return
        when (val result = SelectionOps.translate(grid, current.origin, current.delta)) {
            is SelectionResult.Done -> {
                commit(result.command)
                _state.update { it.copy(selection = result.selection, translation = it.translation + current.delta) }
            }
            else -> Unit
        }
    }

    // ---- history / selection -------------------------------------------------------------------

    /** One history step: applies the command, bumps the commit token and schedules the auto-save. */
    internal fun commit(command: EditCommand.CellsChange?, large: Boolean = false) {
        if (command == null || command.isEmpty) return
        _state.update { s ->
            val project = s.project ?: return@update s
            val next = history.commit(project.voxels, command)
            s.copy(
                project = project.copy(voxels = next, updatedAt = System.currentTimeMillis()),
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                commitToken = s.commitToken + 1,
                lastCommitted = command.changedCells,
                largeFill = large,
            )
        }
        scheduleSave()
    }

    private fun undo() = stepHistory { history.undo(it) }

    private fun redo() = stepHistory { history.redo(it) }

    private fun stepHistory(move: (grid: VoxelGrid) -> VoxelGrid) {
        _state.update { s ->
            val project = s.project ?: return@update s
            s.copy(
                project = project.copy(voxels = move(project.voxels), updatedAt = System.currentTimeMillis()),
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                selection = emptySet(),
                translation = Cell.ZERO,
                preview = null,
                commitToken = s.commitToken + 1,
                lastCommitted = emptySet(),
            )
        }
        scheduleSave()
    }

    private fun selectionOp(op: (grid: VoxelGrid, selection: Set<Cell>) -> SelectionResult) {
        val s = _state.value
        val grid = s.project?.voxels ?: return
        if (s.selection.isEmpty() || s.dragging) return
        when (val result = op(grid, s.selection)) {
            is SelectionResult.Done -> {
                commit(result.command)
                _state.update { it.copy(selection = result.selection, translation = if (result.selection.isEmpty()) Cell.ZERO else it.translation) }
            }
            SelectionResult.OutOfBounds -> _state.update { it.copy(notice = EditorNotice.RotateOutOfGrid) }
            SelectionResult.NoRoom -> _state.update { it.copy(notice = EditorNotice.NoRoom) }
        }
    }

    private fun step(event: EditorEvent.SelectionStep) {
        val s = _state.value
        val grid = s.project?.voxels ?: return
        // Stepper taps during a drag-move are ignored until the drag ends (spec R4 O.2.4).
        if (s.dragging) return
        val key = StepKey(event.axis, event.delta)
        if (s.blockedStep == key) return
        val delta = event.axis.unit(event.delta)
        when (val result = SelectionOps.translate(grid, s.selection, delta)) {
            is SelectionResult.Done -> {
                commit(result.command)
                _state.update { it.copy(selection = result.selection, translation = it.translation + delta) }
            }
            // Out of bounds: nothing moves, no snack; the button is disabled for 100 ms (spec E.4).
            else -> blockStep(key)
        }
    }

    private fun blockStep(key: StepKey) {
        _state.update { it.copy(blockedStep = key) }
        viewModelScope.launch {
            delay(STEP_BLOCK_MS)
            _state.update { if (it.blockedStep == key) it.copy(blockedStep = null) else it }
        }
    }

    // ---- persistence ---------------------------------------------------------------------------

    internal fun scheduleSave() {
        _state.update { it.copy(saveState = SaveState.SAVING) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private fun saveNow() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { persist() }
    }

    /**
     * Writes the current project. The write itself is non-cancellable (a started save always
     * completes), and a cancellation is propagated instead of being reported: only a real IO
     * failure turns the dot coral (QA D-01). A save that a newer commit cancelled while it was
     * writing still finishes its (older) snapshot but must not report SAVED: the newer commit's
     * own save is still pending (QA N-02), so the state stays SAVING until that one completes.
     */
    private suspend fun persist() {
        val project = _state.value.project ?: return
        val result = withContext(NonCancellable) { runCatching { repository.save(project) } }
        val error = result.exceptionOrNull()
        if (error is CancellationException) throw error
        coroutineContext.ensureActive()
        val ok = error == null
        _state.update { it.copy(saveState = if (ok) SaveState.SAVED else SaveState.FAILED, notice = if (ok) it.notice else EditorNotice.SaveFailed) }
    }

    /** onPause / onStop: a pending debounce is flushed right away (spec G.3). */
    private fun flushPendingSave() {
        val pending = saveJob?.isActive == true || _state.value.saveState == SaveState.SAVING
        if (pending) saveNow()
    }

    /** The window is going away: write on an app-level scope so cancellation cannot drop it. */
    internal fun saveOnClose() {
        val project = _state.value.project ?: return
        saveJob?.cancel()
        closeScope.launch { runCatching { repository.save(project) } }
    }

    fun project(): Project? = _state.value.project

    class Factory(
        private val projectId: String,
        private val untitledName: (Int) -> String,
        private val untitledPattern: Regex,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(
                projectId = projectId,
                repository = AppGraph.projects,
                exporter = AppGraph.exporter,
                catalog = AppGraph.projects,
                onLanguage = AppGraph::selectLanguage,
                untitledName = untitledName,
                untitledPattern = untitledPattern,
                panels = AppGraph.panels,
            ) as T
    }

    companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 1_000L
        const val OCCUPIED_FLASH_MS = 120L
        const val STEP_BLOCK_MS = 100L
        internal val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
