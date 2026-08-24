package io.github.shuixingqianfeng.morireader.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BookStatus { UNREAD, READING, FINISHED }

@Entity(
    tableName = "books",
    indices = [Index("lastReadAt"), Index("epubIdentifier")],
)
data class BookEntity(
    @PrimaryKey val id: String,
    val epubIdentifier: String?,
    val title: String,
    val author: String,
    val description: String,
    val coverPath: String?,
    val filePath: String,
    val readingDirection: String,
    val currentCfi: String? = null,
    val currentChapterIndex: Int = 0,
    val currentChapterTitle: String = "",
    val progress: Double = 0.0,
    val status: BookStatus = BookStatus.UNREAD,
    val importedAt: Long,
    val lastReadAt: Long? = null,
    val totalReadingTimeMs: Long = 0,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "book_tags",
    primaryKeys = ["bookId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("tagId")],
)
data class BookTagCrossRef(val bookId: String, val tagId: Long)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("localDate")],
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val localDate: String,
)

data class BookWithTags(
    val book: BookEntity,
    val tags: List<TagEntity>,
)

data class DailyReading(val localDate: String, val durationMs: Long)
