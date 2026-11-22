package com.example.recipelibrary.data



import androidx.room.withTransaction

/**
 * @param db base complète : la suppression d'une recette retire aussi ses entrées du menu dans
 *   la même transaction (spec 002, FR-005). Null dans les tests qui n'utilisent que [dao].
 */
class RecipeRepository(private val dao: RecipeDao, private val db: AppDatabase? = null) {
    fun observeAll() = dao.getAll()

    suspend fun get(id: String) = dao.getById(id)

    suspend fun upsert(recipe: Recipe) {
        val now = RecipeUtils.nowMillis()
        val r = if (recipe.createdAt == 0L) recipe.copy(createdAt = now, updatedAt = now) else recipe.copy(updatedAt = now)
        val normalized = r.copy(tags = RecipeUtils.normalizeTags(r.tags))
        dao.upsert(normalized)
    }

    suspend fun delete(id: String) {
        val database = db ?: return dao.deleteById(id)
        database.withTransaction {
            database.menuDao().deleteForRecipe(id)
            dao.deleteById(id)
        }
    }

    /** Nombre d'entrées du menu qui visent cette recette (avertissement avant suppression). */
    suspend fun menuCount(id: String): Int = db?.menuDao()?.countForRecipe(id) ?: 0
}
