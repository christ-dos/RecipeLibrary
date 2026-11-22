package com.example.recipelibrary.data.nutrition

/**
 * Aliments de remplacement (story 6) : règles pures autour du choix d'un aliment pour un
 * ingrédient non reconnu. Le choix est retenu par texte normalisé et vaut pour toutes les recettes.
 */
object FoodSubstitutions {

    /** Nombre maximal de résultats de recherche affichés. */
    const val MAX_RESULTS = 50

    /** Texte proposé pour retenir le choix : le texte de l'aliment, normalisé (« Skyr nature » → « skyr nature »). */
    fun defaultKey(parsed: ParsedIngredient): String =
        Normalize.text(parsed.foodText).ifEmpty { Normalize.text(parsed.line) }

    /**
     * Texte retenu valide pour [line] : au moins un mot, et le remplacement s'appliquerait bien à
     * cette ligne (mêmes règles que le calcul). Rend le texte normalisé, ou null.
     */
    fun normalizedKey(key: String, line: String, table: FoodTable): String? {
        val k = Normalize.text(key)
        if (k.isEmpty()) return null
        val anyCode = table.foods.firstOrNull()?.code ?: return null
        val trial = IngredientParser(table.withSubstitutions(mapOf(k to anyCode))).parse(line)
        return if (trial.substitutionKey == k) k else null
    }

    /**
     * Recherche dans la table : chaque mot saisi doit commencer un mot du nom ou d'une variante.
     * Tri : nom commençant par la recherche d'abord, puis ordre alphabétique.
     */
    fun search(table: FoodTable, query: String, limit: Int = MAX_RESULTS): List<Food> {
        val q = Normalize.words(query)
        if (q.isEmpty()) return emptyList()
        val first = Normalize.text(query)
        return table.foods
            .filter { f ->
                val words = (Normalize.words(f.name) + f.aliases.flatMap { Normalize.words(it) }).toSet()
                q.all { w -> words.any { it.startsWith(w) } }
            }
            .sortedWith(compareBy<Food>({ !Normalize.text(it.name).startsWith(first) }, { Normalize.plain(it.name) }))
            .take(limit)
    }
}
