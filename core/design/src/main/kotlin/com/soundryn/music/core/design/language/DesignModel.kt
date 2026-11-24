package com.soundryn.music.core.design.language

enum class ColorVariant { Light, Dark, Amoled }

sealed class ComponentValue {
    data class Flag(val value: Boolean) : ComponentValue()
    data class Number(val value: Float) : ComponentValue()
    data class Text(val value: String) : ComponentValue()
}

data class DesignDefinition(
    val schemaVersion: Int,
    val id: String,
    val name: String,
    val color: Map<ColorVariant, Map<String, Int>> = emptyMap(),
    val shape: Map<String, Float> = emptyMap(),
    val spacing: Map<String, Float> = emptyMap(),
    val elevation: Map<String, Float> = emptyMap(),
    val blur: Map<String, Float> = emptyMap(),
    val type: Map<String, Map<String, Float>> = emptyMap(),
    val components: Map<String, Map<String, ComponentValue>> = emptyMap()
)

data class ResolvedDesign(
    val color: Map<String, Int>,
    val shape: Map<String, Float>,
    val spacing: Map<String, Float>,
    val elevation: Map<String, Float>,
    val blur: Map<String, Float>,
    val type: Map<String, Map<String, Float>>,
    val components: Map<String, Map<String, ComponentValue>>
)

data class DesignIssue(val path: String, val message: String)

sealed class ParseResult {
    data class Success(
        val definition: DesignDefinition,
        val warnings: List<DesignIssue> = emptyList()
    ) : ParseResult()

    data class Failure(val errors: List<DesignIssue>) : ParseResult()
}
