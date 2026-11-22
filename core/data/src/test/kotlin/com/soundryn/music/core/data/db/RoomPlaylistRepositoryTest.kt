package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
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
class RoomPlaylistRepositoryTest {

    private lateinit var database: SoundrynDatabase
    private lateinit var repository: RoomPlaylistRepository

    @Before
    fun setup() {
        database = createTestDatabase()
        repository = RoomPlaylistRepository(database.playlistDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun save_then_observe_keeps_entry_order() = runTest {
        val k1 = TrackKey.of("local", "1").getOrNull()!!
        val k2 = TrackKey.of("local", "2").getOrNull()!!
        val k3 = TrackKey.of("local", "3").getOrNull()!!
        val playlist = Playlist(
            id = PlaylistId("pl1"),
            name = "Favorites",
            entries = listOf(k2, k3, k1),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )

        val outcome = repository.save(playlist)
        assertTrue(outcome is Outcome.Success)

        val playlists = repository.observePlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals(listOf(k2, k3, k1), playlists[0].entries)
        assertEquals("Favorites", playlists[0].name)
    }

    @Test
    fun save_again_replaces_entries() = runTest {
        val x = TrackKey.of("local", "x").getOrNull()!!
        val y = TrackKey.of("local", "y").getOrNull()!!
        val z = TrackKey.of("local", "z").getOrNull()!!
        val plId = PlaylistId("pl1")

        val initial = Playlist(
            id = plId,
            name = "Replaced",
            entries = listOf(x, y, z),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )
        repository.save(initial)

        val updated = Playlist(
            id = plId,
            name = "Replaced",
            entries = listOf(z, x),
            createdAtMs = 1000L,
            updatedAtMs = 2000L
        )
        repository.save(updated)

        val playlists = repository.observePlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals(listOf(z, x), playlists[0].entries)
    }

    @Test
    fun delete_removes_playlist_and_entries() = runTest {
        val x = TrackKey.of("local", "x").getOrNull()!!
        val plId = PlaylistId("pl1")
        val playlist = Playlist(
            id = plId,
            name = "To Delete",
            entries = listOf(x),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )
        repository.save(playlist)
        assertEquals(1, repository.observePlaylists().first().size)

        val outcome1 = repository.delete(plId)
        assertTrue(outcome1 is Outcome.Success)
        assertEquals(0, repository.observePlaylists().first().size)

        val missingOutcome = repository.delete(PlaylistId("missing"))
        assertTrue(missingOutcome is Outcome.Success)
    }

    @Test
    fun entry_for_missing_track_is_kept() = runTest {
        val missingTrackKey = TrackKey.of("local", "non-existent").getOrNull()!!
        val playlist = Playlist(
            id = PlaylistId("pl1"),
            name = "Orphan Playlist",
            entries = listOf(missingTrackKey),
            createdAtMs = 1000L,
            updatedAtMs = 1000L
        )
        repository.save(playlist)

        val playlists = repository.observePlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals(listOf(missingTrackKey), playlists[0].entries)
    }

    @Test
    fun corrupt_entry_row_is_skipped() = runTest {
        val k1 = TrackKey.of("local", "1").getOrNull()!!
        val k2 = TrackKey.of("local", "2").getOrNull()!!
        val plId = "pl1"

        database.playlistDao().upsertPlaylist(
            PlaylistEntity(id = plId, name = "Mixed Entries", createdAtMs = 1000L, updatedAtMs = 1000L)
        )
        database.playlistDao().insertPlaylistEntries(
            listOf(
                PlaylistEntryEntity(playlistId = plId, position = 0, trackKey = k1.encode()),
                PlaylistEntryEntity(playlistId = plId, position = 1, trackKey = "bad"),
                PlaylistEntryEntity(playlistId = plId, position = 2, trackKey = k2.encode())
            )
        )

        val playlists = repository.observePlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals(listOf(k1, k2), playlists[0].entries)
    }
}
