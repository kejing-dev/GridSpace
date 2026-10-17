package com.kejing.gridspace.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kejing.gridspace.data.AppGraph
import com.kejing.gridspace.data.EditorSession
import com.kejing.gridspace.data.ProjectRepository
import com.kejing.gridspace.domain.model.Palette
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.SampleProject
import com.kejing.gridspace.ui.theme.AppLanguage
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Entry window state. The editor id currently open lives in the shared [EditorSession] so opening
 * another project closes the previous Volume first (one editor at a time, design spec B.1), and a
 * Volume the user closed with the system button no longer counts as open (QA D-16).
 */
class EntryViewModel(
    private val repository: ProjectRepository,
    private val session: EditorSession,
    /** Produces "Untitled k" in the current language; injected so the ViewModel stays resource-free. */
    private val untitledName: (Int) -> String,
    private val untitledPattern: Regex,
    private val onLanguage: (AppLanguage) -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(EntryUiState())
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.summaries.collect { list -> _state.update { it.copy(projects = list) } } }
        viewModelScope.launch { repository.refresh() }
    }

    fun onEvent(event: EntryEvent) {
        when (event) {
            is EntryEvent.GridSizeSelected -> _state.update { it.copy(gridSize = event.size) }
            EntryEvent.NewProject -> createProject()
            is EntryEvent.ProjectClicked -> openProject(event.summary)
            is EntryEvent.DeleteRequested -> _state.update { it.copy(confirmDelete = event.summary) }
            EntryEvent.DeleteCancelled -> _state.update { it.copy(confirmDelete = null) }
            EntryEvent.DeleteConfirmed -> deleteConfirmed()
            EntryEvent.OpenHandled -> _state.update { it.copy(openRequest = null) }
            EntryEvent.NoticeShown -> _state.update { it.copy(notice = null) }
            is EntryEvent.OpenSample -> openSample(event.name)
            is EntryEvent.LanguageSelected -> onLanguage(event.language)
        }
    }

    private fun createProject() {
        val size = _state.value.gridSize
        viewModelScope.launch {
            // The index may still be loading right after start-up: refresh before numbering.
            runCatching { repository.refresh() }
            val now = System.currentTimeMillis()
            val project =
                Project(
                    id = UUID.randomUUID().toString(),
                    name = untitledName(repository.nextUntitledNumber(untitledPattern)),
                    gridSize = size,
                    voxels = VoxelGrid(size),
                    slots = Palette.DEFAULT_SLOTS,
                    createdAt = now,
                    updatedAt = now,
                )
            runCatching { repository.save(project) }
            requestOpen(project.id)
        }
    }

    private fun openProject(summary: ProjectSummary) {
        if (summary.corrupt) {
            _state.update { it.copy(notice = EntryNotice.Corrupt) }
            return
        }
        requestOpen(summary.id)
    }

    private fun openSample(name: String) {
        viewModelScope.launch {
            // The sample is rebuilt on every demo launch so recordings always start from the same model.
            runCatching { repository.save(SampleProject.build(name, System.currentTimeMillis())) }
            requestOpen(SampleProject.ID)
        }
    }

    private fun requestOpen(id: String) {
        val previous = session.openProjectId.value?.takeIf { it != id }
        session.opened(id)
        _state.update { it.copy(openRequest = OpenRequest(open = id, close = previous)) }
    }

    private fun deleteConfirmed() {
        val target = _state.value.confirmDelete ?: return
        _state.update { it.copy(confirmDelete = null) }
        viewModelScope.launch {
            repository.delete(target.id)
            session.closed(target.id)
            _state.update { it.copy(notice = EntryNotice.Deleted(target.name)) }
        }
    }

    class Factory(private val untitledName: (Int) -> String, private val untitledPattern: Regex) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EntryViewModel(AppGraph.projects, AppGraph.editorSession, untitledName, untitledPattern, AppGraph::selectLanguage) as T
    }
}
