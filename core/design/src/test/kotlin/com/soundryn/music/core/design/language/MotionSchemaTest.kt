package com.soundryn.music.core.design.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSchemaTest {

    private fun loadBase(): DesignDefinition {
        val result = BaseLanguage.load()
        assertTrue(result is ParseResult.Success)
        return (result as ParseResult.Success).definition
    }

    @Test
    fun motionRoleParses() {
        val base = loadBase()
        val json = """{"schemaVersion":1,"id":"t1","name":"T","motion":{"press":{"stiffness":900,"dampingRatio":0.5}}}"""
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.warnings.isEmpty())
        val press = checkNotNull(success.definition.motion["press"])
        assertEquals(900f, checkNotNull(press["stiffness"]), 0f)
        assertEquals(0.5f, checkNotNull(press["dampingRatio"]), 0f)
    }

    @Test
    fun motionStiffnessOutOfRangeIsDropped() {
        val base = loadBase()
        val json = """
            {
              "schemaVersion": 1,
              "id": "t2",
              "name": "T",
              "motion": {
                "press": { "stiffness": 5, "dampingRatio": 0.5 },
                "expand": { "stiffness": 20001, "dampingRatio": 0.5 }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(mapOf("dampingRatio" to 0.5f), success.definition.motion["press"])
        assertEquals(mapOf("dampingRatio" to 0.5f), success.definition.motion["expand"])
        val paths = success.warnings.map { it.path }.toSet()
        assertTrue(paths.contains("motion.press.stiffness"))
        assertTrue(paths.contains("motion.expand.stiffness"))
    }

    @Test
    fun motionDampingOutOfRangeIsDropped() {
        val base = loadBase()
        val json = """
            {
              "schemaVersion": 1,
              "id": "t3",
              "name": "T",
              "motion": {
                "press": { "stiffness": 900, "dampingRatio": 0 },
                "expand": { "stiffness": 900, "dampingRatio": 2.5 }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(mapOf("stiffness" to 900f), success.definition.motion["press"])
        assertEquals(mapOf("stiffness" to 900f), success.definition.motion["expand"])
        val paths = success.warnings.map { it.path }.toSet()
        assertTrue(paths.contains("motion.press.dampingRatio"))
        assertTrue(paths.contains("motion.expand.dampingRatio"))
    }

    @Test
    fun motionStringValueIsDropped() {
        val base = loadBase()
        val json = """
            {
              "schemaVersion": 1,
              "id": "t4",
              "name": "T",
              "motion": {
                "press": { "stiffness": "900", "dampingRatio": 0.5 },
                "listReorder": { "stiffness": "x" }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.warnings.any { it.path == "motion.press.stiffness" })
        assertEquals(mapOf("dampingRatio" to 0.5f), success.definition.motion["press"])
        assertFalse(success.definition.motion.containsKey("listReorder"))
    }

    @Test
    fun motionUnknownRoleWithBaseIsWarning() {
        val base = loadBase()
        val json = """
            {
              "schemaVersion": 1,
              "id": "t5",
              "name": "T",
              "motion": {
                "wobble": { "stiffness": 500, "dampingRatio": 0.8 }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertFalse(success.definition.motion.containsKey("wobble"))
        assertTrue(
            success.warnings.any { it.path == "motion.wobble" && it.message.contains("unknown token") }
        )
    }

    @Test
    fun motionUnknownFieldIsWarning() {
        val base = loadBase()
        val json = """
            {
              "schemaVersion": 1,
              "id": "t6",
              "name": "T",
              "motion": {
                "press": { "mass": 2, "stiffness": 900, "dampingRatio": 0.6 }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.warnings.any { it.path == "motion.press.mass" })
        assertEquals(
            mapOf("stiffness" to 900f, "dampingRatio" to 0.6f),
            success.definition.motion["press"]
        )
    }

    @Test
    fun motionNotAnObjectIsWarning() {
        val json = """{"schemaVersion":1,"id":"t7","name":"T","motion":5}"""
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.definition.motion.isEmpty())
        assertTrue(success.warnings.any { it.path == "motion" })
    }

    @Test
    fun motionRoleNotAnObjectIsWarning() {
        val base = loadBase()
        val json = """{"schemaVersion":1,"id":"t8","name":"T","motion":{"press":3}}"""
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertFalse(success.definition.motion.containsKey("press"))
        assertTrue(success.warnings.any { it.path == "motion.press" })
    }

    @Test
    fun motionWithoutBaseAcceptsAnyValidRoleName() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "t9",
              "name": "T",
              "motion": {
                "wobble": { "stiffness": 500, "dampingRatio": 0.8 },
                "Bad Role": { "stiffness": 500, "dampingRatio": 0.8 }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json, null)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.definition.motion.containsKey("wobble"))
        assertFalse(success.definition.motion.containsKey("Bad Role"))
        assertTrue(success.warnings.any { it.path == "motion.Bad Role" })
    }

    @Test
    fun resolverMotionFirstLayerWinsPerField() {
        val base = loadBase()
        val layerA = (DesignParser.parse(
            """{"schemaVersion":1,"id":"la","name":"A","motion":{"press":{"stiffness":900}}}""",
            base
        ) as ParseResult.Success).definition
        val layerB = (DesignParser.parse(
            """{"schemaVersion":1,"id":"lb","name":"B","motion":{"press":{"stiffness":100,"dampingRatio":0.4}}}""",
            base
        ) as ParseResult.Success).definition
        val resolved = DesignResolver.resolve(base, listOf(layerA, layerB), ColorVariant.Light)
        val press = checkNotNull(resolved.motion["press"])
        assertEquals(900f, checkNotNull(press["stiffness"]), 0f)
        assertEquals(0.4f, checkNotNull(press["dampingRatio"]), 0f)
    }

    @Test
    fun resolverMotionRoleAbsentFromBaseIsDropped() {
        val base = loadBase()
        val layer = DesignDefinition(
            schemaVersion = 1,
            id = "lw",
            name = "W",
            motion = mapOf("wobble" to mapOf("stiffness" to 500f, "dampingRatio" to 0.8f))
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Light)
        assertFalse(resolved.motion.containsKey("wobble"))
    }

    @Test
    fun baseDefinesEveryMotionRole() {
        val base = loadBase()
        val expectedRoles = setOf(
            "spatialFast",
            "spatialDefault",
            "spatialSlow",
            "effectsFast",
            "effectsDefault",
            "effectsSlow",
            "press",
            "dragRelease",
            "expand",
            "containerTransform",
            "listReorder",
            "sliderPress",
            "reduced"
        )
        assertEquals(expectedRoles, base.motion.keys)
        for ((_, fields) in base.motion) {
            val stiffness = checkNotNull(fields["stiffness"])
            val dampingRatio = checkNotNull(fields["dampingRatio"])
            assertTrue(stiffness in 10f..20000f)
            assertTrue(dampingRatio in 0.1f..2.0f)
        }
    }

    @Test
    fun baseMotionMatchesSpecifiedValues() {
        val base = loadBase()
        val expected = mapOf(
            "spatialFast" to (800f to 0.6f),
            "spatialDefault" to (380f to 0.8f),
            "spatialSlow" to (200f to 0.8f),
            "effectsFast" to (3800f to 1.0f),
            "effectsDefault" to (1600f to 1.0f),
            "effectsSlow" to (800f to 1.0f),
            "press" to (1400f to 0.6f),
            "dragRelease" to (380f to 0.8f),
            "expand" to (300f to 0.75f),
            "containerTransform" to (380f to 0.9f),
            "listReorder" to (800f to 0.75f),
            "sliderPress" to (500f to 0.7f),
            "reduced" to (1500f to 1.0f)
        )
        for ((role, values) in expected) {
            val actual = checkNotNull(base.motion[role])
            assertEquals(values.first, checkNotNull(actual["stiffness"]), 0f)
            assertEquals(values.second, checkNotNull(actual["dampingRatio"]), 0f)
        }
    }
}
