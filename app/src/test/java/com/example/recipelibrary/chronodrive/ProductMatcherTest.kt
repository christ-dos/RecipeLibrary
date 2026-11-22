package com.example.recipelibrary.chronodrive

import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveResolution
import com.example.recipelibrary.data.chronodrive.FavoritesFileParser
import com.example.recipelibrary.data.chronodrive.ProductChoice
import com.example.recipelibrary.data.chronodrive.ProductMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Correspondance article → favoris et résolution de « Chrono » (spec 003, FR-002, FR-004, research R3). */
class ProductMatcherTest {

    private fun p(id: Long, name: String, brand: String? = null, still: Boolean = true) =
        ChronodriveProduct(productId = id, name = name, brand = brand, aisle = "A", stillFavorite = still, importedAt = 0)

    private val real: List<ChronodriveProduct> by lazy {
        FavoritesFileParser.parse(File("src/test/resources/chronodrive/favoris-releve.txt").readText(Charsets.UTF_8))!!
            .products.map { ChronodriveProduct(it.productId, it.name, it.brand, it.aisle, true, 0) }
    }

    private fun ids(term: String, products: List<ChronodriveProduct>) = ProductMatcher.candidates(term, products).map { it.productId }

    @Test fun wholeWordsOnly_eauIsNotGateau() {
        assertEquals(listOf(1L), ids("eau", listOf(p(1, "eau de source"), p(2, "gateau au chocolat"))))
    }

    @Test fun pluralsAccentsAndCase() {
        assertEquals(listOf(1L), ids("oeuf", listOf(p(1, "oeufs de poules"))))
        assertEquals(listOf(1L), ids("Crème fraîche", listOf(p(1, "creme fraiche epaisse legere"))))
        assertEquals(listOf(1L), ids("bananes", listOf(p(1, "banane"))))
    }

    @Test fun everySignificantWordRequired_stopWordsIgnored() {
        val ps = listOf(p(1, "lait demi ecreme"), p(2, "lait de coco"), p(3, "yaourt"))
        assertEquals(listOf(2L), ids("lait de coco", ps))
        // triés par nom : « lait de coco » avant « lait demi ecreme »
        assertEquals(listOf(2L, 1L), ids("du lait", ps))
        assertTrue(ids("de la", ps).isEmpty())
    }

    @Test fun brandNeverMatches() {
        assertTrue(ids("auchan", listOf(p(1, "lentilles blondes", brand = "auchan"))).isEmpty())
    }

    @Test fun sortedByNameThenNumber() {
        assertEquals(listOf(305254L, 365920L), ids("banane", listOf(p(365920, "banane"), p(305254, "banane"))))
    }

    @Test fun realFavorites() {
        assertEquals(listOf(305254L, 365920L), ids("banane", real))
        assertEquals(listOf(3416L), ids("ciboulette", real))
        // « papier cuisson » : deux favoris (429125, 503483), pas les cotons-tiges « en papier »
        assertEquals(listOf(429125L, 503483L), ids("papier cuisson", real).sorted())
        assertTrue(ids("papier toilette", real).isEmpty())
        assertTrue(ids("caviar", real).isEmpty())
    }

    // ── Résolution ─────────────────────────────────────────────────────────

    private val two = listOf(p(365920, "banane"), p(305254, "banane"), p(3416, "ciboulette", "daregal"))

    @Test fun existingChoice_isRetained_evenIfNoLongerFavorite() {
        val ps = listOf(p(365920, "banane", still = false), p(305254, "banane"))
        val r = ProductMatcher.resolve(ProductChoice("food:13005", 365920, 0), "banane", ps)
        assertEquals(ChronodriveResolution.Retained(ps[0]), r)
    }

    @Test fun oneCandidate_single() {
        assertEquals(ChronodriveResolution.Single(two[2]), ProductMatcher.resolve(null, "ciboulette", two))
    }

    @Test fun severalCandidates_neverChosenAutomatically() {
        val r = ProductMatcher.resolve(null, "bananes", two)
        assertEquals(listOf(305254L, 365920L), (r as ChronodriveResolution.Several).candidates.map { it.productId })
    }

    @Test fun noCandidateOrNoFavorites_none() {
        assertEquals(ChronodriveResolution.None, ProductMatcher.resolve(null, "caviar", two))
        assertEquals(ChronodriveResolution.None, ProductMatcher.resolve(null, "banane", emptyList()))
    }

    @Test fun choiceOfUnknownProduct_ignored_resolutionContinues() {
        val r = ProductMatcher.resolve(ProductChoice("txt:ciboulette", 999, 0), "ciboulette", two)
        assertEquals(ChronodriveResolution.Single(two[2]), r)
    }

    @Test fun noLongerFavorite_notProposedAsCandidate() {
        val ps = listOf(p(365920, "banane", still = false), p(305254, "banane"))
        assertEquals(ChronodriveResolution.Single(ps[1]), ProductMatcher.resolve(null, "banane", ps))
    }
}
