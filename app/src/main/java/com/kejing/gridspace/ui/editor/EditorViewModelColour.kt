package com.kejing.gridspace.ui.editor

import com.kejing.gridspace.domain.model.ColorMath
import kotlinx.coroutines.flow.update

/**
 * Colour slots, the persistent colour rail and the colour-edit popup (design spec D, R4 N.3).
 * Slot colours belong to the project and are auto-saved, but they never enter the undo history.
 * The slot being edited is always the current slot: tapping a slot makes it current, a swatch
 * writes into it at once, and "Edit colour" opens the popup for it. There is no long press.
 */
internal fun EditorViewModel.onColourEvent(event: EditorEvent) {
    when (event) {
        is EditorEvent.SlotTapped -> {
            setActiveSlot(event.index)
            _state.update { s -> s.copy(picker = s.picker?.copy(hsl = hslOfSlot(event.index), eyedropper = false)) }
        }
        is EditorEvent.PresetSelected -> _state.update { it.copy(lastPreset = event.preset) }
        is EditorEvent.SwatchPicked -> writeActiveSlot(event.color)
        is EditorEvent.HslChanged -> {
            _state.update { it.copy(picker = it.picker?.copy(hsl = event.hsl)) }
            writeActiveSlot(ColorMath.fromHsl(event.hsl.h, event.hsl.s, event.hsl.l), keepHsl = true)
        }
        EditorEvent.ColorEditOpened ->
            _state.update { s ->
                val slot = s.project?.activeSlot ?: return@update s
                s.copy(picker = PickerState(hsl = hslOfSlot(slot)), preview = null)
            }
        EditorEvent.EyedropperStarted -> _state.update { it.copy(picker = it.picker?.copy(eyedropper = true), preview = null) }
        EditorEvent.EyedropperCancelled -> _state.update { it.copy(picker = it.picker?.copy(eyedropper = false)) }
        EditorEvent.PickerDismissed -> _state.update { it.copy(picker = null) }
        else -> Unit
    }
}

private fun EditorViewModel.setActiveSlot(index: Int) {
    _state.update { s ->
        val project = s.project ?: return@update s
        if (project.activeSlot == index) s else s.copy(project = project.copy(activeSlot = index))
    }
    scheduleSave()
}

private fun EditorViewModel.hslOfSlot(index: Int): ColorMath.Hsl {
    val color = _state.value.project?.slots?.getOrNull(index) ?: 0
    return ColorMath.toHsl(color)
}

/** Writes the colour straight into the current slot; the rail and the next add / paint use it. */
private fun EditorViewModel.writeActiveSlot(color: Int, keepHsl: Boolean = false) {
    _state.update { s ->
        val project = s.project ?: return@update s
        val slots = project.slots.toMutableList().also { it[project.activeSlot] = color }
        val picker = s.picker
        val nextPicker =
            when {
                picker == null -> null
                keepHsl -> picker
                else -> picker.copy(hsl = ColorMath.keepHueIfGrey(picker.hsl, ColorMath.toHsl(color)))
            }
        s.copy(project = project.copy(slots = slots), picker = nextPicker)
    }
    scheduleSave()
}

/** Eyedropper hit a cube: take its colour and return the popup to its full state. */
internal fun EditorViewModel.pickFromCube(color: Int) {
    writeActiveSlot(color)
    _state.update { it.copy(picker = it.picker?.copy(eyedropper = false)) }
}
