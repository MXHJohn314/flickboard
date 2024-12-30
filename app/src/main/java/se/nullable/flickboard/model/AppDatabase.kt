package se.nullable.flickboard.model

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.model.clipboard.ClipboardEntryDao
import java.time.Instant

@Database(entities = [ClipboardEntry::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardEntryDao(): ClipboardEntryDao
}

class Converters {
    @TypeConverter
    fun instantFromTimestamp(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun instantToTimestamp(value: Instant?): Long? = value?.toEpochMilli()
}