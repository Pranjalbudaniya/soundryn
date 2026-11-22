package com.soundryn.music.core.domain

import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeTracks(): Flow<List<Track>>
    fun observeAlbums(): Flow<List<Album>>
    fun observeArtists(): Flow<List<Artist>>
    suspend fun getTrack(key: TrackKey): Outcome<Track?>
    suspend fun upsertTracks(sourceId: SourceId, tracks: List<Track>): Outcome<Unit>
    suspend fun removeSourceData(sourceId: SourceId): Outcome<Unit>
}

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    suspend fun save(p: Playlist): Outcome<Unit>
    suspend fun delete(id: PlaylistId): Outcome<Unit>
}

interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun save(s: Source): Outcome<Unit>
    suspend fun setEnabled(id: SourceId, enabled: Boolean): Outcome<Unit>
    suspend fun remove(id: SourceId): Outcome<Unit>
}

interface SettingsRepository {
    fun observe(setting: Setting.Toggle): Flow<Boolean>
    fun observe(setting: Setting.Range): Flow<Float>
    suspend fun set(setting: Setting.Toggle, value: Boolean): Outcome<Unit>
    suspend fun set(setting: Setting.Range, value: Float): Outcome<Unit>
    suspend fun reset(setting: Setting): Outcome<Unit>
    suspend fun resetAll(): Outcome<Unit>
}

enum class SourceCapability {
    LIST_TRACKS,
    SEARCH,
    STREAM,
    DOWNLOAD,
}

data class StreamLocator(
    val uri: String,
    val mimeType: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = "StreamLocator(uri=$uri, mimeType=$mimeType, headers=[REDACTED])"
}

data class Page<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)

data class PageRequest(
    val cursor: String? = null,
    val limit: Int = 50,
) {
    companion object {
        fun of(cursor: String?, limit: Int): PageRequest {
            return PageRequest(
                cursor = cursor,
                limit = limit.coerceIn(1, 200)
            )
        }
    }
}

interface MusicSource {
    val source: Source
    val capabilities: Set<SourceCapability>
    suspend fun listTracks(request: PageRequest): Outcome<Page<Track>>
    suspend fun search(query: String, request: PageRequest): Outcome<Page<Track>>
    suspend fun resolveStream(key: TrackKey): Outcome<StreamLocator>
}
