package se.nullable.flickboard.model.zalgo

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
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
abstract class ZalgoSubstitutionDao() {
    @Query("SELECT * from zalgosubstitution")
    abstract fun allCustom(): Flow<List<ZalgoSubstitution>>

    @Upsert
    abstract suspend fun upsert(substitution: ZalgoSubstitution)

    @Delete
    abstract suspend fun deleteOrRevert(substitution: ZalgoSubstitution)
}