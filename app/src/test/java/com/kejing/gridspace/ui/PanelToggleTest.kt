package com.kejing.gridspace.ui

import com.kejing.gridspace.data.ExportResult
import com.kejing.gridspace.data.Exporter
import com.kejing.gridspace.data.PanelMemory
import com.kejing.gridspace.data.ProjectStore
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec T.8 (R10): the size row and the colour rail are toolbar toggles, closed by default, remembered per session. */
@OptIn(ExperimentalCoroutinesApi::class)
class PanelToggleTest {
    private val dispatcher = StandardTestDispatcher()
    private val project = Project(id = "p1", name = "test", gridSize = 16, voxels = VoxelGrid(16), createdAt = 0L, updatedAt = 0L)

    private object Store : ProjectStore {
        override suspend fun load(id: String): Project? = Project(id = "p1", name = "test", gridSize = 16, voxels = VoxelGrid(16), createdAt = 0L, updatedAt = 0L)

        override suspend fun save(project: Project) = Unit
    }

    private object NoExport : Exporter {
        override suspend fun export(grid: VoxelGrid, baseName: String): ExportResult = ExportResult("x.vox", 0, 0)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun bothPanelsStartClosedAndToggle() =
        runTest(dispatcher) {
            val memory = PanelMemory()
            val vm = EditorViewModel(project.id, Store, NoExport, panels = memory)
            advanceUntilIdle()
            assertFalse(vm.state.value.showSizeRow)
            assertFalse(vm.state.value.showColorRail)
            assertFalse(vm.state.value.showBrushRow)
            vm.onEvent(EditorEvent.ToggleSizeRow)
            assertTrue(vm.state.value.showSizeRow)
            assertTrue(vm.state.value.showBrushRow)
            assertTrue(memory.showSizeRow)
            vm.onEvent(EditorEvent.ToggleSizeRow)
            assertFalse(vm.state.value.showSizeRow)
            assertFalse(memory.showSizeRow)
        }

    @Test
    fun sessionMemoryIsAppliedToANewEditor() =
        runTest(dispatcher) {
            val memory = PanelMemory().apply { showColorRail = true }
            val vm = EditorViewModel(project.id, Store, NoExport, panels = memory)
            advanceUntilIdle()
            assertTrue(vm.state.value.showColorRail)
            assertFalse(vm.state.value.showSizeRow)
        }

    @Test
    fun hidingTheRailClosesTheColourCard() =
        runTest(dispatcher) {
            val vm = EditorViewModel(project.id, Store, NoExport, panels = PanelMemory())
            advanceUntilIdle()
            vm.onEvent(EditorEvent.ToggleColorRail)
            vm.onEvent(EditorEvent.ColorEditOpened)
            assertTrue(vm.state.value.picker != null)
            vm.onEvent(EditorEvent.ToggleColorRail)
            assertFalse(vm.state.value.showColorRail)
            assertNull(vm.state.value.picker)
        }
}
