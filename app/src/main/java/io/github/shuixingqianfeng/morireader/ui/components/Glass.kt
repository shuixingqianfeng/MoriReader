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

@Composable
fun LiquidGlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(0.dp),
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // Haze's RenderEffect capture renders the complete Compose source layer
    // transparent on some OEM Android 12/13 GPU drivers. Keep the glass
    // language deterministic with an opaque translucent-looking surface so
    // the app shell never disappears on those devices.
    val top = if (strong) Color(0xFFF9FCFF) else Color(0xFFF7FBFF)
    val middle = if (strong) Color(0xFFF0F7FD) else Color(0xFFF2F8FD)
    val bottom = if (strong) Color(0xFFF7FAFD) else Color(0xFFF5F9FC)
    Box(
        modifier = modifier
            .shadow(18.dp, shape, ambientColor = Color(0x1A557A9E), spotColor = Color(0x24506E8A))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(top, middle, bottom),
                ),
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.88f)), shape)
            .padding(padding),
        content = content,
    )
}

val GlassCardShape = RoundedCornerShape(28.dp)
val GlassButtonShape = RoundedCornerShape(50)
