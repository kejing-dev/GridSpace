package com.kejing.gridspace.ui

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode
import com.kejing.gridspace.ui.editor.DragKind
import com.kejing.gridspace.ui.editor.DragRouting
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec R4 O.2.1 drag-start routing; QA N4-05: in SELECT mode the plate orbits. */
class DragRoutingTest {
    private val cube = Cell(3, 1, 12)

    @Test
    fun lockedSceneRoutesNothing() {
        assertEquals(DragKind.NONE, DragRouting.route(locked = true, mode = EditMode.SELECT, cell = cube, plate = false, inSelection = true))
    }

    @Test
    fun emptySpaceOrbitsInEveryMode() {
        for (mode in EditMode.entries) {
            assertEquals(mode.name, DragKind.ORBIT, DragRouting.route(false, mode, cell = null, plate = false, inSelection = false))
        }
    }

    @Test
    fun selectModePlateOrbits() {
        assertEquals(DragKind.ORBIT, DragRouting.route(false, EditMode.SELECT, cell = null, plate = true, inSelection = false))
    }

    @Test
    fun selectModeSelectedCubeMoves() {
        assertEquals(DragKind.MOVE, DragRouting.route(false, EditMode.SELECT, cell = cube, plate = false, inSelection = true))
    }

    @Test
    fun selectModeOtherCubeEdits() {
        assertEquals(DragKind.EDIT, DragRouting.route(false, EditMode.SELECT, cell = cube, plate = false, inSelection = false))
    }

    @Test
    fun addModePlateAndCubeEdit() {
        assertEquals(DragKind.EDIT, DragRouting.route(false, EditMode.ADD, cell = null, plate = true, inSelection = false))
        assertEquals(DragKind.EDIT, DragRouting.route(false, EditMode.ADD, cell = cube, plate = false, inSelection = false))
        assertEquals(DragKind.EDIT, DragRouting.route(false, EditMode.ERASE, cell = cube, plate = false, inSelection = true))
    }
}
