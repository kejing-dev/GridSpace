package com.kejing.gridspace.ui.entry

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kejing.gridspace.R
import com.kejing.gridspace.data.AppGraph
import com.kejing.gridspace.ui.common.LanguageSegment
import com.kejing.gridspace.ui.common.settleRedraw
import com.kejing.gridspace.ui.entry.components.DeleteDialog
import com.kejing.gridspace.ui.entry.components.NewProjectColumn
import com.kejing.gridspace.ui.entry.components.RecentProjects
import com.kejing.gridspace.ui.navigation.DemoMode
import com.kejing.gridspace.ui.navigation.DemoScript
import com.kejing.gridspace.ui.navigation.WindowIds
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.appString
import com.kejing.gridspace.ui.theme.str
import com.kejing.gridspace.ui.theme.untitledPattern
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TitleBar
import com.pico.spatial.ui.design.windows.LocalSnackbarHostState
import com.pico.spatial.ui.design.windows.SnackbarHost
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

private val ContentPadding = 32.dp
/** Spec R4 N.6.A: left column 440, gap 32, right column takes the rest of the 1200 dp window. */
private val LeftColumnWidth = 440.dp
private const val TAG = "GridSpaceEntry"

/**
 * W1: new project, recent projects, first-use steps, 中/EN. Opens the editor Volume and then
 * (branch A, spec R4 N.6.A) closes itself; the editor reopens it when the last Volume closes.
 */
@Composable
fun EntryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: EntryViewModel =
        viewModel(
            factory =
                EntryViewModel.Factory(
                    untitledName = { context.appString(R.string.untitled, it) },
                    untitledPattern = context.untitledPattern(),
                )
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = AppGraph.language.collectAsStateWithLifecycle()
    val navigator = LocalSpatialNavigator.current

    // Demo recording only; returns at once unless the emulator setting is present. Guarded once
    // per process: reopening the entry window creates a fresh LaunchActivity and ViewModel, so
    // this effect runs again after every editor close (R4 probe caveat).
    LaunchedEffect(Unit) {
        val script = DemoMode.script(context) ?: return@LaunchedEffect
        if (!AppGraph.demoAutoOpened.compareAndSet(false, true)) return@LaunchedEffect
        when (script) {
            DemoScript.NEWPROJECT -> viewModel.onEvent(EntryEvent.NewProject)
            else -> viewModel.onEvent(EntryEvent.OpenSample(context.appString(R.string.sample_project_name)))
        }
    }

    LaunchedEffect(state.openRequest) {
        val request = state.openRequest ?: return@LaunchedEffect
        request.close?.let { navigator.closeWindowContainer(WindowIds.EDITOR_VOLUME, WindowIds.editorTag(it)) }
        navigator.openWindowContainer(
            WindowIds.EDITOR_VOLUME,
            tag = WindowIds.editorTag(request.open),
            bundle = Bundle().apply { putString(WindowIds.ARG_PROJECT_ID, request.open) },
        )
        viewModel.onEvent(EntryEvent.OpenHandled)
        // Open first, then close: the app never has zero windows (spec R4 N.6.A).
        runCatching { navigator.closeWindowContainer(WindowIds.ENTRY_WINDOW, null) }
            .onFailure { Log.w(TAG, "closing the entry window failed", it) }
    }

    SnackbarHost {
        EntryContent(
            state = state,
            language = language,
            onEvent = viewModel::onEvent,
            modifier = modifier,
        )
    }
}

@Composable
internal fun EntryContent(
    state: EntryUiState,
    language: androidx.compose.runtime.State<com.kejing.gridspace.ui.theme.AppLanguage>,
    onEvent: (EntryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = LocalSnackbarHostState.current
    val notice = state.notice
    val noticeText =
        when (notice) {
            is EntryNotice.Deleted -> str(R.string.deleted_snack, notice.name)
            EntryNotice.Corrupt -> str(R.string.corrupt_snack)
            null -> null
        }
    LaunchedEffect(notice) {
        if (noticeText != null) {
            onEvent(EntryEvent.NoticeShown)
            snackbar.show(message = noticeText)
        }
    }

    // settleRedraw: on a cold start the emulator shows the window only partly drawn until a redraw (R5).
    Column(modifier = modifier.fillMaxSize().settleRedraw()) {
        TitleBar(
            title = { Text(str(R.string.entry_title), style = GridType.title, maxLines = 1) },
            trailingActions = { LanguageSegment(language = language, onSelect = { onEvent(EntryEvent.LanguageSelected(it)) }) },
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(start = ContentPadding, end = ContentPadding, bottom = ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(ContentPadding),
        ) {
            NewProjectColumn(
                gridSize = state.gridSize,
                onGridSize = { onEvent(EntryEvent.GridSizeSelected(it)) },
                onNewProject = { onEvent(EntryEvent.NewProject) },
                modifier = Modifier.width(LeftColumnWidth),
            )
            RecentProjects(
                projects = state.projects,
                onOpen = { onEvent(EntryEvent.ProjectClicked(it)) },
                onDelete = { onEvent(EntryEvent.DeleteRequested(it)) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }

    state.confirmDelete?.let { target ->
        DeleteDialog(
            name = target.name,
            onCancel = { onEvent(EntryEvent.DeleteCancelled) },
            onConfirm = { onEvent(EntryEvent.DeleteConfirmed) },
        )
    }
}
