package io.github.shuixingqianfeng.morireader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import io.github.shuixingqianfeng.morireader.BuildConfig
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.DailyReading
import io.github.shuixingqianfeng.morireader.data.PageTurnEffect
import io.github.shuixingqianfeng.morireader.data.ReaderMode
import io.github.shuixingqianfeng.morireader.data.ReaderPreferences
import io.github.shuixingqianfeng.morireader.data.ReaderTheme
import io.github.shuixingqianfeng.morireader.data.TagEntity
import io.github.shuixingqianfeng.morireader.ui.components.LiquidGlassSurface
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Composable
fun TagsScreen(
    hazeState: HazeState,
    tags: List<TagEntity>,
    books: List<BookEntity>,
    booksForTag: (Long) -> Flow<List<BookEntity>>,
    onBookClick: (BookEntity) -> Unit,
) {
    var selectedTag by remember { mutableStateOf<TagEntity?>(null) }
    val visible = selectedTag?.let { booksForTag(it.id).collectAsState(initial = emptyList()).value } ?: books
    ScreenList("标签") {
        item {
            LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("整理你的书架", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (tags.isEmpty()) Text("在书籍详情中添加标签。", color = Color(0xFF6F7882))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.take(5).forEach { tag ->
                            FilterChip(selected = selectedTag?.id == tag.id, onClick = { selectedTag = if (selectedTag?.id == tag.id) null else tag }, label = { Text(tag.name) })
                        }
                    }
                }
            }
        }
        items(visible, key = { it.id }) { book -> CompactBookRow(hazeState, book) { onBookClick(book) } }
    }
}

@Composable
fun SearchScreen(
    hazeState: HazeState,
    books: List<BookEntity>,
    searchBooks: (String) -> Flow<List<BookEntity>>,
    onBookClick: (BookEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = if (query.isBlank()) books else remember(query) { searchBooks(query) }.collectAsState(initial = emptyList()).value
    ScreenList("搜索") {
        item {
            LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("书名、作者或标签") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )
            }
        }
        if (results.isEmpty()) item { Text("没有找到匹配的书籍", color = Color(0xFF6F7882), modifier = Modifier.padding(12.dp)) }
        items(results, key = { it.id }) { book -> CompactBookRow(hazeState, book) { onBookClick(book) } }
    }
}

@Composable
fun StatsScreen(hazeState: HazeState, daily: List<DailyReading>, books: List<BookEntity>) {
    val today = LocalDate.now()
    val map = daily.associate { it.localDate to it.durationMs }
    val sevenDays = (6 downTo 0).map { day -> today.minusDays(day.toLong()) to (map[today.minusDays(day.toLong()).toString()] ?: 0L) }
    val todayMs = map[today.toString()] ?: 0L
    val total = daily.sumOf { it.durationMs }
    val streak = calculateStreak(map.keys)
    ScreenList("统计") {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(hazeState, "今日", formatDuration(todayMs), Modifier.weight(1f))
                StatCard(hazeState, "连续", "${streak}天", Modifier.weight(1f))
                StatCard(hazeState, "累计", formatDuration(total), Modifier.weight(1f))
            }
        }
        item {
            LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("最近 7 天", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        val max = sevenDays.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                        sevenDays.forEach { (date, value) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text("${value / 60_000}", fontSize = 10.sp, color = Color(0xFF687681))
                                Box(Modifier.width(22.dp).height((108f * value / max).coerceAtLeast(4f).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF9FC3DD)))
                                Text("${date.dayOfMonth}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Text("每本书", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(books.sortedByDescending { it.totalReadingTimeMs }, key = { it.id }) { book ->
            CompactBookRow(hazeState, book, trailing = formatDuration(book.totalReadingTimeMs)) {}
        }
    }
}

@Composable
fun SettingsScreen(
    hazeState: HazeState,
    preferences: ReaderPreferences,
    onUpdate: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
) {
    ScreenList("设置") {
        item {
            SettingCard(hazeState, "每日阅读目标", "${preferences.dailyGoalMinutes} 分钟") {
                Slider(
                    value = preferences.dailyGoalMinutes.toFloat(),
                    onValueChange = { value -> onUpdate { it.copy(dailyGoalMinutes = value.toInt()) } },
                    valueRange = 5f..120f,
                    steps = 22,
                )
            }
        }
        item {
            SettingCard(hazeState, "默认字号", "${preferences.fontSizeSp.toInt()} sp") {
                Slider(value = preferences.fontSizeSp, onValueChange = { value -> onUpdate { it.copy(fontSizeSp = value) } }, valueRange = 14f..32f)
            }
        }
        item {
            SettingCard(hazeState, "阅读方式", if (preferences.mode == ReaderMode.PAGED) "分页" else "滚动") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(preferences.mode == ReaderMode.PAGED, { onUpdate { it.copy(mode = ReaderMode.PAGED) } }, { Text("分页") })
                    FilterChip(preferences.mode == ReaderMode.SCROLLED, { onUpdate { it.copy(mode = ReaderMode.SCROLLED) } }, { Text("滚动") })
                }
            }
        }
        item {
            SettingCard(hazeState, "阅读背景", themeName(preferences.theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReaderTheme.entries.forEach { theme ->
                        AssistChip(onClick = { onUpdate { it.copy(theme = theme) } }, label = { Text(themeName(theme)) })
                    }
                }
            }
        }
        item {
            LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("滑动翻页", fontWeight = FontWeight.Bold)
                        Text("左右滑动或点击页面边缘", color = Color(0xFF6F7882), fontSize = 13.sp)
                    }
                    Switch(preferences.swipeEnabled, { checked -> onUpdate { it.copy(swipeEnabled = checked) } })
                }
            }
        }
        item {
            SettingCard(hazeState, "翻页效果", if (preferences.pageTurnEffect == PageTurnEffect.SIMULATION) "仿真翻页" else "简洁翻页") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        preferences.pageTurnEffect == PageTurnEffect.DIRECT,
                        { onUpdate { it.copy(pageTurnEffect = PageTurnEffect.DIRECT) } },
                        { Text("简洁") },
                    )
                    FilterChip(
                        preferences.pageTurnEffect == PageTurnEffect.SIMULATION,
                        { onUpdate { it.copy(pageTurnEffect = PageTurnEffect.SIMULATION) } },
                        { Text("仿真") },
                    )
                }
            }
        }
        item {
            Text("MoriReader ${BuildConfig.VERSION_NAME}", color = Color(0xFF76818B), modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun ScreenList(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black) }
        content()
    }
}

@Composable
private fun CompactBookRow(hazeState: HazeState, book: BookEntity, trailing: String = "${(book.progress * 100).toInt()}%", onClick: () -> Unit) {
    LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(book, Modifier.size(width = 54.dp, height = 76.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, color = Color(0xFF6F7882), maxLines = 1)
            }
            Text(trailing, color = Color(0xFF4B7FA8), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatCard(hazeState: HazeState, label: String, value: String, modifier: Modifier) {
    LiquidGlassSurface(hazeState, modifier, padding = PaddingValues(vertical = 18.dp, horizontal = 12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = Color(0xFF6F7882), fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun SettingCard(hazeState: HazeState, title: String, value: String, content: @Composable () -> Unit) {
    LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(value, color = Color(0xFF4B7FA8)) }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private fun calculateStreak(days: Set<String>): Int {
    var date = LocalDate.now()
    var count = 0
    while (days.contains(date.toString())) { count += 1; date = date.minusDays(1) }
    return count
}

private fun themeName(theme: ReaderTheme) = when (theme) {
    ReaderTheme.WHITE -> "纯白"
    ReaderTheme.SEPIA -> "米白"
    ReaderTheme.GRAY -> "浅灰"
    ReaderTheme.DARK -> "深色"
}
