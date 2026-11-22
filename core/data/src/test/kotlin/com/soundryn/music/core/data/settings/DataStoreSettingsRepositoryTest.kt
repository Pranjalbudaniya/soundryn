package com.soundryn.music.core.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.soundryn.music.core.domain.DomainError
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.SettingsRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun observeEmitsDefaultWhenNothingStored() = runTest {
        val file = File(tempFolder.root, "test1.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        assertEquals(false, repo.observe(SettingsRegistry.ReduceMotion).first())
        assertEquals(true, repo.observe(SettingsRegistry.HapticsEnabled).first())
        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())
        assertEquals(0.5f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun setToggleThenObserveEmitsNewValue() = runTest {
        val file = File(tempFolder.root, "test2.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        val result = repo.set(SettingsRegistry.BlurEnabled, false)
        assertTrue(result is Outcome.Success)
        assertEquals(false, repo.observe(SettingsRegistry.BlurEnabled).first())
    }

    @Test
    fun setRangeStoresValueInsideRange() = runTest {
        val file = File(tempFolder.root, "test3.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        val result = repo.set(SettingsRegistry.BlurStrength, 0.8f)
        assertTrue(result is Outcome.Success)
        assertEquals(0.8f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun setRangeClampsOutOfRangeValue() = runTest {
        val file = File(tempFolder.root, "test4.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        repo.set(SettingsRegistry.BlurStrength, 5f)
        assertEquals(1f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)

        repo.set(SettingsRegistry.BlurStrength, -5f)
        assertEquals(0f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun setRangeNonFiniteStoresDefault() = runTest {
        val file = File(tempFolder.root, "test5.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        repo.set(SettingsRegistry.BlurStrength, 0.9f)
        assertEquals(0.9f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)

        val result = repo.set(SettingsRegistry.BlurStrength, Float.NaN)
        assertTrue(result is Outcome.Success)
        assertEquals(0.5f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun valuesSurviveNewRepositoryOverSameFile() = runTest {
        val file = File(tempFolder.root, "test6.preferences_pb")
        val scope1 = CoroutineScope(Job() + Dispatchers.IO)
        val store1 = createSettingsDataStore(scope1, file)
        val repo1 = DataStoreSettingsRepository(store1)

        repo1.set(SettingsRegistry.BlurEnabled, false)
        repo1.set(SettingsRegistry.BlurStrength, 0.2f)
        scope1.cancel()

        val store2 = createSettingsDataStore(backgroundScope, file)
        val repo2 = DataStoreSettingsRepository(store2)

        assertEquals(false, repo2.observe(SettingsRegistry.BlurEnabled).first())
        assertEquals(0.2f, repo2.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun resetOneSettingKeepsOthers() = runTest {
        val file = File(tempFolder.root, "test7.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        repo.set(SettingsRegistry.BlurEnabled, false)
        repo.set(SettingsRegistry.HapticsEnabled, false)

        val resetResult = repo.reset(SettingsRegistry.BlurEnabled)
        assertTrue(resetResult is Outcome.Success)

        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())
        assertEquals(false, repo.observe(SettingsRegistry.HapticsEnabled).first())
    }

    @Test
    fun resetAllRestoresEveryDefault() = runTest {
        val file = File(tempFolder.root, "test8.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        repo.set(SettingsRegistry.ReduceMotion, true)
        repo.set(SettingsRegistry.HapticsEnabled, false)
        repo.set(SettingsRegistry.BlurEnabled, false)
        repo.set(SettingsRegistry.BlurStrength, 0.9f)

        val resetResult = repo.resetAll()
        assertTrue(resetResult is Outcome.Success)

        assertEquals(false, repo.observe(SettingsRegistry.ReduceMotion).first())
        assertEquals(true, repo.observe(SettingsRegistry.HapticsEnabled).first())
        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())
        assertEquals(0.5f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun storedValueOfWrongTypeReadsAsDefault() = runTest {
        val file = File(tempFolder.root, "test9.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        store.edit { prefs ->
            prefs[floatPreferencesKey(SettingsRegistry.BlurEnabled.id)] = 1.0f
            prefs[booleanPreferencesKey(SettingsRegistry.BlurStrength.id)] = true
        }

        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())
        assertEquals(0.5f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun storedRangeOutsideRangeIsClampedOnRead() = runTest {
        val file = File(tempFolder.root, "test10.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        store.edit { prefs ->
            prefs[floatPreferencesKey(SettingsRegistry.BlurStrength.id)] = 7f
        }
        assertEquals(1f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)

        store.edit { prefs ->
            prefs[floatPreferencesKey(SettingsRegistry.BlurStrength.id)] = -2f
        }
        assertEquals(0f, repo.observe(SettingsRegistry.BlurStrength).first(), 0.0001f)
    }

    @Test
    fun unknownStoredKeysAreIgnoredAndClearedByResetAll() = runTest {
        val file = File(tempFolder.root, "test11.preferences_pb")
        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        store.edit { prefs ->
            prefs[booleanPreferencesKey("old.feature")] = true
        }

        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())

        repo.resetAll()
        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())

        val remaining = store.data.first().asMap()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun corruptFileReadsAsDefaultsAndStaysWritable() = runTest {
        val file = File(tempFolder.root, "test12.preferences_pb")
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

        val store = createSettingsDataStore(backgroundScope, file)
        val repo = DataStoreSettingsRepository(store)

        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())

        val setResult = repo.set(SettingsRegistry.BlurEnabled, false)
        assertTrue(setResult is Outcome.Success)
        assertEquals(false, repo.observe(SettingsRegistry.BlurEnabled).first())
    }

    @Test
    fun unwritableStoreReturnsStorageFailure() = runTest {
        val regularFile = tempFolder.newFile("regular_parent_file")
        val unwritableFile = File(regularFile, "settings.preferences_pb")

        val store = createSettingsDataStore(backgroundScope, unwritableFile)
        val repo = DataStoreSettingsRepository(store)

        val result = repo.set(SettingsRegistry.BlurEnabled, false)
        assertTrue(result is Outcome.Failure)
        assertEquals(DomainError.Storage, (result as Outcome.Failure).error)

        assertEquals(true, repo.observe(SettingsRegistry.BlurEnabled).first())
    }
}
