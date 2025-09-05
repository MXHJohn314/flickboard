package se.nullable.flickboard.model

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.model.clipboard.ClipboardEntryDao
import se.nullable.flickboard.model.zalgo.ZalgoSubstitution
import se.nullable.flickboard.model.zalgo.ZalgoSubstitutionDao
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Database(
    version = 5,
    entities = [ClipboardEntry::class, ZalgoSubstitution::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = AppDatabase.Migrate4To5::class),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardEntryDao(): ClipboardEntryDao

    abstract fun zalgoSubstitutionDao(): ZalgoSubstitutionDao

    class Migrate4To5 : AutoMigrationSpec {
        @OptIn(ExperimentalUuidApi::class)
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.query("SELECT `from` FROM zalgosubstitution").use { cursor ->
                while (cursor.moveToNext()) {
                    val from = cursor.getString(0)
                    val id = Uuid.random()
                    db.execSQL(
                        "UPDATE zalgosubstitution SET visualId=? WHERE `from`=?",
                        arrayOf(id.toByteArray(), from),
                    )
                }
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun instantFromTimestamp(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun instantToTimestamp(value: Instant?): Long? = value?.toEpochMilli()
}