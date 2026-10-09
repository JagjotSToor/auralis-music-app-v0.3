package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.BuildConfig
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.data.HomePresets
import com.zhehr.auralis.theme.Contrast
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.theme.MotionLevel
import com.zhehr.auralis.theme.ThemeMode
import com.zhehr.auralis.theme.paletteFor
import com.zhehr.auralis.ui.components.DevStateTag
import com.zhehr.auralis.ui.components.ScreenHeader
import kotlinx.coroutines.launch
import java.util.Locale

private val ACCENT_OPTIONS: List<Int?> = listOf(
    null, 0xFFFFB454.toInt(), 0xFFFF4D5E.toInt(), 0xFF3D9BFF.toInt(), 0xFFA78BFA.toInt(), 0xFF34D399.toInt(),
    0xFF00E5FF.toInt(), 0xFFFF8AD8.toInt(), 0xFFFFE066.toInt(), 0xFFFF7A3D.toInt(), 0xFF9EF01A.toInt(),
)

@Composable
fun SettingsScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    val themeMode by c.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val accent by c.settings.accent.collectAsStateWithLifecycle(initialValue = null)
    val motion by c.settings.motion.collectAsStateWithLifecycle(initialValue = MotionLevel.BALANCED)
    val texture by c.settings.texture.collectAsStateWithLifecycle(initialValue = false)
    val incRec by c.settings.includeRecordings.collectAsStateWithLifecycle(initialValue = false)
    val incCall by c.settings.includeCall.collectAsStateWithLifecycle(initialValue = false)
    val cfg by c.settings.homeConfig.collectAsStateWithLifecycle(initialValue = HomePresets.DEFAULT.toConfig())
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    val count by c.repo.count.collectAsStateWithLifecycle(initialValue = 0)
    val hidden by c.repo.hiddenSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    var hex by rememberSaveable { mutableStateOf("") }
    val textRatio = Contrast.ratio(palette.text, palette.background)
    val mutedRatio = Contrast.ratio(palette.mutedText, palette.background)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Settings")

        // ---------------- Appearance ----------------
        Section("Appearance")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(ThemeMode.entries.toList(), key = { it.name }) { m ->
                ThemeChip(m, m == themeMode) { scope.launch { c.settings.setThemeMode(m) } }
            }
        }
        Caption("Accent colour")
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ACCENT_OPTIONS) { argb ->
                AccentDot(argb, selected = argb == accent) { scope.launch { c.settings.setAccent(argb) } }
            }
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { v ->
                hex = v
                if (v.length == 7 && v.startsWith("#")) {
                    runCatching { android.graphics.Color.parseColor(v) }.getOrNull()?.let { argb ->
                        scope.launch { c.settings.setAccent(argb) }
                    }
                }
            },
            singleLine = true,
            label = { Text("Custom accent, e.g. #FF8800") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Note("Accents are automatically adjusted so text stays readable. Contrast now: text ${fmt(textRatio)}:1, muted ${fmt(mutedRatio)}:1 " +
            if (Contrast.meetsAA(palette.mutedText, palette.background)) "(passes AA)" else "(fails AA)")

        Caption("Motion")
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionLevel.entries.forEach { m ->
                FilterChip(
                    selected = m == motion,
                    onClick = { scope.launch { c.settings.setMotion(m) } },
                    label = { Text(m.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent, selectedLabelColor = palette.onAccent),
                )
            }
        }
        Note(motion.blurb)

        SettingRow(
            "Film grain texture", "A subtle grain over the whole app.",
            onClick = { scope.launch { c.settings.setTexture(!texture) } },
        ) { Switch(checked = texture, onCheckedChange = { scope.launch { c.settings.setTexture(it) } }) }
        SettingRow("Wallpaper and background blur", "Custom wallpaper from your gallery.") { DevStateTag("Next update") }

        // ---------------- Home ----------------
        Section("Home")
        SettingRow(
            "Home layout", "${HomePresets.titleFor(cfg.preset)} · ${cfg.order.size} sections · ${cfg.density.label}",
            onClick = { nav.navigate("homelayout") },
        ) { Text("›", color = palette.mutedText, style = MaterialTheme.typography.titleMedium) }

        // ---------------- Playback ----------------
        Section("Playback")
        SettingRow("Equalizer and audio effects", "Bands, presets, bass boost, virtualizer.") { DevStateTag("Next update") }
        SettingRow("Resume", "Your queue, position and speed are restored automatically.")

        // ---------------- Library ----------------
        Section("Library")
        Note("$count songs in your music library")
        Note(scan.message.ifBlank { "Not scanned yet in this session." })
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Button(enabled = !scan.running, onClick = { scope.launch { c.scanner.scan(force = true) } }) {
                Text(if (scan.running) "Scanning…" else "Rescan")
            }
        }
        SettingRow(
            "Voice and screen recordings in Music library", "Off keeps them in the Recordings tab only.",
            onClick = { scope.launch { c.settings.setIncludeRecordings(!incRec) } },
        ) { Switch(checked = incRec, onCheckedChange = { scope.launch { c.settings.setIncludeRecordings(it) } }) }
        SettingRow(
            "Call recordings in Music library", "Off by default. They stay playable in Recordings.",
            onClick = { scope.launch { c.settings.setIncludeCall(!incCall) } },
        ) { Switch(checked = incCall, onCheckedChange = { scope.launch { c.settings.setIncludeCall(it) } }) }
        SettingRow("Recording folders", "See and correct what was detected.", onClick = { nav.navigate("recfolders") }) {
            Text("›", color = palette.mutedText, style = MaterialTheme.typography.titleMedium)
        }
        SettingRow("Hidden songs", "${hidden.size} hidden. Restore them here.", onClick = { nav.navigate("hidden") }) {
            Text("›", color = palette.mutedText, style = MaterialTheme.typography.titleMedium)
        }

        // ---------------- Lyrics ----------------
        Section("Lyrics")
        SettingRow("Lyrics", "Embedded and .lrc lyrics, synced display, manual import.") { DevStateTag("Next update") }

        // ---------------- Privacy ----------------
        Section("Privacy")
        Note("AURALIS has no internet permission and no microphone permission. Your library, playlists and settings are stored only on this phone.")

        // ---------------- About ----------------
        Section("About")
        Text("AURALIS", style = MaterialTheme.typography.titleLarge, color = palette.text, modifier = Modifier.padding(horizontal = 20.dp))
        Note("by ZHEHR STUDIO")
        Note("Owner: Jagjot Singh")
        Note("Version ${BuildConfig.VERSION_NAME}")
        Note("Built with Kotlin, Jetpack Compose, Media3 and Room.")
        Box(Modifier.padding(bottom = 24.dp))
    }
}

private fun fmt(v: Float) = String.format(Locale.US, "%.1f", v)

@Composable
private fun Section(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAuralisPalette.current.accent,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
    )
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = LocalAuralisPalette.current.text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalAuralisPalette.current.mutedText,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val palette = LocalAuralisPalette.current
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
        }
        Box(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun ThemeChip(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    val current = LocalAuralisPalette.current
    val p = paletteFor(mode, isSystemInDarkTheme())
    Column(
        Modifier.width(76.dp).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(p.background)
                .border(if (selected) 3.dp else 1.dp, if (selected) current.accent else current.mutedText.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(20.dp).clip(CircleShape).background(p.accent)) }
        Text(
            mode.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) current.accent else current.mutedText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AccentDot(argb: Int?, selected: Boolean, onClick: () -> Unit) {
    val current = LocalAuralisPalette.current
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .background(if (argb == null) current.surfaceHigh else Color(argb))
            .border(if (selected) 3.dp else 1.dp, if (selected) current.text else current.mutedText.copy(alpha = 0.4f), CircleShape)
            .semantics { contentDescription = if (argb == null) "Theme default accent" else "Accent colour option" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { if (argb == null) Text("A", style = MaterialTheme.typography.labelLarge, color = current.text) }
}
