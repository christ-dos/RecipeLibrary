package com.example.recipelibrary.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Lien de recette en file d'attente : mis de côté quand le PC Hermes est injoignable,
 * importé plus tard en arrière-plan, puis validé par l'utilisatrice.
 */
@Entity(tableName = "pending_imports")
data class PendingImport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    /** [WAITING], [READY] ou [FAILED]. */
    val status: String = WAITING,
    /** JSON de la recette extraite, quand [status] vaut [READY]. */
    @ColumnInfo(name = "recipe_json") val recipeJson: String? = null,
    /** Raison de l'échec, quand [status] vaut [FAILED]. */
    val error: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val WAITING = "WAITING"
        const val READY = "READY"
        const val FAILED = "FAILED"
    }
}

@Dao
interface PendingImportDao {
    @Query("SELECT * FROM pending_imports ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<PendingImport>>

    @Query("SELECT * FROM pending_imports WHERE status = 'WAITING' ORDER BY created_at ASC, id ASC LIMIT 1")
    suspend fun nextWaiting(): PendingImport?

    @Query("SELECT COUNT(*) FROM pending_imports WHERE status = 'WAITING'")
    suspend fun countWaiting(): Int

    @Query("SELECT * FROM pending_imports WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): PendingImport?

    @Insert
    suspend fun insert(item: PendingImport): Long

    @Update
    suspend fun update(item: PendingImport)

    @Query("DELETE FROM pending_imports WHERE id = :id")
    suspend fun deleteById(id: Long)
}
