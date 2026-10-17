package com.kejing.gridspace.ui.navigation

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.withFrameNanos
import com.kejing.gridspace.data.AppGraph
import com.kejing.gridspace.domain.model.Axis
import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.domain.model.PalettePreset
import com.kejing.gridspace.domain.usecase.SampleProject
import com.kejing.gridspace.ui.editor.EditorEvent
import com.kejing.gridspace.ui.editor.EditorUiState
import com.kejing.gridspace.ui.editor.ExportPhase
import com.kejing.gridspace.ui.editor.SceneHit
import kotlinx.coroutines.delay

/**
 * Scripted demo for the emulator, where nobody can pinch. Driven by the global setting
 * `gridspace_demo` (design spec L.4): `sample`, `walk`, `export` or `newproject`. Absent = normal.
 *
 *   adb shell settings put global gridspace_demo walk
 *   adb shell settings delete global gridspace_demo
 *
 * The script sends the same events the gestures do; previews are driven explicitly with
 * PreviewStarted / PreviewMoved / Committed so every state can be screenshotted.
 *
 * Every `step <name>` log line (tag GridSpaceDemo) is written AFTER the state it names has been
 * applied and two frames have been drawn. The state is then held for at least [HOLD_MS] and, when a
 * capture script acknowledges it (`settings put global gridspace_demo_ack <name>`), until that
 * acknowledgement arrives (at most [ACK_TIMEOUT_MS]); the emulator's screencap can take several
 * seconds, so a fixed hold let the capture loop drift behind the script (QA D-11).
 */
enum class DemoScript { SAMPLE, WALK, EXPORT, NEWPROJECT, EDIT, SIZE }

object DemoMode {
    const val SETTING = "gridspace_demo"

    /** Pause between walk steps. */
    const val STEP_MS = 4_000L
    /** How long every marked state stays on screen (emulator screencap takes about 1.5 s). */
    const val HOLD_MS = 3_500L
    /** Frames plus a short delay between applying a state and logging its marker. */
    private const val SETTLE_MS = 400L
    /** Interval of the rapid undo / redo commits (equal to the autosave debounce on purpose). */
    private const val RAPID_MS = 1_000L
    /** Longest wait for a capture acknowledgement before the walk moves on by itself. */
    private const val ACK_TIMEOUT_MS = 40_000L   // R6: cap.sh may retry 8 x ~3 s past a black quad
    /**
     * Wait between creating a spatial window (target label, colour popup) and its marker: on the
     * emulator the window's glass settles only after a few redraws (`EditorScaled` nudges it at
     * 0.5 / 1.5 / 3.5 / 7.5 s), so the frame is taken after the fourth nudge (R6: 8 s, the eyedropper bar was still black at 4 s).
     */
    private const val WINDOW_SETTLE_MS = 8_000L
    /** `edit` script: let the side cards of a fresh launch settle before the card opens. */
    private const val EDIT_SCRIPT_START_MS = 30_000L
    private const val ACK_POLL_MS = 250L
    const val ACK_SETTING = "gridspace_demo_ack"

    /**
     * Emulator-only overrides of the Augment placements, "ax,ay,az,ox,oy,oz[,align]" (anchor in
     * normalised Volume coordinates, offset in dp, optional 7th value = alignment: 0 BottomCenter,
     * 1 CenterLeft, 2 CenterRight, 3 TopCenter), e.g. `settings put global gridspace_rail
     * 1,0.5,0.5,32,0,0,1`. A negative first value hides the card. Read once per editor composition;
     * lets QA try placements without a rebuild. Delete the settings afterwards.
     */
    const val BRUSH_SETTING = "gridspace_brush"
    const val RAIL_SETTING = "gridspace_rail"
    const val SELPANEL_SETTING = "gridspace_selpanel"
    /** R6: the colour-edit card / eyedropper bar Augment. */
    const val POPUP_SETTING = "gridspace_popup"

    fun placement(context: Context, setting: String): List<Float>? =
        Settings.Global.getString(context.contentResolver, setting)
            ?.split(',')
            ?.mapNotNull { it.trim().toFloatOrNull() }
            ?.takeIf { it.size == 6 || it.size == 7 }

    fun script(context: Context): DemoScript? =
        when (Settings.Global.getString(context.contentResolver, SETTING)?.trim()?.lowercase()) {
            "sample" -> DemoScript.SAMPLE
            "walk" -> DemoScript.WALK
            "export" -> DemoScript.EXPORT
            "newproject" -> DemoScript.NEWPROJECT
            "edit" -> DemoScript.EDIT
            "size" -> DemoScript.SIZE
            else -> null
        }

    /** Drives the editor; returns true when the editor should close afterwards (`export`). */
    suspend fun runEditorScript(context: Context, projectId: String, state: () -> EditorUiState, onEvent: (EditorEvent) -> Unit): Boolean {
        val script = script(context) ?: return false
        // Every script but `newproject` drives the sample project; `newproject` runs in the blank one.
        if (script != DemoScript.NEWPROJECT && projectId != SampleProject.ID) return false
        val ack: () -> String? = { Settings.Global.getString(context.contentResolver, ACK_SETTING)?.trim() }
        when (script) {
            DemoScript.SAMPLE -> return false
            DemoScript.SIZE -> {
                // R8 probe: the sized brush 3 x 2 x 1 previewed at (1, 0, 1) as in the walk, then released.
                delay(EDIT_SCRIPT_START_MS)
                onEvent(EditorEvent.ModeSelected(EditMode.ADD))
                markAndHold("workbench", ack)
                onEvent(EditorEvent.SizeStep(Axis.X, 2))
                onEvent(EditorEvent.SizeStep(Axis.Y, 1))
                onEvent(EditorEvent.PreviewStarted(target(1, 0, 1)))
                delay(WINDOW_SETTLE_MS)
                markAndHold("size", ack)
                onEvent(EditorEvent.PreviewCancelled)
                mark("done")
                return false
            }
            DemoScript.EDIT -> {
                // R6 placement probe: the colour-edit card, the eyedropper bar, closed. The rail and
                // brush row of a fresh launch take ~30 s to settle (AGENTS), so wait before opening.
                delay(EDIT_SCRIPT_START_MS)
                onEvent(EditorEvent.ColorEditOpened)
                delay(WINDOW_SETTLE_MS)
                markAndHold("edit", ack)
                onEvent(EditorEvent.EyedropperStarted)
                delay(WINDOW_SETTLE_MS)
                markAndHold("eyedrop", ack)
                onEvent(EditorEvent.PickerDismissed)
                delay(WINDOW_SETTLE_MS)
                markAndHold("edit-close", ack)
                mark("done")
                return false
            }
            DemoScript.NEWPROJECT -> {
                // K4.42 evidence (QA N4-06): the white default slot on the Soft page, then the Vivid
                // page whose #FFFFFF swatch shows the dark matching ring.
                delay(STEP_MS)
                markAndHold("newproject", ack)
                onEvent(EditorEvent.PresetSelected(PalettePreset.VIVID))
                markAndHold("newproject-vivid", ack)
                mark("done")
                return false
            }
            DemoScript.EXPORT -> {
                delay(1_500)
                onEvent(EditorEvent.ExportOpened)
                mark("export")
                delay(1_000)
                onEvent(EditorEvent.ExportConfirmed)
                awaitExportEnd(state)
                mark("exported")
                delay(1_500)
                return true
            }
            DemoScript.WALK -> {
                walk(state, onEvent, ack)
                return false
            }
        }
    }

    private suspend fun awaitExportEnd(state: () -> EditorUiState) {
        repeat(40) {
            delay(250)
            val phase = state().exportSheet?.phase
            if (phase == ExportPhase.DONE || phase == ExportPhase.FAILED) return
        }
    }

    private fun target(x: Int, y: Int, z: Int) = SceneHit.Target(Cell(x, y, z))

    private const val TAG = "GridSpaceDemo"

    /** Waits for the state to be drawn, then logs the marker a screenshot loop syncs on. */
    private suspend fun mark(name: String) {
        repeat(2) { runCatching { withFrameNanos { } } }
        delay(SETTLE_MS)
        Log.i(TAG, "step $name")
    }

    /**
     * Keeps the current state for at least [HOLD_MS], then until the capture script acknowledges
     * [name] (or [ACK_TIMEOUT_MS] passes). [during] runs once per poll so a step can keep acting
     * (the rapid redos) while the host is still shooting.
     */
    private suspend fun holdFor(name: String, ack: () -> String?, during: suspend () -> Unit = {}) {
        val start = System.currentTimeMillis()
        while (true) {
            during()
            val elapsed = System.currentTimeMillis() - start
            if (elapsed >= HOLD_MS && (ack() == name || elapsed >= ACK_TIMEOUT_MS)) return
            delay(ACK_POLL_MS)
        }
    }

    /** Logs [name] and holds the state until it is captured. */
    private suspend fun markAndHold(name: String, ack: () -> String?) {
        mark(name)
        holdFor(name, ack)
    }

    /** Pinch-hold at one cell, log [marker], keep it visible until captured, then release. */
    private suspend fun hold(onEvent: (EditorEvent) -> Unit, from: SceneHit, marker: String?, ack: () -> String?) {
        onEvent(EditorEvent.PreviewStarted(from))
        if (marker != null) {
            delay(WINDOW_SETTLE_MS)
            markAndHold(marker, ack)
        } else {
            delay(HOLD_MS)
        }
        onEvent(EditorEvent.Committed)
    }

    /**
     * R4 walk (spec K4.33, R8 steps): ghost with footprint and guide column (1, 2, 2) -> occupied
     * (1, 0, 0) -> size (3 x 2 x 1 at (1, 0, 1)) -> erase -> paint -> selection -> drag-move
     * preview and commit -> rotate / duplicate / step / delete -> rail (Vivid + swatch) -> colour
     * edit -> eyedropper -> close -> language toggle and back -> undo x3 -> redo burst -> reset ->
     * export -> projects sheet -> done.
     */
    private suspend fun walk(state: () -> EditorUiState, onEvent: (EditorEvent) -> Unit, ack: () -> String?) {
        delay(STEP_MS)
        // Add: a floating ghost over the free strip left of the floor (shows the footprint on the
        // plate and the guide column; (3, 2, 5) sat over a floor cube that hid the footprint), then
        // the cube just placed at (1, 0, 0) again: it stands on the free plate, so the coral
        // footprint rim shows around it (the pillar base of R3 sat on a floor cube that hid it).
        onEvent(EditorEvent.ModeSelected(EditMode.ADD))
        hold(onEvent, target(1, 2, 2), "ghost", ack)
        delay(STEP_MS / 2)
        hold(onEvent, target(1, 0, 0), null, ack)
        delay(STEP_MS / 2)
        hold(onEvent, target(1, 0, 0), "occupied", ack)
        delay(STEP_MS / 2)
        // Sized brush (R8): 3 x 2 x 1 from (1, 0, 1) on the free strip left of the floor; all six
        // cells are free, so the commit adds exactly 6 cubes (voxdump check in the report).
        onEvent(EditorEvent.SizeStep(Axis.X, 2))
        onEvent(EditorEvent.SizeStep(Axis.Y, 1))
        hold(onEvent, target(1, 0, 1), "size", ack)
        // U6: the size survives a mode round trip.
        onEvent(EditorEvent.ModeSelected(EditMode.ERASE))
        onEvent(EditorEvent.ModeSelected(EditMode.ADD))
        delay(STEP_MS / 2)
        // The row of five cubes at z = 14 that the selection steps use later: a 5 x 1 x 1 tap
        // (no marker), then the size goes back to 1 x 1 x 1 for the erase / paint steps.
        onEvent(EditorEvent.SizeStep(Axis.X, 2))
        onEvent(EditorEvent.SizeStep(Axis.Y, -1))
        onEvent(EditorEvent.SceneTapped(target(8, 0, 14)))
        delay(STEP_MS / 2)
        onEvent(EditorEvent.SizeStep(Axis.X, -4))
        // Erase the floating cube again.
        onEvent(EditorEvent.ModeSelected(EditMode.ERASE))
        hold(onEvent, target(1, 2, 2), "erase", ack)
        delay(STEP_MS / 2)
        // Paint two cubes with slot 2 (mint).
        onEvent(EditorEvent.ModeSelected(EditMode.PAINT))
        onEvent(EditorEvent.SlotTapped(1))
        hold(onEvent, target(1, 0, 0), "paint", ack)
        onEvent(EditorEvent.SceneTapped(target(3, 0, 3)))
        delay(STEP_MS / 2)
        // Select the row of five, drag-move it by (+2, 0, -1) onto the free strip at z = 13, then
        // rotate, duplicate, nudge, delete, cancel.
        onEvent(EditorEvent.ModeSelected(EditMode.SELECT))
        for (x in 8..12) onEvent(EditorEvent.SceneTapped(target(x, 0, 14)))
        // The selection panel Augment is created here: let its glass settle before the marker (R8).
        delay(WINDOW_SETTLE_MS)
        markAndHold("selection", ack)
        onEvent(EditorEvent.SelectionDragStarted(Cell(8, 0, 14)))
        delay(300)
        onEvent(EditorEvent.SelectionDragMoved(Cell(2, 0, -1)))
        delay(WINDOW_SETTLE_MS)
        markAndHold("dragmove", ack)
        onEvent(EditorEvent.SelectionDragEnded)
        markAndHold("dragmove-commit", ack)
        onEvent(EditorEvent.SelectionRotate)
        markAndHold("rotate", ack)
        onEvent(EditorEvent.SelectionDuplicate)
        markAndHold("duplicate", ack)
        onEvent(EditorEvent.SelectionStep(Axis.X, 1))
        markAndHold("stepx", ack)
        onEvent(EditorEvent.SelectionDelete)
        markAndHold("delete", ack)
        onEvent(EditorEvent.SelectionClear)
        delay(STEP_MS / 2)
        // Colour rail: Vivid page, pick its first swatch into the current slot (U7), place one
        // cube with it; then the edit popup, the eyedropper (picks the coral pillar), close.
        onEvent(EditorEvent.ModeSelected(EditMode.ADD))
        onEvent(EditorEvent.SlotTapped(0))
        onEvent(EditorEvent.PresetSelected(PalettePreset.VIVID))
        onEvent(EditorEvent.SwatchPicked(PalettePreset.VIVID.colors[0]))
        // ADD after SELECT recreates the brush-row Augment: let its glass settle before the marker.
        delay(WINDOW_SETTLE_MS)
        markAndHold("vivid", ack)
        onEvent(EditorEvent.SceneTapped(target(0, 0, 1)))
        delay(STEP_MS / 2)
        onEvent(EditorEvent.ColorEditOpened)
        delay(WINDOW_SETTLE_MS)
        markAndHold("edit", ack)
        onEvent(EditorEvent.EyedropperStarted)
        delay(WINDOW_SETTLE_MS)
        markAndHold("eyedrop", ack)
        onEvent(EditorEvent.SceneTapped(SceneHit.Voxel(Cell(3, 1, 12), null)))
        delay(STEP_MS / 2)
        onEvent(EditorEvent.PickerDismissed)
        delay(WINDOW_SETTLE_MS)
        markAndHold("edit-close", ack)
        // Language: one frame in the other language, then back (K4.29 / U8).
        val language = AppGraph.language.value
        onEvent(EditorEvent.LanguageSelected(language.other))
        markAndHold("lang", ack)
        onEvent(EditorEvent.LanguageSelected(language))
        markAndHold("lang-back", ack)
        // History: three undos one second apart, then three redos. "burst" is logged right before
        // the redos, which run one second apart while the host shoots the rail header through the
        // rapid autosaves; "redo" then marks the settled state.
        repeat(3) {
            onEvent(EditorEvent.Undo)
            delay(RAPID_MS)
        }
        markAndHold("undo", ack)
        mark("burst")
        var redos = 0
        var nextRedoAt = System.currentTimeMillis()
        holdFor("burst", ack) {
            if (redos < 3 && System.currentTimeMillis() >= nextRedoAt) {
                onEvent(EditorEvent.Redo)
                redos++
                nextRedoAt += RAPID_MS
            }
        }
        while (redos < 3) {
            onEvent(EditorEvent.Redo)
            redos++
            delay(RAPID_MS)
        }
        markAndHold("redo", ack)
        onEvent(EditorEvent.ResetView)
        markAndHold("reset", ack)
        // Export: the idle sheet, then the success state, then closed.
        onEvent(EditorEvent.ExportOpened)
        markAndHold("export", ack)
        onEvent(EditorEvent.ExportConfirmed)
        awaitExportEnd(state)
        markAndHold("exported", ack)
        onEvent(EditorEvent.ExportConfirmed)
        delay(STEP_MS / 2)
        // Projects sheet (branch A), then closed.
        onEvent(EditorEvent.ProjectSheetOpened)
        markAndHold("projects", ack)
        onEvent(EditorEvent.ProjectSheetDismissed)
        mark("done")
    }
}
