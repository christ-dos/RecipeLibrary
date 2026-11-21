package com.example.recipelibrary.data

object RecipeCategories {
    val ALL = listOf(
        "Apéritifs", "Biscuits et petits gâteaux", "Boissons",
        "Accompagnements", "Desserts", "Entrées", "Gâteaux", "Goûters",
        "Pâtes et riz", "Petits-déjeuners", "Plats principaux", "Salades",
        "Sandwichs et wraps", "Sauces", "Soupes", "Tartes et quiches"
    )

    /** Forme au singulier, mot par mot : "Plats principaux" -> "plat principal", "Gâteaux" -> "gâteau". */
    private fun key(value: String): String =
        value.trim().lowercase().split(Regex("\\s+")).joinToString(" ") { w ->
            when {
                w.endsWith("eaux") -> w.dropLast(1)
                w.endsWith("aux") -> w.dropLast(3) + "al"
                w.endsWith("s") || w.endsWith("x") -> w.dropLast(1)
                else -> w
            }
        }

    /**
     * Ramène une catégorie proposée par Hermes vers une catégorie de l'app
     * (casse et pluriel ignorés : "dessert" -> "Desserts"), ou null si elle n'existe pas.
     */
    fun match(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val k = key(raw)
        return ALL.firstOrNull { key(it) == k }
    }

    /** Recette importée : garde la catégorie si elle est connue, sinon la vide pour la faire choisir. */
    fun keepKnown(recipe: Recipe): Recipe = recipe.copy(category = match(recipe.category))
}
