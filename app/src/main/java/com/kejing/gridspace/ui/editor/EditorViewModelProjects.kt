package com.kejing.gridspace.ui.editor

import androidx.lifecycle.viewModelScope
import com.kejing.gridspace.domain.model.Palette
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.domain.model.VoxelGrid
import java.util.UUID
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Projects sheet (spec R4 N.6.A): new project / grid size / recent list / delete. Switching
 * project is "open the new Volume, then close this one" so the app never has zero windows; the
 * screen performs the navigation from [EditorUiState.switchRequest].
 */
internal fun EditorViewModel.onProjectEvent(event: EditorEvent) {
    when (event) {
        EditorEvent.ProjectSheetOpened ->
            _state.update { s ->
                // Modal like the export sheet: closes the colour popup and drops any preview.
                s.copy(projectSheet = ProjectSheetState(gridSize = s.project?.gridSize ?: VoxelGrid.DEFAULT_SIZE), picker = null, preview = null)
            }
        EditorEvent.ProjectSheetDismissed -> _state.update { it.copy(projectSheet = null) }
        is EditorEvent.ProjectSheetGridSize -> _state.update { it.copy(projectSheet = it.projectSheet?.copy(gridSize = event.size)) }
        EditorEvent.ProjectSheetNew -> createAndSwitch()
        is EditorEvent.ProjectSheetOpen -> openOther(event.summary)
        is EditorEvent.ProjectDeleteRequested ->
            _state.update { it.copy(projectSheet = it.projectSheet?.copy(confirmDelete = event.summary)) }
        EditorEvent.ProjectDeleteCancelled -> _state.update { it.copy(projectSheet = it.projectSheet?.copy(confirmDelete = null)) }
        EditorEvent.ProjectDeleteConfirmed -> deleteConfirmed()
        EditorEvent.SwitchHandled -> _state.update { it.copy(switchRequest = null, projectSheet = null) }
        else -> Unit
    }
}

private fun EditorViewModel.createAndSwitch() {
    val s = _state.value
    val sheet = s.projectSheet ?: return
    val catalog = catalog ?: return
    val now = System.currentTimeMillis()
    val project =
        Project(
            id = UUID.randomUUID().toString(),
            name = untitledName(catalog.nextUntitledNumber(untitledPattern)),
            gridSize = sheet.gridSize,
            voxels = VoxelGrid(sheet.gridSize),
            slots = Palette.DEFAULT_SLOTS,
            createdAt = now,
            updatedAt = now,
        )
    viewModelScope.launch {
        runCatching { repository.save(project) }
        requestSwitch(project.id)
    }
}

private fun EditorViewModel.openOther(summary: ProjectSummary) {
    val current = _state.value.project ?: return
    if (summary.id == current.id || summary.corrupt) return
    requestSwitch(summary.id)
}

/** Saves this project at once (the Volume is about to close) and asks the screen to navigate. */
private fun EditorViewModel.requestSwitch(newId: String) {
    val current = _state.value.project ?: return
    saveOnClose()
    _state.update { it.copy(switchRequest = SwitchRequest(open = newId, close = current.id)) }
}

private fun EditorViewModel.deleteConfirmed() {
    val target = _state.value.projectSheet?.confirmDelete ?: return
    val catalog = catalog ?: return
    _state.update { it.copy(projectSheet = it.projectSheet?.copy(confirmDelete = null)) }
    viewModelScope.launch {
        catalog.delete(target.id)
        _state.update { it.copy(notice = EditorNotice.Deleted(target.name)) }
    }
}
