package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.Album
import com.soundryn.music.core.domain.Artist
import com.soundryn.music.core.domain.DomainError
import com.soundryn.music.core.domain.LibraryRepository
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
import com.soundryn.music.core.domain.PlaylistRepository
import com.soundryn.music.core.domain.Source
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.SourceRepository
import com.soundryn.music.core.domain.Track
import com.soundryn.music.core.domain.TrackKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomLibraryRepository @Inject constructor(
    private val trackDao: TrackDao
) : LibraryRepository {

    override fun observeTracks(): Flow<List<Track>> {
        return trackDao.observeTracks()
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }
            .catch { emit(emptyList()) }
    }

    override fun observeAlbums(): Flow<List<Album>> {
        return trackDao.observeAlbums()
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeArtists(): Flow<List<Artist>> {
        return trackDao.observeArtists()
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun getTrack(key: TrackKey): Outcome<Track?> {
        return try {
            val trackWithArtists = trackDao.getTrack(key.encode())
            Outcome.Success(trackWithArtists?.toDomainOrNull())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun upsertTracks(sourceId: SourceId, tracks: List<Track>): Outcome<Unit> {
        if (tracks.any { it.key.sourceId != sourceId }) {
            return Outcome.Failure(DomainError.Parse("source mismatch"))
        }
        return try {
            val items = tracks.map { it.toEntities() }
            trackDao.replaceTracksWithArtists(items)
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun removeSourceData(sourceId: SourceId): Outcome<Unit> {
        return try {
            trackDao.deleteTracksBySource(sourceId.value)
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }
}

class RoomPlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> {
        return playlistDao.observePlaylists()
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun save(p: Playlist): Outcome<Unit> {
        return try {
            val (entity, entries) = p.toEntities()
            playlistDao.savePlaylistWithEntries(entity, entries)
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun delete(id: PlaylistId): Outcome<Unit> {
        return try {
            playlistDao.deletePlaylist(id.value)
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }
}

class RoomSourceRepository @Inject constructor(
    private val sourceDao: SourceDao
) : SourceRepository {

    override fun observeSources(): Flow<List<Source>> {
        return sourceDao.observeAll()
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun save(s: Source): Outcome<Unit> {
        return try {
            sourceDao.upsert(s.toEntity())
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun setEnabled(id: SourceId, enabled: Boolean): Outcome<Unit> {
        return try {
            val rows = sourceDao.setEnabled(id.value, enabled)
            if (rows > 0) {
                Outcome.Success(Unit)
            } else {
                Outcome.Failure(DomainError.NotFound)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }

    override suspend fun remove(id: SourceId): Outcome<Unit> {
        return try {
            sourceDao.delete(id.value)
            Outcome.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Outcome.Failure(DomainError.Storage)
        }
    }
}
