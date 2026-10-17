package com.kejing.gridspace.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.SliderDefaults
import com.pico.spatial.ui.design.Text

/** Spec R4 S.5: label 72 + 16, track 400 x 12 (488 in all), rows 72 high and 16 apart; thumb area 72. */
private val TrackWidth = 400.dp
private val TrackHeight = 12.dp
private val LabelWidth = 72.dp
private val LabelGap = 16.dp
private val RowHeight = 72.dp
private val RowGap = 16.dp

/**
 * Hue / saturation / lightness rows (design spec D.2 row 5). The system Slider draws a transparent
 * track so the gradient Box underneath shows what each position means.
 */
@Composable
fun HslSliders(hsl: ColorMath.Hsl, onChange: (ColorMath.Hsl) -> Unit) {
    val hueStops = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color(ColorMath.fromHsl(it, 100f, 50f)) }
    val satStops = listOf(Color(ColorMath.fromHsl(hsl.h, 0f, hsl.l)), Color(ColorMath.fromHsl(hsl.h, 100f, hsl.l)))
    val lightStops = listOf(Color.Black, Color(ColorMath.fromHsl(hsl.h, hsl.s, 50f)), Color.White)
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        SliderRow(str(R.string.hue), hsl.h, 0f..360f, hueStops) { onChange(hsl.copy(h = it)) }
        SliderRow(str(R.string.saturation), hsl.s, 0f..100f, satStops) { onChange(hsl.copy(s = it)) }
        SliderRow(str(R.string.lightness), hsl.l, 0f..100f, lightStops) { onChange(hsl.copy(l = it)) }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, stops: List<Color>, onValue: (Float) -> Unit) {
    Row(modifier = Modifier.height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = GridType.caption, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1, modifier = Modifier.width(LabelWidth))
        Spacer(Modifier.width(LabelGap))
        Box(modifier = Modifier.width(TrackWidth).height(RowHeight), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier.width(TrackWidth)
                        .height(TrackHeight)
                        .clip(RoundedCornerShape(TrackHeight / 2))
                        .background(Brush.horizontalGradient(stops))
            )
            Slider(
                value = value.coerceIn(range),
                onValueChange = onValue,
                valueRange = range,
                modifier = Modifier.width(TrackWidth),
                sliderSpec =
                    SliderDefaults.sliderSpec(
                        thumbAreaSize = 72.dp,
                        thumbSize = 48.dp,
                        thumbPressedSize = 52.dp,
                        thumbHoverSize = 52.dp,
                        trackHeight = TrackHeight,
                    ),
                colors = SliderDefaults.sliderColors(trackColor = Color.Transparent, progressColor = Color.Transparent, progressHighColor = Color.Transparent),
            )
        }
    }
}
