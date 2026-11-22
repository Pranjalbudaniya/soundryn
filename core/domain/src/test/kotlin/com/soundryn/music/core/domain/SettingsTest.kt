package com.soundryn.music.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun toggleExposesIdAndDefault() {
        val t = Setting.Toggle("a.b", true)
        assertEquals("a.b", t.id)
        assertTrue(t.default)
    }

    @Test
    fun rangeCoerceKeepsValueInsideRange() {
        val r = Setting.Range("a.b", 0.5f, 0f, 1f)
        assertEquals(0.25f, r.coerce(0.25f), 0.0001f)
        assertEquals(0f, r.coerce(0f), 0.0001f)
        assertEquals(1f, r.coerce(1f), 0.0001f)
    }

    @Test
    fun rangeCoerceClampsAboveMax() {
        val r = Setting.Range("a.b", 0.5f, 0f, 1f)
        assertEquals(1f, r.coerce(7f), 0.0001f)
    }

    @Test
    fun rangeCoerceClampsBelowMin() {
        val r = Setting.Range("a.b", 0.5f, 0f, 1f)
        assertEquals(0f, r.coerce(-3f), 0.0001f)
    }

    @Test
    fun rangeCoerceNonFiniteGivesDefault() {
        val r = Setting.Range("a.b", 0.5f, 0f, 1f)
        assertEquals(0.5f, r.coerce(Float.NaN), 0.0001f)
        assertEquals(0.5f, r.coerce(Float.POSITIVE_INFINITY), 0.0001f)
        assertEquals(0.5f, r.coerce(Float.NEGATIVE_INFINITY), 0.0001f)
    }

    @Test
    fun rangeRejectsDefaultOutsideRange() {
        assertThrows(IllegalArgumentException::class.java) {
            Setting.Range("a.b", 2f, 0f, 1f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Setting.Range("a.b", -0.1f, 0f, 1f)
        }
    }

    @Test
    fun rangeRejectsMinNotBelowMax() {
        assertThrows(IllegalArgumentException::class.java) {
            Setting.Range("a.b", 0f, 1f, 1f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Setting.Range("a.b", 0f, 2f, 1f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Setting.Range("a.b", 0f, Float.NaN, 1f)
        }
    }

    @Test
    fun settingAcceptsValidIds() {
        val validIds = listOf("motion.reduce", "blur.strength", "a.b", "a1.b_2", "feature.sub.item")
        for (id in validIds) {
            val s = Setting.Toggle(id, false)
            assertEquals(id, s.id)
        }
    }

    @Test
    fun settingRejectsInvalidIds() {
        val invalidIds = listOf(
            "",
            "motion",
            "Motion.reduce",
            "motion.Reduce",
            "motion.",
            ".reduce",
            "motion..reduce",
            "motion.re-duce",
            "motion.re duce",
            "1motion.reduce",
            "motion.1reduce",
            "_motion.reduce"
        )
        for (id in invalidIds) {
            assertThrows("Expected IllegalArgumentException for id: '$id'", IllegalArgumentException::class.java) {
                Setting.Toggle(id, false)
            }
        }
    }

    @Test
    fun registryIdsAreUnique() {
        val ids = SettingsRegistry.all.map { it.id }
        assertEquals(4, ids.size)
        assertEquals(4, ids.distinct().size)
    }

    @Test
    fun registryHoldsTheFourSeedSettings() {
        val all = SettingsRegistry.all
        assertEquals(4, all.size)

        assertEquals(SettingsRegistry.ReduceMotion, all[0])
        val reduceMotion = all[0] as Setting.Toggle
        assertEquals("motion.reduce", reduceMotion.id)
        assertEquals(false, reduceMotion.default)

        assertEquals(SettingsRegistry.HapticsEnabled, all[1])
        val haptics = all[1] as Setting.Toggle
        assertEquals("haptics.enabled", haptics.id)
        assertEquals(true, haptics.default)

        assertEquals(SettingsRegistry.BlurEnabled, all[2])
        val blur = all[2] as Setting.Toggle
        assertEquals("blur.enabled", blur.id)
        assertEquals(true, blur.default)

        assertEquals(SettingsRegistry.BlurStrength, all[3])
        val blurStrength = all[3] as Setting.Range
        assertEquals("blur.strength", blurStrength.id)
        assertEquals(0.5f, blurStrength.default, 0.0001f)
        assertEquals(0.0f, blurStrength.min, 0.0001f)
        assertEquals(1.0f, blurStrength.max, 0.0001f)
    }
}
