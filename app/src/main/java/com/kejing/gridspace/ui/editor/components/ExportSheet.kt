package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.common.PrimaryButton
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.ExportPhase
import com.kejing.gridspace.ui.editor.ExportState
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.CircularProgressIndicator
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextField
import com.pico.spatial.ui.design.windows.Sheet

/**
 * Spec R4 S.7: the SDK Sheet has a fixed content width (about 410 dp measured, QA N4-04), so the
 * content fills it; format item 64 in a 72 container, file name 72, button 80.
 */
private val FieldHeight = 72.dp
private val ResultIcon = 56.dp

/** Modal export form (design spec H) with its exporting, done and failed states. */
@Composable
fun ExportSheet(export: ExportState, tooManyColours: Boolean, onEvent: (EditorEvent) -> Unit) {
    Sheet(
        onDismissRequest = { if (export.phase != ExportPhase.EXPORTING) onEvent(EditorEvent.ExportDismissed) },
        title = { Text(str(R.string.export), style = GridType.title) },
        bottom = {
            val label =
                when (export.phase) {
                    ExportPhase.IDLE -> R.string.export_to_device
                    ExportPhase.EXPORTING -> R.string.exporting
                    ExportPhase.DONE -> R.string.done
                    ExportPhase.FAILED -> R.string.retry
                }
            PrimaryButton(
                label = str(label),
                onClick = { onEvent(EditorEvent.ExportConfirmed) },
                enabled = export.phase != ExportPhase.EXPORTING,
                modifier = Modifier.fillMaxWidth(),
                leading =
                    if (export.phase == ExportPhase.EXPORTING) {
                        { CircularProgressIndicator(modifier = Modifier.size(GridSizes.ButtonIcon)) }
                    } else {
                        null
                    },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            when (export.phase) {
                ExportPhase.DONE -> DoneContent(export)
                ExportPhase.FAILED -> FailedContent(export)
                else -> FormContent(export, tooManyColours, onEvent)
            }
        }
    }
}

@Composable
private fun FormContent(export: ExportState, tooManyColours: Boolean, onEvent: (EditorEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(str(R.string.export_format), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
        // v1 has one format; PNG arrives in v1.1 (a SegmentItem cannot be disabled, so it is not listed).
        SegmentControl(
            modifier = Modifier.fillMaxWidth().height(GridSizes.SegmentHeight),
            itemSpace = GridSizes.SegmentGap,
            contentPadding = GridSizes.SegmentPadding,
        ) {
            SegmentItem(
                selected = true,
                onClick = {},
                modifier = Modifier.weight(1f).height(GridSizes.SegmentItemHeight),
                title = { Text(str(R.string.export_vox), style = GridType.bodyMedium, maxLines = 1) },
            )
        }
        Text(str(R.string.export_format_hint), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(str(R.string.export_filename), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
        TextField(
            value = export.fileName,
            onValueChange = { onEvent(EditorEvent.ExportNameChanged(it)) },
            singleLine = true,
            enabled = export.phase == ExportPhase.IDLE,
            textStyle = GridType.body,
            modifier = Modifier.fillMaxWidth().height(FieldHeight),
            trailingContent = { Text(str(R.string.export_ext), style = GridType.body, color = PicoTheme.colorScheme.labelSecondary) },
        )
    }
    if (tooManyColours) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_exclamation_mark_fill), contentDescription = null, modifier = Modifier.size(GridSizes.ButtonIcon), tint = GridColors.Coral)
            Text(str(R.string.export_too_many_colours), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
        }
    }
}

@Composable
private fun DoneContent(export: ExportState) {
    val result = export.result ?: return
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(ResultIcon), tint = GridColors.Mint)
        Text(str(R.string.export_done), style = GridType.label, color = PicoTheme.colorScheme.labelPrimary)
        Text(
            str(R.string.export_done_meta, result.displayName, result.voxelCount, str(R.string.size_kb, (result.bytes + 1023) / 1024)),
            style = GridType.caption,
            color = PicoTheme.colorScheme.labelSecondary,
        )
    }
}

@Composable
private fun FailedContent(export: ExportState) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_exclamation_mark_fill), contentDescription = null, modifier = Modifier.size(ResultIcon), tint = GridColors.Coral)
        Text(str(R.string.export_failed), style = GridType.label, color = PicoTheme.colorScheme.labelPrimary)
        Text(str(R.string.export_failed_reason, export.error.orEmpty()), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
    }
}
