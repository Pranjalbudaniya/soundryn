package com.soundryn.music.devconsole

import android.content.Context
import com.soundryn.music.core.data.db.PlaylistEntity
import com.soundryn.music.core.data.db.PlaylistEntryEntity
import com.soundryn.music.core.data.db.RoomLibraryRepository
import com.soundryn.music.core.data.db.RoomPlaylistRepository
import com.soundryn.music.core.data.db.buildSoundrynDatabase
import com.soundryn.music.core.domain.ArtistId
import com.soundryn.music.core.domain.ArtistRef
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.Track
import com.soundryn.music.core.domain.TrackKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class SelfTestResult(
    val name: String,
    val ok: Boolean,
    val detail: String,
)

suspend fun runDataSelfTest(context: Context): List<SelfTestResult> = withContext(Dispatchers.IO) {
    context.deleteDatabase("soundryn-selftest.db")
    val db = buildSoundrynDatabase(context, "soundryn-selftest.db")
    try {
        val libraryRepo = RoomLibraryRepository(db.trackDao())
        val playlistRepo = RoomPlaylistRepository(db.playlistDao())

        val cascadeResult = try {
            val t1 = Track(
                key = TrackKey.of("local", "1").getOrNull()!!,
                title = "Local Song",
                artists = listOf(ArtistRef(id = ArtistId("a1"), name = "Artist A")),
                addedAtMs = 1000L,
                streamOnly = false
            )
            val t2 = Track(
                key = TrackKey.of("jellyfin", "1").getOrNull()!!,
                title = "Jellyfin Song",
                artists = listOf(ArtistRef(id = ArtistId("b1"), name = "Artist B")),
                addedAtMs = 1000L,
                streamOnly = true
            )
            libraryRepo.upsertTracks(SourceId("local"), listOf(t1))
            libraryRepo.upsertTracks(SourceId("jellyfin"), listOf(t2))
            libraryRepo.removeSourceData(SourceId("local"))
            val remaining = libraryRepo.observeArtists().first()
            val pass = remaining.size == 1 && remaining[0].id == ArtistId("b1")
            SelfTestResult(
                name = "cascade",
                ok = pass,
                detail = if (pass) "" else "unexpected artists: ${remaining.map { it.id.value }}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SelfTestResult(name = "cascade", ok = false, detail = "ERROR: ${e::class.java.simpleName}")
        }

        val playlistCascadeResult = try {
            val plId = PlaylistId("p1")
            val k1 = TrackKey.of("local", "1").getOrNull()!!
            val k2 = TrackKey.of("local", "2").getOrNull()!!
            val p = Playlist(
                id = plId,
                name = "Test Playlist",
                entries = listOf(k1, k2),
                createdAtMs = 1000L,
                updatedAtMs = 1000L
            )
            playlistRepo.save(p)
            playlistRepo.delete(plId)
            db.playlistDao().upsertPlaylist(
                PlaylistEntity(id = "p1", name = "Bare", createdAtMs = 2000L, updatedAtMs = 2000L)
            )
            val playlists = playlistRepo.observePlaylists().first()
            val target = playlists.find { it.id == plId }
            val pass = target != null && target.entries.isEmpty()
            SelfTestResult(
                name = "playlist cascade",
                ok = pass,
                detail = if (pass) "" else "unexpected entries: ${target?.entries?.size}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SelfTestResult(name = "playlist cascade", ok = false, detail = "ERROR: ${e::class.java.simpleName}")
        }

        val rollbackResult = try {
            val plId = PlaylistId("p1")
            val a = TrackKey.of("local", "a").getOrNull()!!
            val b = TrackKey.of("local", "b").getOrNull()!!
            val oldP = Playlist(
                id = plId,
                name = "Old",
                entries = listOf(a, b),
                createdAtMs = 1000L,
                updatedAtMs = 1000L
            )
            playlistRepo.save(oldP)

            var threw = false
            try {
                val newEntity = PlaylistEntity(id = "p1", name = "New", createdAtMs = 1000L, updatedAtMs = 2000L)
                val failingEntry = listOf(
                    PlaylistEntryEntity(playlistId = "non_existent_pl", position = 0, trackKey = "local/c")
                )
                db.playlistDao().savePlaylistWithEntries(newEntity, failingEntry)
            } catch (_: Exception) {
                threw = true
            }

            val playlists = playlistRepo.observePlaylists().first()
            val target = playlists.find { it.id == plId }
            val pass = threw && target != null && target.name == "Old" && target.entries == listOf(a, b)
            SelfTestResult(
                name = "rollback",
                ok = pass,
                detail = if (pass) "" else "rollback failed: threw=$threw, name=${target?.name}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SelfTestResult(name = "rollback", ok = false, detail = "ERROR: ${e::class.java.simpleName}")
        }

        listOf(cascadeResult, playlistCascadeResult, rollbackResult)
    } finally {
        db.close()
        context.deleteDatabase("soundryn-selftest.db")
    }
}
