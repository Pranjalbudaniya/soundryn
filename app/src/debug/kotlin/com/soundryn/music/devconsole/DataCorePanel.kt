package com.soundryn.music.devconsole

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soundryn.music.R

fun dataCorePanel(injectedCount: Int) = DevConsolePanel(
    title = "Data core"
) {
    DataCorePanelContent(injectedCount = injectedCount)
}

@Composable
fun DataCorePanelContent(injectedCount: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var results by remember { mutableStateOf<List<SelfTestResult>?>(null) }

    LaunchedEffect(Unit) {
        results = runDataSelfTest(context)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val currentResults = results
        if (currentResults == null) {
            Text(
                text = stringResource(R.string.dev_data_running),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = stringResource(R.string.dev_data_hilt_injected, injectedCount),
                style = MaterialTheme.typography.bodyMedium
            )
            val dbExists = context.getDatabasePath("soundryn.db").exists()
            val dbExistsStr = if (dbExists) "yes" else "no"
            Text(
                text = stringResource(R.string.dev_data_real_db, dbExistsStr),
                style = MaterialTheme.typography.bodyMedium
            )
            val cascade = currentResults.find { it.name == "cascade" }
            val cascadeStr = if (cascade?.ok == true) "OK" else "FAILED ${cascade?.detail.orEmpty()}".trim()
            Text(
                text = stringResource(R.string.dev_data_self_test_cascade, cascadeStr),
                style = MaterialTheme.typography.bodyMedium
            )
            val plCascade = currentResults.find { it.name == "playlist cascade" }
            val plCascadeStr = if (plCascade?.ok == true) "OK" else "FAILED ${plCascade?.detail.orEmpty()}".trim()
            Text(
                text = stringResource(R.string.dev_data_self_test_playlist_cascade, plCascadeStr),
                style = MaterialTheme.typography.bodyMedium
            )
            val rollback = currentResults.find { it.name == "rollback" }
            val rollbackStr = if (rollback?.ok == true) "OK" else "FAILED ${rollback?.detail.orEmpty()}".trim()
            Text(
                text = stringResource(R.string.dev_data_self_test_rollback, rollbackStr),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
