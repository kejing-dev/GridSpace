package com.kejing.gridspace.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** ARGB helpers and plain HSL conversion, kept free of android.graphics so tests run on the JVM. */
object ColorMath {
    fun parseHex(text: String): Int {
        val clean = text.trim().removePrefix("#")
        require(clean.length == 6) { "Expected #rrggbb, got $text" }
        return (0xFF shl 24) or clean.toInt(16)
    }

    fun toHex(argb: Int): String = "#%06x".format(argb and 0xFFFFFF)

    fun red(argb: Int): Int = (argb shr 16) and 0xFF

    fun green(argb: Int): Int = (argb shr 8) and 0xFF

    fun blue(argb: Int): Int = argb and 0xFF

    fun rgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Squared RGB distance, used to merge colours when a model has more than 255. */
    fun distanceSquared(a: Int, b: Int): Int {
        val dr = red(a) - red(b)
        val dg = green(a) - green(b)
        val db = blue(a) - blue(b)
        return dr * dr + dg * dg + db * db
    }

    /** True when the colour is so light that a white ring would vanish against it. */
    fun isNearWhite(argb: Int): Boolean = red(argb) + green(argb) + blue(argb) > 3 * 225

    /** Linear RGB blend: [weight] = 0 gives [from], 1 gives [to]. Alpha is forced opaque. */
    fun mix(from: Int, to: Int, weight: Float): Int {
        val w = weight.coerceIn(0f, 1f)
        fun channel(a: Int, b: Int) = (a + (b - a) * w).toInt().coerceIn(0, 255)
        return rgb(channel(red(from), red(to)), channel(green(from), green(to)), channel(blue(from), blue(to)))
    }

    /** h in 0..360, s and l in 0..100. */
    data class Hsl(val h: Float, val s: Float, val l: Float)

    fun toHsl(argb: Int): Hsl {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (max == min) return Hsl(0f, 0f, l * 100f)
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        var h =
            when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
        h *= 60f
        return Hsl(h, s * 100f, l * 100f)
    }

    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val hh = ((h % 360f) + 360f) % 360f / 360f
        val ss = (s / 100f).coerceIn(0f, 1f)
        val ll = (l / 100f).coerceIn(0f, 1f)
        if (ss == 0f) {
            val v = (ll * 255f).roundToInt()
            return rgb(v, v, v)
        }
        val q = if (ll < 0.5f) ll * (1f + ss) else ll + ss - ll * ss
        val p = 2f * ll - q
        fun channel(t0: Float): Int {
            var t = t0
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            val v =
                when {
                    t < 1f / 6f -> p + (q - p) * 6f * t
                    t < 1f / 2f -> q
                    t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                    else -> p
                }
            return (v * 255f).roundToInt()
        }
        return rgb(channel(hh + 1f / 3f), channel(hh), channel(hh - 1f / 3f))
    }

    /** Hue is meaningless for greys; keep the previous hue so the slider does not jump to 0. */
    fun keepHueIfGrey(previous: Hsl, next: Hsl): Hsl =
        if (abs(next.s) < 0.5f) next.copy(h = previous.h) else next
}
