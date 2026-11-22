package com.example.recipelibrary.data.menu

import com.example.recipelibrary.data.Recipe

/** Accès au menu, aux coches et aux articles ajoutés à la main (spec 002). */
class MenuRepository(private val dao: MenuDao) {

    fun observeEntries() = dao.observeEntries()
    fun observeCheckedKeys() = dao.observeCheckedKeys()
    fun observeExtras() = dao.observeExtras()

    /** Ajoute la recette en fin de menu, avec les parts par défaut de [MenuLogic.defaultServings]. */
    suspend fun add(recipe: Recipe, defaultServings: Int?) =
        dao.appendEntry(MenuEntry(recipeId = recipe.id, servings = defaultServings))

    suspend fun setServings(id: String, servings: Int?) {
        if (servings == null || servings in Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS) dao.setServings(id, servings)
    }

    suspend fun setTimes(id: String, times: Int) {
        if (times in MenuEntry.MIN_TIMES..MenuEntry.MAX_TIMES) dao.setTimes(id, times)
    }

    suspend fun setDay(id: String, day: Int?) {
        if (day == null || day in 1..7) dao.setDay(id, day)
    }

    suspend fun remove(id: String) = dao.deleteEntry(id)
    suspend fun clear() = dao.clearMenu()

    suspend fun check(key: String) = dao.check(key)
    suspend fun uncheck(key: String) = dao.uncheck(key)
    /** Efface les coches de plus de 3 jours (évite un « j'en ai » périmé). */
    suspend fun expireChecks(now: Long = System.currentTimeMillis()) = dao.deleteChecksBefore(MenuLogic.checkCutoff(now))

    /** Ajoute un article à la main ; texte vide ignoré, coupé à 200 caractères. */
    suspend fun addExtra(text: String) {
        val t = text.trim().take(ShoppingExtra.MAX_LENGTH)
        if (t.isNotEmpty()) dao.appendExtra(ShoppingExtra(text = t))
    }

    suspend fun setExtraText(id: String, text: String) {
        val t = text.trim().take(ShoppingExtra.MAX_LENGTH)
        if (t.isNotEmpty()) dao.setExtraText(id, t)
    }

    suspend fun setExtraChecked(id: String, checked: Boolean) = dao.setExtraChecked(id, checked)
    suspend fun removeExtra(id: String) = dao.deleteExtra(id)
}
