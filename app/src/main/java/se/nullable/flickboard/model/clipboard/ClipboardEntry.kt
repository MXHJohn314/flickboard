package se.nullable.flickboard.model.clipboard

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

@Entity
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val lastAddedOn: Instant,
    val clipData: String,
)

val clipboardHistorySize = 100

@Dao
interface ClipboardEntryDao {
    @Query("SELECT * FROM clipboardentry ORDER BY lastAddedOn DESC")
    fun getAll(): PagingSource<Int, ClipboardEntry>

    @Query("UPDATE clipboardentry SET lastAddedOn = :lastAddedOn WHERE clipData = :clipData")
    suspend fun touchByData(clipData: String, lastAddedOn: Instant): Int

    @Insert
    suspend fun insertUnconditionally(vararg entries: ClipboardEntry)

    suspend fun insertOrTouch(entry: ClipboardEntry) {
        // Lock to avoid letting multiple concurrent callers create duplicate entries
        upsertMutex.withLock {
            if (touchByData(entry.clipData, entry.lastAddedOn) == 0) {
                insertUnconditionally(entry)
                prune()
            }
        }
    }

    @Query("DELETE FROM clipboardentry")
    suspend fun clear()

    /**
     * Only keep the last [keep] rows, to avoid the db growing indefinitely.
     */
    @Query("DELETE FROM clipboardentry WHERE id IN (SELECT id FROM clipboardentry ORDER BY lastAddedOn DESC LIMIT -1 OFFSET :keep)")
    suspend fun prune(keep: Int = clipboardHistorySize)

    @Delete
    suspend fun delete(entry: ClipboardEntry)

    companion object {
        private val upsertMutex = Mutex()
    }
}