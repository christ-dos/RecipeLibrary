package com.example.recipelibrary.data.chronodrive

import com.example.recipelibrary.data.nutrition.Normalize

/** Ce que fait « Chrono » pour un article (contracts/chrono-link.md). */
sealed interface ChronodriveResolution {
    /** Un choix existe déjà : ouvrir sa fiche. */
    data class Retained(val product: ChronodriveProduct) : ChronodriveResolution
    /** Un seul favori correspond : il devient le choix. */
    data class Single(val product: ChronodriveProduct) : ChronodriveResolution
    /** Plusieurs favoris correspondent : l'utilisatrice choisit (jamais de choix automatique). */
    data class Several(val candidates: List<ChronodriveProduct>) : ChronodriveResolution
    /** Aucun favori : comportement de la feature 002. */
    object None : ChronodriveResolution
}

/**
 * Correspondance article → favoris (FR-002, research R3) : chaque mot significatif du terme doit
 * être un mot entier du nom du produit (sans la marque), après normalisation (accents, pluriels).
 */
object ProductMatcher {

    private val STOP = setOf("de", "du", "des", "la", "le", "les", "un", "une", "et", "au", "aux", "en", "a", "d", "l")

    fun significantWords(text: String): Set<String> =
        Normalize.words(text).filter { it.length > 1 && it !in STOP }.toSet()

    fun candidates(term: String, products: List<ChronodriveProduct>): List<ChronodriveProduct> {
        val wanted = significantWords(term)
        if (wanted.isEmpty()) return emptyList()
        return products
            .filter { Normalize.words(it.name).toSet().containsAll(wanted) }
            .sortedWith(compareBy({ it.name }, { it.productId }))
    }

    /**
     * Résolution de « Chrono ». Un choix dont le produit n'existe plus dans les favoris importés
     * est ignoré (sans être supprimé) ; un produit qui n'est plus en favori reste valable s'il a
     * été choisi, mais n'est plus proposé comme candidat.
     */
    fun resolve(choice: ProductChoice?, term: String, products: List<ChronodriveProduct>): ChronodriveResolution {
        if (choice != null) {
            products.firstOrNull { it.productId == choice.productId }?.let { return ChronodriveResolution.Retained(it) }
        }
        val found = candidates(term, products.filter { it.stillFavorite })
        return when (found.size) {
            0 -> ChronodriveResolution.None
            1 -> ChronodriveResolution.Single(found.single())
            else -> ChronodriveResolution.Several(found)
        }
    }
}
