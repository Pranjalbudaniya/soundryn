package com.soundryn.music.devconsole

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.soundryn.music.core.design.language.BaseLanguage
import com.soundryn.music.core.design.language.ColorVariant
import com.soundryn.music.core.design.language.DesignResolver
import com.soundryn.music.core.design.language.ParseResult
import com.soundryn.music.core.design.motion.LocalMotionTheme
import com.soundryn.music.core.design.motion.MotionMode
import com.soundryn.music.core.design.motion.MotionPolicy
import com.soundryn.music.core.design.motion.MotionRole
import com.soundryn.music.core.design.motion.ProvideMotion
import com.soundryn.music.core.design.motion.systemAnimationScale
import com.soundryn.music.core.design.settings.uiSettingsFlow
import com.soundryn.music.core.domain.SettingsRegistry
import com.soundryn.music.core.domain.SettingsRepository
import java.util.Locale
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun motionPanel(settings: SettingsRepository) = DevConsolePanel(
    title = "Motion"
) {
    MotionPanelContent(settings = settings)
}

@Composable
fun MotionPanelContent(
    settings: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val uiSettings by remember(settings) { uiSettingsFlow(settings) }.collectAsState(initial = null)
    val ui = uiSettings
    if (ui == null) {
        Text(
            text = "Settings: loading...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val initialScale = remember(context) {
        Settings.Global.getFloat(
            context.applicationContext.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }
    val systemScale by remember(context) { systemAnimationScale(context) }
        .collectAsState(initial = initialScale)

    val activity = remember(context) { context.findActivity() }
    var animatorsEnabled by remember(systemScale) {
        mutableStateOf(ValueAnimator.areAnimatorsEnabled())
    }
    val lifecycleOwner = activity as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    animatorsEnabled = ValueAnimator.areAnimatorsEnabled()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    val baseResult = remember { BaseLanguage.load() }
    when (baseResult) {
        is ParseResult.Failure -> {
            val err = baseResult.errors.firstOrNull()
            Text(
                text = "Base: FAILED (${err?.path}: ${err?.message})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
            return
        }
        is ParseResult.Success -> {
            val motionTokens = remember(baseResult.definition) {
                DesignResolver.resolve(baseResult.definition, emptyList(), ColorVariant.Light).motion
            }
            val mode = MotionPolicy.mode(ui.reduceMotion, systemScale)

            ProvideMotion(tokens = motionTokens, mode = mode) {
                MotionLoadedContent(
                    settings = settings,
                    reduceMotion = ui.reduceMotion,
                    systemScale = systemScale,
                    animatorsEnabled = animatorsEnabled,
                    mode = mode,
                    motionTokens = motionTokens,
                    window = activity?.window,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun MotionLoadedContent(
    settings: SettingsRepository,
    reduceMotion: Boolean,
    systemScale: Float,
    animatorsEnabled: Boolean,
    mode: MotionMode,
    motionTokens: Map<String, Map<String, Float>>,
    window: android.view.Window?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val motionTheme = LocalMotionTheme.current
    var selectedRole by remember { mutableStateOf(MotionRole.Press) }

    val frameMeter = remember(window) { window?.let { FrameMeter(it) } }
    var stats by remember { mutableStateOf(FrameStats(0, 0, 0f, 0f)) }
    var lastDeadlineMs by remember { mutableFloatStateOf(0f) }

    DisposableEffect(frameMeter) {
        frameMeter?.start()
        onDispose { frameMeter?.stop() }
    }

    LaunchedEffect(frameMeter) {
        if (frameMeter != null) {
            while (true) {
                delay(500)
                val (snapStats, snapDeadline) = frameMeter.snapshot()
                stats = snapStats
                lastDeadlineMs = snapDeadline
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Settings: loaded",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Reduce motion: ${if (reduceMotion) "on" else "off"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        settings.set(SettingsRegistry.ReduceMotion, !reduceMotion)
                    }
                }
            ) {
                Text(if (reduceMotion) "Turn off" else "Turn on")
            }
        }

        Text(
            text = "System animator scale: $systemScale   areAnimatorsEnabled: $animatorsEnabled",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = "Mode: ${mode.name}",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = "Roles:",
            style = MaterialTheme.typography.bodyMedium,
        )

        RoleChipGroup(
            selectedRole = selectedRole,
            onSelect = { selectedRole = it },
        )

        val roleFields = motionTokens[selectedRole]
        val effective = MotionPolicy.springFor(motionTokens, selectedRole, mode)
        val rawStiff = roleFields?.get("stiffness")?.let { formatNumber(it) } ?: "?"
        val rawDamp = roleFields?.get("dampingRatio")?.let { formatNumber(it) } ?: "?"
        val effStr = if (effective != null) {
            "${formatNumber(effective.stiffness)} / ${formatNumber(effective.dampingRatio)} (${mode.name})"
        } else {
            "none (${mode.name})"
        }
        Text(
            text = "Selected: $selectedRole   stiffness $rawStiff  damping $rawDamp   effective now: $effStr",
            style = MaterialTheme.typography.bodySmall,
        )

        val activeSpec = motionTheme.spec<Float>(selectedRole)
        val dragReleaseSpec = motionTheme.spec<Float>(MotionRole.DragRelease)

        MoveDemoRow(
            activeSpec = activeSpec,
            frameMeter = frameMeter,
            onStatsUpdated = { s, d ->
                stats = s
                lastDeadlineMs = d
            },
        )

        DragDemoRow(dragReleaseSpec = dragReleaseSpec)

        PressDemoRow(activeSpec = activeSpec)

        FrameMeterRow(
            stats = stats,
            deadlineMs = lastDeadlineMs,
            onReset = {
                frameMeter?.reset()
                stats = FrameStats(0, 0, 0f, 0f)
                lastDeadlineMs = 0f
            },
        )
    }
}

@Composable
private fun RoleChipGroup(
    selectedRole: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (role in MotionRole.all) {
            FilterChip(
                selected = selectedRole == role,
                onClick = { onSelect(role) },
                label = { Text(role) },
            )
        }
    }
}

@Composable
private fun MoveDemoRow(
    activeSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float>,
    frameMeter: FrameMeter?,
    onStatsUpdated: (FrameStats, Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val squareSizeDp = 36.dp
    val squareSizePx = with(density) { squareSizeDp.toPx() }
    val paddingPx = with(density) { 8.dp.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val moveAnim = remember { Animatable(0f) }
    var targetAtEnd by remember { mutableStateOf(false) }
    var running10s by remember { mutableStateOf(false) }
    var benchmarkSummary by remember { mutableStateOf<String?>(null) }

    val currentActiveSpec by rememberUpdatedState(activeSpec)
    val maxTravel = (trackWidthPx - squareSizePx - paddingPx).coerceAtLeast(0f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Move:", style = MaterialTheme.typography.bodySmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 4.dp)
                .onSizeChanged { trackWidthPx = it.width },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(squareSizeDp)
                    .graphicsLayer { translationX = moveAnim.value * maxTravel }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    targetAtEnd = !targetAtEnd
                    val target = if (targetAtEnd) 1f else 0f
                    scope.launch {
                        moveAnim.animateTo(target, animationSpec = currentActiveSpec)
                    }
                }
            ) {
                Text("Move")
            }

            OutlinedButton(
                enabled = !running10s,
                onClick = {
                    running10s = true
                    benchmarkSummary = "Running 10 s..."
                    scope.launch {
                        try {
                            frameMeter?.reset()
                            val startMs = System.currentTimeMillis()
                            while (System.currentTimeMillis() - startMs < 10_000L) {
                                targetAtEnd = !targetAtEnd
                                val target = if (targetAtEnd) 1f else 0f
                                launch {
                                    moveAnim.animateTo(target, animationSpec = currentActiveSpec)
                                }
                                delay(700L)
                            }
                            if (frameMeter != null) {
                                val (s, d) = frameMeter.snapshot()
                                onStatsUpdated(s, d)
                                val pct = if (s.frames > 0) s.overDeadline * 100f / s.frames else 0f
                                val verdict = if (pct <= 5f) "within budget" else "over budget"
                                benchmarkSummary = String.format(
                                    Locale.US,
                                    "10s done: %.1f%% over (%s)",
                                    pct,
                                    verdict,
                                )
                            } else {
                                benchmarkSummary = "10s done (no window)"
                            }
                        } finally {
                            running10s = false
                        }
                    }
                }
            ) {
                Text("Run 10 s")
            }

            benchmarkSummary?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DragDemoRow(
    dragReleaseSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float>,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val squareSizeDp = 36.dp
    val squareSizePx = with(density) { squareSizeDp.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val dragAnim = remember { Animatable(0f) }

    val halfTravelPx = ((trackWidthPx - squareSizePx - with(density) { 8.dp.toPx() }) / 2f)
        .coerceAtLeast(0f)
    val currentHalfTravel by rememberUpdatedState(halfTravelPx)
    val currentDragSpec by rememberUpdatedState(dragReleaseSpec)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                dragAnim.stop()
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val next = (dragAnim.value + dragAmount)
                                .coerceIn(-currentHalfTravel, currentHalfTravel)
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                dragAnim.snapTo(next)
                            }
                        },
                        onDragEnd = {
                            val vx = velocityTracker.calculateVelocity().x
                            scope.launch {
                                dragAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = currentDragSpec,
                                    initialVelocity = vx,
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                dragAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = currentDragSpec,
                                )
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(squareSizeDp)
                    .graphicsLayer { translationX = dragAnim.value }
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
        }
        Text(
            text = "Drag: drag the square",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PressDemoRow(
    activeSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float>,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.15f else 1.0f,
        animationSpec = activeSpec,
        label = "pressScale",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(12.dp),
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressed = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "box",
                color = MaterialTheme.colorScheme.onTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = "Press: press and hold",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FrameMeterRow(
    stats: FrameStats,
    deadlineMs: Float,
    onReset: () -> Unit,
) {
    val pct = if (stats.frames > 0) stats.overDeadline * 100f / stats.frames else 0f
    val verdict = when {
        stats.frames == 0 -> ""
        pct <= 5f -> " | within budget"
        else -> " | over budget"
    }
    val text = String.format(
        Locale.US,
        "frames %d | over deadline %d (%.1f%%) | worst %.1f ms | p95 %.1f ms | deadline %.1f ms | budget: <= 5%% over%s",
        stats.frames,
        stats.overDeadline,
        pct,
        stats.worstMs,
        stats.p95Ms,
        deadlineMs,
        verdict,
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Frames:", style = MaterialTheme.typography.bodySmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onReset) {
                Text("Reset")
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatNumber(value: Float): String {
    return if (value == value.toInt().toFloat()) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
