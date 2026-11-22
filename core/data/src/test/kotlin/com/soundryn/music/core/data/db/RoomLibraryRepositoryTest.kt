package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.AlbumId
import com.soundryn.music.core.domain.ArtistId
import com.soundryn.music.core.domain.ArtistRef
import com.soundryn.music.core.domain.DomainError
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.Track
import com.soundryn.music.core.domain.TrackKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomLibraryRepositoryTest {

    private lateinit var database: SoundrynDatabase
    private lateinit var repository: RoomLibraryRepository

    @Before
    fun setup() {
        database = createTestDatabase()
        repository = RoomLibraryRepository(database.trackDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun upsert_then_observeTracks_returns_tracks_with_artists_in_order() = runTest {
        val key = TrackKey.of("local", "1").getOrNull()!!
        val track = Track(
            key = key,
            title = "Song One",
            artists = listOf(
                ArtistRef(id = ArtistId("a1"), name = "Artist A"),
                ArtistRef(id = null, name = "Artist B")
            ),
            addedAtMs = 1000L,
            streamOnly = false
        )

        val outcome = repository.upsertTracks(SourceId("local"), listOf(track))
        assertTrue(outcome is Outcome.Success)

        val tracks = repository.observeTracks().first()
        assertEquals(1, tracks.size)
        assertEquals(track, tracks[0])
        assertEquals(2, tracks[0].artists.size)
        assertEquals(ArtistId("a1"), tracks[0].artists[0].id)
        assertEquals("Artist A", tracks[0].artists[0].name)
        assertNull(tracks[0].artists[1].id)
        assertEquals("Artist B", tracks[0].artists[1].name)
    }

    @Test
    fun upsert_same_key_replaces_track_and_artists() = runTest {
        val key = TrackKey.of("local", "1").getOrNull()!!
        val track1 = Track(
            key = key,
            title = "Original",
            artists = listOf(ArtistRef(id = ArtistId("a1"), name = "Artist A")),
            addedAtMs = 1000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(track1))

        val track2 = Track(
            key = key,
            title = "Updated",
            artists = listOf(ArtistRef(id = ArtistId("c1"), name = "Artist C")),
            addedAtMs = 2000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(track2))

        val tracks = repository.observeTracks().first()
        assertEquals(1, tracks.size)
        assertEquals("Updated", tracks[0].title)
        assertEquals(1, tracks[0].artists.size)
        assertEquals("Artist C", tracks[0].artists[0].name)
    }

    @Test
    fun upsert_rejects_track_from_other_source() = runTest {
        val localTrack = Track(
            key = TrackKey.of("local", "1").getOrNull()!!,
            title = "Local Song",
            artists = emptyList(),
            addedAtMs = 1000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(localTrack))

        val foreignTrack = Track(
            key = TrackKey.of("local", "foreign-1").getOrNull()!!,
            title = "Foreign",
            artists = emptyList(),
            addedAtMs = 2000L,
            streamOnly = false
        )
        val outcome = repository.upsertTracks(SourceId("jellyfin"), listOf(foreignTrack))
        assertTrue(outcome is Outcome.Failure)
        assertEquals(DomainError.Parse("source mismatch"), (outcome as Outcome.Failure).error)

        val tracks = repository.observeTracks().first()
        assertEquals(1, tracks.size)
        assertEquals(localTrack, tracks[0])
    }

    @Test
    fun same_song_in_two_sources_gives_two_entries() = runTest {
        val trackLocal = Track(
            key = TrackKey.of("local", "1").getOrNull()!!,
            title = "Song",
            artists = emptyList(),
            addedAtMs = 1000L,
            streamOnly = false
        )
        val trackJellyfin = Track(
            key = TrackKey.of("jellyfin", "1").getOrNull()!!,
            title = "Song",
            artists = emptyList(),
            addedAtMs = 1000L,
            streamOnly = true
        )
        repository.upsertTracks(SourceId("local"), listOf(trackLocal))
        repository.upsertTracks(SourceId("jellyfin"), listOf(trackJellyfin))

        val tracks = repository.observeTracks().first()
        assertEquals(2, tracks.size)
        assertTrue(tracks.any { it.key.sourceId.value == "local" })
        assertTrue(tracks.any { it.key.sourceId.value == "jellyfin" })
    }

    @Test
    fun removeSourceData_deletes_only_that_source() = runTest {
        val localKey = TrackKey.of("local", "1").getOrNull()!!
        val jellyfinKey = TrackKey.of("jellyfin", "1").getOrNull()!!

        val localTrack = Track(
            key = localKey,
            title = "Local Song",
            artists = listOf(ArtistRef(id = ArtistId("a1"), name = "Artist A")),
            addedAtMs = 1000L,
            streamOnly = false
        )
        val jellyfinTrack = Track(
            key = jellyfinKey,
            title = "Jellyfin Song",
            artists = listOf(ArtistRef(id = ArtistId("a2"), name = "Artist B")),
            addedAtMs = 1000L,
            streamOnly = true
        )
        repository.upsertTracks(SourceId("local"), listOf(localTrack))
        repository.upsertTracks(SourceId("jellyfin"), listOf(jellyfinTrack))

        val playlistRepo = RoomPlaylistRepository(database.playlistDao())
        val playlist = Playlist(
            id = PlaylistId("pl1"),
            name = "My Playlist",
            entries = listOf(localKey),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )
        playlistRepo.save(playlist)

        val res = repository.removeSourceData(SourceId("local"))
        assertTrue(res is Outcome.Success)

        val remainingTracks = repository.observeTracks().first()
        assertEquals(1, remainingTracks.size)
        assertEquals(jellyfinKey, remainingTracks[0].key)

        val playlists = playlistRepo.observePlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals(listOf(localKey), playlists[0].entries)
    }

    @Test
    fun observeAlbums_groups_by_albumId_and_skips_null() = runTest {
        val t1 = Track(
            key = TrackKey.of("local", "1").getOrNull()!!,
            title = "Track 1",
            artists = emptyList(),
            albumId = AlbumId("al1"),
            albumTitle = "Album One",
            albumArtist = "Various Artists",
            year = 2001,
            artworkUri = "uri1",
            addedAtMs = 1000L,
            streamOnly = false
        )
        val t2 = Track(
            key = TrackKey.of("local", "2").getOrNull()!!,
            title = "Track 2",
            artists = emptyList(),
            albumId = AlbumId("al1"),
            albumTitle = "Album One",
            albumArtist = "Various Artists",
            year = 1999,
            artworkUri = "uri1",
            addedAtMs = 2000L,
            streamOnly = false
        )
        val t3 = Track(
            key = TrackKey.of("local", "3").getOrNull()!!,
            title = "Track 3",
            artists = emptyList(),
            albumId = null,
            addedAtMs = 3000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(t1, t2, t3))

        val albums = repository.observeAlbums().first()
        assertEquals(1, albums.size)
        assertEquals(AlbumId("al1"), albums[0].id)
        assertEquals("Album One", albums[0].title)
        assertEquals("Various Artists", albums[0].albumArtist)
        assertEquals(1999, albums[0].year)
        assertEquals("uri1", albums[0].artworkUri)
    }

    @Test
    fun observeArtists_uses_id_or_name_key() = runTest {
        val t1 = Track(
            key = TrackKey.of("local", "1").getOrNull()!!,
            title = "Song 1",
            artists = listOf(ArtistRef(id = ArtistId("a1"), name = "Artist A")),
            addedAtMs = 1000L,
            streamOnly = false
        )
        val t2 = Track(
            key = TrackKey.of("local", "2").getOrNull()!!,
            title = "Song 2",
            artists = listOf(ArtistRef(id = null, name = "The Beatles")),
            addedAtMs = 2000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(t1, t2))

        val artists = repository.observeArtists().first()
        assertEquals(2, artists.size)
        val artistIds = artists.map { it.id.value }.toSet()
        assertTrue(artistIds.contains("a1"))
        assertTrue(artistIds.contains("name:beatles"))
    }

    @Test
    fun observeTracks_orders_by_sort_title() = runTest {
        val t1 = Track(
            key = TrackKey.of("local", "1").getOrNull()!!,
            title = "Zebra",
            artists = emptyList(),
            addedAtMs = 1000L,
            streamOnly = false
        )
        val t2 = Track(
            key = TrackKey.of("local", "2").getOrNull()!!,
            title = "the apple",
            artists = emptyList(),
            addedAtMs = 2000L,
            streamOnly = false
        )
        val t3 = Track(
            key = TrackKey.of("local", "3").getOrNull()!!,
            title = "Banana",
            artists = emptyList(),
            addedAtMs = 3000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(t1, t2, t3))

        val tracks = repository.observeTracks().first()
        assertEquals(3, tracks.size)
        assertEquals("the apple", tracks[0].title)
        assertEquals("Banana", tracks[1].title)
        assertEquals("Zebra", tracks[2].title)
    }

    @Test
    fun corrupt_track_row_is_skipped() = runTest {
        val validTrack = Track(
            key = TrackKey.of("local", "valid").getOrNull()!!,
            title = "Valid Song",
            artists = emptyList(),
            addedAtMs = 1000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(validTrack))

        val corruptTrackEntity = TrackEntity(
            trackKey = "bad",
            sourceId = "local",
            title = "Corrupt Track",
            titleSort = "corrupt track",
            addedAtMs = 2000L,
            streamOnly = false
        )
        database.trackDao().upsertTrack(corruptTrackEntity)

        val tracks = repository.observeTracks().first()
        assertEquals(1, tracks.size)
        assertEquals(validTrack.key, tracks[0].key)
    }

    @Test
    fun getTrack_missing_returns_success_null() = runTest {
        val missingKey = TrackKey.of("local", "does-not-exist").getOrNull()!!
        val outcome = repository.getTrack(missingKey)
        assertTrue(outcome is Outcome.Success)
        assertNull((outcome as Outcome.Success).value)
    }

    @Test
    fun getTrack_returns_track_with_artists() = runTest {
        val key = TrackKey.of("local", "track-full").getOrNull()!!
        val track = Track(
            key = key,
            title = "Full Track",
            artists = listOf(
                ArtistRef(id = ArtistId("a1"), name = "First Artist"),
                ArtistRef(id = null, name = "Second Artist")
            ),
            albumId = AlbumId("alb1"),
            albumTitle = "Full Album",
            albumArtist = "Various",
            durationMs = 240000L,
            trackNumber = 3,
            discNumber = 1,
            year = 2023,
            genre = "Electronic",
            mimeType = "audio/mp3",
            artworkUri = "content://art/full",
            addedAtMs = 5000L,
            streamOnly = false
        )
        repository.upsertTracks(SourceId("local"), listOf(track))

        val outcome = repository.getTrack(key)
        assertTrue(outcome is Outcome.Success)
        val fetched = (outcome as Outcome.Success).value
        assertNotNull(fetched)
        assertEquals(track, fetched)
        assertEquals(2, fetched!!.artists.size)
        assertEquals("First Artist", fetched.artists[0].name)
        assertEquals("Second Artist", fetched.artists[1].name)
    }
}
