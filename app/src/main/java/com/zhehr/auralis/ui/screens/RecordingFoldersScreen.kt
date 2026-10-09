package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.ui.components.SubHeader
import kotlinx.coroutines.launch

@Composable
fun RecordingFoldersScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    val detected by c.repo.recordingFolders.collectAsStateWithLifecycle(initialValue = emptyList())
    val overrides by c.settings.musicFolders.collectAsStateWithLifecycle(initialValue = emptySet())

    Column(Modifier.fillMaxSize()) {
        SubHeader("Recording folders") { nav.popBackStack() }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "AURALIS guesses which audio is a recording from folder and file names. If a folder is really music, " +
                        "mark it here. Nothing is moved or deleted.",
                    style = MaterialTheme.typography.bodyMedium, color = palette.mutedText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Text("DETECTED AS RECORDINGS", style = MaterialTheme.typography.labelSmall, color = palette.accent,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
                if (detected.isEmpty()) {
                    Text("No recording folders detected.", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText,
                        modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
            items(detected, key = { "d-${it.folder}" }) { f ->
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.folder.substringAfterLast('/').ifBlank { f.folder }, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${f.folder} · ${f.songCount} files", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { scope.launch { c.settings.setMusicFolder(f.folder, true); c.scanner.scan(force = true) } }) {
                        Text("Treat as music")
                    }
                }
            }
            if (overrides.isNotEmpty()) {
                item {
                    Text("TREATED AS MUSIC", style = MaterialTheme.typography.labelSmall, color = palette.accent,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp))
                }
                items(overrides.toList().sorted(), key = { "o-$it" }) { folder ->
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(folder, style = MaterialTheme.typography.bodyMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        TextButton(onClick = { scope.launch { c.settings.setMusicFolder(folder, false); c.scanner.scan(force = true) } }) {
                            Text("Detect again")
                        }
                    }
                }
            }
        }
    }
}
