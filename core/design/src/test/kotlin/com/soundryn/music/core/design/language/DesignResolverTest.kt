package com.soundryn.music.core.design.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignResolverTest {

    private fun loadBase(): DesignDefinition {
        val result = BaseLanguage.load()
        assertTrue(result is ParseResult.Success)
        return (result as ParseResult.Success).definition
    }

    private fun parseLayer(json: String, base: DesignDefinition): DesignDefinition {
        val result = DesignParser.parse(json, base)
        assertTrue(result is ParseResult.Success)
        return (result as ParseResult.Success).definition
    }

    @Test
    fun emptyLayersResolveBase() {
        val base = loadBase()
        val resolved = DesignResolver.resolve(base, emptyList(), ColorVariant.Light)

        assertEquals(base.color[ColorVariant.Light], resolved.color)
        assertEquals(base.shape, resolved.shape)
        assertEquals(base.spacing, resolved.spacing)
        assertEquals(base.elevation, resolved.elevation)
        assertEquals(base.blur, resolved.blur)
        assertEquals(base.type, resolved.type)
    }

    @Test
    fun higherLayerWinsOverLowerLayer() {
        val base = loadBase()
        val layerA = parseLayer(
            """{"schemaVersion":1,"id":"layer-a","name":"A","color":{"light":{"primary":"#00000001"}}}""",
            base
        )
        val layerB = parseLayer(
            """{"schemaVersion":1,"id":"layer-b","name":"B","color":{"light":{"primary":"#00000002"}}}""",
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layerA, layerB), ColorVariant.Light)
        assertEquals(1, resolved.color["primary"])
    }

    @Test
    fun missingTokenFallsThroughToBase() {
        val base = loadBase()
        val layer = parseLayer(
            """{"schemaVersion":1,"id":"layer-x","name":"X","color":{"light":{"primary":"#00000001"}}}""",
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Light)
        assertEquals(1, resolved.color["primary"])
        assertEquals(base.color[ColorVariant.Light]?.get("secondary"), resolved.color["secondary"])
    }

    @Test
    fun lightIgnoresDarkAndAmoled() {
        val base = loadBase()
        val layer = parseLayer(
            """{
                "schemaVersion": 1,
                "id": "layer-x",
                "name": "X",
                "color": {
                    "dark": { "primary": "#00000001" },
                    "amoled": { "primary": "#00000002" }
                }
            }""".trimIndent(),
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Light)
        assertEquals(base.color[ColorVariant.Light]?.get("primary"), resolved.color["primary"])
    }

    @Test
    fun amoledPrefersAmoledOfAnyLayerThenDark() {
        val base = loadBase()
        val layer = parseLayer(
            """{
                "schemaVersion": 1,
                "id": "layer-x",
                "name": "X",
                "color": {
                    "dark": { "surface": "#FF112233", "primary": "#FF445566" }
                }
            }""".trimIndent(),
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Amoled)
        assertEquals(0xFF000000.toInt(), resolved.color["surface"])
        assertEquals(0xFF445566.toInt(), resolved.color["primary"])
    }

    @Test
    fun amoledLayerBeatsBaseAmoled() {
        val base = loadBase()
        val layer = parseLayer(
            """{
                "schemaVersion": 1,
                "id": "layer-x",
                "name": "X",
                "color": {
                    "amoled": { "surface": "#FF223344" }
                }
            }""".trimIndent(),
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Amoled)
        assertEquals(0xFF223344.toInt(), resolved.color["surface"])
    }

    @Test
    fun typeFieldsMergePerField() {
        val base = loadBase()
        val layer = parseLayer(
            """{
                "schemaVersion": 1,
                "id": "layer-x",
                "name": "X",
                "type": {
                    "bodyLarge": { "size": 22.0 }
                }
            }""".trimIndent(),
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Light)
        val bodyLarge = checkNotNull(resolved.type["bodyLarge"])
        assertEquals(22.0f, bodyLarge["size"])
        assertEquals(base.type["bodyLarge"]?.get("lineHeight"), bodyLarge["lineHeight"])
        assertEquals(base.type["bodyLarge"]?.get("weight"), bodyLarge["weight"])
        assertEquals(base.type["bodyLarge"]?.get("letterSpacing"), bodyLarge["letterSpacing"])
    }

    @Test
    fun componentParamsMergePerParameter() {
        val base = loadBase()
        val layer1 = parseLayer(
            """{
                "schemaVersion": 1,
                "id": "l1",
                "name": "L1",
                "components": {
                    "button": { "cornerRadius": 16.0 }
                }
            }""".trimIndent(),
            base
        )
        val layer2 = parseLayer(
            """{
                "schemaVersion": 1,
                "id": "l2",
                "name": "L2",
                "components": {
                    "button": { "cornerRadius": 24.0, "filled": true }
                }
            }""".trimIndent(),
            base
        )
        val resolved = DesignResolver.resolve(base, listOf(layer1, layer2), ColorVariant.Light)
        val button = checkNotNull(resolved.components["button"])
        assertEquals(ComponentValue.Number(16.0f), button["cornerRadius"])
        assertEquals(ComponentValue.Flag(true), button["filled"])
    }

    @Test
    fun rolesOutsideBaseNeverAppear() {
        val base = loadBase()
        val parsedNoBase = DesignParser.parse(
            """{
                "schemaVersion": 1,
                "id": "layer-x",
                "name": "X",
                "color": {
                    "light": { "unsupportedRole": "#FF112233" }
                }
            }""".trimIndent(),
            null
        )
        val layer = (parsedNoBase as ParseResult.Success).definition
        val resolved = DesignResolver.resolve(base, listOf(layer), ColorVariant.Light)
        assertFalse(resolved.color.containsKey("unsupportedRole"))
    }
}
