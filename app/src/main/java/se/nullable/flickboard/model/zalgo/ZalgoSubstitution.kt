package se.nullable.flickboard.model.zalgo

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import se.nullable.flickboard.model.AppDatabase
import java.util.UUID

@Entity
data class ZalgoSubstitution @JvmOverloads constructor(
    @PrimaryKey
    val from: String,
    val to: String,
    // Nonsense default to let SQLite add the column,
    // values will be generated in Migrate4To5
    @ColumnInfo(defaultValue = "'(migration placeholder)'")
    val visualId: UUID,
    @Ignore
    val isDefault: Boolean = false,
)

@Dao
abstract class ZalgoSubstitutionDao(private val db: AppDatabase) {
    @Query("SELECT * from zalgosubstitution")
    abstract fun allCustom(): Flow<List<ZalgoSubstitution>>

    @Upsert
    protected abstract suspend fun upsertNew(substitution: ZalgoSubstitution)

    suspend fun upsert(substitution: ZalgoSubstitution, old: ZalgoSubstitution?) {
        db.withTransaction {
            val new = when {
                old == null || old.from == substitution.from -> substitution
                deleteOrRevert(old) == 0 -> {
                    // old exists but isn't in the database, so it'll still be there..
                    // as a compromise, change the visualId of the new to avoid conflicts
                    substitution.copy(visualId = UUID.randomUUID())
                }

                else -> substitution
            }
            upsertNew(new)
        }
    }

    @Delete
    abstract suspend fun deleteOrRevert(substitution: ZalgoSubstitution): Int
}