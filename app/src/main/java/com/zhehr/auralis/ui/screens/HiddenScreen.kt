package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.ui.components.ArtworkImage
import com.zhehr.auralis.ui.components.EmptyState
import com.zhehr.auralis.ui.components.SubHeader
import kotlinx.coroutines.launch

/** Hidden songs: muted, grayscale and (on Android 12+) softly blurred. Nothing here can play by accident. */
@Composable
fun HiddenScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    val hidden by c.repo.hiddenSongs.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize()) {
        SubHeader("Hidden songs") { nav.popBackStack() }
        if (hidden.isEmpty()) {
            EmptyState("No hidden songs", "Songs you hide stay on your phone. They just disappear from your library, search and shuffle.")
        } else {
            Text(
                "These songs are kept out of your library, search and shuffle. Nothing is deleted.",
                style = MaterialTheme.typography.bodyMedium, color = palette.mutedText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(hidden, key = { it.id }) { s ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkImage(s.id, s.albumId, Modifier.size(48.dp).blur(3.dp), px = 144, grayscale = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { scope.launch { c.repo.unhideSong(s.id) } }) { Text("Unhide") }
                    }
                }
            }
        }
    }
}
