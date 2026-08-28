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
    content: @Composable BoxScope.() -> Unit,
) {
    // This deterministic glass stack avoids the RenderEffect transparency bug
    // seen on some Android 12/13 OEM GPUs while preserving a liquid, layered
    // refraction language through directional highlights and double borders.
    // Glass has no fixed colour of its own. The translucent white body lets the
    // ambient blue/purple background show through; the edges provide the
    // brighter refracted rim seen in real clear glass.
    val fill = if (strong) {
        listOf(
            Color.White.copy(alpha = 0.62f),
            Color(0xFFD9F1FF).copy(alpha = 0.34f),
            Color.White.copy(alpha = 0.48f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.48f),
            Color(0xFFD8F0FF).copy(alpha = 0.24f),
            Color.White.copy(alpha = 0.38f),
        )
    }
    val outerBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.98f),
            Color.White.copy(alpha = 0.52f),
            Color(0xFF8CB7D4).copy(alpha = 0.34f),
            Color.White.copy(alpha = 0.80f),
        ),
    )
    val topRefraction = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (strong) 0.72f else 0.58f),
            Color.Transparent,
            Color(0xFF90D2F4).copy(alpha = 0.10f),
        ),
    )
    val lowerRefraction = Brush.verticalGradient(
        listOf(
            Color.Transparent,
            Color.Transparent,
            Color(0xFF86BADA).copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.22f),
        ),
    )
    val innerBorder = Color.White.copy(alpha = if (strong) 0.58f else 0.45f)
    val ambient = Color(0x20557A9E)
    val spot = Color(0x2B506E8A)

    Box(
        modifier = modifier
            .shadow(if (strong) 24.dp else 17.dp, shape, ambientColor = ambient, spotColor = spot)
            .clip(shape)
            .background(Brush.linearGradient(fill))
            .border(BorderStroke(1.dp, outerBorder), shape),
    ) {
        Box(Modifier.matchParentSize().background(topRefraction))
        Box(Modifier.matchParentSize().background(lowerRefraction))
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
