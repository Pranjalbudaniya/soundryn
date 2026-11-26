package com.soundryn.music.devconsole

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.soundryn.music.core.design.blur.BlurPolicy
import com.soundryn.music.core.design.blur.BlurState
import com.soundryn.music.core.design.blur.isSlowDevice
import com.soundryn.music.core.design.haptics.HapticRole
import com.soundryn.music.core.design.haptics.LocalHaptics
import com.soundryn.music.core.design.haptics.ProvideHaptics
import com.soundryn.music.core.design.language.BaseLanguage
import com.soundryn.music.core.design.language.ColorVariant
import com.soundryn.music.core.design.language.DesignResolver
import com.soundryn.music.core.design.language.ParseResult
import com.soundryn.music.core.design.settings.uiSettingsFlow
import com.soundryn.music.core.domain.SettingsRepository
import java.util.Locale

private val STRIPE_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFF43A047), Color(0xFF00ACC1), Color(0xFF1E88E5)
)

fun hapticsBlurPanel(settings: SettingsRepository) = DevConsolePanel(
    title = "Haptics and blur"
) { HapticsBlurPanelContent(settings = settings) }

@Composable
fun HapticsBlurPanelContent(
    settings: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val ui = remember(settings) { uiSettingsFlow(settings) }.collectAsState(initial = null).value
    if (ui == null) {
        Text("Settings: loading...", style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }

    val context = LocalContext.current
    val debuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    var touchFeedbackOn by remember(context) { mutableStateOf(isTouchFeedbackOn(context)) }
    val lifecycleOwner = remember(context) { context.findOwnerActivity() as? LifecycleOwner }
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) touchFeedbackOn = isTouchFeedbackOn(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val baseResult = remember { BaseLanguage.load() }
    if (baseResult is ParseResult.Failure) {
        val err = baseResult.errors.firstOrNull()
        Text(
            text = "Base: FAILED (${err?.path}: ${err?.message})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        return
    }

    val resolved = remember(baseResult) {
        DesignResolver.resolve((baseResult as ParseResult.Success).definition, emptyList(), ColorVariant.Light)
    }

    ProvideHaptics(tokens = resolved.haptics, enabled = ui.hapticsEnabled) {
        HapticsBlurLoadedBody(
            debuggable = debuggable,
            hapticsEnabled = ui.hapticsEnabled,
            touchFeedbackOn = touchFeedbackOn,
            hapticsTokens = resolved.haptics,
            blurEnabled = ui.blurEnabled,
            blurStrength = ui.blurStrength,
            maxRadius = resolved.blur["maxRadius"] ?: 0f,
            actualLowRam = remember(context) { isSlowDevice(context) },
            modifier = modifier,
        )
    }
}

@Composable
private fun HapticsBlurLoadedBody(
    debuggable: Boolean,
    hapticsEnabled: Boolean,
    touchFeedbackOn: Boolean,
    hapticsTokens: Map<String, String>,
    blurEnabled: Boolean,
    blurStrength: Float,
    maxRadius: Float,
    actualLowRam: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    var pretendSlow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Build: debuggable=$debuggable     Android API: ${Build.VERSION.SDK_INT}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Haptics setting: ${onOff(hapticsEnabled)}   System touch feedback: ${onOff(touchFeedbackOn)}",
            style = MaterialTheme.typography.bodyMedium,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (role in HapticRole.all) {
                val kind = hapticsTokens[role] ?: "?"
                OutlinedButton(onClick = { haptics.perform(role) }) {
                    Text("$role: $kind")
                }
            }
        }

        Text(
            text = String.format(
                Locale.US,
                "Blur setting: %s   strength %.2f   max radius %.1f dp   device isLowRamDevice: %s",
                onOff(blurEnabled),
                blurStrength,
                maxRadius,
                actualLowRam,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = pretendSlow,
                onCheckedChange = { pretendSlow = it },
            )
            Text(
                text = "Pretend slow device",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val decision = BlurPolicy.decide(
            enabled = blurEnabled,
            strength = blurStrength,
            maxRadius = maxRadius,
            slowDevice = pretendSlow || actualLowRam,
        )
        val blurLine = when (decision.state) {
            BlurState.On -> String.format(Locale.US, "Blur: On, radius %.1f dp", decision.radiusDp)
            BlurState.OffBySetting -> "Blur: Off: setting"
            BlurState.OffSlowDevice -> "Blur: Off: slow device"
        }
        Text(text = blurLine, style = MaterialTheme.typography.bodyMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .blur(decision.radiusDp.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (color in STRIPE_COLORS) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(color),
                    )
                }
            }
            Text(
                text = "Soundryn",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
        }
    }
}

private fun onOff(value: Boolean): String = if (value) "on" else "off"

private fun isTouchFeedbackOn(context: Context): Boolean {
    return Settings.System.getInt(
        context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        0,
    ) != 0
}

private tailrec fun Context.findOwnerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findOwnerActivity()
    else -> null
}
