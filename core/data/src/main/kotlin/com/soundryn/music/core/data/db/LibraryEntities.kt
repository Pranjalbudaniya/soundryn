package com.soundryn.music.core.data.db

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["titleSort"]),
    ]
)
data class TrackEntity(
    @PrimaryKey val trackKey: String,
    val sourceId: String,
    val title: String,
    val titleSort: String,
    val albumId: String? = null,
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

@Entity(
    tableName = "track_artists",
    primaryKeys = ["trackKey", "position"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["trackKey"],
            childColumns = ["trackKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["artistKey"]),
    ]
)
data class TrackArtistEntity(
    val trackKey: String,
    val position: Int,
    val artistKey: String,
    val artistId: String? = null,
    val name: String,
    val sortName: String,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistEntryEntity(
    val playlistId: String,
    val position: Int,
    val trackKey: String,
)

data class TrackWithArtists(
    @Embedded val track: TrackEntity,
    @Relation(
        parentColumns = ["trackKey"],
        entityColumns = ["trackKey"]
    )
    val artists: List<TrackArtistEntity>,
)

data class PlaylistWithEntries(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["playlistId"]
    )
    val entries: List<PlaylistEntryEntity>,
)

data class AlbumRow(
    val id: String,
    val title: String,
    val albumArtist: String? = null,
    val year: Int? = null,
    val artworkUri: String? = null,
)

data class ArtistRow(
    val id: String,
    val name: String,
    val sortName: String,
)
