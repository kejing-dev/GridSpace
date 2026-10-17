package com.kejing.gridspace.ui.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.common.GridSizes
import com.kejing.gridspace.ui.common.PrimaryButton
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text

/** Spec R4 M.3: column 440; button 440 x 80; size items 104 x 64 in 440 x 72; steps 56 high, 36 dp circle. */
private val SizeItemWidth = 104.dp
private val StepHeight = 56.dp
private val StepCircle = 36.dp

/** Left column of the entry window: the primary action, grid size and the three first-use steps. */
@Composable
fun NewProjectColumn(
    gridSize: Int,
    onGridSize: (Int) -> Unit,
    onNewProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(
            label = str(R.string.new_project),
            icon = R.drawable.ic_plus,
            onClick = onNewProject,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(str(R.string.grid_size), style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary)
        SegmentControl(
            modifier = Modifier.fillMaxWidth().height(GridSizes.SegmentHeight),
            itemSpace = GridSizes.SegmentGap,
            contentPadding = GridSizes.SegmentPadding,
        ) {
            VoxelGrid.SIZES.forEach { size ->
                SegmentItem(
                    selected = size == gridSize,
                    onClick = { onGridSize(size) },
                    modifier = Modifier.width(SizeItemWidth).height(GridSizes.SegmentItemHeight),
                    title = { Text(size.toString(), style = GridType.bodyMedium, maxLines = 1) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(str(R.string.before_start), style = GridType.bodyMedium, color = PicoTheme.colorScheme.labelPrimary)
        StepRow(1, str(R.string.step_1))
        StepRow(2, str(R.string.step_2))
        StepRow(3, str(R.string.step_3))
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(StepHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(StepCircle).clip(CircleShape).background(GridColors.Mint),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = GridType.captionMedium, color = GridColors.OnMint, maxLines = 1)
        }
        Text(text, style = GridType.body, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
    }
}
