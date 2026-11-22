package com.example.recipelibrary.data.shopping

/** Rayons de la liste de courses (spec 002, story 4, research R5), dans l'ordre d'affichage. */
object Aisles {
    const val OTHER = "Autres"
    val ORDER = listOf(
        "Fruits et légumes", "Crèmerie", "Boucherie et poisson", "Épicerie salée",
        "Épicerie sucrée", "Surgelés", "Boissons", OTHER
    )

    /** Rayon connu, sinon « Autres » (aliment non reconnu, rayon absent ou inconnu). */
    fun of(aisle: String?): String = aisle?.takeIf { it in ORDER } ?: OTHER

    /**
     * Articles regroupés par rayon dans l'ordre fixe, rayons vides omis ; l'ordre des articles
     * est conservé dans chaque rayon.
     */
    fun group(items: List<ShoppingItem>): List<Pair<String, List<ShoppingItem>>> {
        val byAisle = items.groupBy { of(it.aisle) }
        return ORDER.mapNotNull { a -> byAisle[a]?.let { a to it } }
    }

    /** Titres affichés seulement si au moins un article est rangé ailleurs que dans « Autres ». */
    fun showHeaders(items: List<ShoppingItem>): Boolean = items.any { of(it.aisle) != OTHER }
}
