package com.example.recipelibrary.data.chronodrive

import androidx.room.withTransaction
import com.example.recipelibrary.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Un choix avec son produit (null si le numéro n'est plus dans les favoris importés). */
data class ChoiceWithProduct(val choice: ProductChoice, val product: ChronodriveProduct?)

/** Résultat d'un import : compte rendu, ou null si le fichier ne contenait aucun favori reconnu. */
sealed interface ImportResult {
    data class Done(val report: ImportReport) : ImportResult
    object Invalid : ImportResult
}

/** Favoris Chronodrive et choix (spec 003). Ne touche aucune table existante. */
class ChronodriveRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {

    private val dao = db.chronodriveDao()

    val products: Flow<List<ChronodriveProduct>> = dao.observeProducts()

    val choices: Flow<List<ChoiceWithProduct>> = combine(dao.observeChoices(), dao.observeProducts()) { cs, ps ->
        val byId = ps.associateBy { it.productId }
        cs.map { ChoiceWithProduct(it, byId[it.productId]) }
    }

    /** Analyse complète avant toute écriture ; écriture en une seule transaction. */
    suspend fun importFavorites(text: String): ImportResult {
        val parsed = FavoritesFileParser.parse(text) ?: return ImportResult.Invalid
        return db.withTransaction {
            val plan = FavoritesMerge.merge(dao.products(), parsed, clock())
            dao.upsertProducts(plan.upserts)
            if (plan.markGone.isNotEmpty()) dao.markGone(plan.markGone)
            ImportResult.Done(plan.report)
        }
    }

    /** Résolution de « Chrono » ; un candidat unique devient le choix avant de répondre. */
    suspend fun resolveChrono(itemKey: String, term: String): ChronodriveResolution {
        val resolution = ProductMatcher.resolve(dao.getChoice(itemKey), term, dao.products())
        if (resolution is ChronodriveResolution.Single) setChoice(itemKey, resolution.product.productId)
        return resolution
    }

    suspend fun setChoice(itemKey: String, productId: Long) =
        dao.setChoice(ProductChoice(itemKey = itemKey, productId = productId, chosenAt = clock()))

    suspend fun forgetChoice(itemKey: String) = dao.forgetChoice(itemKey)
}
