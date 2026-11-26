package com.soundryn.music.core.design.haptics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HapticsPolicyTest {

    @Test
    fun disabledGivesNull() {
        val map = HapticRole.all.associateWith { HapticKind.Confirm.token }
        for (role in HapticRole.all) {
            assertNull(HapticsPolicy.kindFor(map, role, enabled = false))
        }
    }

    @Test
    fun enabledMapsRoleToKind() {
        val map = mapOf(HapticRole.Tap to "virtualKey")
        assertEquals(
            HapticKind.VirtualKey,
            HapticsPolicy.kindFor(map, HapticRole.Tap, enabled = true),
        )
    }

    @Test
    fun unknownRoleGivesNull() {
        val map = mapOf(HapticRole.Tap to "virtualKey")
        assertNull(HapticsPolicy.kindFor(map, "wobble", enabled = true))
    }

    @Test
    fun unknownKindTokenGivesNull() {
        val map = mapOf(HapticRole.Tap to "buzz")
        assertNull(HapticsPolicy.kindFor(map, HapticRole.Tap, enabled = true))
    }

    @Test
    fun everyKindHasAUniqueToken() {
        val kinds = HapticKind.entries
        assertEquals(11, kinds.size)
        val tokens = kinds.map { it.token }.toSet()
        assertEquals(11, tokens.size)
        for (kind in kinds) {
            assertEquals(kind, HapticKind.fromToken(kind.token))
        }
        assertNull(HapticKind.fromToken("buzz"))
    }
}
