package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.ArtistId
import com.soundryn.music.core.domain.ArtistRef
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.Track
import com.soundryn.music.core.domain.TrackKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CascadeAndTransactionTest {

    private lateinit var database: SoundrynDatabase
    private lateinit var libraryRepo: RoomLibraryRepository
    private lateinit var playlistRepo: RoomPlaylistRepository

    @Before
    fun setup() {
        database = createTestDatabase()
        libraryRepo = RoomLibraryRepository(database.trackDao())
        playlistRepo = RoomPlaylistRepository(database.playlistDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun removeSourceData_removes_artists_of_that_source() = runTest {
        val trackLocal = Track(
            key = TrackKey.of("local", "1").getOrNull()!!,
            title = "Local Track",
            artists = listOf(ArtistRef(id = ArtistId("a1"), name = "Artist A")),
            addedAtMs = 1000L,
            streamOnly = false
        )
        val trackJellyfin = Track(
            key = TrackKey.of("jellyfin", "1").getOrNull()!!,
            title = "Jellyfin Track",
            artists = listOf(ArtistRef(id = ArtistId("b1"), name = "Artist B")),
            addedAtMs = 1000L,
            streamOnly = true
        )
        libraryRepo.upsertTracks(SourceId("local"), listOf(trackLocal))
        libraryRepo.upsertTracks(SourceId("jellyfin"), listOf(trackJellyfin))

        val beforeArtists = libraryRepo.observeArtists().first()
        assertEquals(2, beforeArtists.size)

        libraryRepo.removeSourceData(SourceId("local"))

        val remainingArtists = libraryRepo.observeArtists().first()
        assertEquals(1, remainingArtists.size)
        assertEquals(ArtistId("b1"), remainingArtists[0].id)
    }

    @Test
    fun deleting_playlist_leaves_no_entry_rows() = runTest {
        val k1 = TrackKey.of("local", "1").getOrNull()!!
        val k2 = TrackKey.of("local", "2").getOrNull()!!
        val plId = PlaylistId("p1")
        val playlist = Playlist(
            id = plId,
            name = "My Playlist",
            entries = listOf(k1, k2),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )
        playlistRepo.save(playlist)

        val beforePlaylists = playlistRepo.observePlaylists().first()
        assertEquals(1, beforePlaylists.size)
        assertEquals(2, beforePlaylists[0].entries.size)

        playlistRepo.delete(plId)

        // Insert only PlaylistEntity with id p1 through PlaylistDao.upsertPlaylist (no entries)
        database.playlistDao().upsertPlaylist(
            PlaylistEntity(
                id = "p1",
                name = "Bare Playlist",
                createdAtMs = 2000L,
                updatedAtMs = 2000L
            )
        )

        val observed = playlistRepo.observePlaylists().first()
        assertEquals(1, observed.size)
        assertEquals(0, observed[0].entries.size)
    }

    @Test
    fun replaceTracksWithArtists_rolls_back_all_rows_when_one_fails() = runTest {
        val validTrack = TrackEntity(
            trackKey = "local/1",
            sourceId = "local",
            title = "Track 1",
            titleSort = "track 1",
            addedAtMs = 1000L,
            streamOnly = false
        )
        val validArtists = listOf(
            TrackArtistEntity(
                trackKey = "local/1",
                position = 0,
                artistKey = "a1",
                name = "Artist A",
                sortName = "artist a"
            )
        )

        val failingTrack = TrackEntity(
            trackKey = "local/2",
            sourceId = "local",
            title = "Track 2",
            titleSort = "track 2",
            addedAtMs = 1000L,
            streamOnly = false
        )
        // Artist row whose trackKey has no row in tracks (foreign key violation)
        val failingArtists = listOf(
            TrackArtistEntity(
                trackKey = "local/nonexistent",
                position = 0,
                artistKey = "bad",
                name = "Bad Artist",
                sortName = "bad artist"
            )
        )

        val items = listOf(
            Pair(validTrack, validArtists),
            Pair(failingTrack, failingArtists)
        )

        var threw = false
        try {
            database.trackDao().replaceTracksWithArtists(items)
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected replaceTracksWithArtists to throw on failing item", threw)

        val tracks = database.trackDao().observeTracks().first()
        assertTrue("Expected observeTracks to be empty after rollback, but had ${tracks.size}", tracks.isEmpty())
    }

    @Test
    fun savePlaylistWithEntries_keeps_old_state_when_it_fails() = runTest {
        val a = TrackKey.of("local", "a").getOrNull()!!
        val b = TrackKey.of("local", "b").getOrNull()!!
        val plId = PlaylistId("p1")
        val oldPlaylist = Playlist(
            id = plId,
            name = "Old",
            entries = listOf(a, b),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )
        playlistRepo.save(oldPlaylist)

        val before = playlistRepo.observePlaylists().first()
        assertEquals(1, before.size)
        assertEquals("Old", before[0].name)
        assertEquals(listOf(a, b), before[0].entries)

        // Call PlaylistDao.savePlaylistWithEntries with failing entry (foreign key violation)
        val newEntity = PlaylistEntity(
            id = "p1",
            name = "New",
            createdAtMs = 1000L,
            updatedAtMs = 2000L
        )
        val failingEntries = listOf(
            PlaylistEntryEntity(
                playlistId = "non_existent_playlist",
                position = 0,
                trackKey = "local/c"
            )
        )

        var threw = false
        try {
            database.playlistDao().savePlaylistWithEntries(newEntity, failingEntries)
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected savePlaylistWithEntries to throw on failing entry", threw)

        val observed = playlistRepo.observePlaylists().first()
        assertEquals(1, observed.size)
        assertEquals("Old", observed[0].name)
        assertEquals(listOf(a, b), observed[0].entries)
    }
}
