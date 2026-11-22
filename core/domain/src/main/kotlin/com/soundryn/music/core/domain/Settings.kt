package com.soundryn.music.core.domain

private val ID_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

sealed interface Setting {
    val id: String

    data class Toggle(
        override val id: String,
        val default: Boolean,
    ) : Setting {
        init {
            require(ID_REGEX.matches(id)) { "Invalid setting id: $id" }
        }
    }

    data class Range(
        override val id: String,
        val default: Float,
        val min: Float,
        val max: Float,
    ) : Setting {
        init {
            require(ID_REGEX.matches(id)) { "Invalid setting id: $id" }
            require(min.isFinite() && max.isFinite()) { "min and max must be finite: min=$min, max=$max" }
            require(min < max) { "min must be less than max: min=$min, max=$max" }
            require(default.isFinite() && default in min..max) {
                "default must be between min and max: default=$default, min=$min, max=$max"
            }
        }

        fun coerce(value: Float): Float {
            return if (!value.isFinite()) default else value.coerceIn(min, max)
        }
    }
}

// The setting id is the storage key for good. Renaming a setting later loses the user's value.
object SettingsRegistry {
    val ReduceMotion = Setting.Toggle(
        id = "motion.reduce",
        default = false,
    )

    val HapticsEnabled = Setting.Toggle(
        id = "haptics.enabled",
        default = true,
    )

    val BlurEnabled = Setting.Toggle(
        id = "blur.enabled",
        default = true,
    )

    val BlurStrength = Setting.Range(
        id = "blur.strength",
        default = 0.5f,
        min = 0.0f,
        max = 1.0f,
    )

    val all: List<Setting> = listOf(
        ReduceMotion,
        HapticsEnabled,
        BlurEnabled,
        BlurStrength,
    )
}
