package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.data.HomeConfig
import com.zhehr.auralis.data.HomeDensity
import com.zhehr.auralis.data.HomePresets
import com.zhehr.auralis.data.HomeSection
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.ui.components.SubHeader
import kotlinx.coroutines.launch
import java.util.Collections

@Composable
fun HomeLayoutScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    val cfg by c.settings.homeConfig.collectAsStateWithLifecycle(initialValue = HomePresets.DEFAULT.toConfig())
    fun save(new: HomeConfig) { scope.launch { c.settings.setHomeConfig(new) } }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent, selectedLabelColor = palette.onAccent)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SubHeader("Home layout") { nav.popBackStack() }

        Label("Preset")
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(HomePresets.all, key = { it.id }) { p ->
                FilterChip(selected = cfg.preset == p.id, onClick = { save(p.toConfig()) }, label = { Text(p.title) }, colors = chipColors)
            }
        }

        Label("Card size")
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeDensity.entries.forEach { d ->
                FilterChip(
                    selected = cfg.density == d,
                    onClick = { save(cfg.copy(preset = "custom", density = d)) },
                    label = { Text(d.label) },
                    colors = chipColors,
                )
            }
        }

        Label("Sections (the first one is your top section)")
        cfg.order.forEachIndexed { i, s ->
            SectionRow(
                section = s, on = true, top = i == 0,
                canUp = i > 0, canDown = i < cfg.order.lastIndex,
                onUp = { save(cfg.copy(preset = "custom", order = cfg.order.toMutableList().also { Collections.swap(it, i, i - 1) })) },
                onDown = { save(cfg.copy(preset = "custom", order = cfg.order.toMutableList().also { Collections.swap(it, i, i + 1) })) },
                onToggle = { save(cfg.copy(preset = "custom", order = cfg.order - s)) },
            )
        }
        HomeSection.entries.filter { it !in cfg.order }.forEach { s ->
            SectionRow(
                section = s, on = false, top = false, canUp = false, canDown = false,
                onUp = {}, onDown = {},
                onToggle = { save(cfg.copy(preset = "custom", order = cfg.order + s)) },
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAuralisPalette.current.accent,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun SectionRow(
    section: HomeSection, on: Boolean, top: Boolean, canUp: Boolean, canDown: Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onToggle: () -> Unit,
) {
    val palette = LocalAuralisPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(section.label, style = MaterialTheme.typography.titleMedium, color = if (on) palette.text else palette.mutedText)
            if (top) Text("Top section", style = MaterialTheme.typography.labelSmall, color = palette.accent)
        }
        if (on) {
            IconButton(enabled = canUp, onClick = onUp) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${section.label} up", tint = palette.text) }
            IconButton(enabled = canDown, onClick = onDown) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${section.label} down", tint = palette.text) }
        }
        Switch(checked = on, onCheckedChange = { onToggle() })
    }
}
