package com.soundryn.music.core.design.language

import com.soundryn.music.core.design.haptics.HapticKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsSchemaTest {

    private fun loadBase(): DesignDefinition {
        val result = BaseLanguage.load()
        assertTrue(result is ParseResult.Success)
        return (result as ParseResult.Success).definition
    }

    @Test
    fun hapticsRoleParses() {
        val base = loadBase()
        val json = """{"schemaVersion":1,"id":"t1","name":"T","haptics":{"tap":"confirm"}}"""
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.warnings.isEmpty())
        assertEquals("confirm", success.definition.haptics["tap"])
    }

    @Test
    fun hapticsUnknownKindIsWarning() {
        val base = loadBase()
        val json = """{"schemaVersion":1,"id":"t2","name":"T","haptics":{"tap":"buzz"}}"""
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertFalse(success.definition.haptics.containsKey("tap"))
        assertTrue(
            success.warnings.any { it.path == "haptics.tap" && it.message.contains("unknown haptic kind") }
        )
    }

    @Test
    fun hapticsNonStringIsWarning() {
        val base = loadBase()
        val json = """{"schemaVersion":1,"id":"t3","name":"T","haptics":{"tap":5}}"""
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertFalse(success.definition.haptics.containsKey("tap"))
        assertTrue(success.warnings.any { it.path == "haptics.tap" })
    }

    @Test
    fun hapticsUnknownRoleWithBaseIsWarning() {
        val base = loadBase()
        val json = """{"schemaVersion":1,"id":"t4","name":"T","haptics":{"wobble":"confirm"}}"""
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertFalse(success.definition.haptics.containsKey("wobble"))
        assertTrue(
            success.warnings.any { it.path == "haptics.wobble" && it.message.contains("unknown token") }
        )
    }

    @Test
    fun hapticsNotAnObjectIsWarning() {
        val json = """{"schemaVersion":1,"id":"t5","name":"T","haptics":[]}"""
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.definition.haptics.isEmpty())
        assertTrue(success.warnings.any { it.path == "haptics" })
    }

    @Test
    fun hapticsWithoutBaseAcceptsAnyValidRoleName() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "t6",
              "name": "T",
              "haptics": {
                "wobble": "confirm",
                "Bad Role": "confirm"
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, null)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals("confirm", success.definition.haptics["wobble"])
        assertFalse(success.definition.haptics.containsKey("Bad Role"))
        assertTrue(success.warnings.any { it.path == "haptics.Bad Role" })
    }

    @Test
    fun resolverHapticsFirstLayerWins() {
        val base = loadBase()
        val layerA = (DesignParser.parse(
            """{"schemaVersion":1,"id":"la","name":"A","haptics":{"tap":"confirm"}}""",
            base
        ) as ParseResult.Success).definition
        val layerB = (DesignParser.parse(
            """{"schemaVersion":1,"id":"lb","name":"B","haptics":{"tap":"reject"}}""",
            base
        ) as ParseResult.Success).definition
        val resolved = DesignResolver.resolve(base, listOf(layerA, layerB), ColorVariant.Light)
        assertEquals("confirm", resolved.haptics["tap"])
    }

    @Test
    fun resolverHapticsRoleAbsentFromBaseIsDropped() {
        val base = loadBase()
        val layer = DesignDefinition(
            schemaVersion = 1,
            id = "lw",
            name = "W",
            haptics = mapOf("wobble" to "confirm")
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Light)
        assertFalse(resolved.haptics.containsKey("wobble"))
    }

    @Test
    fun baseDefinesEveryHapticRole() {
        val base = loadBase()
        val expectedRoles = setOf(
            "tap",
            "toggleOn",
            "toggleOff",
            "longPress",
            "confirm",
            "reject",
            "tick",
            "tickFast",
            "threshold",
            "gestureEnd",
        )
        assertEquals(expectedRoles, base.haptics.keys)
        for ((_, kindToken) in base.haptics) {
            assertTrue(HapticKind.fromToken(kindToken) != null)
        }
    }

    @Test
    fun baseHapticsMatchSpecifiedValues() {
        val base = loadBase()
        val expected = mapOf(
            "tap" to "virtualKey",
            "toggleOn" to "toggleOn",
            "toggleOff" to "toggleOff",
            "longPress" to "longPress",
            "confirm" to "confirm",
            "reject" to "reject",
            "tick" to "segmentTick",
            "tickFast" to "segmentFrequentTick",
            "threshold" to "gestureThresholdActivate",
            "gestureEnd" to "gestureEnd",
        )
        assertEquals(expected, base.haptics)
    }
}
