package com.soundryn.music.core.design.haptics

enum class HapticKind(val token: String) {
    VirtualKey("virtualKey"),
    ContextClick("contextClick"),
    LongPress("longPress"),
    Confirm("confirm"),
    Reject("reject"),
    ToggleOn("toggleOn"),
    ToggleOff("toggleOff"),
    SegmentTick("segmentTick"),
    SegmentFrequentTick("segmentFrequentTick"),
    GestureThresholdActivate("gestureThresholdActivate"),
    GestureEnd("gestureEnd");

    companion object {
        private val byToken = entries.associateBy { it.token }

        fun fromToken(token: String): HapticKind? = byToken[token]
    }
}

object HapticRole {
    const val Tap = "tap"
    const val ToggleOn = "toggleOn"
    const val ToggleOff = "toggleOff"
    const val LongPress = "longPress"
    const val Confirm = "confirm"
    const val Reject = "reject"
    const val Tick = "tick"
    const val TickFast = "tickFast"
    const val Threshold = "threshold"
    const val GestureEnd = "gestureEnd"

    val all: List<String> = listOf(
        Tap,
        ToggleOn,
        ToggleOff,
        LongPress,
        Confirm,
        Reject,
        Tick,
        TickFast,
        Threshold,
        GestureEnd,
    )
}

object HapticsPolicy {
    fun kindFor(
        haptics: Map<String, String>,
        role: String,
        enabled: Boolean,
    ): HapticKind? {
        if (!enabled) return null
        val token = haptics[role] ?: return null
        return HapticKind.fromToken(token)
    }
}
