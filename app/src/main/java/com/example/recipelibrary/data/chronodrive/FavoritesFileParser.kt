package com.example.recipelibrary.data.chronodrive

/** Produit lu dans le fichier, avant fusion avec la base. */
data class ParsedProduct(val productId: Long, val name: String, val brand: String?, val aisle: String)

/** Résultat de la lecture : produits valides (premier numéro gagnant) et éléments ignorés. */
data class ParsedFavorites(val products: List<ParsedProduct>, val ignored: Int)

/**
 * Lecture du fichier de favoris Chronodrive (contracts/favorites-file.md), sans Android.
 * Renvoie null si le fichier ne contient aucun produit valide : rien ne doit alors être modifié.
 */
object FavoritesFileParser {

    const val DEFAULT_AISLE = "Autre"
    /** Garde-fou : un fichier plus gros est refusé comme invalide. */
    const val MAX_BYTES = 1_000_000

    private val PRODUCT = Regex("^(\\d+)\\s+([a-z0-9-]+)$")
    private val TRAILING_PARENS = Regex("\\s*\\([^()]*\\)\\s*$")
    private val SPACES = Regex("\\s+")

    fun parse(text: String): ParsedFavorites? {
        var aisle = DEFAULT_AISLE
        val seen = LinkedHashMap<Long, ParsedProduct>()
        var ignored = 0
        for (raw in text.removePrefix("﻿").lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("## ") -> aisle = aisleName(line.removePrefix("## "))
                line.startsWith("#") -> Unit   // commentaire
                else -> for (part in line.split(';')) {
                    val item = part.trim()
                    if (item.isEmpty()) continue
                    val p = product(item, aisle)
                    if (p == null) ignored++ else seen.putIfAbsent(p.productId, p)
                }
            }
        }
        return if (seen.isEmpty()) null else ParsedFavorites(seen.values.toList(), ignored)
    }

    /** Nom du rayon : texte avant la parenthèse finale (« Produits laitiers & Oeufs (20 sur 21) »). */
    fun aisleName(title: String): String = TRAILING_PARENS.replace(title, "").trim().ifEmpty { DEFAULT_AISLE }

    /** `<numéro> <slug>` → produit, ou null si mal formé (numéro non positif, slug invalide). */
    fun product(item: String, aisle: String): ParsedProduct? {
        val m = PRODUCT.matchEntire(item.trim()) ?: return null
        val id = m.groupValues[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val (brand, name) = brandAndName(m.groupValues[2])
        if (name.isEmpty()) return null
        return ParsedProduct(id, name, brand, aisle)
    }

    /** Slug → (marque, nom) : marque avant le premier `--` ; `---` → « - », `-` → espace. */
    fun brandAndName(slug: String): Pair<String?, String> {
        val i = slug.indexOf("--")
        val brand = if (i > 0) tidy(slug.substring(0, i).replace('-', ' ')) else null
        val rest = if (i > 0) slug.substring(i + 2) else slug
        val name = tidy(rest.replace("---", " - ").replace(Regex("-(?! )|(?<! )-"), " "))
        return brand?.ifEmpty { null } to name
    }

    private fun tidy(s: String) = SPACES.replace(s, " ").trim()
}
