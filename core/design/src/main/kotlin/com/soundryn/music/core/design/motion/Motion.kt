package com.soundryn.music.core.design.motion

data class SpringToken(val stiffness: Float, val dampingRatio: Float)

object MotionRole {
    const val SpatialFast = "spatialFast"
    const val SpatialDefault = "spatialDefault"
    const val SpatialSlow = "spatialSlow"
    const val EffectsFast = "effectsFast"
    const val EffectsDefault = "effectsDefault"
    const val EffectsSlow = "effectsSlow"
    const val Press = "press"
    const val DragRelease = "dragRelease"
    const val Expand = "expand"
    const val ContainerTransform = "containerTransform"
    const val ListReorder = "listReorder"
    const val SliderPress = "sliderPress"
    const val Reduced = "reduced"

    val all: List<String> = listOf(
        SpatialFast,
        SpatialDefault,
        SpatialSlow,
        EffectsFast,
        EffectsDefault,
        EffectsSlow,
        Press,
        DragRelease,
        Expand,
        ContainerTransform,
        ListReorder,
        SliderPress,
        Reduced,
    )
}

enum class MotionMode { Full, Reduced, Off }

object MotionPolicy {
    fun mode(userReduce: Boolean, systemScale: Float): MotionMode {
        val scale = if (!systemScale.isFinite() || systemScale < 0f) 1f else systemScale
        return when {
            scale == 0f -> MotionMode.Off
            userReduce -> MotionMode.Reduced
            else -> MotionMode.Full
        }
    }

    fun springFor(
        motion: Map<String, Map<String, Float>>,
        role: String,
        mode: MotionMode,
    ): SpringToken? {
        val key = if (mode == MotionMode.Full) role else MotionRole.Reduced
        val fields = motion[key] ?: return null
        val stiffness = fields["stiffness"] ?: return null
        val dampingRatio = fields["dampingRatio"] ?: return null
        if (!stiffness.isFinite() || stiffness <= 0f) return null
        if (!dampingRatio.isFinite() || dampingRatio <= 0f) return null
        return SpringToken(stiffness = stiffness, dampingRatio = dampingRatio)
    }
}
