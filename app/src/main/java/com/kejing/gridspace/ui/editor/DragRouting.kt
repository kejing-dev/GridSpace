package com.kejing.gridspace.ui.editor

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.EditMode

/** Which gesture a pinch-drag drives, decided once from what the drag started on. */
enum class DragKind { NONE, ORBIT, EDIT, MOVE }

/**
 * Pure routing of a drag start (spec R4 O.2.1, QA N4-05), shared by every input source:
 *
 *  - scene locked                                  -> NONE
 *  - nothing / the catcher                         -> ORBIT
 *  - SELECT mode: a selected cube                  -> MOVE (whole selection, whole cells)
 *  - SELECT mode: another cube                     -> EDIT (SelectCell preview, toggle on release)
 *  - SELECT mode: the base plate                   -> ORBIT (like empty space; the plate is not a target)
 *  - any other mode: a cube or the plate           -> EDIT (the mode's preview)
 */
object DragRouting {
    fun route(locked: Boolean, mode: EditMode, cell: Cell?, plate: Boolean, inSelection: Boolean): DragKind =
        when {
            locked -> DragKind.NONE
            cell == null && !plate -> DragKind.ORBIT
            mode == EditMode.SELECT && cell == null -> DragKind.ORBIT
            mode == EditMode.SELECT && inSelection -> DragKind.MOVE
            else -> DragKind.EDIT
        }
}
