package com.kejing.gridspace.data

import android.content.Context
import com.kejing.gridspace.ui.theme.AppLanguage
import com.kejing.gridspace.ui.theme.LanguageResolver
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which editor Volume is open right now (its project id), shared by both windows. The entry
 * window sets it when it opens a project; the editor screen clears it when its composition is
 * disposed, so a Volume closed with the system button never leaves a stale id behind (QA D-16).
 */
class EditorSession {
    private val _openProjectId = MutableStateFlow<String?>(null)
    val openProjectId: StateFlow<String?> = _openProjectId.asStateFlow()

    fun opened(projectId: String) {
        _openProjectId.value = projectId
    }

    /** Clears the id only if [projectId] is still the one recorded (a newer open wins). */
    fun closed(projectId: String) {
        _openProjectId.compareAndSet(projectId, null)
    }
}

/**
 * R10: which editor side panels the user has opened from the toolbar (size row, colour rail).
 * Remembered for the process (every editor Volume of this session starts with the last choice),
 * never persisted; both default to closed.
 */
class PanelMemory {
    @Volatile var showSizeRow: Boolean = false
    @Volatile var showColorRail: Boolean = false
}

/**
 * Process-wide singletons. Both windows share one repository so the recent list in the entry
 * window reflects saves made by the editor without any extra messaging, and one language flow so
 * a switch in either window restyles both (spec Q.1).
 */
object AppGraph {
    lateinit var projects: ProjectRepository
        private set

    lateinit var exporter: VoxExporter
        private set

    val editorSession = EditorSession()

    val panels = PanelMemory()

    private lateinit var appContext: Context

    private val _language = MutableStateFlow(AppLanguage.system())
    /** Current UI language; resolved once at start-up (setting > preference > system). */
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    /**
     * The demo setting may auto-open the sample only once per process: branch A reopens the entry
     * window (and so re-runs EntryScreen's start-up effect) every time the last editor closes.
     */
    val demoAutoOpened = AtomicBoolean(false)

    fun init(context: Context) {
        if (::projects.isInitialized) return
        val app = context.applicationContext
        appContext = app
        projects = ProjectRepository(app.filesDir)
        exporter = VoxExporter(app)
        _language.value = LanguageResolver.resolve(app)
    }

    /** User choice from the 中/EN segment: persisted and applied to both windows at once. */
    fun selectLanguage(language: AppLanguage) {
        if (::appContext.isInitialized) LanguageResolver.persist(appContext, language)
        _language.value = language
    }
}
