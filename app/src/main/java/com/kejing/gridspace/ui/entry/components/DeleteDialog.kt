package com.kejing.gridspace.ui.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.ui.common.SecondaryButton
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.AlertDialog

/** "Delete <name>? This cannot be undone." with Cancel and a coral Delete (72 dp buttons, spec R4 M.3). */
@Composable
fun DeleteDialog(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(str(R.string.delete_title, name), style = GridType.title) },
        content = { Text(str(R.string.delete_body), style = GridType.body, color = PicoTheme.colorScheme.labelSecondary) },
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = str(R.string.cancel), onClick = onCancel)
                SecondaryButton(label = str(R.string.delete), onClick = onConfirm, tint = GridColors.Coral)
            }
        },
    )
}
