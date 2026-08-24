package io.github.shuixingqianfeng.morireader.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.Instant
import java.time.ZoneId

sealed interface ImportResult {
    data class Success(val book: BookEntity) : ImportResult
    data class Duplicate(val book: BookEntity) : ImportResult
    data class Failure(val message: String) : ImportResult
}

class BookRepository(
    private val dao: MoriDao,
    private val importer: EpubImporter,
) {
    val books: Flow<List<BookEntity>> = dao.observeBooks()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val sessions: Flow<List<ReadingSessionEntity>> = dao.observeSessions()
    val dailyReading: Flow<List<DailyReading>> = dao.observeDailyReading()

    fun observeBook(bookId: String): Flow<BookEntity?> = dao.observeBook(bookId)
    fun observeTagsForBook(bookId: String): Flow<List<TagEntity>> = dao.observeTagsForBook(bookId)
    fun booksForTag(tagId: Long): Flow<List<BookEntity>> = dao.observeBooksForTag(tagId)
    fun search(query: String): Flow<List<BookEntity>> = if (query.isBlank()) books else dao.searchBooks(query.trim())

    suspend fun import(uri: Uri): ImportResult = try {
        val hash = importer.hash(uri)
        dao.getBook(hash)?.let { return ImportResult.Duplicate(it) }
        val parsed = importer.copyAndParse(uri, hash)
        val now = System.currentTimeMillis()
        val book = BookEntity(
            id = hash,
            epubIdentifier = parsed.identifier,
            title = parsed.title,
            author = parsed.author,
            description = parsed.description,
            coverPath = parsed.cover?.absolutePath,
            filePath = parsed.file.absolutePath,
            readingDirection = parsed.readingDirection,
            importedAt = now,
        )
        dao.insertBook(book)
        ImportResult.Success(book)
    } catch (error: Throwable) {
        ImportResult.Failure(error.message ?: "导入失败")
    }

    suspend fun saveProgress(
        bookId: String,
        cfi: String?,
        chapterIndex: Int,
        chapterTitle: String,
        progress: Double,
    ) {
        val safeProgress = progress.coerceIn(0.0, 1.0)
        val status = when {
            safeProgress >= 0.995 -> BookStatus.FINISHED
            safeProgress > 0.0 -> BookStatus.READING
            else -> BookStatus.UNREAD
        }
        dao.updateProgress(
            bookId = bookId,
            cfi = cfi,
            chapterIndex = chapterIndex.coerceAtLeast(0),
            chapterTitle = chapterTitle,
            progress = safeProgress,
            status = status,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun addTag(bookId: String, name: String) = dao.addTagToBook(bookId, name)
    suspend fun removeTag(bookId: String, tagId: Long) = dao.removeBookTag(bookId, tagId)
    suspend fun delete(book: BookEntity) {
        dao.deleteBook(book.id)
        File(book.filePath).parentFile?.deleteRecursively()
    }

    suspend fun recordSession(bookId: String, startedAt: Long, endedAt: Long, durationMs: Long) {
        if (durationMs < 1_000) return
        val zone = ZoneId.systemDefault()
        var segmentStart = startedAt
        var remaining = durationMs
        while (segmentStart < endedAt && remaining > 0) {
            val startDate = Instant.ofEpochMilli(segmentStart).atZone(zone)
            val nextMidnight = startDate.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segmentEnd = minOf(endedAt, nextMidnight)
            val wallDuration = (segmentEnd - segmentStart).coerceAtLeast(1)
            val segmentDuration = minOf(remaining, wallDuration)
            dao.insertSession(
                ReadingSessionEntity(
                    bookId = bookId,
                    startedAt = segmentStart,
                    endedAt = segmentEnd,
                    durationMs = segmentDuration,
                    localDate = startDate.toLocalDate().toString(),
                ),
            )
            dao.addReadingTime(bookId, segmentDuration)
            remaining -= segmentDuration
            segmentStart = segmentEnd
        }
    }
}
