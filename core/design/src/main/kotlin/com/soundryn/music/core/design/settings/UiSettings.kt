package com.soundryn.music.core.design.settings

import com.soundryn.music.core.domain.SettingsRegistry
import com.soundryn.music.core.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class UiSettings(
    val reduceMotion: Boolean,
    val hapticsEnabled: Boolean,
    val blurEnabled: Boolean,
    val blurStrength: Float,
)

fun uiSettingsFlow(settings: SettingsRepository): Flow<UiSettings> = combine(
    settings.observe(SettingsRegistry.ReduceMotion),
    settings.observe(SettingsRegistry.HapticsEnabled),
    settings.observe(SettingsRegistry.BlurEnabled),
    settings.observe(SettingsRegistry.BlurStrength),
) { reduceMotion, hapticsEnabled, blurEnabled, blurStrength ->
    UiSettings(
        reduceMotion = reduceMotion,
        hapticsEnabled = hapticsEnabled,
        blurEnabled = blurEnabled,
        blurStrength = blurStrength,
    )
}
