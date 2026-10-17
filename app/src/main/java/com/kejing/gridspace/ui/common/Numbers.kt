package com.kejing.gridspace.ui.common

/**
 * Integer with the typographic minus U+2212 (spec R4 O.2.3 / S.8, QA N4-07): "−3", "0" and, with
 * [plus], "+2" (the drag-move label shows a Δ with its sign; the selection panel shows an
 * accumulated value, so it leaves the "+" out).
 */
fun signed(v: Int, plus: Boolean = true): String =
    when {
        v > 0 -> if (plus) "+$v" else "$v"
        v < 0 -> "−${-v}"
        else -> "0"
    }
