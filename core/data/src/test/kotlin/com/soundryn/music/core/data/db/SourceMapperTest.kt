package com.soundryn.music.core.data.db

import com.soundryn.music.core.domain.Source
import com.soundryn.music.core.domain.SourceId
import com.soundryn.music.core.domain.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SourceMapperTest {

    @Test
    fun unknown_kind_maps_to_null() {
        val entity = SourceEntity(
            id = "local",
            kind = "FUTURE_KIND",
            displayName = "Future",
            enabled = true,
            addonId = null
        )
        assertNull(entity.toDomainOrNull())
    }

    @Test
    fun invalid_id_maps_to_null() {
        val entity = SourceEntity(
            id = "Bad Id",
            kind = "LOCAL",
            displayName = "Phone",
            enabled = true,
            addonId = null
        )
        assertNull(entity.toDomainOrNull())
    }

    @Test
    fun non_addon_kind_drops_addonId() {
        val entity = SourceEntity(
            id = "local",
            kind = "LOCAL",
            displayName = "Phone",
            enabled = true,
            addonId = "x"
        )
        val domain = entity.toDomainOrNull()
        assertNotNull(domain)
        assertNull(domain?.addonId)
    }

    @Test
    fun addon_kind_keeps_addonId() {
        val entity = SourceEntity(
            id = "addon-1",
            kind = "ADDON",
            displayName = "Custom Addon",
            enabled = true,
            addonId = "x"
        )
        val domain = entity.toDomainOrNull()
        assertNotNull(domain)
        assertEquals("x", domain?.addonId)
    }

    @Test
    fun every_source_kind_round_trips() {
        for (kind in SourceKind.entries) {
            val addonId = if (kind == SourceKind.ADDON) "addon-id" else null
            val original = Source(
                id = SourceId("src-${kind.name.lowercase()}"),
                kind = kind,
                displayName = "Source ${kind.name}",
                enabled = true,
                addonId = addonId
            )
            val entity = original.toEntity()
            val restored = entity.toDomainOrNull()
            assertEquals("Round trip failed for kind: $kind", original, restored)
        }
    }
}
