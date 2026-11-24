package com.soundryn.music.devconsole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundryn.music.core.domain.LibraryRepository
import com.soundryn.music.core.domain.PlaylistRepository
import com.soundryn.music.core.domain.SettingsRepository
import com.soundryn.music.core.domain.SourceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class DevConsolePanel(
    val title: String,
    val content: @Composable () -> Unit,
)

@AndroidEntryPoint
class DevConsoleActivity : ComponentActivity() {
    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var playlists: PlaylistRepository
    @Inject lateinit var sources: SourceRepository
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val info = DeviceInfo.from(this)
        val lines = deviceInfoLines(info)
        val injectedCount = listOf(
            ::library.isInitialized,
            ::playlists.isInitialized,
            ::sources.isInitialized
        ).count { it }
        val panels = listOf(domainPanel, dataCorePanel(injectedCount), settingsPanel(settings), designPanel())
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevConsoleScreen(lines = lines, panels = panels)
                }
            }
        }
    }
}

@Composable
fun DevConsoleScreen(
    lines: List<String>,
    panels: List<DevConsolePanel> = listOf(domainPanel),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Soundryn Dev Console",
            style = MaterialTheme.typography.headlineMedium
        )
        for (line in lines) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (panels.isEmpty()) {
            Text(
                text = "No panels yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            for (panel in panels) {
                Text(
                    text = panel.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                panel.content()
            }
        }
    }
}
