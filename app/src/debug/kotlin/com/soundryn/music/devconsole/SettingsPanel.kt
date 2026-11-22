package com.soundryn.music.devconsole

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.Setting
import com.soundryn.music.core.domain.SettingsRegistry
import com.soundryn.music.core.domain.SettingsRepository
import java.util.Locale
import kotlinx.coroutines.launch

fun settingsPanel(repository: SettingsRepository) = DevConsolePanel(
    title = "Settings"
) {
    SettingsPanelContent(repository = repository)
}

@Composable
fun SettingsPanelContent(
    repository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var lastWrite by remember { mutableStateOf("none") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (setting in SettingsRegistry.all) {
            when (setting) {
                is Setting.Toggle -> {
                    val currentVal by repository.observe(setting).collectAsState(initial = setting.default)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = setting.id,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "default: ${if (setting.default) "on" else "off"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = currentVal,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    val res = repository.set(setting, checked)
                                    lastWrite = formatOutcome(res)
                                }
                            }
                        )
                    }
                }
                is Setting.Range -> {
                    val observedVal by repository.observe(setting).collectAsState(initial = setting.default)
                    var sliderVal by remember(observedVal) { mutableFloatStateOf(observedVal) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = setting.id,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "value %.2f   default %.2f",
                                    sliderVal,
                                    setting.default
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Slider(
                            value = sliderVal,
                            onValueChange = { sliderVal = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    val res = repository.set(setting, sliderVal)
                                    lastWrite = formatOutcome(res)
                                }
                            },
                            valueRange = setting.min..setting.max,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val res = repository.resetAll()
                    lastWrite = formatOutcome(res)
                }
            }
        ) {
            Text("Reset all")
        }

        Text(
            text = "Last write: $lastWrite",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatOutcome(outcome: Outcome<Unit>): String {
    return when (outcome) {
        is Outcome.Success -> "OK"
        is Outcome.Failure -> "FAILED (${outcome.error::class.java.simpleName})"
    }
}
