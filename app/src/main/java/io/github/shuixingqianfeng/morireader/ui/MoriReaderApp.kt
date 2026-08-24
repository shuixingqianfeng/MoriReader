package io.github.shuixingqianfeng.morireader.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.shuixingqianfeng.morireader.MainViewModel
import io.github.shuixingqianfeng.morireader.UiMessage
import io.github.shuixingqianfeng.morireader.ui.components.GlassBottomNavigation
import io.github.shuixingqianfeng.morireader.ui.components.MainTab
import io.github.shuixingqianfeng.morireader.ui.screens.BookDetailSheet
import io.github.shuixingqianfeng.morireader.ui.screens.LibraryScreen
import io.github.shuixingqianfeng.morireader.ui.screens.ReaderScreen
import io.github.shuixingqianfeng.morireader.ui.screens.SearchScreen
import io.github.shuixingqianfeng.morireader.ui.screens.SettingsScreen
import io.github.shuixingqianfeng.morireader.ui.screens.StatsScreen
import io.github.shuixingqianfeng.morireader.ui.screens.TagsScreen
import java.time.LocalDate

@Composable
fun MoriReaderApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val daily by viewModel.dailyReading.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val hazeState = remember { HazeState() }
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.LIBRARY) }
    var detailBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var readerBookId by rememberSaveable { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.import(uri)
        }
    }
    val onImport = { importLauncher.launch(arrayOf("application/epub+zip", "application/octet-stream")) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                is UiMessage.Imported -> {
                    detailBookId = message.book.id
                    snackbar.showSnackbar("《${message.book.title}》已导入")
                }
                is UiMessage.Info -> snackbar.showSnackbar(message.text)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF7F8FB)).hazeSource(hazeState)) {
        val readerBook = books.firstOrNull { it.id == readerBookId }
        if (readerBook != null) {
            ReaderScreen(
                hazeState = hazeState,
                book = readerBook,
                preferences = preferences,
                onBack = { readerBookId = null },
                onSaveLocation = { location ->
                    viewModel.saveProgress(readerBook.id, location.cfi, location.chapterIndex, location.chapterTitle, location.fraction)
                },
                onRecordSession = viewModel::record,
                onUpdatePreferences = viewModel::updatePreferences,
            )
        } else {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                when (selectedTab) {
                    MainTab.LIBRARY -> LibraryScreen(
                        hazeState = hazeState,
                        books = books,
                        todayMs = daily.firstOrNull { it.localDate == LocalDate.now().toString() }?.durationMs ?: 0,
                        goalMinutes = preferences.dailyGoalMinutes,
                        onImport = onImport,
                        onBookClick = { detailBookId = it.id },
                    )
                    MainTab.TAGS -> TagsScreen(hazeState, tags, books, { tagId -> viewModel.booksForTag(tagId) }) { detailBookId = it.id }
                    MainTab.STATS -> StatsScreen(hazeState, daily, books)
                    MainTab.SETTINGS -> SettingsScreen(hazeState, preferences, viewModel::updatePreferences)
                    MainTab.SEARCH -> SearchScreen(hazeState, books, viewModel::searchBooks) { detailBookId = it.id }
                }

                GlassBottomNavigation(
                    hazeState = hazeState,
                    selectedTab = selectedTab,
                    onSelected = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp).height(76.dp),
                )
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 100.dp))
    }

    val detailBook = books.firstOrNull { it.id == detailBookId }
    if (readerBookId == null && detailBook != null) {
        val detailTags by viewModel.tagsForBook(detailBook.id).collectAsState(initial = emptyList())
        BookDetailSheet(
            hazeState = hazeState,
            book = detailBook,
            tags = detailTags,
            onDismiss = { detailBookId = null },
            onRead = { detailBookId = null; readerBookId = detailBook.id },
            onAddTag = { viewModel.addTag(detailBook.id, it) },
            onRemoveTag = { viewModel.removeTag(detailBook.id, it) },
            onDelete = { viewModel.delete(detailBook); detailBookId = null },
        )
    }
}
