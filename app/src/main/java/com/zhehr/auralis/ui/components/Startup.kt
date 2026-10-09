package com.zhehr.auralis.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zhehr.auralis.R
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.theme.MotionLevel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs on top of the real UI, which is already composing underneath, so it never delays startup.
 * Balanced is about 0.9 s, Cinematic about 1.4 s, Minimal skips it.
 */
@Composable
fun StartupOverlay(motion: MotionLevel, onDone: () -> Unit) {
    val palette = LocalAuralisPalette.current
    val appear = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (motion == MotionLevel.MINIMAL) {
            onDone()
            return@LaunchedEffect
        }
        val k = if (motion == MotionLevel.CINEMATIC) 1.5f else 1f
        coroutineScope {
            launch { appear.animateTo(1f, tween((320 * k).toInt())) }
            launch { glow.animateTo(1f, tween((520 * k).toInt())) }
        }
        delay((160 * k).toLong())
        out.animateTo(0f, tween((220 * k).toInt()))
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = out.value }
            .background(palette.background)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(340.dp).background(
                Brush.radialGradient(listOf(palette.accent.copy(alpha = 0.30f * glow.value), Color.Transparent)),
                CircleShape,
            )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.accent),
                modifier = Modifier.size(132.dp).graphicsLayer {
                    val s = 1.25f - 0.25f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "AURALIS",
                style = MaterialTheme.typography.titleLarge,
                color = palette.text,
                modifier = Modifier.graphicsLayer { alpha = appear.value },
            )
            Text(
                "ZHEHR STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = palette.mutedText,
                modifier = Modifier.graphicsLayer { alpha = ((appear.value - 0.4f) / 0.6f).coerceIn(0f, 1f) },
            )
        }
    }
}

/** Optional film grain: one small tiled bitmap, drawn once per frame as a single rect. Touch-transparent. */
@Composable
fun GrainOverlay() {
    val brush = remember {
        val n = 128
        val rnd = java.util.Random(7)
        val px = IntArray(n * n) {
            val g = 100 + rnd.nextInt(156)
            val a = 40 + rnd.nextInt(60)
            (a shl 24) or (g shl 16) or (g shl 8) or g
        }
        val bmp = Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    Box(Modifier.fillMaxSize().drawBehind { drawRect(brush, alpha = 0.06f) })
}
