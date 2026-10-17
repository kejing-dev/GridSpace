package com.kejing.gridspace.ui.navigation

/** Ids and bundle keys shared by the window declarations and the code that opens them. */
object WindowIds {
    /** Must match pico.spatial.windowcontainer.id in AndroidManifest.xml. */
    const val ENTRY_WINDOW = "GridSpaceMainWindow"
    const val EDITOR_VOLUME = "EditorVolume"
    const val ARG_PROJECT_ID = "projectId"

    /** One editor at a time: the tag is the project id so reopening the same project reuses it. */
    fun editorTag(projectId: String): String = "project_$projectId"
}
