package com.soundryn.music.core.data.di

import android.content.Context
import com.soundryn.music.core.data.db.PlaylistDao
import com.soundryn.music.core.data.db.RoomLibraryRepository
import com.soundryn.music.core.data.db.RoomPlaylistRepository
import com.soundryn.music.core.data.db.RoomSourceRepository
import com.soundryn.music.core.data.db.SoundrynDatabase
import com.soundryn.music.core.data.db.SourceDao
import com.soundryn.music.core.data.db.TrackDao
import com.soundryn.music.core.data.db.buildSoundrynDatabase
import com.soundryn.music.core.domain.LibraryRepository
import com.soundryn.music.core.domain.PlaylistRepository
import com.soundryn.music.core.domain.SettingsRepository
import com.soundryn.music.core.domain.SourceRepository
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soundryn.music.core.data.settings.DataStoreSettingsRepository
import com.soundryn.music.core.data.settings.createSettingsDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {
    @Binds
    abstract fun library(impl: RoomLibraryRepository): LibraryRepository

    @Binds
    abstract fun playlists(impl: RoomPlaylistRepository): PlaylistRepository

    @Binds
    abstract fun sources(impl: RoomSourceRepository): SourceRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProviders {
    @Provides
    @Singleton
    fun database(@ApplicationContext c: Context): SoundrynDatabase = buildSoundrynDatabase(c)

    @Provides
    fun trackDao(db: SoundrynDatabase): TrackDao = db.trackDao()

    @Provides
    fun playlistDao(db: SoundrynDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun sourceDao(db: SoundrynDatabase): SourceDao = db.sourceDao()

    @Provides
    @Singleton
    fun settingsRepository(@ApplicationContext context: Context): SettingsRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = context.preferencesDataStoreFile("settings")
        val store = createSettingsDataStore(scope, file)
        return DataStoreSettingsRepository(store)
    }
}
