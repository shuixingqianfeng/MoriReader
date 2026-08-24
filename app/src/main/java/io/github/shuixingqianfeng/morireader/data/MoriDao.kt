package io.github.shuixingqianfeng.morireader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoriDao {
    @Query("SELECT * FROM books ORDER BY COALESCE(lastReadAt, importedAt) DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun observeBook(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBook(bookId: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query(
        """
        UPDATE books SET currentCfi = :cfi, currentChapterIndex = :chapterIndex,
        currentChapterTitle = :chapterTitle, progress = :progress,
        status = :status, lastReadAt = :updatedAt WHERE id = :bookId
        """,
    )
    suspend fun updateProgress(
        bookId: String,
        cfi: String?,
        chapterIndex: Int,
        chapterTitle: String,
        progress: Double,
        status: BookStatus,
        updatedAt: Long,
    )

    @Query("UPDATE books SET totalReadingTimeMs = totalReadingTimeMs + :durationMs WHERE id = :bookId")
    suspend fun addReadingTime(bookId: String, durationMs: Long)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query(
        """
        SELECT DISTINCT books.* FROM books
        LEFT JOIN book_tags ON books.id = book_tags.bookId
        LEFT JOIN tags ON tags.id = book_tags.tagId
        WHERE books.title LIKE '%' || :query || '%'
           OR books.author LIKE '%' || :query || '%'
           OR tags.name LIKE '%' || :query || '%'
        ORDER BY COALESCE(books.lastReadAt, books.importedAt) DESC
        """,
    )
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT id FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTagId(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookTag(ref: BookTagCrossRef)

    @Query("DELETE FROM book_tags WHERE bookId = :bookId AND tagId = :tagId")
    suspend fun removeBookTag(bookId: String, tagId: Long)

    @Query(
        """
        SELECT books.* FROM books
        INNER JOIN book_tags ON books.id = book_tags.bookId
        WHERE book_tags.tagId = :tagId
        ORDER BY COALESCE(books.lastReadAt, books.importedAt) DESC
        """,
    )
    fun observeBooksForTag(tagId: Long): Flow<List<BookEntity>>

    @Query("SELECT tags.* FROM tags INNER JOIN book_tags ON tags.id = book_tags.tagId WHERE book_tags.bookId = :bookId ORDER BY tags.name")
    fun observeTagsForBook(bookId: String): Flow<List<TagEntity>>

    @Insert
    suspend fun insertSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT localDate, SUM(durationMs) AS durationMs FROM reading_sessions GROUP BY localDate ORDER BY localDate")
    fun observeDailyReading(): Flow<List<DailyReading>>

    @Transaction
    suspend fun addTagToBook(bookId: String, rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) return
        val inserted = insertTag(TagEntity(name = name))
        val tagId = if (inserted == -1L) findTagId(name) else inserted
        if (tagId != null) addBookTag(BookTagCrossRef(bookId, tagId))
    }
}
