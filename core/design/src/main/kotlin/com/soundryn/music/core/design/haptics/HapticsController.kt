package com.soundryn.music.core.design.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class Haptics(
    private val tokens: Map<String, String>,
    private val enabled: Boolean,
    private val feedback: HapticFeedback,
) {
    fun perform(role: String) {
        val kind = HapticsPolicy.kindFor(tokens, role, enabled) ?: return
        feedback.performHapticFeedback(type(kind))
    }

    private fun type(kind: HapticKind): HapticFeedbackType = when (kind) {
        HapticKind.VirtualKey -> HapticFeedbackType.VirtualKey
        HapticKind.ContextClick -> HapticFeedbackType.ContextClick
        HapticKind.LongPress -> HapticFeedbackType.LongPress
        HapticKind.Confirm -> HapticFeedbackType.Confirm
        HapticKind.Reject -> HapticFeedbackType.Reject
        HapticKind.ToggleOn -> HapticFeedbackType.ToggleOn
        HapticKind.ToggleOff -> HapticFeedbackType.ToggleOff
        HapticKind.SegmentTick -> HapticFeedbackType.SegmentTick
        HapticKind.SegmentFrequentTick -> HapticFeedbackType.SegmentFrequentTick
        HapticKind.GestureThresholdActivate -> HapticFeedbackType.GestureThresholdActivate
        HapticKind.GestureEnd -> HapticFeedbackType.GestureEnd
    }
}

private val NoOpHapticFeedback = object : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

val LocalHaptics = staticCompositionLocalOf {
    Haptics(emptyMap(), false, NoOpHapticFeedback)
}

@Composable
fun ProvideHaptics(
    tokens: Map<String, String>,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val feedback = LocalHapticFeedback.current
    val haptics = remember(tokens, enabled, feedback) {
        Haptics(tokens, enabled, feedback)
    }
    CompositionLocalProvider(LocalHaptics provides haptics, content = content)
}
