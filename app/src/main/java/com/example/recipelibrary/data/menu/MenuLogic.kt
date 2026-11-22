package com.example.recipelibrary.data.menu

import com.example.recipelibrary.data.Recipe

/** Groupe d'affichage du menu : un jour (1 = lundi … 7 = dimanche) ou « Sans jour » (null). */
data class MenuDayGroup(val day: Int?, val entries: List<MenuEntry>)

/** Règles pures du menu (spec 002, T010). */
object MenuLogic {

    /** Une coche « déjà à la maison ou dans le panier » s'efface au bout de 3 jours (décision du 2026-11-22). */
    const val CHECK_TTL_MS = 3L * 24 * 60 * 60 * 1000

    /** Les coches posées avant cette date sont expirées. */
    fun checkCutoff(now: Long): Long = now - CHECK_TTL_MS

    val DAY_NAMES = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

    fun dayName(day: Int?): String = day?.let { DAY_NAMES.getOrNull(it - 1) } ?: "Sans jour"

    fun isValidServings(n: Int?): Boolean = n != null && n in Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS
    fun isValidTimes(n: Int): Boolean = n in MenuEntry.MIN_TIMES..MenuEntry.MAX_TIMES

    /**
     * Parts proposées à l'ajout : parts indiquées de la recette, sinon [estimated]
     * (estimation du calcul des calories), sinon null (l'entrée est alors réglée par `times`).
     */
    fun defaultServings(recipe: Recipe, estimated: Int?): Int? =
        recipe.servings?.takeIf { isValidServings(it) } ?: estimated?.takeIf { isValidServings(it) }

    /**
     * Facteur appliqué aux quantités de la recette : parts prévues ÷ parts de la recette
     * quand les deux sont connus, sinon le nombre de fois.
     */
    fun factor(entry: MenuEntry, recipeServings: Int?): Double {
        val s = entry.servings
        return if (s != null && recipeServings != null && recipeServings > 0) s.toDouble() / recipeServings
        else entry.times.coerceIn(MenuEntry.MIN_TIMES, MenuEntry.MAX_TIMES).toDouble()
    }

    /** Groupes lundi → dimanche puis « Sans jour » ; groupes vides omis ; ordre par position. */
    fun groupByDay(entries: List<MenuEntry>): List<MenuDayGroup> {
        val sorted = entries.sortedWith(compareBy({ it.position }, { it.createdAt }))
        val days = (1..7).mapNotNull { d -> sorted.filter { it.day == d }.takeIf { it.isNotEmpty() }?.let { MenuDayGroup(d, it) } }
        val none = sorted.filter { it.day == null || it.day !in 1..7 }
        return if (none.isEmpty()) days else days + MenuDayGroup(null, none)
    }
}
