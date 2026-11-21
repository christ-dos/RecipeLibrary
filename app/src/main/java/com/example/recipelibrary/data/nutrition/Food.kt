package com.example.recipelibrary.data.nutrition

/** Catégorie de traitement d'un aliment de référence. */
enum class FoodKind {
    /** Compté normalement. */
    NORMAL,
    /** Sel, poivre, épices, herbes : ignoré sans avertissement. */
    SEASONING,
    /** Huile de friture : comptée mais signalée approximative. */
    FRYING
}

/** Aliment de référence (table Ciqual réduite), valeurs pour 100 g. */
data class Food(
    val code: String,
    val name: String,
    val aliases: List<String>,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val pieceG: Double? = null,
    val density: Double? = null,
    val kind: FoodKind = FoodKind.NORMAL
) {
    /** Masse volumique en g/ml (1,0 par défaut). */
    val densityOrDefault: Double get() = density ?: 1.0
}
