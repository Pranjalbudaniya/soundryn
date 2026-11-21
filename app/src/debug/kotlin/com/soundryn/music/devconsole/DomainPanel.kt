package com.soundryn.music.devconsole

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soundryn.music.R
import com.soundryn.music.core.domain.DomainError
import com.soundryn.music.core.domain.Outcome
import com.soundryn.music.core.domain.PageRequest
import com.soundryn.music.core.domain.TrackKey
import com.soundryn.music.core.domain.formatDurationMs
import com.soundryn.music.core.domain.sortKeyOf

val domainPanel = DevConsolePanel(
    title = "Domain core"
) {
    DomainPanelContent()
}

@Composable
fun DomainPanelContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lines = remember { computeDomainPanelLines(context) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (line in lines) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun computeDomainPanelLines(context: Context): List<String> {
    return listOf(
        safeCompute {
            val keyOutcome = TrackKey.of("local", "a/b%c")
            val encoded = (keyOutcome as? Outcome.Success)?.value?.encode() ?: "error"
            val roundTripOk = when (val parsed = TrackKey.parse(encoded)) {
                is Outcome.Success -> parsed.value.itemId == "a/b%c"
                is Outcome.Failure -> false
            }
            val status = if (roundTripOk) "round trip OK" else "round trip FAILED"
            context.getString(R.string.dev_domain_track_key, encoded, status)
        },
        safeCompute {
            val result = sortKeyOf("The Beatles")
            context.getString(R.string.dev_domain_sort_key, result)
        },
        safeCompute {
            val result = formatDurationMs(3723000L) ?: "null"
            context.getString(R.string.dev_domain_duration, result)
        },
        safeCompute {
            val mapped = Outcome.Success(2).map { it * 2 }
            val failure = (Outcome.Failure(DomainError.NotFound) as Outcome<Int>).map { it * 2 }
            val kept = failure is Outcome.Failure && failure.error is DomainError.NotFound
            val valueStr = (mapped as? Outcome.Success)?.value?.toString() ?: "error"
            val keptStr = if (kept) "yes" else "no"
            context.getString(R.string.dev_domain_outcome, valueStr, keptStr)
        },
        safeCompute {
            val limit0 = PageRequest.of(null, 0).limit
            val limit999 = PageRequest.of(null, 999).limit
            context.getString(R.string.dev_domain_page_limit, limit0, limit999)
        }
    )
}

private inline fun safeCompute(block: () -> String): String {
    return try {
        block()
    } catch (e: Throwable) {
        "ERROR: ${e::class.java.simpleName}"
    }
}
