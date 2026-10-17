package com.kejing.gridspace

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kejing.gridspace.ui.editor.EditorScreen
import com.kejing.gridspace.ui.entry.EntryScreen
import com.kejing.gridspace.ui.navigation.WindowIds
import com.kejing.gridspace.ui.theme.AppLocale
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.foundation.dsl.WorldScale

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        // W1: the entry window (Planar, system glass from the manifest; 1200 x 800, spec R4 M.3 / N.6.A).
        // Branch A: it is closed while an editor is open and reopened when the last editor closes.
        DefaultWindowContainer {
            val bounds = Modifier.windowConstraints(minWidth = 1080.dp, minHeight = 720.dp, maxWidth = 1600.dp, maxHeight = 1000.dp)
            PicoTheme { AppLocale { EntryScreen(bounds) } }
        }

        // W2: the editor Volume. 1600 dp reads as desk-sized at arm's length because a Volume keeps
        // its physical size; the model floats inside without glass.
        WindowContainer(
            id = WindowIds.EDITOR_VOLUME,
            form = Form.Volumetric,
            defaultSize = WindowContainerSize(width = 1600.dp, height = 1600.dp, depth = 1600.dp),
            enableMaterialBackground = false,
            worldScale = WorldScale.Dynamic,
        ) {
            val projectId = bundle?.getString(WindowIds.ARG_PROJECT_ID) ?: return@WindowContainer
            val bounds =
                Modifier.windowConstraints(
                    minWidth = 960.dp, minHeight = 960.dp, minDepth = 960.dp,
                    maxWidth = 2700.dp, maxHeight = 2700.dp, maxDepth = 2700.dp,
                )
            PicoTheme { AppLocale { EditorScreen(projectId, bounds) } }
        }
    }
