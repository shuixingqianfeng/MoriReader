package io.github.shuixingqianfeng.morireader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MoriBlue = Color(0xFF4B7FA8)
val MoriBlueSoft = Color(0xFFDCEBFA)
val MoriBackground = Color(0xFFF7F8FB)
val MoriText = Color(0xFF17202A)
val MoriMuted = Color(0xFF6F7882)

private val MoriColors = lightColorScheme(
    primary = MoriBlue,
    onPrimary = Color.White,
    primaryContainer = MoriBlueSoft,
    onPrimaryContainer = MoriText,
    background = MoriBackground,
    onBackground = MoriText,
    surface = Color.White,
    onSurface = MoriText,
    surfaceVariant = Color(0xFFF0F3F7),
    onSurfaceVariant = MoriMuted,
)

@Composable
fun MoriReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MoriColors, content = content)
}
