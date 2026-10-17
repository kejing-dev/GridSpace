package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.common.PrimaryButton
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.ProjectSheetState
import com.kejing.gridspace.ui.entry.components.DeleteDialog
import com.kejing.gridspace.ui.entry.components.ProjectRow
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.Sheet

/**
 * Spec R4 S.7: the SDK Sheet has a fixed content width (about 410 dp measured, QA N4-04), so the
 * content fills it and the grid-size items share it by weight; six 88 dp rows visible (548).
 */
private val ListMaxHeight = 548.dp
private val RowHeight = 88.dp

/**
 * The Projects sheet opened from the toolbar (branch A): new project, grid size, recent list with
 * the current project marked "Editing" (no delete button on it), delete elsewhere.
 */
@Composable
fun ProjectSheet(sheet: ProjectSheetState, currentId: String, recent: List<ProjectSummary>, onEvent: (EditorEvent) -> Unit) {
    Sheet(
        onDismissRequest = { onEvent(EditorEvent.ProjectSheetDismissed) },
        title = { Text(str(R.string.projects), style = GridType.title) },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrimaryButton(
                label = str(R.string.new_project),
                icon = R.drawable.ic_plus,
                onClick = { onEvent(EditorEvent.ProjectSheetNew) },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(str(R.string.grid_size), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
                SegmentControl(
                    modifier = Modifier.fillMaxWidth().height(GridSizes.SegmentHeight),
                    itemSpace = GridSizes.SegmentGap,
                    contentPadding = GridSizes.SegmentPadding,
                ) {
                    VoxelGrid.SIZES.forEach { size ->
                        SegmentItem(
                            selected = size == sheet.gridSize,
                            onClick = { onEvent(EditorEvent.ProjectSheetGridSize(size)) },
                            modifier = Modifier.weight(1f).height(GridSizes.SegmentItemHeight),
                            title = { Text(size.toString(), style = GridType.bodyMedium, maxLines = 1) },
                        )
                    }
                }
            }
            Text(str(R.string.recent_projects), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary, modifier = Modifier.height(22.dp))
            val others = recent.filter { it.id != currentId }
            val current = recent.firstOrNull { it.id == currentId }
            if (current == null && others.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(RowHeight), contentAlignment = Alignment.Center) {
                    Text(str(R.string.no_other_projects), style = GridType.body, color = PicoTheme.colorScheme.labelSecondary)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = ListMaxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    current?.let { ProjectRow(it, editing = true, onOpen = {}, onDelete = null) }
                    others.forEach { project ->
                        ProjectRow(
                            project,
                            editing = false,
                            onOpen = { onEvent(EditorEvent.ProjectSheetOpen(project)) },
                            onDelete = { onEvent(EditorEvent.ProjectDeleteRequested(project)) },
                        )
                    }
                    if (others.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(RowHeight), contentAlignment = Alignment.Center) {
                            Text(str(R.string.no_other_projects), style = GridType.body, color = PicoTheme.colorScheme.labelSecondary)
                        }
                    }
                }
            }
        }
    }
    sheet.confirmDelete?.let { target ->
        DeleteDialog(
            name = target.name,
            onCancel = { onEvent(EditorEvent.ProjectDeleteCancelled) },
            onConfirm = { onEvent(EditorEvent.ProjectDeleteConfirmed) },
        )
    }
}
