package com.example.recipelibrary.chronodrive

import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.FavoritesMerge
import com.example.recipelibrary.data.chronodrive.ParsedFavorites
import com.example.recipelibrary.data.chronodrive.ParsedProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fusion d'un import avec les favoris connus (spec 003, research R6) : jamais de suppression. */
class FavoritesMergeTest {

    private fun parsed(vararg ids: Long, ignored: Int = 0) =
        ParsedFavorites(ids.map { ParsedProduct(it, "produit $it", null, "Rayon") }, ignored)

    private fun stored(id: Long, still: Boolean = true, name: String = "produit $id") =
        ChronodriveProduct(id, name, null, "Ancien rayon", still, importedAt = 1)

    @Test fun firstImport_allAdded() {
        val plan = FavoritesMerge.merge(emptyList(), parsed(1, 2, 3), now = 10)
        assertEquals(3, plan.report.added)
        assertEquals("3 produits Chronodrive importés", plan.report.message)
        assertTrue(plan.upserts.all { it.stillFavorite && it.importedAt == 10L })
    }

    @Test fun sameFileAgain_zeroAdded() {
        val plan = FavoritesMerge.merge(listOf(stored(1), stored(2)), parsed(1, 2), now = 10)
        assertEquals("0 ajouté, 2 déjà connus", plan.report.message)
    }

    @Test fun threeNew() {
        val plan = FavoritesMerge.merge(listOf(stored(1), stored(2)), parsed(1, 2, 3, 4, 5), now = 10)
        assertEquals("3 ajoutés, 2 déjà connus", plan.report.message)
    }

    @Test fun knownProduct_updated() {
        val plan = FavoritesMerge.merge(listOf(stored(1, name = "ancien nom")), parsed(1), now = 10)
        val p = plan.upserts.single()
        assertEquals("produit 1", p.name)
        assertEquals("Rayon", p.aisle)
        assertEquals(10L, p.importedAt)
    }

    @Test fun absentProduct_keptButMarkedGone_neverDeleted() {
        val plan = FavoritesMerge.merge(listOf(stored(1), stored(2)), parsed(1), now = 10)
        assertEquals(listOf(2L), plan.markGone)
        assertEquals("0 ajouté, 1 déjà connus, 1 plus en favori", plan.report.message)
        assertTrue(plan.upserts.none { it.productId == 2L })
    }

    @Test fun goneProductComingBack_isFavoriteAgain() {
        val plan = FavoritesMerge.merge(listOf(stored(1, still = false)), parsed(1), now = 10)
        assertTrue(plan.upserts.single().stillFavorite)
        assertTrue(plan.markGone.isEmpty())
    }

    @Test fun alreadyGone_notCountedAgain() {
        val plan = FavoritesMerge.merge(listOf(stored(1), stored(2, still = false)), parsed(1), now = 10)
        assertEquals(0, plan.report.markedGone)
    }

    @Test fun ignoredItems_mentioned() {
        val plan = FavoritesMerge.merge(emptyList(), parsed(1, ignored = 2), now = 10)
        assertEquals("1 produit Chronodrive importé (2 éléments ignorés)", plan.report.message)
    }
}
