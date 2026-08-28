package io.github.shuixingqianfeng.morireader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
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

@Composable
fun GlassBottomNavigation(
    hazeState: HazeState,
    selectedTab: MainTab,
    onSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { MainTab.entries }
    val selectedIndex = tabs.indexOf(selectedTab)
    val density = LocalDensity.current
    var dragX by remember { mutableFloatStateOf(Float.NaN) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val circleWidthPx = with(density) { 72.dp.toPx() }
        val gapPx = with(density) { 12.dp.toPx() }
        val capsuleWidthPx = (totalWidthPx - circleWidthPx - gapPx).coerceAtLeast(1f)
        val tabWidthPx = capsuleWidthPx / 4f
        val lensWidthPx = with(density) { 56.dp.toPx() }

        fun center(index: Int): Float = if (index < 4) {
            tabWidthPx * (index + 0.5f)
        } else {
            capsuleWidthPx + gapPx + circleWidthPx / 2f
        }

        fun nearest(x: Float): Int = tabs.indices.minBy { abs(center(it) - x) }

        val lensX by animateFloatAsState(
            targetValue = if (dragX.isNaN()) center(selectedIndex) else dragX,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
            label = "glass-lens",
        )

        Box(
            Modifier
                .fillMaxWidth()
                .testTag("bottom_navigation")
                .pointerInput(totalWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var targetIndex = nearest(down.position.x)
                        dragX = down.position.x.coerceIn(lensWidthPx / 2, totalWidthPx - lensWidthPx / 2)
                        onSelected(tabs[targetIndex])
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val x = change.position.x.coerceIn(lensWidthPx / 2, totalWidthPx - lensWidthPx / 2)
                            dragX = x
                            val next = nearest(x)
                            if (next != targetIndex) {
                                targetIndex = next
                                onSelected(tabs[targetIndex])
                            }
                            change.consume()
                        }
                        dragX = Float.NaN
                    }
                },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LiquidGlassSurface(
                    hazeState = hazeState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(36.dp),
                    strong = true,
                ) {}
                Spacer(Modifier.width(12.dp))
                LiquidGlassSurface(
                    hazeState = hazeState,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    strong = true,
                ) {}
            }

            LiquidGlassSurface(
                hazeState = hazeState,
                modifier = Modifier
                    .offset { IntOffset((lensX - lensWidthPx / 2).roundToInt(), with(density) { 8.dp.roundToPx() }) }
                    .size(56.dp),
                shape = CircleShape,
                strong = true,
            ) {}

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).fillMaxHeight()) {
                    tabs.take(4).forEach { tab ->
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
                            NavigationItemContent(tab, selectedTab == tab)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(72.dp)
                        .testTag("tab_SEARCH")
                        .semantics {
                            role = Role.Tab
                            selected = selectedTab == MainTab.SEARCH
                            onClick { onSelected(MainTab.SEARCH); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    NavigationItemContent(MainTab.SEARCH, selectedTab == MainTab.SEARCH, showLabel = false)
                }
            }
        }
    }
}

@Composable
private fun NavigationItemContent(tab: MainTab, selected: Boolean, showLabel: Boolean = true) {
    val color by animateColorAsState(
        if (selected) Color(0xFF397DA9) else Color(0xFF273039),
        label = "nav-color",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(if (showLabel) 24.dp else 30.dp))
        if (showLabel) {
            Text(tab.label, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
