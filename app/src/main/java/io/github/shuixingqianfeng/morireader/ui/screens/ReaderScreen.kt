package io.github.shuixingqianfeng.morireader.ui.screens

import android.content.Context
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.chrisbanes.haze.HazeState
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.ReaderMode
import io.github.shuixingqianfeng.morireader.data.ReaderPreferences
import io.github.shuixingqianfeng.morireader.data.ReaderTheme
import io.github.shuixingqianfeng.morireader.data.ReadingSessionTracker
import io.github.shuixingqianfeng.morireader.reader.ReaderController
import io.github.shuixingqianfeng.morireader.reader.ReaderEvent
import io.github.shuixingqianfeng.morireader.reader.ReaderLocation
import io.github.shuixingqianfeng.morireader.reader.ReaderWebView
import io.github.shuixingqianfeng.morireader.reader.TocItem
import io.github.shuixingqianfeng.morireader.ui.components.LiquidGlassSurface
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    hazeState: HazeState,
    book: BookEntity,
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onSaveLocation: (ReaderLocation) -> Unit,
    onRecordSession: (ReadingSessionTracker.Segment) -> Unit,
    onUpdatePreferences: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
) {
    val controller = remember(book.id) { ReaderController() }
    val tracker = remember(book.id) { ReadingSessionTracker() }
    var toolsVisible by remember { mutableStateOf(false) }
    var tocVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var toc by remember { mutableStateOf<List<TocItem>>(emptyList()) }
    var location by remember {
        mutableStateOf(ReaderLocation(book.currentCfi, book.progress, book.currentChapterIndex, book.currentChapterTitle))
    }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(location) {
        delay(450)
        onSaveLocation(location)
    }

    ReaderLifecycle(book.id, tracker, onRecordSession) { onSaveLocation(location) }
    BackHandler {
        when {
            tocVisible -> tocVisible = false
            settingsVisible -> settingsVisible = false
            toolsVisible -> toolsVisible = false
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize()) {
        ReaderWebView(
            book = book,
            preferences = preferences,
            controller = controller,
            modifier = Modifier.fillMaxSize(),
        ) { event ->
            when (event) {
                ReaderEvent.CenterTap -> toolsVisible = !toolsVisible
                is ReaderEvent.Relocated -> location = event.location
                is ReaderEvent.Toc -> toc = event.items
                is ReaderEvent.Error -> error = event.message
                else -> Unit
            }
        }

        AnimatedVisibility(toolsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            LiquidGlassSurface(
                hazeState,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                padding = PaddingValues(8.dp),
                strong = true,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSaveLocation(location); onBack() }) { Icon(Icons.Outlined.ArrowBack, "返回") }
                    Column(Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(location.chapterTitle.ifBlank { "阅读中" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFF687681), maxLines = 1)
                    }
                    IconButton(onClick = { tocVisible = true }) { Icon(Icons.Outlined.MenuBook, "目录") }
                    IconButton(onClick = { settingsVisible = true }) { Icon(Icons.Outlined.FormatSize, "阅读设置") }
                }
            }
        }

        AnimatedVisibility(toolsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            LiquidGlassSurface(
                hazeState,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
                shape = RoundedCornerShape(28.dp),
                padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                strong = true,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(location.chapterTitle.ifBlank { "当前章节" }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(location.fraction * 100).toInt()}%", color = Color(0xFF4B7FA8), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = controller::previous) { Icon(Icons.Outlined.ArrowBackIosNew, "上一页") }
                        Text(if (preferences.mode == ReaderMode.PAGED) "分页阅读" else "滚动阅读", color = Color(0xFF687681))
                        IconButton(onClick = controller::next) { Icon(Icons.Outlined.ArrowForwardIos, "下一页") }
                    }
                }
            }
        }

        error?.let { message ->
            Snackbar(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(18.dp), action = { Text("关闭", Modifier.padding(8.dp)) }) { Text(message) }
        }
    }

    if (tocVisible) {
        ModalBottomSheet(onDismissRequest = { tocVisible = false }, containerColor = Color.Transparent) {
            LiquidGlassSurface(hazeState, Modifier.fillMaxWidth().height(560.dp), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), PaddingValues(20.dp), true) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Text("目录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp)) }
                    itemsIndexed(toc) { _, item ->
                        Text(
                            item.label.ifBlank { "未命名章节" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { controller.goToHref(item.href); tocVisible = false }
                                .padding(start = (item.depth * 14).dp, top = 10.dp, bottom = 10.dp),
                            color = Color(0xFF27343E),
                        )
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth().height(1.dp)
                        )
                    }
                }
            }
        }
    }

    if (settingsVisible) {
        ModalBottomSheet(onDismissRequest = { settingsVisible = false }, containerColor = Color.Transparent) {
            ReaderSettingsSheet(hazeState, preferences, onUpdatePreferences)
        }
    }
}

@Composable
private fun ReaderLifecycle(
    bookId: String,
    tracker: ReadingSessionTracker,
    onRecord: (ReadingSessionTracker.Segment) -> Unit,
    onPause: () -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    DisposableEffect(bookId, lifecycle) {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        fun stop() {
            tracker.stop()?.let(onRecord)
            onPause()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (power.isInteractive) tracker.start(bookId)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && power.isInteractive) tracker.start(bookId)
        onDispose { lifecycle.removeObserver(observer); stop() }
    }
}

@Composable
private fun ReaderSettingsSheet(
    hazeState: HazeState,
    preferences: ReaderPreferences,
    onUpdate: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
) {
    LiquidGlassSurface(hazeState, Modifier.fillMaxWidth(), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), PaddingValues(24.dp), true) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("阅读设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("字号 ${preferences.fontSizeSp.toInt()} sp")
            Slider(preferences.fontSizeSp, { value -> onUpdate { it.copy(fontSizeSp = value) } }, valueRange = 14f..32f)
            Text("行距 ${"%.2f".format(preferences.lineHeight)}")
            Slider(preferences.lineHeight, { value -> onUpdate { it.copy(lineHeight = value) } }, valueRange = 1.2f..2.4f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(preferences.mode == ReaderMode.PAGED, { onUpdate { it.copy(mode = ReaderMode.PAGED) } }, { Text("分页") })
                FilterChip(preferences.mode == ReaderMode.SCROLLED, { onUpdate { it.copy(mode = ReaderMode.SCROLLED) } }, { Text("滚动") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.navigationBarsPadding()) {
                ReaderTheme.entries.forEach { theme ->
                    FilterChip(preferences.theme == theme, { onUpdate { it.copy(theme = theme) } }, { Text(readerThemeLabel(theme)) })
                }
            }
        }
    }
}

private fun readerThemeLabel(theme: ReaderTheme) = when (theme) {
    ReaderTheme.WHITE -> "白"
    ReaderTheme.SEPIA -> "米白"
    ReaderTheme.GRAY -> "灰"
    ReaderTheme.DARK -> "深色"
}
