package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.Food
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.data.nutrition.ServingsEstimator
import com.example.recipelibrary.data.nutrition.ServingsOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServingsEstimatorTest {

    @Test
    fun everyCategoryOfFr016() {
        val expected = mapOf(
            "Plats principaux" to 350, "Pâtes et riz" to 350, "Soupes" to 300, "Boissons" to 250,
            "Entrées" to 200, "Salades" to 200, "Accompagnements" to 200, "Sandwichs et wraps" to 200,
            "Tartes et quiches" to 150, "Petits-déjeuners" to 150, "Desserts" to 120, "Gâteaux" to 100,
            "Goûters" to 80, "Apéritifs" to 50, "Sauces" to 50, "Biscuits et petits gâteaux" to 25
        )
        assertEquals(16, ServingsEstimator.PORTION_G.size)
        for ((cat, g) in expected) {
            assertEquals(cat, g.toDouble(), ServingsEstimator.PORTION_G[cat]!!, 0.0)
            assertEquals("$cat : 4 portions", 4, ServingsEstimator.estimate(cat, g * 4.0, false))
        }
    }

    @Test fun cake600gGives6() = assertEquals(6, ServingsEstimator.estimate("Gâteaux", 600.0, false))

    @Test fun roundsToNearest() {
        assertEquals(6, ServingsEstimator.estimate("Gâteaux", 649.0, false))
        assertEquals(7, ServingsEstimator.estimate("Gâteaux", 650.0, false))
    }

    @Test fun minimumOne() = assertEquals(1, ServingsEstimator.estimate("Plats principaux", 20.0, false))
    @Test fun categoryVariantsAccepted() = assertEquals(6, ServingsEstimator.estimate("gâteau", 600.0, false))
    @Test fun noCategory() = assertNull(ServingsEstimator.estimate(null, 600.0, false))
    @Test fun unknownCategory() = assertNull(ServingsEstimator.estimate("Divers", 600.0, false))
    @Test fun tooManyUncounted() = assertNull(ServingsEstimator.estimate("Gâteaux", 600.0, true))
    @Test fun noWeight() = assertNull(ServingsEstimator.estimate("Gâteaux", 0.0, false))

    // ── Priorité USER > SOURCE > estimé, et règle des lignes non comptées (FR-017) ──

    private val table = FoodTable(listOf(
        Food("1", "Farine", listOf("farine"), 350.0, 10.0, 70.0, 1.0),
        Food("2", "Sucre", listOf("sucre"), 400.0, 0.0, 100.0, 0.0),
        Food("3", "Beurre", listOf("beurre"), 750.0, 1.0, 1.0, 82.0),
        Food("4", "Sel", listOf("sel"), 0.0, 0.0, 0.0, 0.0, kind = com.example.recipelibrary.data.nutrition.FoodKind.SEASONING)
    ))
    private val calc = NutritionCalculator(table)
    private val cake = Recipe(id = "g", title = "Gâteau", category = "Gâteaux", ingredients = listOf("400 g de farine", "200 g de sucre"))

    @Test fun estimatedWhenNoServings() {
        val e = calc.estimate(cake)
        assertEquals(6, e.servings)
        assertEquals(ServingsOrigin.ESTIMATED, e.servingsOrigin)
    }

    @Test fun sourceBeatsEstimate() {
        val e = calc.estimate(cake.copy(servings = 10, servingsSource = Recipe.SERVINGS_SOURCE))
        assertEquals(10, e.servings)
        assertEquals(ServingsOrigin.SOURCE, e.servingsOrigin)
    }

    @Test fun userBeatsEverything() {
        val e = calc.estimate(cake.copy(servings = 3, servingsSource = Recipe.SERVINGS_USER))
        assertEquals(3, e.servings)
        assertEquals(ServingsOrigin.USER, e.servingsOrigin)
    }

    @Test fun noEstimateWithoutCategory() {
        val e = calc.estimate(cake.copy(category = null))
        assertNull(e.servings)
        assertNull(e.servingsOrigin)
    }

    @Test fun noEstimateWhenMoreThanHalfLinesNotCounted() {
        // 1 ligne comptée sur 3 (le sel est ignoré, hors décompte) → pas d'estimation
        val e = calc.estimate(cake.copy(ingredients = listOf("600 g de farine", "1 noix de beurre", "xyzzy", "sel")))
        assertNull(e.servings)
    }

    @Test fun estimateWhenExactlyHalfNotCounted() {
        val e = calc.estimate(cake.copy(ingredients = listOf("600 g de farine", "1 noix de beurre")))
        assertEquals(6, e.servings)
    }
}
