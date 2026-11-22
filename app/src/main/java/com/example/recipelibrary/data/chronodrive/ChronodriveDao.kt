package com.example.recipelibrary.data.chronodrive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Favoris Chronodrive et choix d'articles (spec 003). Aucune requête de suppression sur les
 * produits (un import ne supprime jamais rien) ; aucune requête sur les tables existantes.
 */
@Dao
interface ChronodriveDao {

    // ── Produits ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM chronodrive_products ORDER BY aisle, name, product_id")
    fun observeProducts(): Flow<List<ChronodriveProduct>>

    @Query("SELECT * FROM chronodrive_products")
    suspend fun products(): List<ChronodriveProduct>

    @Query("SELECT * FROM chronodrive_products WHERE product_id = :id")
    suspend fun getById(id: Long): ChronodriveProduct?

    @Upsert
    suspend fun upsertProducts(products: List<ChronodriveProduct>)

    @Query("UPDATE chronodrive_products SET still_favorite = 0 WHERE product_id IN (:ids)")
    suspend fun markGone(ids: List<Long>)

    // ── Choix ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM product_choices ORDER BY item_key")
    fun observeChoices(): Flow<List<ProductChoice>>

    @Query("SELECT * FROM product_choices WHERE item_key = :itemKey")
    suspend fun getChoice(itemKey: String): ProductChoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setChoice(choice: ProductChoice)

    @Query("DELETE FROM product_choices WHERE item_key = :itemKey")
    suspend fun forgetChoice(itemKey: String)

    @Query("SELECT COUNT(*) FROM product_choices WHERE product_id = :productId")
    suspend fun countForProduct(productId: Long): Int
}
