package com.kejing.gridspace.data

import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.ProjectSummary
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The number captured by an untitled-name pattern, or null when [name] is not an untitled name.
 * The pattern is an alternation over languages ("Untitled (\d+)|未命名 (\d+)"), so the number sits
 * in whichever group matched, not necessarily group 1 (QA N7-01).
 */
fun Regex.untitledNumber(name: String): Int? =
    matchEntire(name)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toIntOrNull()

/** What the editor needs from storage; the repository implements it, tests substitute a fake. */
interface ProjectStore {
    @Throws(IOException::class)
    suspend fun save(project: Project)

    suspend fun load(id: String): Project?
}

/** The recent list and its management, used by the entry window and the editor's Projects sheet. */
interface ProjectCatalog {
    /** Recent projects, newest first. */
    val summaries: StateFlow<List<ProjectSummary>>

    suspend fun delete(id: String)

    /** Next free "Untitled k" number, counting existing names that match [pattern]. */
    fun nextUntitledNumber(pattern: Regex): Int
}

/**
 * Projects live in the app-private directory: files/projects/{id}.json plus an index.json for the
 * recent list. Every write goes to a .tmp file first and is renamed into place, so a crash mid-write
 * never leaves a half file where a project used to be. All IO runs on [Dispatchers.IO].
 */
class ProjectRepository(filesDir: File) : ProjectStore, ProjectCatalog {
    private val root = File(filesDir, "projects")
    private val indexFile = File(root, "index.json")
    private val lock = Mutex()

    private val _summaries = MutableStateFlow<List<ProjectSummary>>(emptyList())
    override val summaries: StateFlow<List<ProjectSummary>> = _summaries.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) { lock.withLock { _summaries.value = readIndex() } }

    @Throws(IOException::class)
    override suspend fun save(project: Project): Unit =
        withContext(Dispatchers.IO) {
            lock.withLock {
                root.mkdirs()
                writeAtomic(fileFor(project.id), ProjectJson.encode(project))
                val others = readIndex().filter { it.id != project.id }
                val index = (listOf(ProjectJson.summaryOf(project)) + others).sortedByDescending { it.updatedAt }
                writeAtomic(indexFile, ProjectJson.encodeIndex(index))
                _summaries.value = index
            }
        }

    /** Null when the file is missing or damaged; a damaged file is flagged in the index. */
    override suspend fun load(id: String): Project? =
        withContext(Dispatchers.IO) {
            val file = fileFor(id)
            if (!file.isFile) return@withContext null
            val project = runCatching { ProjectJson.decode(file.readText()) }.getOrNull()
            if (project == null) markCorrupt(id)
            project
        }

    override suspend fun delete(id: String) =
        withContext(Dispatchers.IO) {
            lock.withLock {
                fileFor(id).delete()
                File(root, "$id.json.tmp").delete()
                val index = readIndex().filter { it.id != id }
                runCatching { writeAtomic(indexFile, ProjectJson.encodeIndex(index)) }
                _summaries.value = index
            }
        }

    override fun nextUntitledNumber(pattern: Regex): Int {
        val used = _summaries.value.mapNotNull { pattern.untitledNumber(it.name) }
        return (used.maxOrNull() ?: 0) + 1
    }

    private suspend fun markCorrupt(id: String) =
        lock.withLock {
            val index = readIndex().map { if (it.id == id) it.copy(corrupt = true) else it }
            runCatching { writeAtomic(indexFile, ProjectJson.encodeIndex(index)) }
            _summaries.value = index
        }

    private fun readIndex(): List<ProjectSummary> {
        if (!indexFile.isFile) return rebuildIndex()
        return runCatching { ProjectJson.decodeIndex(indexFile.readText()) }.getOrElse { rebuildIndex() }
    }

    /** Falls back to scanning the files when the index is missing or unreadable. */
    private fun rebuildIndex(): List<ProjectSummary> {
        val files = root.listFiles { f -> f.isFile && f.name.endsWith(".json") && f.name != indexFile.name } ?: return emptyList()
        return files
            .map { file ->
                runCatching { ProjectJson.summaryOf(ProjectJson.decode(file.readText())) }
                    .getOrElse {
                        ProjectSummary(file.nameWithoutExtension, file.nameWithoutExtension, 0, 0, null, file.lastModified(), corrupt = true)
                    }
            }
            .sortedByDescending { it.updatedAt }
    }

    @Throws(IOException::class)
    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            // rename() fails across some filesystems when the target exists; replace explicitly.
            if (!(target.delete() && tmp.renameTo(target))) throw IOException("Cannot replace ${target.name}")
        }
    }

    private fun fileFor(id: String) = File(root, "$id.json")
}
