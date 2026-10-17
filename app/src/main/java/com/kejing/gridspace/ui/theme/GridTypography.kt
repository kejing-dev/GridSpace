package com.kejing.gridspace.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Typography

/**
 * The seven text roles of the design spec R4 (M.1), derived from the native type scale by changing
 * only size, weight and line height. Nothing is smaller than 14 sp and nothing is Bold: CJK glyphs
 * smear on glass. The font family comes from [GridTypographyTheme] (Noto Sans / Noto Sans SC).
 */
object GridType {
    /** Window / panel / Sheet titles, the project name, the colour-edit title: 28 sp Medium. */
    val title: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium)

    /** Buttons, mode labels, list headlines, target label line 1, "N selected": 20 sp Medium. */
    val label: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)

    /** Body copy, stepper values, entry steps, eyedropper hint, file name: 18 sp Regular. */
    val body: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)

    /** Segment items (brush / grid size / format), axis letters, "Before you start": 18 sp Medium. */
    val bodyMedium: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)

    /** Captions, secondary text, save status, slider labels, target label line 2: 16 sp Regular. */
    val caption: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)

    /** Selection tiles, preset tabs, step numbers, "Editing": 16 sp Medium. */
    val captionMedium: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)

    /** The one smallest size: the hex value in the colour-edit panel (non-interactive): 14 sp Medium. */
    val small: TextStyle
        @Composable @ReadOnlyComposable
        get() = PicoTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
}

/** Bundled variable fonts (SIL Open Font License, OFL texts alongside in assets/fonts). */
private const val LATIN_FONT = "NotoSans-VF.ttf"
private const val CHINESE_FONT = "NotoSansSC-VF.ttf"
private const val FONT_DIR = "fonts"

/** 400 / 500 for the app's own roles; 600 / 700 so the SDK's built-in styles (Sheet titles) resolve. */
private val WEIGHTS = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight(600), FontWeight(700))

/**
 * Noto Sans SC for Chinese (it includes Latin glyphs, so mixed digits never fall back) and Noto
 * Sans for English. Only the `wght` axis is used. Null when the asset is missing, so the system
 * font is used and the app still runs (spec Q.2).
 */
private fun Context.notoFontFamily(language: AppLanguage): FontFamily? {
    val file =
        when (language) {
            AppLanguage.ZH -> CHINESE_FONT
            AppLanguage.EN -> LATIN_FONT
        }
    val bundled = runCatching { assets.list(FONT_DIR)?.toSet() }.getOrNull().orEmpty()
    if (file !in bundled) return null
    return runCatching {
        FontFamily(
            WEIGHTS.map { weight ->
                Font(
                    path = "$FONT_DIR/$file",
                    assetManager = assets,
                    weight = weight,
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
                )
            }
        )
    }.getOrNull()
}

/**
 * Re-provides the native SpatialUI type scale with only the font family swapped for [language].
 * Call it inside [PicoTheme] so the colour scheme and every other token stay the system ones.
 */
@Composable
fun GridTypographyTheme(language: AppLanguage, content: @Composable () -> Unit) {
    PicoTheme(typography = rememberGridTypography(PicoTheme.typography, language), content = content)
}

@Composable
private fun rememberGridTypography(base: Typography, language: AppLanguage): Typography {
    val context = LocalContext.current
    return remember(context, base, language) {
        val family = context.notoFontFamily(language) ?: return@remember base
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = family),
            displayMedium = base.displayMedium.copy(fontFamily = family),
            displaySmall = base.displaySmall.copy(fontFamily = family),
            headlineLarge = base.headlineLarge.copy(fontFamily = family),
            headlineMedium = base.headlineMedium.copy(fontFamily = family),
            headlineSmall = base.headlineSmall.copy(fontFamily = family),
            titleLarge = base.titleLarge.copy(fontFamily = family),
            titleMedium = base.titleMedium.copy(fontFamily = family),
            titleSmall = base.titleSmall.copy(fontFamily = family),
            bodyLarge = base.bodyLarge.copy(fontFamily = family),
            bodyLargeMultiline = base.bodyLargeMultiline.copy(fontFamily = family),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            bodyMediumMultiline = base.bodyMediumMultiline.copy(fontFamily = family),
            bodySmall = base.bodySmall.copy(fontFamily = family),
            bodyTiny = base.bodyTiny.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family),
            labelMedium = base.labelMedium.copy(fontFamily = family),
            labelSmall = base.labelSmall.copy(fontFamily = family),
        )
    }
}
