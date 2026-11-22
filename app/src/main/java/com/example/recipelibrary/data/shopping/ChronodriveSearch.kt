package com.example.recipelibrary.data.shopping

import java.net.URLEncoder

/** Terme et adresse de secours du bouton « Chronodrive » (contracts/chronodrive-search.md). */
object ChronodriveSearch {

    const val PACKAGE = "com.ikomobi.chronodrive"
    /** « Mon espace favoris » de l'application (vérifié sur le téléphone le 2026-11-22). */
    const val FAVORITES_URL = "chronodrive://favorites"
    private const val MAX = 60

    /** Terme d'un article calculé : déjà sans quantité (premier alias ou texte de l'aliment). */
    fun term(item: ShoppingItem): String = clean(item.searchTerm)

    /** Terme d'un article ajouté à la main : son texte sans quantité de tête (« 2 papier toilette » → « papier toilette »). */
    fun termForExtra(text: String): String = clean(LEADING_QTY.replace(text.trim(), "")).ifEmpty { clean(text) }

    /** Adresse de recherche du site, ouverte dans le navigateur seulement si l'application n'est pas installée. */
    fun searchUrl(term: String): String =
        "https://www.chronodrive.com/search/" + URLEncoder.encode(term, "UTF-8").replace("+", "%20")

    private fun clean(s: String): String = SPACES.replace(s, " ").trim().take(MAX).trim()

    private val SPACES = Regex("\\s+")
    private val LEADING_QTY = Regex("^\\d+(?:[.,]\\d+)?\\s*(?:x\\s+)?")
}
