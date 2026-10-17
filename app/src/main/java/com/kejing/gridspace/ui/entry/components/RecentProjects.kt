package com.kejing.gridspace.ui.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.ui.common.ActionIcon
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.ListItem
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect

private const val MAX_ROWS = 20

/** Spec R4 M.3: rows at least 88 high, 56 dp colour tile (radius 12), 72 dp delete button. */
private val RowMinHeight = 88.dp
private val Tile = 56.dp
private val TileRadius = 12.dp
private val EditingWidth = 96.dp

/** Right column: label, then up to 20 project rows, or the empty-state line. */
@Composable
fun RecentProjects(
    projects: List<ProjectSummary>,
    onOpen: (ProjectSummary) -> Unit,
    onDelete: (ProjectSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(str(R.string.recent_projects), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(str(R.string.no_projects), style = GridType.body, color = PicoTheme.colorScheme.labelSecondary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                projects.take(MAX_ROWS).forEach { project ->
                    ProjectRow(project, editing = false, onOpen = { onOpen(project) }, onDelete = { onDelete(project) })
                }
            }
        }
    }
}

/**
 * One recent-project row, shared by the entry window and the editor's Projects sheet. [editing]
 * marks the project currently open (Mint "Editing" text, no delete button); [onDelete] null hides
 * the delete button. The whole row is clickable with the system hover effect.
 */
@Composable
fun ProjectRow(project: ProjectSummary, editing: Boolean, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    val meta =
        if (project.corrupt) str(R.string.file_corrupt)
        else str(R.string.project_meta, project.voxelCount, relativeTime(project.updatedAt))
    ListItem(
        headlineContent = { Text(project.name, style = GridType.label, maxLines = 1) },
        supportingContent = { Text(meta, style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1) },
        leadingContent = {
            // The dominant colour stands in for a thumbnail (thumbnails are v1.1). Near-white
            // colours get the same faint inner edge as the colour slots so they show on glass.
            val dominant = project.dominantColor
            val shape = RoundedCornerShape(TileRadius)
            Box(
                modifier =
                    Modifier.size(Tile)
                        .clip(shape)
                        .background(dominant?.let { Color(it) } ?: GridColors.EmptyProject)
                        .then(if (dominant != null && ColorMath.isNearWhite(dominant)) Modifier.border(1.dp, GridColors.BaseInk.copy(alpha = 0.3f), shape) else Modifier)
            )
        },
        trailingContent = {
            when {
                editing -> Text(str(R.string.editing_now), style = GridType.captionMedium, color = GridColors.Mint, maxLines = 1, modifier = Modifier.width(EditingWidth))
                onDelete != null -> ActionIcon(icon = R.drawable.ic_trash, label = str(R.string.delete_project), onClick = onDelete, transparent = true)
            }
        },
        // A minimum, not a fixed height: the SDK ListItem adds its own padding around the two text
        // lines, and a fixed height clipped the meta line (QA D-02).
        modifier = Modifier.fillMaxWidth().heightIn(min = RowMinHeight).spatialHoverEffect().clickable(enabled = !editing, onClick = onOpen),
    )
}

/** "2 h ago" style text for the list; coarse on purpose. */
@Composable
fun relativeTime(updatedAt: Long): String {
    val delta = (System.currentTimeMillis() - updatedAt).coerceAtLeast(0L)
    val minutes = delta / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> str(R.string.time_just_now)
        minutes < 60 -> str(R.string.time_minutes, minutes.toInt())
        hours < 24 -> str(R.string.time_hours, hours.toInt())
        days < 2 -> str(R.string.time_yesterday)
        else -> str(R.string.time_days, days.toInt())
    }
}
