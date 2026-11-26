package com.soundryn.music.core.design.language

object DesignResolver {

    fun resolve(
        base: DesignDefinition,
        layers: List<DesignDefinition>,
        variant: ColorVariant
    ): ResolvedDesign {
        val search = layers + listOf(base)

        // Color resolution
        val chain = when (variant) {
            ColorVariant.Light -> listOf(ColorVariant.Light)
            ColorVariant.Dark -> listOf(ColorVariant.Dark)
            ColorVariant.Amoled -> listOf(ColorVariant.Amoled, ColorVariant.Dark)
        }
        val resolvedColors = mutableMapOf<String, Int>()
        val baseRoles = base.color[ColorVariant.Light]?.keys ?: emptySet()

        for (role in baseRoles) {
            findColor@ for (v in chain) {
                for (layer in search) {
                    val colorVal = layer.color[v]?.get(role)
                    if (colorVal != null) {
                        resolvedColors[role] = colorVal
                        break@findColor
                    }
                }
            }
        }

        // Shape resolution
        val resolvedShape = mutableMapOf<String, Float>()
        for (key in base.shape.keys) {
            for (layer in search) {
                val value = layer.shape[key]
                if (value != null) {
                    resolvedShape[key] = value
                    break
                }
            }
        }

        // Spacing resolution
        val resolvedSpacing = mutableMapOf<String, Float>()
        for (key in base.spacing.keys) {
            for (layer in search) {
                val value = layer.spacing[key]
                if (value != null) {
                    resolvedSpacing[key] = value
                    break
                }
            }
        }

        // Elevation resolution
        val resolvedElevation = mutableMapOf<String, Float>()
        for (key in base.elevation.keys) {
            for (layer in search) {
                val value = layer.elevation[key]
                if (value != null) {
                    resolvedElevation[key] = value
                    break
                }
            }
        }

        // Blur resolution
        val resolvedBlur = mutableMapOf<String, Float>()
        for (key in base.blur.keys) {
            for (layer in search) {
                val value = layer.blur[key]
                if (value != null) {
                    resolvedBlur[key] = value
                    break
                }
            }
        }

        // Type, Motion, and Haptics resolution
        val resolvedType = resolveNestedFloats(base.type, search) { it.type }
        val resolvedMotion = resolveNestedFloats(base.motion, search) { it.motion }
        val resolvedHaptics = resolveHaptics(base.haptics, search)

        // Components resolution
        val resolvedComponents = mutableMapOf<String, Map<String, ComponentValue>>()
        val allComponentIds = search.flatMap { it.components.keys }.toSet()
        for (compId in allComponentIds) {
            val allParams = search.flatMap { it.components[compId]?.keys ?: emptySet() }.toSet()
            val paramsMap = mutableMapOf<String, ComponentValue>()
            for (param in allParams) {
                for (layer in search) {
                    val value = layer.components[compId]?.get(param)
                    if (value != null) {
                        paramsMap[param] = value
                        break
                    }
                }
            }
            resolvedComponents[compId] = paramsMap
        }

        return ResolvedDesign(
            color = resolvedColors,
            shape = resolvedShape,
            spacing = resolvedSpacing,
            elevation = resolvedElevation,
            blur = resolvedBlur,
            type = resolvedType,
            components = resolvedComponents,
            motion = resolvedMotion,
            haptics = resolvedHaptics
        )
    }

    private fun resolveHaptics(
        baseMap: Map<String, String>,
        search: List<DesignDefinition>
    ): Map<String, String> {
        val resolved = mutableMapOf<String, String>()
        for (role in baseMap.keys) {
            for (layer in search) {
                val value = layer.haptics[role]
                if (value != null) {
                    resolved[role] = value
                    break
                }
            }
        }
        return resolved
    }

    private fun resolveNestedFloats(
        baseMap: Map<String, Map<String, Float>>,
        search: List<DesignDefinition>,
        selector: (DesignDefinition) -> Map<String, Map<String, Float>>
    ): Map<String, Map<String, Float>> {
        val resolved = mutableMapOf<String, Map<String, Float>>()
        for ((role, baseFields) in baseMap) {
            val roleMap = mutableMapOf<String, Float>()
            for (field in baseFields.keys) {
                for (layer in search) {
                    val value = selector(layer)[role]?.get(field)
                    if (value != null) {
                        roleMap[field] = value
                        break
                    }
                }
            }
            resolved[role] = roleMap
        }
        return resolved
    }
}
