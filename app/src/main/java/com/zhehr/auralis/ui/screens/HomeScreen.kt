package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.R
import com.zhehr.auralis.data.HomeDensity
import com.zhehr.auralis.data.HomePresets
import com.zhehr.auralis.data.HomeSection
import com.zhehr.auralis.data.SongEntity
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.ui.components.ArtworkImage
import com.zhehr.auralis.ui.components.EmptyState
import com.zhehr.auralis.ui.components.SongRow
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

@Composable
fun HomeScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val hello = remember { greeting() }
    val total by c.repo.count.collectAsStateWithLifecycle(initialValue = 0)
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    val config by c.settings.homeConfig.collectAsStateWithLifecycle(initialValue = HomePresets.DEFAULT.toConfig())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
            // Brand hierarchy: AURALIS is the product; ZHEHR STUDIO stays small and secondary.
            Text("AURALIS", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Text("by ZHEHR STUDIO", style = MaterialTheme.typography.labelSmall, color = palette.mutedText)
            Text(hello, style = MaterialTheme.typography.headlineMedium, color = palette.text, modifier = Modifier.padding(top = 20.dp))
        }
        if (total == 0) {
            EmptyState(
                title = if (scan.running) "Scanning your music…" else "No music found yet",
                body = if (scan.running) "Your library appears as soon as it is ready."
                else "Copy some audio files to this phone, then open Settings and tap Rescan.",
                modifier = Modifier.padding(top = 48.dp),
            )
        } else {
            // Only the sections the user keeps are composed, so hidden sections cost nothing.
            config.order.forEach { sec -> key(sec) { HomeSectionView(sec, config.density, nav) } }
        }
    }
}

@Composable
private fun HomeSectionView(sec: HomeSection, density: HomeDensity, nav: NavHostController) {
    val repo = LocalContainer.current.repo
    when (sec) {
        HomeSection.RECENTLY_PLAYED -> SongShelf("Recently played", repo.recentlyPlayed, density)
        HomeSection.RECENTLY_ADDED -> SongShelf("Recently added", repo.recentlyAdded, density)
        HomeSection.MOST_PLAYED -> SongShelf("Most played", repo.mostPlayed, density)
        HomeSection.FAVORITES -> SongShelf("Favorites", repo.favorites, density) { nav.navigate("detail/favorites/0") }
        HomeSection.PLAYLISTS -> PlaylistShelf(density, nav)
        HomeSection.ALBUMS -> AlbumShelf(density, nav)
        HomeSection.ALL_SONGS -> AllSongsPreview(nav)
    }
}

@Composable
private fun ShelfHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = LocalAuralisPalette.current.text, modifier = Modifier.weight(1f))
        if (onSeeAll != null) TextButton(onClick = onSeeAll) { Text("See all") }
    }
}

@Composable
private fun SongShelf(title: String, flow: Flow<List<SongEntity>>, density: HomeDensity, onSeeAll: (() -> Unit)? = null) {
    val songs by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    if (songs.isEmpty()) return
    val player = LocalContainer.current.player
    val palette = LocalAuralisPalette.current
    val size = density.card.dp
    Column(Modifier.padding(bottom = 20.dp)) {
        ShelfHeader(title, onSeeAll)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
                Column(Modifier.width(size).clickable { player.playSongs(songs, i) }) {
                    ArtworkImage(s.id, s.albumId, Modifier.size(size), px = 320, shape = RoundedCornerShape(16.dp))
                    Text(s.title, style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(s.artist, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PlaylistShelf(density: HomeDensity, nav: NavHostController) {
    val playlists by LocalContainer.current.repo.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    if (playlists.isEmpty()) return
    val palette = LocalAuralisPalette.current
    val size = density.card.dp
    Column(Modifier.padding(bottom = 20.dp)) {
        ShelfHeader("Playlists", null)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(playlists, key = { it.id }) { p ->
                Column(Modifier.width(size).clickable { nav.navigate("detail/playlist/${p.id}") }) {
                    Box(
                        Modifier.size(size).clip(RoundedCornerShape(16.dp)).background(palette.surfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(R.drawable.ic_queue), contentDescription = null, tint = palette.accent, modifier = Modifier.size(size / 3)) }
                    Text(p.name, style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text("${p.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
                }
            }
        }
    }
}

@Composable
private fun AlbumShelf(density: HomeDensity, nav: NavHostController) {
    val albums by LocalContainer.current.repo.albums.collectAsStateWithLifecycle(initialValue = emptyList())
    if (albums.isEmpty()) return
    val palette = LocalAuralisPalette.current
    val size = density.card.dp
    val shown = remember(albums) { albums.take(20) }
    Column(Modifier.padding(bottom = 20.dp)) {
        ShelfHeader("Albums", null)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(shown, key = { "${it.albumId}-${it.album}" }) { a ->
                Column(Modifier.width(size).clickable { nav.navigate("detail/album/${a.albumId}") }) {
                    ArtworkImage(a.songId, a.albumId, Modifier.size(size), px = 320, shape = RoundedCornerShape(16.dp))
                    Text(a.album, style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(a.artist, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AllSongsPreview(nav: NavHostController) {
    val c = LocalContainer.current
    val songs by c.repo.allSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val favIds by c.repo.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val shown = remember(songs) { songs.take(25) }
    if (shown.isEmpty()) return
    Column(Modifier.padding(bottom = 12.dp)) {
        ShelfHeader("All songs", onSeeAll = { nav.navigate("library") })
        shown.forEachIndexed { i, s ->
            key(s.id) { SongRow(s, isFavorite = s.id in favIds, onClick = { c.player.playSongs(songs, i) }) }
        }
    }
}
