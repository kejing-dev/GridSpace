package com.kejing.gridspace.data

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.domain.usecase.VoxWriter
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of a successful export: the display name MediaStore settled on and the byte count. */
data class ExportResult(val displayName: String, val bytes: Int, val voxelCount: Int)

/** Export entry point seen by the editor ViewModel. */
interface Exporter {
    @Throws(IOException::class)
    suspend fun export(grid: VoxelGrid, baseName: String): ExportResult
}

/**
 * Writes a .vox into the shared Download/GridSpace folder through MediaStore. No storage permission
 * is needed on API 29+, and MediaStore appends " (1)" itself when the name is taken.
 */
class VoxExporter(private val context: Context) : Exporter {
    @Throws(IOException::class)
    override suspend fun export(grid: VoxelGrid, baseName: String): ExportResult =
        withContext(Dispatchers.IO) {
            val bytes = VoxWriter.write(grid)
            val displayName = sanitize(baseName) + EXTENSION
            val resolver = context.contentResolver
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
                    put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw IOException("MediaStore stream unavailable")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: IOException) {
                resolver.delete(uri, null, null)
                throw e
            }
            // Read back the final name in case MediaStore de-duplicated it.
            val finalName =
                resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: displayName
            ExportResult(finalName, bytes.size, grid.count)
        }

    companion object {
        const val EXTENSION = ".vox"
        const val FOLDER = "GridSpace"
        const val RELATIVE_PATH = "Download/$FOLDER"
        private const val MIME_TYPE = "application/octet-stream"

        /** Replaces characters that are illegal in file names with underscores. */
        fun sanitize(name: String): String {
            val cleaned = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().removeSuffix(EXTENSION)
            return cleaned.ifEmpty { "gridspace" }
        }
    }
}
