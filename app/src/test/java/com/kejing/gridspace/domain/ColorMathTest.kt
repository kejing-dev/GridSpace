package com.kejing.gridspace.domain

import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.PalettePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {
    @Test
    fun hexRoundTrips() {
        assertEquals("#f3bc9c", ColorMath.toHex(ColorMath.parseHex("#F3BC9C")))
        assertEquals(0xFFA5EDCC.toInt(), ColorMath.parseHex("#a5edcc"))
    }

    @Test
    fun hslRoundTripsWithinOneStep() {
        for (preset in PalettePreset.entries) {
            assertEquals(24, preset.colors.size)
            for (color in preset.colors) {
                val hsl = ColorMath.toHsl(color)
                val back = ColorMath.fromHsl(hsl.h, hsl.s, hsl.l)
                assertTrue(ColorMath.toHex(color) + " vs " + ColorMath.toHex(back), ColorMath.distanceSquared(color, back) <= 3)
            }
        }
    }

    @Test
    fun nearWhiteDetection() {
        assertTrue(ColorMath.isNearWhite(ColorMath.parseHex("#f3f5ed")))
        assertTrue(!ColorMath.isNearWhite(ColorMath.parseHex("#2d383b")))
    }
}
