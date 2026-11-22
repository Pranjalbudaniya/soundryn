package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.AlbumId
import com.soundryn.music.core.domain.ArtistId
import com.soundryn.music.core.domain.ArtistRef
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.Track
import com.soundryn.music.core.domain.TrackKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryMapperTest {

    @Test
    fun track_with_all_fields_round_trips() {
        val key = TrackKey.of("local", "song-1").getOrNull()!!
        val track = Track(
            key = key,
            title = "A Great Song",
            artists = listOf(
                ArtistRef(id = ArtistId("art-1"), name = "Artist One"),
                ArtistRef(id = null, name = "Artist Two")
            ),
            albumId = AlbumId("alb-1"),
            albumTitle = "Great Album",
            albumArtist = "Artist One",
            durationMs = 180000L,
            trackNumber = 1,
            discNumber = 1,
            year = 2024,
            genre = "Rock",
            mimeType = "audio/flac",
            artworkUri = "content://artwork/1",
            addedAtMs = 1700000000000L,
            streamOnly = false
        )

        val (trackEntity, artistEntities) = track.toEntities()
        val restored = TrackWithArtists(trackEntity, artistEntities).toDomainOrNull()

        assertNotNull(restored)
        assertEquals(track, restored)
    }

    @Test
    fun track_with_only_required_fields_round_trips() {
        val key = TrackKey.of("local", "simple-song").getOrNull()!!
        val track = Track(
            key = key,
            title = "Simple",
            artists = emptyList(),
            addedAtMs = 1700000000000L,
            streamOnly = true
        )

        val (trackEntity, artistEntities) = track.toEntities()
        val restored = TrackWithArtists(trackEntity, artistEntities).toDomainOrNull()

        assertNotNull(restored)
        assertEquals(track, restored)
    }

    @Test
    fun artist_without_id_gets_name_key() {
        val key = TrackKey.of("local", "1").getOrNull()!!
        val track = Track(
            key = key,
            title = "Song",
            artists = listOf(
                ArtistRef(id = ArtistId("a1"), name = "Artist Alpha"),
                ArtistRef(id = null, name = "The Beatles")
            ),
            addedAtMs = 1000L,
            streamOnly = false
        )

        val (_, artistEntities) = track.toEntities()
        assertEquals(2, artistEntities.size)
        assertEquals("a1", artistEntities[0].artistKey)
        assertEquals("name:beatles", artistEntities[1].artistKey)
    }

    @Test
    fun title_sort_uses_sortKeyOf() {
        val key = TrackKey.of("local", "1").getOrNull()!!
        val track = Track(
            key = key,
            title = "The Rolling Stones",
            artists = emptyList(),
            addedAtMs = 1000L,
            streamOnly = false
        )

        val (trackEntity, _) = track.toEntities()
        assertEquals("rolling stones", trackEntity.titleSort)
    }

    @Test
    fun bad_track_key_maps_to_null() {
        val badTrackEntity = TrackEntity(
            trackKey = "bad",
            sourceId = "local",
            title = "Invalid Key Song",
            titleSort = "invalid key song",
            addedAtMs = 1000L,
            streamOnly = false
        )
        val restored = TrackWithArtists(badTrackEntity, emptyList()).toDomainOrNull()
        assertNull(restored)
    }

    @Test
    fun playlist_entries_skip_bad_keys() {
        val playlistEntity = PlaylistEntity(
            id = "pl-1",
            name = "My Playlist",
            createdAtMs = 1000L,
            updatedAtMs = 2000L
        )
        val entries = listOf(
            PlaylistEntryEntity(playlistId = "pl-1", position = 0, trackKey = "local/good-1"),
            PlaylistEntryEntity(playlistId = "pl-1", position = 1, trackKey = "bad"),
            PlaylistEntryEntity(playlistId = "pl-1", position = 2, trackKey = "local/good-2")
        )

        val playlistWithEntries = PlaylistWithEntries(playlistEntity, entries)
        val domain = playlistWithEntries.toDomain()

        assertEquals(2, domain.entries.size)
        assertEquals("local/good-1", domain.entries[0].encode())
        assertEquals("local/good-2", domain.entries[1].encode())
    }
}
