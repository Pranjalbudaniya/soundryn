package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.Source
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.SourceKind

private val SOURCE_ID_REGEX = Regex("^[a-z0-9._-]+$")

fun Source.toEntity(): SourceEntity {
    return SourceEntity(
        id = id.value,
        kind = kind.name,
        displayName = displayName,
        enabled = enabled,
        addonId = if (kind == SourceKind.ADDON) addonId else null,
    )
}

fun SourceEntity.toDomainOrNull(): Source? {
    val sourceKind = SourceKind.entries.find { it.name == kind } ?: return null
    if (!SOURCE_ID_REGEX.matches(id)) {
        return null
    }
    return Source(
        id = SourceId(id),
        kind = sourceKind,
        displayName = displayName,
        enabled = enabled,
        addonId = if (sourceKind == SourceKind.ADDON) addonId else null,
    )
}
