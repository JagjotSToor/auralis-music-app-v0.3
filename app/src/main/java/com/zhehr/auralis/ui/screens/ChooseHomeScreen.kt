package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.data.HomePresets
import com.zhehr.auralis.theme.LocalAuralisPalette
import kotlinx.coroutines.launch

@Composable
fun ChooseHomeScreen() {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(HomePresets.DEFAULT.id) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Choose your Home", style = MaterialTheme.typography.headlineMedium, color = palette.text)
        Text(
            "Pick a starting layout. You can change it any time in Settings.",
            style = MaterialTheme.typography.bodyMedium, color = palette.mutedText,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(HomePresets.all, key = { it.id }) { p ->
                val sel = p.id == selected
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape)
                        .background(if (sel) palette.surfaceHigh else palette.surface)
                        .border(if (sel) 2.dp else 1.dp, if (sel) palette.accent else palette.mutedText.copy(alpha = 0.3f), shape)
                        .clickable { selected = p.id }
                        .padding(16.dp),
                ) {
                    Text(p.title, style = MaterialTheme.typography.titleMedium, color = if (sel) palette.accent else palette.text)
                    Text(p.blurb, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
                    Text(
                        p.order.joinToString(" · ") { it.label },
                        style = MaterialTheme.typography.labelSmall, color = palette.mutedText,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { scope.launch { c.settings.setHomeConfig(HomePresets.get(selected).toConfig()) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}
