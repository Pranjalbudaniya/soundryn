package com.soundryn.music.devconsole

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soundryn.music.core.design.language.BaseLanguage
import com.soundryn.music.core.design.language.ColorVariant
import com.soundryn.music.core.design.language.ComponentValue
import com.soundryn.music.core.design.language.DesignParser
import com.soundryn.music.core.design.language.DesignResolver
import com.soundryn.music.core.design.language.ParseResult
import java.util.Locale

private const val SAMPLE_PACK_JSON = """{
  "schemaVersion": 1,
  "id": "sample-pink",
  "name": "Sample pink",
  "color": {
    "light": { "primary": "#B4236B", "onPrimary": "#FFFFFF" },
    "dark": { "primary": "#FFFFB0CB" }
  },
  "shape": { "medium": 20 },
  "type": { "bodyLarge": { "size": 18 } },
  "components": { "button": { "cornerRadius": 28, "filled": true } },
  "futureThing": { "x": 1 }
}"""

private const val BROKEN_SAMPLE_JSON = """{"schemaVersion":2,"id":"Bad Id","name":""}"""

private val SWATCH_ROLES = listOf(
    "primary", "onPrimary", "primaryContainer",
    "secondary", "tertiary", "error",
    "background", "surface", "surfaceContainerLowest",
    "surfaceContainerHigh", "outline", "scrim"
)

fun designPanel(): DevConsolePanel = DevConsolePanel(
    title = "Design"
) {
    DesignPanelContent()
}

@Composable
fun DesignPanelContent(modifier: Modifier = Modifier) {
    val baseResult = remember { BaseLanguage.load() }
    var selectedVariant by remember { mutableStateOf(ColorVariant.Light) }
    var samplePackOn by remember { mutableStateOf(false) }
    var brokenErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (baseResult) {
            is ParseResult.Failure -> {
                val firstErr = baseResult.errors.firstOrNull()
                val errText = if (firstErr != null) "${firstErr.path}: ${firstErr.message}" else "unknown error"
                Text(
                    text = "Base: FAILED ($errText)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is ParseResult.Success -> {
                val base = baseResult.definition
                Text(
                    text = "Base: ${base.name} (id ${base.id}, schema ${base.schemaVersion})  OK",
                    style = MaterialTheme.typography.bodyMedium
                )
                val colorCount = base.color[ColorVariant.Light]?.size ?: 0
                Text(
                    text = "Tokens: color $colorCount | shape ${base.shape.size} | type ${base.type.size} | spacing ${base.spacing.size} | elevation ${base.elevation.size}",
                    style = MaterialTheme.typography.bodySmall
                )

                // Variant selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val variants = listOf(
                        ColorVariant.Light to "Light",
                        ColorVariant.Dark to "Dark",
                        ColorVariant.Amoled to "AMOLED"
                    )
                    for ((variant, label) in variants) {
                        if (selectedVariant == variant) {
                            Button(
                                onClick = { selectedVariant = variant }
                            ) {
                                Text(label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedVariant = variant }
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                // Sample pack layer resolution
                val sampleLayer = remember(base) {
                    (DesignParser.parse(SAMPLE_PACK_JSON, base) as? ParseResult.Success)?.definition
                }
                val layers = if (samplePackOn && sampleLayer != null) listOf(sampleLayer) else emptyList()
                val resolved = remember(base, layers, selectedVariant) {
                    DesignResolver.resolve(base, layers, selectedVariant)
                }

                // 12 swatches
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    for (role in SWATCH_ROLES) {
                        val colorInt = resolved.color[role] ?: 0
                        val hex = String.format(Locale.US, "#%08X", colorInt)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(colorInt))
                                    .border(1.dp, Color.Gray)
                            )
                            Text(
                                text = "$role $hex",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Sample pack toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = samplePackOn,
                        onCheckedChange = { samplePackOn = it }
                    )
                    Text(
                        text = "Sample pack",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Readouts
                val shapeMedium = resolved.shape["medium"]?.let { "$it" } ?: "none"
                Text(
                    text = "shape.medium = $shapeMedium",
                    style = MaterialTheme.typography.bodyMedium
                )

                val typeSize = resolved.type["bodyLarge"]?.get("size")?.let { "$it" } ?: "none"
                Text(
                    text = "type.bodyLarge.size = $typeSize",
                    style = MaterialTheme.typography.bodyMedium
                )

                val btnCornerRadius = resolved.components["button"]?.get("cornerRadius")?.let {
                    if (it is ComponentValue.Number) "${it.value}" else it.toString()
                } ?: "none"
                Text(
                    text = "components.button.cornerRadius = $btnCornerRadius",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Broken sample button
                Button(
                    onClick = {
                        val result = DesignParser.parse(BROKEN_SAMPLE_JSON)
                        brokenErrors = if (result is ParseResult.Failure) {
                            result.errors.take(5).map { "${it.path}: ${it.message}" }
                        } else {
                            emptyList()
                        }
                    }
                ) {
                    Text("Parse broken sample")
                }

                for (err in brokenErrors) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
