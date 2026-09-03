package io.github.shuixingqianfeng.morireader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.roundToInt

enum class MainTab(val label: String, val icon: ImageVector) {
    LIBRARY("书库", Icons.Outlined.LibraryBooks),
    TAGS("标签", Icons.Outlined.Label),
    STATS("统计", Icons.Outlined.BarChart),
    SETTINGS("设置", Icons.Outlined.Settings),
    SEARCH("搜索", Icons.Outlined.Search),
}

/**
 * Moving liquid-glass navigation lens based on the public Coolapk interaction
 * reference. It avoids RenderEffect because that API produced white/transparent
 * app layers on several Android 12 OEM GPUs.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun GlassBottomNavigation(
    hazeState: HazeState,
    selectedTab: MainTab,
    onSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { MainTab.entries }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val itemWidthPx = totalWidthPx / tabs.size
        val restingLensPx = itemWidthPx * 1.02f
        val lensHeightPx = with(density) { 60.dp.toPx() }
        fun center(index: Int) = itemWidthPx * (index + 0.5f)
        fun nearest(x: Float) = tabs.indices.minBy { abs(center(it) - x) }

        val lensCenter = remember(totalWidthPx) { Animatable(center(selectedIndex)) }
        var isDragging by remember { mutableStateOf(false) }
        var dragCenter by remember { mutableFloatStateOf(center(selectedIndex)) }
        var releaseFrom by remember { mutableFloatStateOf(Float.NaN) }
        var dragStretch by remember { mutableFloatStateOf(0f) }
        var releaseVelocity by remember { mutableFloatStateOf(0f) }
        var settleGeneration by remember { mutableIntStateOf(0) }

        LaunchedEffect(selectedIndex, totalWidthPx, isDragging, settleGeneration) {
            if (!isDragging) {
                if (!releaseFrom.isNaN()) {
                    lensCenter.snapTo(releaseFrom)
                    releaseFrom = Float.NaN
                }
                lensCenter.animateTo(
                    targetValue = center(selectedIndex),
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 510f),
                    initialVelocity = releaseVelocity,
                )
                releaseVelocity = 0f
            }
        }

        val smoothStretch by animateFloatAsState(
            targetValue = if (isDragging) dragStretch else 0f,
            animationSpec = spring(dampingRatio = 0.76f, stiffness = 680f),
            label = "liquid-lens-stretch",
        )
        val lensWidthPx = (restingLensPx * (1f + smoothStretch * 0.72f))
            .coerceAtMost(itemWidthPx * 1.76f)
        val lensWidth = with(density) { lensWidthPx.toDp() }
        val displayedCenter = if (isDragging) dragCenter else lensCenter.value
        val highlightedIndex = nearest(displayedCenter)
        val navShape = RoundedCornerShape(38.dp)
        val navHeightPx = constraints.maxHeight.toFloat()

        Box(
            Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 22.dp,
                    shape = navShape,
                    ambientColor = Color(0x2851697A),
                    spotColor = Color(0x3651697A),
                )
                .clip(navShape)
                .testTag("bottom_navigation")
                .pointerInput(totalWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tracker = VelocityTracker()
                        var lastX = down.position.x
                        isDragging = true
                        dragCenter = down.position.x.coerceIn(restingLensPx / 2f, totalWidthPx - restingLensPx / 2f)
                        releaseVelocity = 0f
                        tracker.addPosition(down.uptimeMillis, down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val x = change.position.x.coerceIn(restingLensPx / 2f, totalWidthPx - restingLensPx / 2f)
                            tracker.addPosition(change.uptimeMillis, change.position)
                            val delta = x - lastX
                            lastX = x
                            dragStretch = (abs(delta) / (itemWidthPx * 0.22f)).coerceIn(0f, 1f)
                            dragCenter = x
                            if (!change.pressed) {
                                val velocity = tracker.calculateVelocity().x
                                val projected = (x + velocity * 0.045f).coerceIn(0f, totalWidthPx)
                                val target = nearest(projected)
                                releaseVelocity = velocity.coerceIn(-4_800f, 4_800f)
                                releaseFrom = x
                                isDragging = false
                                dragStretch = 0f
                                onSelected(tabs[target])
                                settleGeneration += 1
                                break
                            }
                            change.consume()
                        }
                        if (isDragging) {
                            isDragging = false
                            dragStretch = 0f
                            settleGeneration += 1
                        }
                    }
                },
        ) {
            NavigationGlassBase()

            MovingGlassLens(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (displayedCenter - lensWidthPx / 2f).roundToInt(),
                            y = ((navHeightPx - lensHeightPx) / 2f).roundToInt(),
                        )
                    }
                    .size(width = lensWidth, height = with(density) { lensHeightPx.toDp() }),
                motion = smoothStretch,
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("tab_${tab.name}")
                            .semantics {
                                role = Role.Tab
                                selected = selectedTab == tab
                                onClick { onSelected(tab); true }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        NavigationItemContent(tab, highlightedIndex == index)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationGlassBase() {
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.height / 2f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.82f),
                    Color(0xFFEFF7FA).copy(alpha = 0.66f),
                    Color.White.copy(alpha = 0.72f),
                ),
            ),
            cornerRadius = CornerRadius(radius),
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFFB8E9F4).copy(alpha = 0.22f),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFFA5D7E6).copy(alpha = 0.16f),
                ),
            ),
            cornerRadius = CornerRadius(radius),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.94f),
            cornerRadius = CornerRadius(radius),
            style = Stroke(1.1.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFF45616B).copy(alpha = 0.13f),
            topLeft = Offset(1.4.dp.toPx(), 1.4.dp.toPx()),
            size = Size(size.width - 2.8.dp.toPx(), size.height - 2.8.dp.toPx()),
            cornerRadius = CornerRadius(radius),
            style = Stroke(0.7.dp.toPx()),
        )
    }
}

@Composable
private fun MovingGlassLens(modifier: Modifier, motion: Float) {
    val shape = RoundedCornerShape(34.dp)
    Box(
        modifier
            .shadow(
                elevation = (8 + motion * 5).dp,
                shape = shape,
                ambientColor = Color(0x36547D91),
                spotColor = Color(0x42547D91),
            )
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color(0xFFBCECF5).copy(alpha = 0.74f),
                        Color(0xFF557A8A).copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.90f),
                    ),
                ),
                shape = shape,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.height / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF82E1E3).copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.52f),
                        Color(0xFFEAF9FB).copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.48f),
                        Color(0xFF72CFD8).copy(alpha = 0.26f),
                    ),
                ),
                cornerRadius = CornerRadius(radius),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.62f),
                        Color.Transparent,
                        Color(0xFF4E8195).copy(alpha = 0.13f),
                        Color.White.copy(alpha = 0.23f),
                    ),
                ),
                cornerRadius = CornerRadius(radius),
            )
            val bloom = size.height * 0.54f
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF70DADE).copy(alpha = 0.31f), Color.Transparent)),
                radius = bloom,
                center = Offset(size.height * 0.28f, size.height * 0.58f),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.48f), Color.Transparent)),
                radius = bloom,
                center = Offset(size.width - size.height * 0.22f, size.height * 0.30f),
            )
            drawRoundRect(
                color = Color(0xFF395A68).copy(alpha = 0.18f),
                topLeft = Offset(1.6.dp.toPx(), 1.6.dp.toPx()),
                size = Size(size.width - 3.2.dp.toPx(), size.height - 3.2.dp.toPx()),
                cornerRadius = CornerRadius(radius),
                style = Stroke(0.8.dp.toPx()),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.96f),
                startAngle = 196f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(2.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 4.dp.toPx(), size.height - 2.dp.toPx()),
                style = Stroke(1.25.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun NavigationItemContent(tab: MainTab, selected: Boolean) {
    val color by animateColorAsState(
        if (selected) Color(0xFF168B91) else Color(0xFF20282D),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "nav-color",
    )
    val scale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 540f),
        label = "nav-scale",
    )
    Column(
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(22.dp))
        Text(
            tab.label,
            color = color,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
