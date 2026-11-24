package com.soundryn.music.core.design.settings

import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Setting
import com.soundryn.music.core.domain.SettingsRegistry
import com.soundryn.music.core.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiSettingsTest {

    private class FakeSettingsRepository : SettingsRepository {
        val reduceMotion = MutableSharedFlow<Boolean>(replay = 1)
        val hapticsEnabled = MutableSharedFlow<Boolean>(replay = 1)
        val blurEnabled = MutableSharedFlow<Boolean>(replay = 1)
        val blurStrength = MutableSharedFlow<Float>(replay = 1)

        override fun observe(setting: Setting.Toggle): Flow<Boolean> = when (setting.id) {
            SettingsRegistry.ReduceMotion.id -> reduceMotion
            SettingsRegistry.HapticsEnabled.id -> hapticsEnabled
            SettingsRegistry.BlurEnabled.id -> blurEnabled
            else -> error("Unknown toggle: ${setting.id}")
        }

        override fun observe(setting: Setting.Range): Flow<Float> = when (setting.id) {
            SettingsRegistry.BlurStrength.id -> blurStrength
            else -> error("Unknown range: ${setting.id}")
        }

        override suspend fun set(setting: Setting.Toggle, value: Boolean): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun set(setting: Setting.Range, value: Float): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun reset(setting: Setting): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun resetAll(): Outcome<Unit> = Outcome.Success(Unit)
    }

    private suspend fun drain() {
        repeat(20) { yield() }
    }

    @Test
    fun noEmissionUntilAllFourSettingsAreRead() = runTest {
        val repo = FakeSettingsRepository()
        val collected = mutableListOf<UiSettings>()
        backgroundScope.launch {
            uiSettingsFlow(repo).collect { collected.add(it) }
        }
        drain()
        assertTrue(collected.isEmpty())

        repo.reduceMotion.emit(true)
        repo.hapticsEnabled.emit(false)
        repo.blurEnabled.emit(true)
        drain()
        assertTrue(collected.isEmpty())

        repo.blurStrength.emit(0.75f)
        drain()
        assertEquals(1, collected.size)
        assertEquals(
            UiSettings(
                reduceMotion = true,
                hapticsEnabled = false,
                blurEnabled = true,
                blurStrength = 0.75f,
            ),
            collected.single()
        )
    }

    @Test
    fun emitsAgainWhenOneSettingChanges() = runTest {
        val repo = FakeSettingsRepository()
        val collected = mutableListOf<UiSettings>()
        backgroundScope.launch {
            uiSettingsFlow(repo).collect { collected.add(it) }
        }
        drain()

        repo.reduceMotion.emit(false)
        repo.hapticsEnabled.emit(true)
        repo.blurEnabled.emit(true)
        repo.blurStrength.emit(0.5f)
        drain()
        assertEquals(1, collected.size)
        assertEquals(false, collected.last().reduceMotion)

        repo.reduceMotion.emit(true)
        drain()
        assertEquals(2, collected.size)
        assertEquals(true, collected.last().reduceMotion)
    }
}
