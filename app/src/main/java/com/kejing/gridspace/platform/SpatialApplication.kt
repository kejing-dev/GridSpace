package com.kejing.gridspace.platform

import android.app.Application
import com.kejing.gridspace.data.AppGraph
import com.kejing.gridspace.mainApp
import com.pico.spatial.ui.foundation.dsl.launch

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        launch(::mainApp)
    }
}
