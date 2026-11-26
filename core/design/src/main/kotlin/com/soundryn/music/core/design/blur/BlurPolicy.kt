package com.soundryn.music.core.design.blur

enum class BlurState { On, OffBySetting, OffSlowDevice }

data class BlurDecision(val state: BlurState, val radiusDp: Float)

object BlurPolicy {
    fun decide(
        enabled: Boolean,
        strength: Float,
        maxRadius: Float,
        slowDevice: Boolean,
    ): BlurDecision {
        if (!enabled) return BlurDecision(BlurState.OffBySetting, 0f)
        if (slowDevice) return BlurDecision(BlurState.OffSlowDevice, 0f)
        val safeStrength = if (strength.isNaN()) 0f else strength.coerceIn(0f, 1f)
        val safeMax = if (!maxRadius.isFinite() || maxRadius < 0f) 0f else maxRadius
        return BlurDecision(BlurState.On, safeMax * safeStrength)
    }
}
