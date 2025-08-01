package se.nullable.flickboard.model.clipboard

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.withTransaction
import se.nullable.flickboard.model.AppDatabase
import java.time.Instant

@Entity
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val lastAddedOn: Instant,
    val clipData: String,
    @ColumnInfo(defaultValue = "0")
    val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val generation: Long = 0,
)

const val clipboardHistorySize = 100

@Dao
abstract class ClipboardEntryDao(private val db: AppDatabase) {
    @Query("SELECT * FROM clipboardentry WHERE pinned = :pinned ORDER BY pinned DESC, lastAddedOn DESC")
    abstract fun getAll(pinned: Boolean): PagingSource<Int, ClipboardEntry>

    @Query("SELECT * FROM clipboardentry WHERE pinned = :pinned ORDER BY pinned DESC, lastAddedOn DESC")
    abstract suspend fun getAllUnpaged(pinned: Boolean): List<ClipboardEntry>

    @Query("UPDATE clipboardentry SET lastAddedOn = :lastAddedOn, pinned = (pinned OR :pinIfUnpinned) WHERE clipData = :clipData")
    protected abstract suspend fun touchByData(
        clipData: String,
        lastAddedOn: Instant,
        pinIfUnpinned: Boolean
    ): Int

    @Insert
    protected abstract suspend fun insertUnconditionally(vararg entries: ClipboardEntry)

    suspend fun insertOrTouch(entry: ClipboardEntry) {
        // Wrap in a transaction to avoid letting multiple concurrent callers create duplicate entries
        db.withTransaction {
            if (touchByData(
                    entry.clipData,
                    lastAddedOn = entry.lastAddedOn,
                    pinIfUnpinned = entry.pinned,
                ) == 0
            ) {
                insertUnconditionally(entry)
                prune()
            }
        }
    }

    @Query("UPDATE clipboardentry SET pinned = :pinned WHERE id = :id")
    abstract suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE clipboardentry SET clipData = :clipData WHERE id = :id")
    abstract suspend fun setClipData(id: Long, clipData: String)

    @Query("DELETE FROM clipboardentry WHERE pinned = 0")
    abstract suspend fun clearUnpinned()

    suspend fun clearAndGetUnpinned(): List<ClipboardEntry> =
        db.withTransaction {
            val oldEntries = getAllUnpaged(false)
            clearUnpinned()
            oldEntries
        }

    /**
     * Only keep the last [keep] unpinned rows, to avoid the db growing indefinitely.
     */
    @Query("DELETE FROM clipboardentry WHERE id IN (SELECT id FROM clipboardentry WHERE pinned = 0 ORDER BY lastAddedOn DESC LIMIT -1 OFFSET :keep)")
    protected abstract suspend fun prune(keep: Int = clipboardHistorySize)

    @Delete
    abstract suspend fun delete(entry: ClipboardEntry)
}