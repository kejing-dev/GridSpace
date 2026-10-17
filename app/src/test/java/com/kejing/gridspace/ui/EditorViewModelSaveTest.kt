package com.kejing.gridspace.ui

import com.kejing.gridspace.data.ExportResult
import com.kejing.gridspace.data.Exporter
import com.kejing.gridspace.data.ProjectStore
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.SaveState
import com.kejing.gridspace.domain.model.VoxelGrid
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.EditorNotice
import com.kejing.gridspace.ui.editor.EditorViewModel
import com.kejing.gridspace.ui.editor.SceneHit
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Autosave reporting (QA D-01): a cancelled in-flight save is not a failure; an IO error is. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelSaveTest {
    private val dispatcher = StandardTestDispatcher()
    private val red = 0xFFFF0000.toInt()
    private val project = Project(id = "p1", name = "test", gridSize = 16, voxels = VoxelGrid(16), createdAt = 0L, updatedAt = 0L)

    /** Saves take [writeMs] of virtual time; [failing] stores throw like a full disk. */
    private class FakeStore(private val project: Project, private val writeMs: Long, private val failing: Boolean = false) : ProjectStore {
        var saves = 0
        var completed = 0

        override suspend fun load(id: String): Project? = project

        override suspend fun save(project: Project) {
            saves++
            delay(writeMs)
            if (failing) throw IOException("disk full")
            completed++
        }
    }

    private object NoExport : Exporter {
        override suspend fun export(grid: VoxelGrid, baseName: String): ExportResult = ExportResult("x.vox", 0, 0)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun EditorViewModel.add(x: Int) = onEvent(EditorEvent.SceneTapped(SceneHit.Target(Cell(x, 0, 0))))

    @Test
    fun cancellingAnInFlightSaveDoesNotReportFailure() =
        runTest(dispatcher) {
            val store = FakeStore(project, writeMs = 500)
            val vm = EditorViewModel(project.id, store, NoExport)
            advanceUntilIdle()
            vm.add(0)
            assertEquals(SaveState.SAVING, vm.state.value.saveState)
            // Debounce elapsed, the write is in flight; the next commit cancels that job.
            advanceTimeBy(EditorViewModel.AUTOSAVE_DEBOUNCE_MS + 200)
            assertEquals(1, store.saves)
            vm.add(1)
            advanceUntilIdle()
            assertEquals(SaveState.SAVED, vm.state.value.saveState)
            assertNull(vm.state.value.notice)
            // The cancelled write still completed (non-cancellable), then the second one ran.
            assertEquals(2, store.saves)
            assertEquals(2, store.completed)
        }

    /** QA N-02: the cancelled write finishing must not show "saved" while the newer commit is still pending. */
    @Test
    fun cancelledSaveDoesNotReportSavedWhileANewerCommitIsPending() =
        runTest(dispatcher) {
            val store = FakeStore(project, writeMs = 500)
            val vm = EditorViewModel(project.id, store, NoExport)
            advanceUntilIdle()
            vm.add(0)
            // t = 1200: the first write started at t = 1000 and is in flight; commit again.
            advanceTimeBy(EditorViewModel.AUTOSAVE_DEBOUNCE_MS + 200)
            assertEquals(1, store.saves)
            vm.add(1)
            assertEquals(SaveState.SAVING, vm.state.value.saveState)
            // t = 1600: the cancelled first write completed (t = 1500) with the old snapshot, but the
            // second commit's save is still in its debounce, so nothing is "saved" yet.
            advanceTimeBy(400)
            assertEquals(1, store.completed)
            assertEquals(1, store.saves)
            assertEquals(SaveState.SAVING, vm.state.value.saveState)
            assertNull(vm.state.value.notice)
            // Only the second write flips the state.
            advanceUntilIdle()
            assertEquals(2, store.completed)
            assertEquals(SaveState.SAVED, vm.state.value.saveState)
        }

    @Test
    fun rapidCommitsAtTheDebounceIntervalNeverShowFailed() =
        runTest(dispatcher) {
            val store = FakeStore(project, writeMs = 300)
            val vm = EditorViewModel(project.id, store, NoExport)
            advanceUntilIdle()
            repeat(3) { i ->
                vm.add(i)
                advanceTimeBy(EditorViewModel.AUTOSAVE_DEBOUNCE_MS)
                assertEquals("after commit $i", null, vm.state.value.notice)
                assert(vm.state.value.saveState != SaveState.FAILED)
            }
            advanceUntilIdle()
            assertEquals(SaveState.SAVED, vm.state.value.saveState)
            assertNull(vm.state.value.notice)
        }

    @Test
    fun ioErrorReportsFailureAndSnack() =
        runTest(dispatcher) {
            val store = FakeStore(project, writeMs = 10, failing = true)
            val vm = EditorViewModel(project.id, store, NoExport)
            advanceUntilIdle()
            vm.add(0)
            advanceUntilIdle()
            assertEquals(SaveState.FAILED, vm.state.value.saveState)
            assertEquals(EditorNotice.SaveFailed, vm.state.value.notice)
        }

    @Test
    fun pauseFlushesThePendingDebounce() =
        runTest(dispatcher) {
            val store = FakeStore(project, writeMs = 10)
            val vm = EditorViewModel(project.id, store, NoExport)
            advanceUntilIdle()
            vm.add(0)
            advanceTimeBy(100)
            assertEquals(0, store.saves)
            vm.onEvent(EditorEvent.Paused)
            advanceTimeBy(50)
            assertEquals(1, store.saves)
            advanceUntilIdle()
            assertEquals(SaveState.SAVED, vm.state.value.saveState)
            assertEquals(1, store.saves)
        }
}
