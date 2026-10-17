package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.theme.GridType
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/**
 * R7 (design dp, rendered unscaled on the front plane): 360 x 96, padding 24 / 16, line 1 icon 24
 * + `label` 20, line 2 `caption` 16, 6 apart, centred vertically.
 */
object LabelSizes {
    val Width = 360.dp
    val Height = 96.dp
}

/**
 * The target label (design spec F.1). The SDK's AttachmentPanelComponent cannot be verified inside
 * a Volume on the emulator, so the label is an Augment whose anchor follows the ghost cube.
 */
@Composable
fun TargetLabel(title: String, subtitle: String?, icon: Int?) {
    Column(
        modifier = Modifier.width(LabelSizes.Width).height(LabelSizes.Height).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(GridSizes.ButtonIcon))
            Text(title, style = GridType.label, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        }
        if (subtitle != null) Text(subtitle, style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
    }
}
