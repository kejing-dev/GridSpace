package com.kejing.gridspace.ui.entry

import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.theme.AppLanguage

/** Asks the screen to close the previous editor (if any) and open a project. */
data class OpenRequest(val open: String, val close: String?)

/** One-shot message for the entry window's Snack. */
sealed class EntryNotice {
    data class Deleted(val name: String) : EntryNotice()

    data object Corrupt : EntryNotice()
}

data class EntryUiState(
    val projects: List<ProjectSummary> = emptyList(),
    val gridSize: Int = VoxelGrid.DEFAULT_SIZE,
    /** Project id whose delete confirmation is showing. */
    val confirmDelete: ProjectSummary? = null,
    val openRequest: OpenRequest? = null,
    val notice: EntryNotice? = null,
)

sealed class EntryEvent {
    data class GridSizeSelected(val size: Int) : EntryEvent()

    data object NewProject : EntryEvent()

    data class ProjectClicked(val summary: ProjectSummary) : EntryEvent()

    data class DeleteRequested(val summary: ProjectSummary) : EntryEvent()

    data object DeleteCancelled : EntryEvent()

    data object DeleteConfirmed : EntryEvent()

    data object OpenHandled : EntryEvent()

    data object NoticeShown : EntryEvent()

    /** Demo mode: create (or refresh) the sample project and open it. */
    data class OpenSample(val name: String) : EntryEvent()

    /** 中 / EN segment in the title bar. */
    data class LanguageSelected(val language: AppLanguage) : EntryEvent()
}
