package se.nullable.flickboard.model

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.model.clipboard.ClipboardEntryDao
import java.time.Instant

@Database(
    version = 3,
    entities = [ClipboardEntry::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
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