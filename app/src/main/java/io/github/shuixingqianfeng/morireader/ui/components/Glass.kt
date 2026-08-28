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

@Suppress("UNUSED_PARAMETER")
@Composable
fun LiquidGlassSurface(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(0.dp),
    strong: Boolean = false,
    dark: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // This deterministic glass stack avoids the RenderEffect transparency bug
    // seen on some Android 12/13 OEM GPUs while preserving a liquid, layered
    // refraction language through directional highlights and double borders.
    val fill = when {
        dark && strong -> listOf(Color(0xF03F454C), Color(0xF02A3036), Color(0xF0363C43))
        dark -> listOf(Color(0xEA4A5057), Color(0xEA30363C), Color(0xEA3C4249))
        strong -> listOf(Color(0xF9FCFEFF), Color(0xF3E9F4FC), Color(0xF8F8FCFF))
        else -> listOf(Color(0xF5FCFEFF), Color(0xEDECF6FC), Color(0xF4F8FCFF))
    }
    val outerBorder = if (dark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.64f), Color(0xFF94ABC0).copy(alpha = 0.22f), Color.Black.copy(alpha = 0.36f)))
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.62f), Color(0xFF9BB9D0).copy(alpha = 0.38f)))
    }
    val highlight = if (dark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Transparent, Color.White.copy(alpha = 0.07f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.74f), Color.Transparent, Color(0xFF9ED3F2).copy(alpha = 0.12f)))
    }
    val innerBorder = if (dark) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.72f)
    val ambient = if (dark) Color(0x55212B34) else Color(0x24557A9E)
    val spot = if (dark) Color(0x66303A44) else Color(0x30506E8A)

    Box(
        modifier = modifier
            .shadow(if (strong) 24.dp else 17.dp, shape, ambientColor = ambient, spotColor = spot)
            .clip(shape)
            .background(Brush.linearGradient(fill))
            .border(BorderStroke(1.dp, outerBorder), shape),
    ) {
        Box(Modifier.matchParentSize().background(highlight))
        Box(
            Modifier
                .matchParentSize()
                .padding(1.5.dp)
                .border(BorderStroke(0.7.dp, innerBorder), shape),
        )
        Box(Modifier.padding(padding), content = content)
    }
}

val GlassCardShape = RoundedCornerShape(28.dp)
val GlassButtonShape = RoundedCornerShape(50)
