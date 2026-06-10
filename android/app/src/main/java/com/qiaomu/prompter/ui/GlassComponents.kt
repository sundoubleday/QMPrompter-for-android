package com.qiaomu.prompter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

object GlassShapes {
    val card = RoundedCornerShape(20.dp)
    val panel = RoundedCornerShape(28.dp)
    val capsule = RoundedCornerShape(50)
    val button = RoundedCornerShape(16.dp)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = GlassShapes.card
    Box(
        modifier = modifier
            .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.055f), spotColor = Color.Black.copy(alpha = 0.055f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.38f))
                )
            )
            .border(
                0.55.dp,
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.035f))
                ),
                shape
            )
            .padding(horizontal = 17.dp, vertical = 16.dp)
    ) { content() }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = GlassShapes.panel
    Box(
        modifier = modifier
            .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.30f))
            .border(
                0.8.dp,
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                ),
                shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
fun GlassCapsuleButton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = GlassShapes.capsule
    Box(
        modifier = modifier
            .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.48f))
            .border(0.7.dp, Color.White.copy(alpha = 0.18f), shape)
    ) { content() }
}

@Composable
fun GlassCircleButton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .shadow(16.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.48f))
            .border(0.7.dp, Color.White.copy(alpha = 0.20f), CircleShape)
    ) { content() }
}
