package io.github.shuixingqianfeng.morireader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.shuixingqianfeng.morireader.data.BookEntity
import io.github.shuixingqianfeng.morireader.data.ImportResult
import io.github.shuixingqianfeng.morireader.data.ReaderPreferences
import io.github.shuixingqianfeng.morireader.data.ReadingSessionTracker
import io.github.shuixingqianfeng.morireader.data.TagEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiMessage {
    data class Info(val text: String) : UiMessage
    data class Imported(val book: BookEntity) : UiMessage
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MoriReaderApplication).container
    private val repository = container.books
    private val settingsRepository = container.settings

    val books = repository.books.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sessions = repository.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dailyReading = repository.dailyReading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences = settingsRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReaderPreferences(),
    )

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    fun import(uri: Uri) = viewModelScope.launch {
        when (val result = repository.import(uri)) {
            is ImportResult.Success -> messageChannel.send(UiMessage.Imported(result.book))
            is ImportResult.Duplicate -> messageChannel.send(UiMessage.Info("《${result.book.title}》已经在书库中"))
            is ImportResult.Failure -> messageChannel.send(UiMessage.Info(result.message))
        }
    }

    fun saveProgress(bookId: String, cfi: String?, chapter: Int, title: String, progress: Double) {
        viewModelScope.launch { repository.saveProgress(bookId, cfi, chapter, title, progress) }
    }

    fun record(segment: ReadingSessionTracker.Segment) {
        viewModelScope.launch {
            repository.recordSession(segment.bookId, segment.startedAt, segment.endedAt, segment.durationMs)
        }
    }

    fun addTag(bookId: String, name: String) = viewModelScope.launch { repository.addTag(bookId, name) }
    fun removeTag(bookId: String, tagId: Long) = viewModelScope.launch { repository.removeTag(bookId, tagId) }
    fun tagsForBook(bookId: String): Flow<List<TagEntity>> = repository.observeTagsForBook(bookId)
    fun booksForTag(tagId: Long): Flow<List<BookEntity>> = repository.booksForTag(tagId)
    fun searchBooks(query: String): Flow<List<BookEntity>> = repository.search(query)
    fun delete(book: BookEntity) = viewModelScope.launch { repository.delete(book) }
    fun updatePreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
