package com.soundryn.music.core.design.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseLanguageTest {

    @Test
    fun baseLoadsFromResource() {
        val result = BaseLanguage.load()
        assertTrue(result is ParseResult.Success)
        val def = (result as ParseResult.Success).definition
        assertEquals("m3-expressive", def.id)
    }

    @Test
    fun baseHasNoWarnings() {
        val result = BaseLanguage.load()
        assertTrue(result is ParseResult.Success)
        val warnings = (result as ParseResult.Success).warnings
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun baseLightAndDarkDefineTheSameRoles() {
        val result = BaseLanguage.load()
        val def = (result as ParseResult.Success).definition
        val lightRoles = checkNotNull(def.color[ColorVariant.Light]?.keys)
        val darkRoles = checkNotNull(def.color[ColorVariant.Dark]?.keys)
        assertTrue(lightRoles.isNotEmpty())
        assertEquals(lightRoles, darkRoles)
    }

    @Test
    fun baseAmoledIsBlackSurfacesOnly() {
        val result = BaseLanguage.load()
        val def = (result as ParseResult.Success).definition
        val amoled = checkNotNull(def.color[ColorVariant.Amoled])
        val expectedKeys = setOf("background", "surface", "surfaceDim", "surfaceContainerLowest")
        assertEquals(expectedKeys, amoled.keys)
        for (key in expectedKeys) {
            assertEquals(0xFF000000.toInt(), amoled[key])
        }
    }

    @Test
    fun baseIsComplete() {
        val result = BaseLanguage.load()
        val def = (result as ParseResult.Success).definition

        assertTrue(def.shape.containsKey("none"))
        assertEquals(0f, def.shape["none"])
        assertTrue(def.shape.containsKey("full"))
        assertEquals(1000f, def.shape["full"])
        assertTrue(def.shape.size >= 10)

        assertTrue(def.type.size >= 15)
        for ((_, fields) in def.type) {
            assertTrue(fields.containsKey("size"))
            assertTrue(fields.containsKey("lineHeight"))
            assertTrue(fields.containsKey("weight"))
            assertTrue(fields.containsKey("letterSpacing"))
        }

        val expectedSpacing = setOf("xxs", "xs", "s", "m", "l", "xl", "xxl", "xxxl")
        assertEquals(expectedSpacing, def.spacing.keys)

        val expectedElevation = setOf("level0", "level1", "level2", "level3", "level4", "level5")
        assertEquals(expectedElevation, def.elevation.keys)

        assertEquals(setOf("maxRadius", "surfaceAlpha"), def.blur.keys)
    }

    @Test
    fun baseValuesMatchSpecifiedSpacingElevationBlur() {
        val result = BaseLanguage.load()
        val def = (result as ParseResult.Success).definition

        assertEquals(2f, def.spacing["xxs"])
        assertEquals(4f, def.spacing["xs"])
        assertEquals(8f, def.spacing["s"])
        assertEquals(12f, def.spacing["m"])
        assertEquals(16f, def.spacing["l"])
        assertEquals(24f, def.spacing["xl"])
        assertEquals(32f, def.spacing["xxl"])
        assertEquals(48f, def.spacing["xxxl"])

        assertEquals(0f, def.elevation["level0"])
        assertEquals(1f, def.elevation["level1"])
        assertEquals(3f, def.elevation["level2"])
        assertEquals(6f, def.elevation["level3"])
        assertEquals(8f, def.elevation["level4"])
        assertEquals(12f, def.elevation["level5"])

        assertEquals(24f, def.blur["maxRadius"])
        assertEquals(1.0f, def.blur["surfaceAlpha"])
    }
}
