package io.github.shuixingqianfeng.morireader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class MoriConverters {
    @TypeConverter fun fromStatus(value: BookStatus): String = value.name
    @TypeConverter fun toStatus(value: String): BookStatus = BookStatus.valueOf(value)
}

@Database(
    entities = [BookEntity::class, TagEntity::class, BookTagCrossRef::class, ReadingSessionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(MoriConverters::class)
abstract class MoriDatabase : RoomDatabase() {
    abstract fun dao(): MoriDao

    companion object {
        @Volatile private var instance: MoriDatabase? = null

        fun get(context: Context): MoriDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MoriDatabase::class.java,
                "morireader.db",
            ).build().also { instance = it }
        }
    }
}
