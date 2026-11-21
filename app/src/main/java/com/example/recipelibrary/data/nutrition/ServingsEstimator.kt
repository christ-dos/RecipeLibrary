package com.example.recipelibrary.data.nutrition

import kotlin.math.roundToInt

/**
 * Estimation du nombre de parts d'après le poids compté et la catégorie (FR-016, FR-017).
 */
object ServingsEstimator {

    /** Poids habituel d'une part, en grammes, par catégorie (FR-016). */
    val PORTION_G: Map<String, Double> = mapOf(
        "Plats principaux" to 350.0,
        "Pâtes et riz" to 350.0,
        "Soupes" to 300.0,
        "Boissons" to 250.0,
        "Entrées" to 200.0,
        "Salades" to 200.0,
        "Accompagnements" to 200.0,
        "Sandwichs et wraps" to 200.0,
        "Tartes et quiches" to 150.0,
        "Petits-déjeuners" to 150.0,
        "Desserts" to 120.0,
        "Gâteaux" to 100.0,
        "Goûters" to 80.0,
        "Apéritifs" to 50.0,
        "Sauces" to 50.0,
        "Biscuits et petits gâteaux" to 25.0
    )

    /**
     * Parts estimées, ou null : sans catégorie connue, sans poids compté, ou quand plus de la
     * moitié des lignes ne sont pas comptées ([tooManyUncounted], FR-017).
     */
    fun estimate(category: String?, countedGrams: Double, tooManyUncounted: Boolean): Int? {
        if (tooManyUncounted || countedGrams <= 0) return null
        val portion = PORTION_G[com.example.recipelibrary.data.RecipeCategories.match(category)] ?: return null
        return (countedGrams / portion).roundToInt().coerceAtLeast(1)
    }
}
