package com.soundryn.music.core.domain

@JvmInline
value class SourceId(val value: String)

@JvmInline
value class ArtistId(val value: String)

@JvmInline
value class AlbumId(val value: String)

@JvmInline
value class PlaylistId(val value: String)

enum class SourceKind {
    LOCAL,
    GOOGLE_DRIVE,
    JELLYFIN,
    SUBSONIC,
    TELEGRAM,
    YOUTUBE,
    YOUTUBE_MUSIC,
    SOUNDCLOUD,
    BANDCAMP,
    PEERTUBE,
    ADDON,
}

data class Source(
    val id: SourceId,
    val kind: SourceKind,
    val displayName: String,
    val enabled: Boolean,
    val addonId: String? = null,
)

data class ArtistRef(
    val id: ArtistId? = null,
    val name: String,
)

data class Artist(
    val id: ArtistId,
    val name: String,
    val sortName: String,
)

data class Album(
    val id: AlbumId,
    val title: String,
    val albumArtist: String? = null,
    val year: Int? = null,
    val artworkUri: String? = null,
)

data class Track(
    val key: TrackKey,
    val title: String,
    val artists: List<ArtistRef>,
    val albumId: AlbumId? = null,
    val albumTitle: String? = null,
    val albumArtist: String? = null,
    val durationMs: Long? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val mimeType: String? = null,
    val artworkUri: String? = null,
    val addedAtMs: Long,
    val streamOnly: Boolean,
)

data class Playlist(
    val id: PlaylistId,
    val name: String,
    val entries: List<TrackKey>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
