package com.soundryn.music.core.design.language

import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseColorTest {

    @Test
    fun baseAccentIsLightBlue() {
        val result = BaseLanguage.load()
        assertTrue(result is ParseResult.Success)
        val def = (result as ParseResult.Success).definition

        val variants = listOf(ColorVariant.Light, ColorVariant.Dark)
        val roles = listOf("primary", "primaryContainer", "inversePrimary")

        for (variant in variants) {
            val colors = checkNotNull(def.color[variant]) { "Missing color variant $variant" }
            for (role in roles) {
                val argb = checkNotNull(colors[role]) { "Missing role $variant.$role" }
                val hue = hsvHueDegrees(argb)
                val hex = String.format(Locale.US, "#%08X", argb)
                assertTrue(
                    "$variant.$role ($hex) hue $hue outside [190, 230]",
                    hue in 190f..230f,
                )
            }
        }
    }

    private fun hsvHueDegrees(argb: Int): Float {
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta == 0f) return 0f
        val rawHue = when (max) {
            r -> (g - b) / delta
            g -> 2f + (b - r) / delta
            else -> 4f + (r - g) / delta
        } * 60f
        return if (rawHue < 0f) rawHue + 360f else rawHue
    }
}
