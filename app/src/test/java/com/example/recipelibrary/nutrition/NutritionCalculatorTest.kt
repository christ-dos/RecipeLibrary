package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.Food
import com.example.recipelibrary.data.nutrition.FoodKind
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.GramsOrigin
import com.example.recipelibrary.data.nutrition.IngredientIssue
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.data.nutrition.NutritionWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    // Table simple aux valeurs rondes, pour des calculs vérifiables à la main
    private val table = FoodTable(listOf(
        Food("F", "Farine", listOf("farine"), 350.0, 10.0, 70.0, 1.0, density = 0.5),
        Food("B", "Beurre", listOf("beurre"), 750.0, 1.0, 1.0, 82.0),
        Food("P", "Pomme", listOf("pomme"), 50.0, 0.5, 12.0, 0.2, pieceG = 150.0),
        Food("O", "Œuf", listOf("oeuf"), 140.0, 12.0, 1.0, 10.0, pieceG = 50.0),
        Food("S", "Sel", listOf("sel"), 0.0, 0.0, 0.0, 0.0, kind = FoodKind.SEASONING),
        Food("H", "Huile de friture", listOf("huile de friture"), 900.0, 0.0, 0.0, 100.0, density = 1.0, kind = FoodKind.FRYING)
    ))
    private val calc = NutritionCalculator(table)

    private fun recipe(vararg lines: String, servings: Int? = 4, weights: Map<String, IngredientWeight> = emptyMap(), category: String? = null) =
        Recipe(id = "r", title = "R", category = category, ingredients = lines.toList(), servings = servings,
            servingsSource = servings?.let { Recipe.SERVINGS_USER }, ingredientWeights = weights)

    @Test
    fun totalAndPerServing() {
        // 200 g farine = 700 kcal ; 100 g beurre = 750 kcal → 1450 kcal ; 4 parts → 362,5 → 360
        val e = calc.estimate(recipe("200 g de farine", "100 g de beurre"))
        assertEquals(1450.0, e.total!!.kcal, 0.01)
        assertEquals(360.0, e.perServing!!.kcal, 0.0)
        // protéines (20 + 1) / 4 = 5,25 → 5 ; glucides (140 + 1) / 4 = 35,25 → 35 ; lipides (2 + 82) / 4 = 21
        assertEquals(5.0, e.perServing!!.protein, 0.0)
        assertEquals(35.0, e.perServing!!.carbs, 0.0)
        assertEquals(21.0, e.perServing!!.fat, 0.0)
        assertTrue(e.warnings.isEmpty())
    }

    @Test
    fun userWeightBeatsWrittenQuantity() {
        val e = calc.estimate(recipe("200 g de farine", weights = mapOf("200 g de farine" to IngredientWeight(100.0, IngredientWeight.USER))))
        assertEquals(100.0, e.counted.single().grams, 0.0)
        assertEquals(GramsOrigin.USER, e.counted.single().origin)
    }

    @Test
    fun writtenQuantityBeatsPcEstimate() {
        val e = calc.estimate(recipe("200 g de farine", weights = mapOf("200 g de farine" to IngredientWeight(50.0, IngredientWeight.PC))))
        assertEquals(200.0, e.counted.single().grams, 0.0)
        assertEquals(GramsOrigin.WRITTEN, e.counted.single().origin)
    }

    @Test
    fun pcEstimateUsedForVagueLine() {
        val e = calc.estimate(recipe("1 noix de beurre", weights = mapOf("1 noix de beurre" to IngredientWeight(10.0, IngredientWeight.PC))))
        assertEquals(GramsOrigin.PC, e.counted.single().origin)
        assertEquals(75.0, e.total!!.kcal, 0.01)
    }

    @Test
    fun pcNullMarkerLeavesLineUncounted() {
        val e = calc.estimate(recipe("200 g de farine", "1 noix de beurre", weights = mapOf("1 noix de beurre" to IngredientWeight(null, IngredientWeight.PC))))
        assertEquals(1, e.counted.size)
        assertEquals(IngredientIssue.VAGUE, e.ignored.single().issue)
    }

    @Test
    fun userCorrectionCountsVagueLine() {
        // Story 2 : « 1 noix de beurre » corrigée à 10 g → + 75 kcal
        val before = calc.estimate(recipe("200 g de farine", "1 noix de beurre"))
        val after = calc.estimate(recipe("200 g de farine", "1 noix de beurre", weights = mapOf("1 noix de beurre" to IngredientWeight(10.0, IngredientWeight.USER))))
        assertEquals(75.0, after.total!!.kcal - before.total!!.kcal, 0.01)
    }

    @Test
    fun piecesUsePieceWeight() {
        val e = calc.estimate(recipe("2 pommes", "3 œufs"))
        assertEquals(300.0 + 150.0, e.countedGrams, 0.01)
    }

    @Test
    fun seasoningIgnoredWithoutWarning() {
        val e = calc.estimate(recipe("200 g de farine", "sel", "1 pincée de sel"))
        assertEquals(2, e.seasonings.size)
        assertTrue(e.ignored.isEmpty())
        assertFalse(NutritionWarning.PARTIAL in e.warnings)
    }

    @Test
    fun fryingCountedWithWarning() {
        val e = calc.estimate(recipe("200 g de farine", "1 l d'huile de friture"))
        assertEquals(2, e.counted.size)
        assertTrue(NutritionWarning.FRYING in e.warnings)
    }

    @Test
    fun partialWhenMoreThanHalfOfLinesNotCounted() {
        // 1 comptée, 2 non comptées (le sel ne compte pas dans le décompte)
        val e = calc.estimate(recipe("200 g de farine", "1 noix de beurre", "xyzzy", "sel"))
        assertTrue(NutritionWarning.PARTIAL in e.warnings)
    }

    @Test
    fun notPartialWhenExactlyHalf() {
        val e = calc.estimate(recipe("200 g de farine", "1 noix de beurre"))
        assertFalse(NutritionWarning.PARTIAL in e.warnings)
    }

    @Test
    fun checkValueAbove3000PerServing() {
        val e = calc.estimate(recipe("1 kg de beurre", servings = 2))  // 3750 kcal par part
        assertTrue(NutritionWarning.CHECK_VALUE in e.warnings)
    }

    @Test
    fun checkValueAbove30EstimatedServings() {
        // 1000 g en « Biscuits et petits gâteaux » (25 g par part) → 40 parts estimées
        val e = calc.estimate(recipe("1 kg de farine", servings = null, category = "Biscuits et petits gâteaux"))
        assertEquals(40, e.servings)
        assertTrue(NutritionWarning.CHECK_VALUE in e.warnings)
    }

    @Test
    fun noCountedLineGivesNoValue() {
        val e = calc.estimate(recipe("1 noix de beurre", "xyzzy"))
        assertNull(e.total)
        assertNull(e.perServing)
    }

    @Test
    fun noServingsGivesTotalOnly() {
        val e = calc.estimate(recipe("200 g de farine", servings = null))
        assertEquals(700.0, e.total!!.kcal, 0.01)
        assertNull(e.servings)
        assertNull(e.perServing)
    }

    @Test
    fun orphanWeightEntriesIgnored() {
        val e = calc.estimate(recipe("200 g de farine", weights = mapOf("ancienne ligne" to IngredientWeight(500.0, IngredientWeight.USER))))
        assertEquals(200.0, e.countedGrams, 0.0)
    }

    @Test
    fun toCompleteAndHeadersHandled() {
        val e = calc.estimate(recipe("Pour la pâte :", "200 g de farine", "À compléter"))
        assertEquals(1, e.counted.size)
        assertEquals(IngredientIssue.TO_COMPLETE, e.ignored.single().issue)
    }

    @Test
    fun unknownFoodNotCountedEvenWithGrams() {
        val e = calc.estimate(recipe("200 g de xyzzy"))
        assertEquals(IngredientIssue.UNKNOWN_FOOD, e.ignored.single().issue)
    }
}
