package io.github.shuixingqianfeng.morireader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun LiquidGlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(0.dp),
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val tint = if (strong) Color(0xE6F8FBFF) else Color(0xCFFBFDFF)
    val style = HazeStyle(
        backgroundColor = Color(0xFFF5F8FC),
        tint = HazeTint(tint),
        blurRadius = if (strong) 30.dp else 22.dp,
        noiseFactor = 0.018f,
        fallbackTint = HazeTint(tint),
    )
    Box(
        modifier = modifier
            .shadow(18.dp, shape, ambientColor = Color(0x1A557A9E), spotColor = Color(0x24506E8A))
            .clip(shape)
            .hazeEffect(state = hazeState, style = style)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color(0xFFE7F1FB).copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.16f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.88f)), shape)
            .padding(padding),
        content = content,
    )
}

val GlassCardShape = RoundedCornerShape(28.dp)
val GlassButtonShape = RoundedCornerShape(50)
