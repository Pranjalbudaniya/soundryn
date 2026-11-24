package com.soundryn.music.core.design.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class MotionTheme(
    private val tokens: Map<String, Map<String, Float>>,
    val mode: MotionMode,
) {
    fun <T> spec(role: String): FiniteAnimationSpec<T> {
        val token = MotionPolicy.springFor(tokens, role, mode)
        return if (token == null) {
            snap()
        } else {
            spring(
                dampingRatio = token.dampingRatio,
                stiffness = token.stiffness,
            )
        }
    }
}

val LocalMotionTheme = staticCompositionLocalOf { MotionTheme(emptyMap(), MotionMode.Off) }

@Composable
fun ProvideMotion(
    tokens: Map<String, Map<String, Float>>,
    mode: MotionMode,
    content: @Composable () -> Unit,
) {
    val theme = remember(tokens, mode) { MotionTheme(tokens, mode) }
    CompositionLocalProvider(LocalMotionTheme provides theme, content = content)
}
