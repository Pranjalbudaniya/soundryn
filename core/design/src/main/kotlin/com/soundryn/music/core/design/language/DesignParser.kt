package com.soundryn.music.core.design.language

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

object DesignParser {

    private const val MAX_TEXT_LENGTH = 262144
    private const val MAX_DEPTH = 100
    private const val MAX_COMPONENTS = 64
    private const val MAX_PARAMS_PER_COMPONENT = 32
    private const val MAX_ISSUES = 50

    private val ID_REGEX = Regex("^[a-z][a-z0-9-]{1,39}$")
    private val IDENTIFIER_REGEX = Regex("^[a-z][a-zA-Z0-9]{0,39}$")
    private val COLOR_REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    private val KNOWN_ROOT_KEYS = setOf(
        "schemaVersion", "id", "name", "color", "shape",
        "spacing", "elevation", "type", "blur", "components"
    )
    private val KNOWN_COLOR_VARIANTS = setOf("light", "dark", "amoled")
    private val KNOWN_BLUR_PROPERTIES = setOf("maxRadius", "surfaceAlpha")
    private val KNOWN_TYPE_FIELDS = setOf("size", "lineHeight", "weight", "letterSpacing")

    fun parse(text: String, base: DesignDefinition? = null): ParseResult {
        if (text.length > MAX_TEXT_LENGTH) {
            return ParseResult.Failure(listOf(DesignIssue("", "too large")))
        }

        if (exceedsMaxDepth(text, MAX_DEPTH)) {
            return ParseResult.Failure(listOf(DesignIssue("", "not valid JSON")))
        }

        val root: JsonElement = try {
            Json.parseToJsonElement(text)
        } catch (_: Throwable) {
            return ParseResult.Failure(listOf(DesignIssue("", "not valid JSON")))
        }

        if (root !is JsonObject) {
            return ParseResult.Failure(listOf(DesignIssue("", "root must be an object")))
        }

        val errors = mutableListOf<DesignIssue>()

        // 1. Validate schemaVersion
        if ("schemaVersion" !in root) {
            errors.add(DesignIssue("schemaVersion", "schemaVersion is required"))
        } else {
            val svElem = root["schemaVersion"]
            if (svElem !is JsonPrimitive || svElem.isString) {
                errors.add(DesignIssue("schemaVersion", "schemaVersion must be an integer"))
            } else {
                val sv = svElem.intOrNull
                if (sv == null || sv < 1) {
                    errors.add(DesignIssue("schemaVersion", "schemaVersion must be an integer >= 1"))
                } else if (sv > 1) {
                    errors.add(DesignIssue("schemaVersion", "newer than this app (supports 1)"))
                }
            }
        }

        // 2. Validate id
        if ("id" !in root) {
            errors.add(DesignIssue("id", "id is required"))
        } else {
            val idElem = root["id"]
            if (idElem !is JsonPrimitive || !idElem.isString || !ID_REGEX.matches(idElem.content)) {
                errors.add(DesignIssue("id", "id must match ^[a-z][a-z0-9-]{1,39}$"))
            }
        }

        // 3. Validate name
        if ("name" !in root) {
            errors.add(DesignIssue("name", "name is required"))
        } else {
            val nameElem = root["name"]
            if (nameElem !is JsonPrimitive || !nameElem.isString) {
                errors.add(DesignIssue("name", "name must be a string"))
            } else {
                val trimmed = nameElem.content.trim()
                if (trimmed.isEmpty() || trimmed.length > 60) {
                    errors.add(DesignIssue("name", "name must be 1 to 60 characters after trim"))
                }
            }
        }

        if (errors.isNotEmpty()) {
            return ParseResult.Failure(errors)
        }

        val schemaVersion = (root["schemaVersion"] as JsonPrimitive).intOrNull ?: 1
        val id = (root["id"] as JsonPrimitive).content
        val name = (root["name"] as JsonPrimitive).content.trim()

        val warnings = mutableListOf<DesignIssue>()
        var issuesCapped = false

        fun warn(path: String, message: String) {
            if (warnings.size < MAX_ISSUES) {
                warnings.add(DesignIssue(path, message))
            } else if (warnings.size == MAX_ISSUES && !issuesCapped) {
                warnings.add(DesignIssue("", "more issues omitted"))
                issuesCapped = true
            }
        }

        // Check unknown root keys
        for (key in root.keys) {
            if (key !in KNOWN_ROOT_KEYS) {
                warn(key, "unknown field: $key")
            }
        }

        // Color section
        val colorMap = mutableMapOf<ColorVariant, Map<String, Int>>()
        if ("color" in root) {
            val colorElem = root["color"]
            if (colorElem !is JsonObject) {
                warn("color", "color must be an object")
            } else {
                for ((vKey, vVal) in colorElem) {
                    when (vKey) {
                        "light" -> parseColorVariant(ColorVariant.Light, vVal, "color.light", base, ::warn)?.let {
                            colorMap[ColorVariant.Light] = it
                        }
                        "dark" -> parseColorVariant(ColorVariant.Dark, vVal, "color.dark", base, ::warn)?.let {
                            colorMap[ColorVariant.Dark] = it
                        }
                        "amoled" -> parseColorVariant(ColorVariant.Amoled, vVal, "color.amoled", base, ::warn)?.let {
                            colorMap[ColorVariant.Amoled] = it
                        }
                        else -> warn("color.$vKey", "unknown color variant: $vKey")
                    }
                }
            }
        }

        // Shape section
        val shapeMap = mutableMapOf<String, Float>()
        if ("shape" in root) {
            val shapeElem = root["shape"]
            if (shapeElem !is JsonObject) {
                warn("shape", "shape must be an object")
            } else {
                for ((key, valElem) in shapeElem) {
                    if (base != null && !base.shape.containsKey(key)) {
                        warn("shape.$key", "unknown token: $key")
                        continue
                    }
                    val num = parseNumber(valElem, 0f, 1000f, wholeNumber = false)
                    if (num != null) {
                        shapeMap[key] = num
                    } else {
                        warn("shape.$key", "invalid shape radius")
                    }
                }
            }
        }

        // Spacing section
        val spacingMap = mutableMapOf<String, Float>()
        if ("spacing" in root) {
            val spElem = root["spacing"]
            if (spElem !is JsonObject) {
                warn("spacing", "spacing must be an object")
            } else {
                for ((key, valElem) in spElem) {
                    if (base != null && !base.spacing.containsKey(key)) {
                        warn("spacing.$key", "unknown token: $key")
                        continue
                    }
                    val num = parseNumber(valElem, 0f, 200f, wholeNumber = false)
                    if (num != null) {
                        spacingMap[key] = num
                    } else {
                        warn("spacing.$key", "invalid spacing value")
                    }
                }
            }
        }

        // Elevation section
        val elevationMap = mutableMapOf<String, Float>()
        if ("elevation" in root) {
            val elElem = root["elevation"]
            if (elElem !is JsonObject) {
                warn("elevation", "elevation must be an object")
            } else {
                for ((key, valElem) in elElem) {
                    if (base != null && !base.elevation.containsKey(key)) {
                        warn("elevation.$key", "unknown token: $key")
                        continue
                    }
                    val num = parseNumber(valElem, 0f, 100f, wholeNumber = false)
                    if (num != null) {
                        elevationMap[key] = num
                    } else {
                        warn("elevation.$key", "invalid elevation value")
                    }
                }
            }
        }

        // Blur section
        val blurMap = mutableMapOf<String, Float>()
        if ("blur" in root) {
            val blurElem = root["blur"]
            if (blurElem !is JsonObject) {
                warn("blur", "blur must be an object")
            } else {
                for ((key, valElem) in blurElem) {
                    if (key !in KNOWN_BLUR_PROPERTIES) {
                        warn("blur.$key", "unknown blur property: $key")
                        continue
                    }
                    when (key) {
                        "maxRadius" -> {
                            val num = parseNumber(valElem, 0f, 100f, wholeNumber = false)
                            if (num != null) blurMap["maxRadius"] = num
                            else warn("blur.maxRadius", "invalid maxRadius")
                        }
                        "surfaceAlpha" -> {
                            val num = parseNumber(valElem, 0f, 1f, wholeNumber = false)
                            if (num != null) blurMap["surfaceAlpha"] = num
                            else warn("blur.surfaceAlpha", "invalid surfaceAlpha")
                        }
                    }
                }
            }
        }

        // Type section
        val typeMap = mutableMapOf<String, Map<String, Float>>()
        if ("type" in root) {
            val typeElem = root["type"]
            if (typeElem !is JsonObject) {
                warn("type", "type must be an object")
            } else {
                for ((role, roleElem) in typeElem) {
                    if (base != null && !base.type.containsKey(role)) {
                        warn("type.$role", "unknown token: $role")
                        continue
                    }
                    if (roleElem !is JsonObject) {
                        warn("type.$role", "type.$role must be an object")
                        continue
                    }
                    val roleFields = mutableMapOf<String, Float>()
                    for ((field, valElem) in roleElem) {
                        if (field !in KNOWN_TYPE_FIELDS) {
                            warn("type.$role.$field", "unknown type field: $field")
                            continue
                        }
                        when (field) {
                            "size" -> {
                                val num = parseNumber(valElem, 1f, 200f, wholeNumber = false)
                                if (num != null) roleFields["size"] = num
                                else warn("type.$role.size", "invalid size")
                            }
                            "lineHeight" -> {
                                val num = parseNumber(valElem, 1f, 300f, wholeNumber = false)
                                if (num != null) roleFields["lineHeight"] = num
                                else warn("type.$role.lineHeight", "invalid lineHeight")
                            }
                            "weight" -> {
                                val num = parseNumber(valElem, 1f, 1000f, wholeNumber = true)
                                if (num != null) roleFields["weight"] = num
                                else warn("type.$role.weight", "invalid weight")
                            }
                            "letterSpacing" -> {
                                val num = parseNumber(valElem, -5f, 20f, wholeNumber = false)
                                if (num != null) roleFields["letterSpacing"] = num
                                else warn("type.$role.letterSpacing", "invalid letterSpacing")
                            }
                        }
                    }
                    if (roleFields.isNotEmpty()) {
                        typeMap[role] = roleFields
                    }
                }
            }
        }

        // Components section
        val componentsMap = mutableMapOf<String, Map<String, ComponentValue>>()
        if ("components" in root) {
            val compsElem = root["components"]
            if (compsElem !is JsonObject) {
                warn("components", "components must be an object")
            } else {
                var compCount = 0
                var compLimitWarned = false

                for ((compKey, compElem) in compsElem) {
                    if (!IDENTIFIER_REGEX.matches(compKey)) {
                        warn("components.$compKey", "invalid component id: $compKey")
                        continue
                    }
                    if (compElem !is JsonObject) {
                        warn("components.$compKey", "components.$compKey must be an object")
                        continue
                    }
                    if (compCount >= MAX_COMPONENTS) {
                        if (!compLimitWarned) {
                            warn("components", "limit of 64 components exceeded")
                            compLimitWarned = true
                        }
                        continue
                    }

                    val paramsMap = mutableMapOf<String, ComponentValue>()
                    var paramCount = 0
                    var paramLimitWarned = false

                    for ((paramKey, paramElem) in compElem) {
                        if (!IDENTIFIER_REGEX.matches(paramKey)) {
                            warn("components.$compKey.$paramKey", "invalid parameter name: $paramKey")
                            continue
                        }
                        if (paramCount >= MAX_PARAMS_PER_COMPONENT) {
                            if (!paramLimitWarned) {
                                warn("components.$compKey", "limit of 32 parameters exceeded")
                                paramLimitWarned = true
                            }
                            continue
                        }

                        if (paramElem !is JsonPrimitive || paramElem is JsonNull) {
                            warn("components.$compKey.$paramKey", "parameter value must be boolean, number, or string")
                            continue
                        }

                        if (paramElem.isString) {
                            val str = paramElem.content
                            if (str.length > 64) {
                                warn("components.$compKey.$paramKey", "parameter string value too long (max 64)")
                                continue
                            }
                            paramsMap[paramKey] = ComponentValue.Text(str)
                            paramCount++
                        } else {
                            val b = paramElem.booleanOrNull
                            if (b != null) {
                                paramsMap[paramKey] = ComponentValue.Flag(b)
                                paramCount++
                            } else {
                                val num = paramElem.floatOrNull
                                if (num != null && num.isFinite()) {
                                    paramsMap[paramKey] = ComponentValue.Number(num)
                                    paramCount++
                                } else {
                                    warn("components.$compKey.$paramKey", "invalid parameter value")
                                }
                            }
                        }
                    }
                    componentsMap[compKey] = paramsMap
                    compCount++
                }
            }
        }

        val definition = DesignDefinition(
            schemaVersion = schemaVersion,
            id = id,
            name = name,
            color = colorMap,
            shape = shapeMap,
            spacing = spacingMap,
            elevation = elevationMap,
            blur = blurMap,
            type = typeMap,
            components = componentsMap
        )

        return ParseResult.Success(definition, warnings)
    }

    private fun parseColorVariant(
        variant: ColorVariant,
        element: JsonElement?,
        path: String,
        base: DesignDefinition?,
        warn: (String, String) -> Unit
    ): Map<String, Int>? {
        if (element !is JsonObject) {
            warn(path, "$path must be an object")
            return null
        }
        val colors = mutableMapOf<String, Int>()
        for ((role, roleElem) in element) {
            if (base != null && base.color[ColorVariant.Light]?.containsKey(role) != true) {
                warn("$path.$role", "unknown token: $role")
                continue
            }
            if (roleElem !is JsonPrimitive || !roleElem.isString) {
                warn("$path.$role", "color must be a string")
                continue
            }
            val colorInt = parseHexColor(roleElem.content)
            if (colorInt == null) {
                warn("$path.$role", "invalid color format: ${roleElem.content}")
            } else {
                colors[role] = colorInt
            }
        }
        return colors
    }

    private fun parseHexColor(str: String): Int? {
        if (!COLOR_REGEX.matches(str)) return null
        val hex = str.substring(1)
        return try {
            if (hex.length == 6) {
                (0xFF000000L or hex.toLong(16)).toInt()
            } else {
                hex.toLong(16).toInt()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseNumber(
        element: JsonElement,
        min: Float,
        max: Float,
        wholeNumber: Boolean
    ): Float? {
        if (element !is JsonPrimitive || element.isString) return null
        val f = element.floatOrNull ?: return null
        if (!f.isFinite()) return null
        if (f < min || f > max) return null
        if (wholeNumber) {
            if (f != f.toLong().toFloat()) return null
        }
        return f
    }

    private fun exceedsMaxDepth(text: String, maxDepth: Int): Boolean {
        var depth = 0
        for (i in text.indices) {
            val c = text[i]
            if (c == '{' || c == '[') {
                depth++
                if (depth > maxDepth) return true
            } else if (c == '}' || c == ']') {
                if (depth > 0) depth--
            }
        }
        return false
    }
}
