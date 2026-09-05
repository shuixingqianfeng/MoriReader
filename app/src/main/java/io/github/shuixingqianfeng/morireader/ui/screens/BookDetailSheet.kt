package io.github.shuixingqianfeng.morireader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.BookStatus
import io.github.shuixingqianfeng.morireader.data.TagEntity
import io.github.shuixingqianfeng.morireader.ui.components.GlassButtonShape
import io.github.shuixingqianfeng.morireader.ui.components.LiquidGlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailSheet(
    hazeState: HazeState,
    book: BookEntity,
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    var tagText by remember(book.id) { mutableStateOf("") }
    var confirmDelete by remember(book.id) { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color(0x7317202A),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    ) {
        LiquidGlassSurface(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            padding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            strong = true,
            readable = true,
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "关闭") }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cover(book, Modifier.width(132.dp).aspectRatio(0.68f))
                        Spacer(Modifier.width(22.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(book.author, color = Color(0xFF46535E))
                            Text(statusText(book.status), color = Color(0xFF28668F), fontWeight = FontWeight.Bold)
                            Text("已读 ${(book.progress * 100).toInt()}% · ${formatDuration(book.totalReadingTimeMs)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF53616C))
                        }
                    }
                }
                item {
                    Button(onClick = onRead, modifier = Modifier.fillMaxWidth().height(56.dp), shape = GlassButtonShape) {
                        Text(if (book.progress > 0) "继续阅读" else "开始阅读", fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    LiquidGlassSurface(hazeState, modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp), strong = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("阅读进度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(book.currentChapterTitle.ifBlank { "尚未开始" }, color = Color(0xFF46535E))
                            Text("EPUB · ${book.readingDirection.uppercase()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF53616C))
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("标签", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (tags.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tags.take(4).forEach { tag ->
                                    AssistChip(onClick = { onRemoveTag(tag.id) }, label = { Text(tag.name) }, trailingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(16.dp)) })
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(tagText, { tagText = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("添加标签") })
                            IconButton(onClick = { if (tagText.isNotBlank()) { onAddTag(tagText); tagText = "" } }) { Icon(Icons.Outlined.Add, "添加") }
                        }
                    }
                }
                item {
                    Text("书籍简介", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(book.description.ifBlank { "这本 EPUB 没有提供简介。" }, color = Color(0xFF26323B), lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.28)
                }
                item {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("从书库删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("从书库删除？") },
            text = { Text("将删除轻阅私有目录中的 EPUB、封面和阅读记录。原始文件不会受到影响。") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

private fun statusText(status: BookStatus) = when (status) {
    BookStatus.UNREAD -> "未读"
    BookStatus.READING -> "阅读中"
    BookStatus.FINISHED -> "已读完"
}

fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes < 60) "${minutes}分钟" else "${minutes / 60}小时${minutes % 60}分"
}
