package com.soundryn.music.core.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val displayName: String,
    val enabled: Boolean,
    val addonId: String? = null,
)
