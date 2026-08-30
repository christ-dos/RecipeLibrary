package com.example.recipelibrary.data

class RecipeRepository(private val dao: RecipeDao) {
    fun observeAll() = dao.getAll()

    suspend fun get(id: String) = dao.getById(id)

    suspend fun upsert(recipe: Recipe) {
        val now = RecipeUtils.nowMillis()
        val r = if (recipe.createdAt == 0L) recipe.copy(createdAt = now, updatedAt = now) else recipe.copy(updatedAt = now)
        val normalized = r.copy(tags = RecipeUtils.normalizeTags(r.tags))
        dao.upsert(normalized)
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}
