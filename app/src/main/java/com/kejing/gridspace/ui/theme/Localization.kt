package com.kejing.gridspace.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kejing.gridspace.R
import com.kejing.gridspace.data.AppGraph
import java.util.Locale

/** Languages the app ships; a new one is a values folder, a font file and an entry here. */
enum class AppLanguage(val code: String, val locale: Locale) {
    ZH("zh", Locale.SIMPLIFIED_CHINESE),
    EN("en", Locale.ENGLISH);

    val other: AppLanguage
        get() = if (this == ZH) EN else ZH

    companion object {
        /** Setting / preference value -> language, or null for anything else. */
        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code?.trim()?.lowercase() }

        /** System default: zh* -> Chinese, everything else English (spec Q.1 priority 3). */
        fun system(): AppLanguage = if (Locale.getDefault().language == "zh") ZH else EN
    }
}

/**
 * Language resolution (spec Q.1): the emulator / QA global setting `gridspace_lang` wins, then the
 * persisted user choice (SharedPreferences "language"), then the system locale.
 *
 *   adb shell settings put global gridspace_lang en     # QA only; delete it afterwards
 */
object LanguageResolver {
    const val SETTING = "gridspace_lang"
    const val PREFS = "gridspace"
    const val PREF_KEY = "language"

    fun resolve(context: Context): AppLanguage =
        AppLanguage.fromCode(Settings.Global.getString(context.contentResolver, SETTING))
            ?: AppLanguage.fromCode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_KEY, null))
            ?: AppLanguage.system()

    fun persist(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PREF_KEY, language.code).apply()
    }
}

val LocalAppResources = staticCompositionLocalOf<Resources> { error("AppLocale not provided") }
val LocalAppLanguage = staticCompositionLocalOf<AppLanguage> { error("AppLocale not provided") }

private fun Context.localizedResources(language: AppLanguage): Resources {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(language.locale)
    return createConfigurationContext(configuration).resources
}

/**
 * Provides string resources and the Noto font family for the current language. Both windows are
 * wrapped in this, and both collect the same [AppGraph.language] flow, so a switch in either
 * window takes effect in both at once (spec Q.1).
 */
@Composable
fun AppLocale(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val language by AppGraph.language.collectAsStateWithLifecycle()
    val resources = remember(context, language) { context.localizedResources(language) }
    CompositionLocalProvider(LocalAppResources provides resources, LocalAppLanguage provides language) {
        GridTypographyTheme(language = language, content = content)
    }
}

@Composable
@ReadOnlyComposable
fun str(@StringRes id: Int): String = LocalAppResources.current.getString(id)

@Composable
@ReadOnlyComposable
fun str(@StringRes id: Int, vararg args: Any): String = LocalAppResources.current.getString(id, *args)

/** Non-composable access for ViewModels (snack texts, default names) in the current language. */
fun Context.appString(@StringRes id: Int, vararg args: Any): String =
    localizedResources(AppGraph.language.value).getString(id, *args)

/**
 * Matches an auto-generated project name ("Untitled 12" / "未命名 12") in every shipped language,
 * so numbering continues across a language switch; group 1 is the number. QA N7-01: the former
 * `Regex.escape(...).replace("0", ...)` put the group inside the `\Q...\E` quote, so nothing ever
 * matched and every new project became number 1.
 */
fun Context.untitledPattern(): Regex =
    Regex(AppLanguage.entries.joinToString("|") { untitledTemplateRegex(localizedResources(it).getString(R.string.untitled)) })

/** Turns a `%1$d` string template into a regex source with one `(\d+)` group; pure for unit tests. */
fun untitledTemplateRegex(template: String): String =
    template.split("%1\$d").joinToString("(\\d+)") { if (it.isEmpty()) "" else Regex.escape(it) }
