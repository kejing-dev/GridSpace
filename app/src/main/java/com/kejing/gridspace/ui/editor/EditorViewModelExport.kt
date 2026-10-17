package com.kejing.gridspace.ui.editor

import androidx.lifecycle.viewModelScope
import com.kejing.gridspace.data.VoxExporter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The export sheet (design spec H): modal, with idle / exporting / done / failed phases. */
internal fun EditorViewModel.onExportEvent(event: EditorEvent) {
    when (event) {
        EditorEvent.ExportOpened ->
            _state.update { s ->
                val name = s.project?.name ?: return@update s
                // Opening the sheet closes the picker and drops any preview: the sheet is modal.
                s.copy(exportSheet = ExportState(fileName = VoxExporter.sanitize(name)), picker = null, preview = null)
            }
        is EditorEvent.ExportNameChanged ->
            _state.update { it.copy(exportSheet = it.exportSheet?.copy(fileName = event.name.take(MAX_NAME_LENGTH))) }
        EditorEvent.ExportConfirmed -> runExport()
        EditorEvent.ExportDismissed -> _state.update { it.copy(exportSheet = null) }
        else -> Unit
    }
}

private const val MAX_NAME_LENGTH = 60

private fun EditorViewModel.runExport() {
    val s = _state.value
    val sheet = s.exportSheet ?: return
    val project = s.project ?: return
    if (sheet.phase == ExportPhase.EXPORTING) return
    if (sheet.phase == ExportPhase.DONE) {
        _state.update { it.copy(exportSheet = null) }
        return
    }
    _state.update { it.copy(exportSheet = sheet.copy(phase = ExportPhase.EXPORTING, error = null)) }
    viewModelScope.launch {
        val result = runCatching { exporter.export(project.voxels, sheet.fileName) }
        _state.update { current ->
            val open = current.exportSheet ?: return@update current
            result.fold(
                onSuccess = { r ->
                    current.copy(exportSheet = open.copy(phase = ExportPhase.DONE, result = r), notice = EditorNotice.Exported(r.displayName))
                },
                onFailure = { e -> current.copy(exportSheet = open.copy(phase = ExportPhase.FAILED, error = e.message ?: e.javaClass.simpleName)) },
            )
        }
    }
}
