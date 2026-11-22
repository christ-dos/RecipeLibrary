package com.example.recipelibrary.data.chronodrive

/** Compte rendu d'un import (contracts/favorites-file.md). */
data class ImportReport(val added: Int, val known: Int, val markedGone: Int, val ignored: Int, val firstImport: Boolean) {
    val message: String
        get() = buildString {
            if (firstImport) append(if (added == 1) "1 produit Chronodrive importé" else "$added produits Chronodrive importés")
            else append("$added ${if (added > 1) "ajoutés" else "ajouté"}, $known déjà connus")
            if (markedGone > 0) append(", $markedGone plus en favori")
            if (ignored > 0) append(" ($ignored ${if (ignored > 1) "éléments ignorés" else "élément ignoré"})")
        }
}

/** Écritures à faire en une transaction. */
data class MergePlan(val upserts: List<ChronodriveProduct>, val markGone: List<Long>, val report: ImportReport)

/**
 * Fusion d'un fichier avec les favoris connus (research R6) : nouveau → ajouté ; connu → nom,
 * marque, rayon et date mis à jour, de nouveau en favori ; absent du fichier → conservé, marqué
 * « plus en favori ». Jamais de suppression.
 */
object FavoritesMerge {

    fun merge(existing: List<ChronodriveProduct>, parsed: ParsedFavorites, now: Long): MergePlan {
        val known = existing.associateBy { it.productId }
        val inFile = parsed.products.map { it.productId }.toSet()
        val upserts = parsed.products.map { p ->
            ChronodriveProduct(productId = p.productId, name = p.name, brand = p.brand, aisle = p.aisle,
                stillFavorite = true, importedAt = now)
        }
        val added = parsed.products.count { it.productId !in known }
        val gone = existing.filter { it.stillFavorite && it.productId !in inFile }.map { it.productId }
        return MergePlan(
            upserts = upserts,
            markGone = gone,
            report = ImportReport(
                added = added, known = parsed.products.size - added, markedGone = gone.size,
                ignored = parsed.ignored, firstImport = existing.isEmpty()
            )
        )
    }
}
