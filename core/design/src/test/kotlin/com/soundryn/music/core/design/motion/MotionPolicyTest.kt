package com.soundryn.music.core.design.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionPolicyTest {

    @Test
    fun systemScaleZeroMeansOff() {
        assertEquals(MotionMode.Off, MotionPolicy.mode(false, 0f))
        assertEquals(MotionMode.Off, MotionPolicy.mode(true, 0f))
    }

    @Test
    fun userReduceMeansReduced() {
        assertEquals(MotionMode.Reduced, MotionPolicy.mode(true, 1f))
        assertEquals(MotionMode.Reduced, MotionPolicy.mode(true, 2f))
    }

    @Test
    fun defaultMeansFull() {
        assertEquals(MotionMode.Full, MotionPolicy.mode(false, 1f))
        assertEquals(MotionMode.Full, MotionPolicy.mode(false, 0.5f))
    }

    @Test
    fun badSystemScaleCountsAsOne() {
        val badScales = listOf(Float.NaN, -1f, Float.POSITIVE_INFINITY)
        for (scale in badScales) {
            assertEquals(MotionMode.Full, MotionPolicy.mode(false, scale))
            assertEquals(MotionMode.Reduced, MotionPolicy.mode(true, scale))
        }
    }

    @Test
    fun fullModeUsesTheRole() {
        val map = mapOf(
            MotionRole.Press to mapOf("stiffness" to 1400f, "dampingRatio" to 0.6f),
            MotionRole.Reduced to mapOf("stiffness" to 1500f, "dampingRatio" to 1.0f),
        )
        assertEquals(
            SpringToken(1400f, 0.6f),
            MotionPolicy.springFor(map, MotionRole.Press, MotionMode.Full)
        )
    }

    @Test
    fun reducedAndOffUseTheReducedRoleForEveryRole() {
        val map = mapOf(
            MotionRole.Press to mapOf("stiffness" to 1400f, "dampingRatio" to 0.6f),
            MotionRole.Expand to mapOf("stiffness" to 300f, "dampingRatio" to 0.75f),
            MotionRole.Reduced to mapOf("stiffness" to 1500f, "dampingRatio" to 1.0f),
        )
        val expected = SpringToken(1500f, 1.0f)
        for (mode in listOf(MotionMode.Reduced, MotionMode.Off)) {
            assertEquals(expected, MotionPolicy.springFor(map, MotionRole.Press, mode))
            assertEquals(expected, MotionPolicy.springFor(map, MotionRole.Expand, mode))
        }
    }

    @Test
    fun unknownRoleGivesNull() {
        val map = mapOf(
            MotionRole.Press to mapOf("stiffness" to 1400f, "dampingRatio" to 0.6f),
        )
        assertNull(MotionPolicy.springFor(map, "wobble", MotionMode.Full))
    }

    @Test
    fun incompleteOrBadRoleGivesNull() {
        val map = mapOf(
            "onlyStiffness" to mapOf("stiffness" to 1400f),
            "zeroStiffness" to mapOf("stiffness" to 0f, "dampingRatio" to 0.6f),
            "nanDamping" to mapOf("stiffness" to 1400f, "dampingRatio" to Float.NaN),
        )
        assertNull(MotionPolicy.springFor(map, "onlyStiffness", MotionMode.Full))
        assertNull(MotionPolicy.springFor(map, "zeroStiffness", MotionMode.Full))
        assertNull(MotionPolicy.springFor(map, "nanDamping", MotionMode.Full))
        assertNull(MotionPolicy.springFor(map, "onlyStiffness", MotionMode.Reduced))
    }
}
