package com.kejing.gridspace.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.R
import com.kejing.gridspace.ui.theme.AppLanguage
import com.kejing.gridspace.ui.theme.GridColors
import com.kejing.gridspace.ui.theme.GridType
import com.kejing.gridspace.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.IconButtonDefaults
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text

/** Spec R4 M.2: every tappable target is at least 72 dp; the one emphasised action is 80 dp high. */
object GridSizes {
    val Primary = 80.dp
    val Control = 72.dp
    val Icon = 28.dp
    val ButtonIcon = 24.dp
    val SegmentHeight = 72.dp
    val SegmentItemHeight = 64.dp
    val SegmentPadding = 4.dp
    val SegmentGap = 4.dp
    val LanguageSegmentWidth = 152.dp
    val LanguageItemWidth = 72.dp
    /** Horizontal inner padding of text buttons (spec R4 M.3: 24; 16 where the label is tight). */
    val ButtonPadding = PaddingValues(horizontal = 24.dp)
    val TightButtonPadding = PaddingValues(horizontal = 16.dp)
}

/**
 * The one emphasised action of a surface: Mint container, dark text, 80 dp high (spec R4 M.2). The
 * SDK has no PrimaryButton, so this wraps [Button] with the design's accent colours (the colour
 * scheme has no accent role).
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = GridSizes.Primary,
    icon: Int? = null,
    iconSize: Dp = GridSizes.Icon,
    leading: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = GridSizes.ButtonPadding,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        size = ButtonDefaults.buttonSize(height = height, minHeight = height, maxHeight = height),
        colors = ButtonDefaults.buttonColors(containerColor = GridColors.Mint, contentColor = GridColors.OnMint),
        contentPadding = contentPadding,
        gap = 10.dp,
        leadingIcon =
            leading ?: icon?.let { { Icon(painter = painterResource(it), contentDescription = null, modifier = Modifier.size(iconSize)) } },
    ) {
        Text(label, style = GridType.label, maxLines = 1)
    }
}

/** Secondary action, 72 dp high, with a visible 20 sp label and an optional 24 dp leading icon. */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = GridSizes.Control,
    icon: Int? = null,
    iconSize: Dp = GridSizes.ButtonIcon,
    tint: Color? = null,
    textStyle: TextStyle? = null,
    contentPadding: PaddingValues = GridSizes.ButtonPadding,
) {
    val style = textStyle ?: GridType.label
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        size = ButtonDefaults.buttonSize(height = height, minHeight = height, maxHeight = height),
        contentPadding = contentPadding,
        gap = 8.dp,
        leadingIcon =
            icon?.let {
                {
                    if (tint != null) Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
                    else Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(iconSize))
                }
            },
    ) {
        if (tint != null) Text(label, style = style, color = tint, maxLines = 1)
        else Text(label, style = style, maxLines = 1)
    }
}

/** 72 dp icon-only action with a 28 dp glyph; [label] is the content description. */
@Composable
fun ActionIcon(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = GridSizes.Control,
    iconSize: Dp = GridSizes.Icon,
    tint: Color? = null,
    transparent: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        size = IconButtonDefaults.iconButtonSize(size),
        colors =
            if (transparent) IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
            else IconButtonDefaults.iconButtonColors(),
    ) {
        if (tint != null) Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(iconSize), tint = tint)
        else Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(iconSize))
    }
}

/** Row-scoped convenience so toolbars can mix these with dividers. */
@Composable
fun RowScope.ToolbarIcon(icon: Int, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    ActionIcon(icon = icon, label = label, onClick = onClick, enabled = enabled, transparent = true)
}

/**
 * The 中 / EN segment (spec R4 N.4 / N.5): 152 x 72 with two 72 x 64 items. [language] is a State
 * so the Augment content lambda that hosts it stays stable across switches.
 */
@Composable
fun LanguageSegment(
    language: State<AppLanguage>,
    onSelect: (AppLanguage) -> Unit,
    width: Dp = GridSizes.LanguageSegmentWidth,
    itemWidth: Dp = GridSizes.LanguageItemWidth,
) {
    LanguageSegment(language = language.value, onSelect = onSelect, width = width, itemWidth = itemWidth)
}

/** Value variant for hosts that already carry the language inside their own State (the colour rail). */
@Composable
fun LanguageSegment(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    width: Dp = GridSizes.LanguageSegmentWidth,
    itemWidth: Dp = GridSizes.LanguageItemWidth,
) {
    SegmentControl(
        modifier = Modifier.width(width).height(GridSizes.SegmentHeight),
        itemSpace = GridSizes.SegmentGap,
        contentPadding = GridSizes.SegmentPadding,
    ) {
        listOf(AppLanguage.ZH to R.string.lang_zh, AppLanguage.EN to R.string.lang_en).forEach { (item, label) ->
            val selected = language == item
            SegmentItem(
                selected = selected,
                onClick = { if (!selected) onSelect(item) },
                modifier = Modifier.width(itemWidth).height(GridSizes.SegmentItemHeight),
                title = { Text(str(label), style = GridType.label, maxLines = 1) },
            )
        }
    }
}
