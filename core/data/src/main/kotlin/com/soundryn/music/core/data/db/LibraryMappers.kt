package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.Album
import com.soundryn.music.core.domain.AlbumId
import com.soundryn.music.core.domain.Artist
import com.soundryn.music.core.domain.ArtistId
import com.soundryn.music.core.domain.ArtistRef
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Playlist
import com.soundryn.music.core.domain.PlaylistId
import com.soundryn.music.core.domain.Track
import com.soundryn.music.core.domain.TrackKey
import com.soundryn.music.core.domain.sortKeyOf

fun Track.toEntities(): Pair<TrackEntity, List<TrackArtistEntity>> {
    val trackKeyStr = key.encode()
    val trackEntity = TrackEntity(
        trackKey = trackKeyStr,
        sourceId = key.sourceId.value,
        title = title,
        titleSort = sortKeyOf(title),
        albumId = albumId?.value,
        albumTitle = albumTitle,
        albumArtist = albumArtist,
        durationMs = durationMs,
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = year,
        genre = genre,
        mimeType = mimeType,
        artworkUri = artworkUri,
        addedAtMs = addedAtMs,
        streamOnly = streamOnly,
    )
    val artistEntities = artists.mapIndexed { index, ref ->
        val sortName = sortKeyOf(ref.name)
        val artistKey = ref.id?.value ?: ("name:$sortName")
        TrackArtistEntity(
            trackKey = trackKeyStr,
            position = index,
            artistKey = artistKey,
            artistId = ref.id?.value,
            name = ref.name,
            sortName = sortName,
        )
    }
    return Pair(trackEntity, artistEntities)
}

fun TrackWithArtists.toDomainOrNull(): Track? {
    return try {
        val parsedKey = when (val res = TrackKey.parse(track.trackKey)) {
            is Outcome.Success -> res.value
            is Outcome.Failure -> return null
        }
        val domainArtists = artists
            .sortedBy { it.position }
            .map { artistEntity ->
                ArtistRef(
                    id = artistEntity.artistId?.let { ArtistId(it) },
                    name = artistEntity.name
                )
            }
        Track(
            key = parsedKey,
            title = track.title,
            artists = domainArtists,
            albumId = track.albumId?.let { AlbumId(it) },
            albumTitle = track.albumTitle,
            albumArtist = track.albumArtist,
            durationMs = track.durationMs,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            year = track.year,
            genre = track.genre,
            mimeType = track.mimeType,
            artworkUri = track.artworkUri,
            addedAtMs = track.addedAtMs,
            streamOnly = track.streamOnly,
        )
    } catch (_: Throwable) {
        null
    }
}

fun AlbumRow.toDomain(): Album {
    return Album(
        id = AlbumId(id),
        title = title,
        albumArtist = albumArtist,
        year = year,
        artworkUri = artworkUri,
    )
}

fun ArtistRow.toDomain(): Artist {
    return Artist(
        id = ArtistId(id),
        name = name,
        sortName = sortName,
    )
}

fun Playlist.toEntities(): Pair<PlaylistEntity, List<PlaylistEntryEntity>> {
    val playlistEntity = PlaylistEntity(
        id = id.value,
        name = name,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )
    val entryEntities = entries.mapIndexed { index, trackKey ->
        PlaylistEntryEntity(
            playlistId = id.value,
            position = index,
            trackKey = trackKey.encode(),
        )
    }
    return Pair(playlistEntity, entryEntities)
}

fun PlaylistWithEntries.toDomain(): Playlist {
    val validEntries = entries
        .sortedBy { it.position }
        .mapNotNull { entry ->
            when (val parsed = TrackKey.parse(entry.trackKey)) {
                is Outcome.Success -> parsed.value
                is Outcome.Failure -> null
            }
        }
    return Playlist(
        id = PlaylistId(playlist.id),
        name = playlist.name,
        entries = validEntries,
        createdAtMs = playlist.createdAtMs,
        updatedAtMs = playlist.updatedAtMs,
    )
}
