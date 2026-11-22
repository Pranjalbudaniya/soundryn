package com.soundryn.music.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import com.soundryn.music.core.domain.DomainError
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Setting
import com.soundryn.music.core.domain.SettingsRepository
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

fun createSettingsDataStore(scope: CoroutineScope, file: File): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = { file }
    )
}

class DataStoreSettingsRepository(
    private val store: DataStore<Preferences>
) : SettingsRepository {

    override fun observe(setting: Setting.Toggle): Flow<Boolean> {
        val key = booleanPreferencesKey(setting.id)
        return store.data
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyPreferences())
            }
            .map { prefs ->
                val raw = try {
                    prefs[key]
                } catch (_: ClassCastException) {
                    null
                }
                raw ?: setting.default
            }
            .distinctUntilChanged()
    }

    override fun observe(setting: Setting.Range): Flow<Float> {
        val key = floatPreferencesKey(setting.id)
        return store.data
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyPreferences())
            }
            .map { prefs ->
                val raw = try {
                    prefs[key]
                } catch (_: ClassCastException) {
                    null
                }
                if (raw != null) setting.coerce(raw) else setting.default
            }
            .distinctUntilChanged()
    }

    override suspend fun set(setting: Setting.Toggle, value: Boolean): Outcome<Unit> {
        return try {
            store.edit { prefs ->
                prefs[booleanPreferencesKey(setting.id)] = value
            }
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun set(setting: Setting.Range, value: Float): Outcome<Unit> {
        return try {
            val clamped = setting.coerce(value)
            store.edit { prefs ->
                prefs[floatPreferencesKey(setting.id)] = clamped
            }
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun reset(setting: Setting): Outcome<Unit> {
        return try {
            store.edit { prefs ->
                val matchingKeys = prefs.asMap().keys.filter { it.name == setting.id }
                for (k in matchingKeys) {
                    prefs -= k
                }
            }
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun resetAll(): Outcome<Unit> {
        return try {
            store.edit { prefs ->
                prefs.clear()
            }
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }
}
