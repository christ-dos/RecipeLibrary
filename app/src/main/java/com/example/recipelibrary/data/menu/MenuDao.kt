package com.example.recipelibrary.data.menu

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    // ── Entrées du menu ───────────────────────────────────────────────────

    @Query("SELECT * FROM menu_entries ORDER BY position ASC, created_at ASC")
    fun observeEntries(): Flow<List<MenuEntry>>

    @Query("SELECT * FROM menu_entries ORDER BY position ASC, created_at ASC")
    suspend fun entries(): List<MenuEntry>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM menu_entries")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insertEntry(entry: MenuEntry)

    /** Ajoute l'entrée en fin de menu. */
    @Transaction
    suspend fun appendEntry(entry: MenuEntry) = insertEntry(entry.copy(position = nextPosition()))

    @Query("UPDATE menu_entries SET servings = :servings WHERE id = :id")
    suspend fun setServings(id: String, servings: Int?)

    @Query("UPDATE menu_entries SET times = :times WHERE id = :id")
    suspend fun setTimes(id: String, times: Int)

    @Query("UPDATE menu_entries SET day = :day WHERE id = :id")
    suspend fun setDay(id: String, day: Int?)

    @Query("DELETE FROM menu_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("SELECT COUNT(*) FROM menu_entries WHERE recipe_id = :recipeId")
    suspend fun countForRecipe(recipeId: String): Int

    @Query("DELETE FROM menu_entries WHERE recipe_id = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)

    @Query("DELETE FROM menu_entries")
    suspend fun deleteAllEntries()

    /** Vide le menu et ses coches ; les articles ajoutés à la main restent. */
    @Transaction
    suspend fun clearMenu() {
        deleteAllEntries()
        deleteAllChecks()
    }

    // ── Coches des articles calculés ──────────────────────────────────────

    @Query("SELECT item_key FROM shopping_checks")
    fun observeCheckedKeys(): Flow<List<String>>

    @Query("INSERT OR REPLACE INTO shopping_checks (item_key, checked_at) VALUES (:key, :at)")
    suspend fun check(key: String, at: Long = System.currentTimeMillis())

    /** Coches plus anciennes que [before] (ms) : effacées, l'article revient dans la liste. */
    @Query("DELETE FROM shopping_checks WHERE checked_at < :before")
    suspend fun deleteChecksBefore(before: Long)

    @Query("DELETE FROM shopping_checks WHERE item_key = :key")
    suspend fun uncheck(key: String)

    @Query("DELETE FROM shopping_checks")
    suspend fun deleteAllChecks()

    // ── Articles ajoutés à la main ────────────────────────────────────────

    @Query("SELECT * FROM shopping_extras ORDER BY position ASC, created_at ASC")
    fun observeExtras(): Flow<List<ShoppingExtra>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM shopping_extras")
    suspend fun nextExtraPosition(): Int

    @Insert
    suspend fun insertExtra(extra: ShoppingExtra)

    @Transaction
    suspend fun appendExtra(extra: ShoppingExtra) = insertExtra(extra.copy(position = nextExtraPosition()))

    @Query("UPDATE shopping_extras SET text = :text WHERE id = :id")
    suspend fun setExtraText(id: String, text: String)

    @Query("UPDATE shopping_extras SET checked = :checked WHERE id = :id")
    suspend fun setExtraChecked(id: String, checked: Boolean)

    @Query("DELETE FROM shopping_extras WHERE id = :id")
    suspend fun deleteExtra(id: String)
}
