package com.soundryn.music.core.design.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignParserTest {

    private fun loadBase(): DesignDefinition {
        val stream = javaClass.classLoader?.getResourceAsStream("design/m3_expressive.json")
            ?: javaClass.getResourceAsStream("/design/m3_expressive.json")
            ?: error("m3_expressive.json missing")
        val text = stream.bufferedReader().use { it.readText() }
        val result = DesignParser.parse(text)
        return (result as ParseResult.Success).definition
    }

    @Test
    fun parsesMinimalPack() {
        val json = """{"schemaVersion":1,"id":"x-pack","name":"X"}"""
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(1, success.definition.schemaVersion)
        assertEquals("x-pack", success.definition.id)
        assertEquals("X", success.definition.name)
        assertTrue(success.warnings.isEmpty())
        assertTrue(success.definition.color.isEmpty())
        assertTrue(success.definition.shape.isEmpty())
        assertTrue(success.definition.spacing.isEmpty())
        assertTrue(success.definition.elevation.isEmpty())
        assertTrue(success.definition.blur.isEmpty())
        assertTrue(success.definition.type.isEmpty())
        assertTrue(success.definition.components.isEmpty())
    }

    @Test
    fun parsesBothHexForms() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "hex-pack",
              "name": "Hex",
              "color": {
                "light": {
                  "c1": "#B4236B",
                  "c2": "#80B4236B"
                }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val colors = checkNotNull((result as ParseResult.Success).definition.color[ColorVariant.Light])
        assertEquals(0xFFB4236B.toInt(), colors["c1"])
        assertEquals(0x80B4236B.toInt(), colors["c2"])
    }

    @Test
    fun rejectsInvalidJson() {
        val inputs = listOf("", "   ", "{", """{"a":""")
        for (input in inputs) {
            val result = DesignParser.parse(input)
            assertTrue(result is ParseResult.Failure)
            val failure = result as ParseResult.Failure
            assertEquals("", failure.errors.first().path)
            assertEquals("not valid JSON", failure.errors.first().message)
        }
    }

    @Test
    fun rejectsNonObjectRoot() {
        val inputs = listOf("null", "[]", "\"x\"", "42")
        for (input in inputs) {
            val result = DesignParser.parse(input)
            assertTrue(result is ParseResult.Failure)
            val failure = result as ParseResult.Failure
            assertEquals("", failure.errors.first().path)
            assertEquals("root must be an object", failure.errors.first().message)
        }
    }

    @Test
    fun rejectsBadSchemaVersion() {
        val inputs = listOf(
            """{"id":"a-b","name":"n"}""",
            """{"schemaVersion":"1","id":"a-b","name":"n"}""",
            """{"schemaVersion":1.5,"id":"a-b","name":"n"}""",
            """{"schemaVersion":0,"id":"a-b","name":"n"}""",
            """{"schemaVersion":-1,"id":"a-b","name":"n"}"""
        )
        for (input in inputs) {
            val result = DesignParser.parse(input)
            assertTrue(result is ParseResult.Failure)
            val failure = result as ParseResult.Failure
            assertTrue(failure.errors.any { it.path == "schemaVersion" })
        }
    }

    @Test
    fun rejectsNewerSchemaVersion() {
        val json = """{"schemaVersion":2,"id":"a-b","name":"n"}"""
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Failure)
        val failure = result as ParseResult.Failure
        val svError = failure.errors.firstOrNull { it.path == "schemaVersion" }
        assertNotNull(svError)
        assertTrue(svError!!.message.contains("newer than this app"))
    }

    @Test
    fun rejectsBadIdAndName() {
        val badIds = listOf(
            """{"schemaVersion":1,"id":"Bad Id","name":"Valid"}""",
            """{"schemaVersion":1,"id":"","name":"Valid"}""",
            """{"schemaVersion":1,"id":"${"a".repeat(41)}","name":"Valid"}"""
        )
        for (json in badIds) {
            val result = DesignParser.parse(json)
            assertTrue(result is ParseResult.Failure)
            val failure = result as ParseResult.Failure
            assertTrue(failure.errors.any { it.path == "id" })
        }

        val badNames = listOf(
            """{"schemaVersion":1,"id":"valid-id","name":""}""",
            """{"schemaVersion":1,"id":"valid-id","name":"   "}""",
            """{"schemaVersion":1,"id":"valid-id","name":"${"a".repeat(61)}"}"""
        )
        for (json in badNames) {
            val result = DesignParser.parse(json)
            assertTrue(result is ParseResult.Failure)
            val failure = result as ParseResult.Failure
            assertTrue(failure.errors.any { it.path == "name" })
        }

        val threeErrors = """{"schemaVersion":2,"id":"Bad Id","name":""}"""
        val threeResult = DesignParser.parse(threeErrors)
        assertTrue(threeResult is ParseResult.Failure)
        val threeFailure = threeResult as ParseResult.Failure
        val paths = threeFailure.errors.map { it.path }.toSet()
        assertTrue(paths.contains("schemaVersion"))
        assertTrue(paths.contains("id"))
        assertTrue(paths.contains("name"))
    }

    @Test
    fun dropsBadColorWithWarning() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "color-test",
              "name": "Color Test",
              "color": {
                "light": {
                  "primary": "#FF6750A4",
                  "bad1": "#12",
                  "bad2": "red",
                  "bad3": "#GGGGGG",
                  "bad4": 123,
                  "bad5": null
                }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        val lightColors = checkNotNull(success.definition.color[ColorVariant.Light])
        assertEquals(0xFF6750A4.toInt(), lightColors["primary"])
        assertEquals(1, lightColors.size)
        val warnedPaths = success.warnings.map { it.path }.toSet()
        assertTrue(warnedPaths.contains("color.light.bad1"))
        assertTrue(warnedPaths.contains("color.light.bad2"))
        assertTrue(warnedPaths.contains("color.light.bad3"))
        assertTrue(warnedPaths.contains("color.light.bad4"))
        assertTrue(warnedPaths.contains("color.light.bad5"))
    }

    @Test
    fun dropsOutOfRangeNumbersWithWarning() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "range-test",
              "name": "Range Test",
              "shape": { "badLow": -1, "badHigh": 1001 },
              "blur": { "surfaceAlpha": 1.5 },
              "type": {
                "bodyLarge": { "weight": 0, "size": 0 },
                "bodyMedium": { "weight": 400.5 }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertTrue(success.definition.shape.isEmpty())
        assertNull(success.definition.blur["surfaceAlpha"])
        assertNull(success.definition.type["bodyLarge"])
        assertNull(success.definition.type["bodyMedium"])

        val paths = success.warnings.map { it.path }.toSet()
        assertTrue(paths.contains("shape.badLow"))
        assertTrue(paths.contains("shape.badHigh"))
        assertTrue(paths.contains("blur.surfaceAlpha"))
        assertTrue(paths.contains("type.bodyLarge.weight"))
        assertTrue(paths.contains("type.bodyLarge.size"))
        assertTrue(paths.contains("type.bodyMedium.weight"))
    }

    @Test
    fun ignoresUnknownFieldsWithWarning() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "unknown-test",
              "name": "Unknown Test",
              "extraField": 123
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(1, success.warnings.size)
        assertEquals("extraField", success.warnings.first().path)
    }

    @Test
    fun warnsOnUnknownTokenNamesWhenBaseGiven() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "token-test",
              "name": "Token Test",
              "color": {
                "light": {
                  "nope": "#FF112233"
                }
              }
            }
        """.trimIndent()
        val base = loadBase()
        val resultWithBase = DesignParser.parse(json, base)
        assertTrue(resultWithBase is ParseResult.Success)
        val successWithBase = resultWithBase as ParseResult.Success
        assertEquals(1, successWithBase.warnings.size)
        assertEquals("color.light.nope", successWithBase.warnings.first().path)
        assertTrue(successWithBase.definition.color[ColorVariant.Light]?.isEmpty() == true)

        val resultWithoutBase = DesignParser.parse(json, null)
        assertTrue(resultWithoutBase is ParseResult.Success)
        val successWithoutBase = resultWithoutBase as ParseResult.Success
        assertTrue(successWithoutBase.warnings.isEmpty())
        assertEquals(0xFF112233.toInt(), successWithoutBase.definition.color[ColorVariant.Light]?.get("nope"))
    }

    @Test
    fun limitsComponentsAndParameters() {
        val compEntries = (0 until 70).joinToString(",\n") { cIdx ->
            val paramEntries = if (cIdx == 0) {
                (0 until 40).joinToString(", ") { pIdx -> "\"param$pIdx\": $pIdx" }
            } else {
                "\"p\": 1"
            }
            "\"comp$cIdx\": { $paramEntries }"
        }
        val json = """
            {
              "schemaVersion": 1,
              "id": "limit-test",
              "name": "Limit Test",
              "components": {
                $compEntries
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(64, success.definition.components.size)
        assertEquals(32, success.definition.components["comp0"]?.size)
        assertEquals(2, success.warnings.size)
        val warningPaths = success.warnings.map { it.path }.toSet()
        assertTrue(warningPaths.contains("components"))
        assertTrue(warningPaths.contains("components.comp0"))
    }

    @Test
    fun dropsBadComponentValues() {
        val json = """
            {
              "schemaVersion": 1,
              "id": "comp-test",
              "name": "Comp Test",
              "components": {
                "123badId": { "p": 1 },
                "btn": {
                  "p1": null,
                  "p2": [1, 2],
                  "p3": { "a": 1 },
                  "good": 42
                }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertFalse(success.definition.components.containsKey("123badId"))
        val btn = checkNotNull(success.definition.components["btn"])
        assertEquals(1, btn.size)
        assertEquals(ComponentValue.Number(42f), btn["good"])
    }

    @Test
    fun rejectsOversizedText() {
        val huge = " ".repeat(262145)
        val result = DesignParser.parse(huge)
        assertTrue(result is ParseResult.Failure)
        val failure = result as ParseResult.Failure
        assertEquals("too large", failure.errors.first().message)
    }

    @Test
    fun capsIssueList() {
        val colors = (0 until 200).joinToString(", ") { "\"bad$it\": \"invalid\"" }
        val json = """
            {
              "schemaVersion": 1,
              "id": "cap-test",
              "name": "Cap Test",
              "color": {
                "light": {
                  $colors
                }
              }
            }
        """.trimIndent()
        val result = DesignParser.parse(json)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(51, success.warnings.size)
        assertEquals("more issues omitted", success.warnings.last().message)
    }

    @Test
    fun neverThrowsOnGarbage() {
        val garbageList = listOf(
            "",
            "   \n\t  ",
            "null",
            "[]",
            "{",
            "{\"a\":",
            "\u0000",
            "[".repeat(5000),
            "{\"a\":".repeat(5000),
            "1".repeat(100000),
            "{\"schemaVersion\":1e999,\"id\":\"a-b\",\"name\":\"n\"}"
        )
        for (garbage in garbageList) {
            val result = DesignParser.parse(garbage)
            assertNotNull(result)
        }
    }

    @Test
    fun parsesSamplePackFixture() {
        val stream = javaClass.classLoader?.getResourceAsStream("design/sample_pack.json")
            ?: javaClass.getResourceAsStream("/design/sample_pack.json")
            ?: error("sample_pack.json missing")
        val sampleText = stream.bufferedReader().use { it.readText() }
        val base = loadBase()
        val result = DesignParser.parse(sampleText, base)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals(1, success.warnings.size)
        assertEquals("futureThing", success.warnings.first().path)
        val button = checkNotNull(success.definition.components["button"])
        assertEquals(ComponentValue.Number(28f), button["cornerRadius"])
        assertEquals(ComponentValue.Flag(true), button["filled"])
    }
}
