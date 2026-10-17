package com.kejing.gridspace.ui

import com.kejing.gridspace.data.untitledNumber
import com.kejing.gridspace.ui.theme.untitledTemplateRegex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** QA N7-01: the untitled-name pattern must capture the number in every language. */
class UntitledPatternTest {
    private val pattern = Regex(listOf("Untitled %1\$d", "未命名 %1\$d").joinToString("|") { untitledTemplateRegex(it) })

    @Test
    fun englishNumberIsCaptured() = assertEquals(12, pattern.untitledNumber("Untitled 12"))

    @Test
    fun chineseNumberIsCaptured() = assertEquals(3, pattern.untitledNumber("未命名 3"))

    @Test
    fun otherNamesDoNotMatch() {
        assertNull(pattern.untitledNumber("庭院小亭"))
        assertNull(pattern.untitledNumber("Untitled"))
        assertNull(pattern.untitledNumber("Untitled 1 copy"))
    }

    @Test
    fun templateCharactersAreEscaped() {
        val p = Regex(untitledTemplateRegex("Draft (%1\$d)"))
        assertEquals(7, p.untitledNumber("Draft (7)"))
        assertNull(p.untitledNumber("Draft 7"))
    }
}
