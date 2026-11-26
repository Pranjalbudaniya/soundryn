package com.soundryn.music.core.design.blur

import org.junit.Assert.assertEquals
import org.junit.Test

class BlurPolicyTest {

    @Test
    fun disabledSettingTurnsBlurOff() {
        assertEquals(
            BlurDecision(BlurState.OffBySetting, 0f),
            BlurPolicy.decide(enabled = false, strength = 0.5f, maxRadius = 24f, slowDevice = false),
        )
        assertEquals(
            BlurDecision(BlurState.OffBySetting, 0f),
            BlurPolicy.decide(enabled = false, strength = 0.5f, maxRadius = 24f, slowDevice = true),
        )
    }

    @Test
    fun slowDeviceTurnsBlurOff() {
        assertEquals(
            BlurDecision(BlurState.OffSlowDevice, 0f),
            BlurPolicy.decide(enabled = true, strength = 0.5f, maxRadius = 24f, slowDevice = true),
        )
    }

    @Test
    fun strengthScalesTheMaxRadius() {
        assertEquals(
            BlurDecision(BlurState.On, 12f),
            BlurPolicy.decide(enabled = true, strength = 0.5f, maxRadius = 24f, slowDevice = false),
        )
        assertEquals(
            BlurDecision(BlurState.On, 24f),
            BlurPolicy.decide(enabled = true, strength = 1f, maxRadius = 24f, slowDevice = false),
        )
        assertEquals(
            BlurDecision(BlurState.On, 0f),
            BlurPolicy.decide(enabled = true, strength = 0f, maxRadius = 24f, slowDevice = false),
        )
    }

    @Test
    fun strengthOutOfRangeIsClamped() {
        assertEquals(
            BlurDecision(BlurState.On, 24f),
            BlurPolicy.decide(enabled = true, strength = 1.5f, maxRadius = 24f, slowDevice = false),
        )
        assertEquals(
            BlurDecision(BlurState.On, 0f),
            BlurPolicy.decide(enabled = true, strength = -0.5f, maxRadius = 24f, slowDevice = false),
        )
        assertEquals(
            BlurDecision(BlurState.On, 0f),
            BlurPolicy.decide(enabled = true, strength = Float.NaN, maxRadius = 24f, slowDevice = false),
        )
    }

    @Test
    fun badMaxRadiusGivesZeroRadius() {
        val badMaxRadii = listOf(Float.NaN, -4f, Float.POSITIVE_INFINITY)
        for (maxRadius in badMaxRadii) {
            assertEquals(
                BlurDecision(BlurState.On, 0f),
                BlurPolicy.decide(enabled = true, strength = 0.5f, maxRadius = maxRadius, slowDevice = false),
            )
        }
    }
}
