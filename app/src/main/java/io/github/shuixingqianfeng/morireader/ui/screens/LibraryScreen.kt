package io.github.shuixingqianfeng.morireader.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.BookStatus
import io.github.shuixingqianfeng.morireader.ui.components.GlassButtonShape
import io.github.shuixingqianfeng.morireader.ui.components.LiquidGlassSurface
import java.io.File

@Composable
fun LibraryScreen(
    hazeState: HazeState,
    books: List<BookEntity>,
    todayMs: Long,
    goalMinutes: Int,
    onImport: () -> Unit,
    onBookClick: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "书库",
                    modifier = Modifier.testTag("library_screen_title"),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                LiquidGlassSurface(
                    hazeState,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    strong = true,
                ) {
                    IconButton(onClick = onImport) { Icon(Icons.Outlined.Add, contentDescription = "导入 EPUB", modifier = Modifier.size(28.dp)) }
                }
            }
        }
        item {
            TodayProgressCard(
                hazeState,
                todayMs,
                goalMinutes,
                books.firstOrNull { it.lastReadAt != null && it.status != BookStatus.FINISHED },
                onBookClick,
            )
        }

        if (books.isEmpty()) {
            item { EmptyLibrary(hazeState, onImport) }
        } else {
            books.firstOrNull { it.lastReadAt != null }?.let { recent ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("最近阅读", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        RecentBookCard(hazeState, recent) { onBookClick(recent) }
                    }
                }
            }
            item { Text("单册书籍", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
            items(books.chunked(3), key = { row -> row.joinToString { it.id } }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { book -> BookCoverCard(book, Modifier.weight(1f)) { onBookClick(book) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TodayProgressCard(
    hazeState: HazeState,
    todayMs: Long,
    goalMinutes: Int,
    recent: BookEntity?,
    onBookClick: (BookEntity) -> Unit,
) {
    val minutes = todayMs / 60_000
    val percent = if (goalMinutes <= 0) 0 else (minutes * 100 / goalMinutes).toInt()
    LiquidGlassSurface(
        hazeState,
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(24.dp),
        strong = true,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("今日阅读进度", fontSize = 23.sp, color = Color(0xFF255D86), fontWeight = FontWeight.ExtraBold)
                Text(if (percent >= 100) "今日目标已完成" else "还差 ${(goalMinutes - minutes).coerceAtLeast(0)} 分钟", color = Color(0xFF5F7384))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { recent?.let(onBookClick) },
                    enabled = recent != null,
                    shape = GlassButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.78f), contentColor = Color(0xFF356F9D)),
                ) { Text(if (recent == null) "导入后开始阅读" else "继续阅读", fontWeight = FontWeight.SemiBold) }
            }
            ProgressRing(hazeState, percent, minutes.toInt())
        }
    }
}

@Composable
private fun ProgressRing(hazeState: HazeState, percent: Int, minutes: Int) {
    LiquidGlassSurface(
        hazeState,
        modifier = Modifier.size(136.dp),
        shape = CircleShape,
        padding = PaddingValues(10.dp),
        strong = true,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                drawArc(Color.White.copy(alpha = 0.82f), -90f, 360f, false, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(Color(0xFF78ACD2), -90f, 360f * (percent.coerceAtMost(100) / 100f), false, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent%", fontSize = 26.sp, fontWeight = FontWeight.Light)
                Text("${minutes}分钟", fontSize = 12.sp, color = Color(0xFF63717C))
            }
        }
    }
}

@Composable
private fun EmptyLibrary(hazeState: HazeState, onImport: () -> Unit) {
    LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(34.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.AutoStories, null, tint = Color(0xFF7EA7C6), modifier = Modifier.size(54.dp))
            Text("书架还是空的", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("从设备中选择 EPUB，书籍只会保存在本机。", color = Color(0xFF6F7882))
            Button(onClick = onImport, shape = GlassButtonShape) { Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(8.dp)); Text("导入 EPUB") }
        }
    }
}

@Composable
private fun RecentBookCard(hazeState: HazeState, book: BookEntity, onClick: () -> Unit) {
    LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(book, Modifier.width(66.dp).aspectRatio(0.68f))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, color = Color(0xFF6F7882), maxLines = 1)
                Text("已读 ${(book.progress * 100).toInt()}%", color = Color(0xFF4B7FA8), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BookCoverCard(book: BookEntity, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Cover(book, Modifier.fillMaxWidth().aspectRatio(0.68f))
        Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Text(book.author, color = Color(0xFF7B838B), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
    }
}

@Composable
fun Cover(book: BookEntity, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.shadow(10.dp, shape, ambientColor = Color(0x28586B7C)).clip(shape).background(Color(0xFFE8EEF4)), contentAlignment = Alignment.Center) {
        if (book.coverPath != null) {
            AsyncImage(File(book.coverPath), contentDescription = book.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Color(0xFF86A6BF), modifier = Modifier.size(40.dp))
        }
        BookProgressBadge(book.progress, Modifier.align(Alignment.TopStart).padding(8.dp))
    }
}

@Composable
private fun BookProgressBadge(progress: Double, modifier: Modifier = Modifier) {
    val fraction = progress.toFloat().coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(20.dp)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                ambientColor = Color(0x24506F88),
                spotColor = Color(0x30506F88),
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color(0xFFD9F1FF).copy(alpha = 0.52f),
                        Color.White.copy(alpha = 0.66f),
                    ),
                ),
            )
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White, Color.White.copy(alpha = 0.54f), Color(0xFF83AFCB).copy(alpha = 0.45f)),
                ),
                shape = CircleShape,
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.5.dp.toPx()
            drawArc(
                color = Color(0x6B7E919F),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (fraction in 0.001f..0.994f) {
                drawArc(
                    color = Color(0xFF4B8DB8),
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        if (fraction >= 0.995f) {
            Icon(Icons.Outlined.Check, contentDescription = "已读完", tint = Color(0xFF397DA9), modifier = Modifier.size(10.dp))
        }
    }
}
