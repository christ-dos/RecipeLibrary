package com.example.recipelibrary.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Aliment de remplacement choisi par l'utilisatrice pour un ingrédient non reconnu (spec 001,
 * story 6). Vaut pour toutes les recettes, existantes et futures ; les recettes ne sont pas modifiées.
 */
@Entity(tableName = "food_substitutions")
data class FoodSubstitution(
    /** Texte de l'ingrédient normalisé (`Normalize.text`) : « skyr nature ». */
    @PrimaryKey @ColumnInfo(name = "text_key") val textKey: String,
    /** Code Ciqual de l'aliment de la table (`foods.json`). */
    @ColumnInfo(name = "food_code") val foodCode: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FoodSubstitutionDao {
    @Query("SELECT * FROM food_substitutions ORDER BY text_key ASC")
    fun observeAll(): Flow<List<FoodSubstitution>>

    @Query("SELECT * FROM food_substitutions ORDER BY text_key ASC")
    suspend fun getAll(): List<FoodSubstitution>

    /** Un nouveau choix pour le même texte remplace l'ancien. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(substitution: FoodSubstitution)

    @Query("DELETE FROM food_substitutions WHERE text_key = :textKey")
    suspend fun delete(textKey: String)
}
