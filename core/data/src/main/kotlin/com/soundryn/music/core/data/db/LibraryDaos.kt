package com.soundryn.music.core.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Transaction
    @Query("SELECT * FROM tracks ORDER BY titleSort ASC, trackKey ASC")
    fun observeTracks(): Flow<List<TrackWithArtists>>

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackKey = :key")
    suspend fun getTrack(key: String): TrackWithArtists?

    @Query("SELECT albumId AS id, COALESCE(MAX(albumTitle), '') AS title, MAX(albumArtist) AS albumArtist, MIN(year) AS year, MAX(artworkUri) AS artworkUri FROM tracks WHERE albumId IS NOT NULL GROUP BY albumId ORDER BY LOWER(COALESCE(MAX(albumTitle), '')) ASC, albumId ASC")
    fun observeAlbums(): Flow<List<AlbumRow>>

    @Query("SELECT artistKey AS id, MAX(name) AS name, MAX(sortName) AS sortName FROM track_artists GROUP BY artistKey ORDER BY MAX(sortName) ASC, artistKey ASC")
    fun observeArtists(): Flow<List<ArtistRow>>

    @Upsert
    suspend fun upsertTrack(track: TrackEntity)

    @Query("DELETE FROM track_artists WHERE trackKey = :trackKey")
    suspend fun deleteTrackArtists(trackKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackArtists(artists: List<TrackArtistEntity>)

    @Query("DELETE FROM tracks WHERE sourceId = :sourceId")
    suspend fun deleteTracksBySource(sourceId: String): Int

    @Transaction
    suspend fun replaceTracksWithArtists(items: List<Pair<TrackEntity, List<TrackArtistEntity>>>) {
        for ((track, artists) in items) {
            upsertTrack(track)
            deleteTrackArtists(track.trackKey)
            if (artists.isNotEmpty()) {
                insertTrackArtists(artists)
            }
        }
    }
}

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    fun observePlaylists(): Flow<List<PlaylistWithEntries>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun deletePlaylistEntries(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistEntries(entries: List<PlaylistEntryEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String): Int

    @Transaction
    suspend fun savePlaylistWithEntries(playlist: PlaylistEntity, entries: List<PlaylistEntryEntity>) {
        upsertPlaylist(playlist)
        deletePlaylistEntries(playlist.id)
        if (entries.isNotEmpty()) {
            insertPlaylistEntries(entries)
        }
    }
}
