package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.IngredientWeight.Companion.PC
import com.example.recipelibrary.data.IngredientWeight.Companion.USER
import com.example.recipelibrary.data.IngredientWeights
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.GramsOrigin
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Story 2 : une correction de poids est conservée, sauf si la ligne change. */
class WeightCorrectionTest {

    private val lines = listOf("1 noix de beurre", "2 pommes", "200 g de farine")
    private val weights = mapOf(
        "1 noix de beurre" to IngredientWeight(10.0, USER),
        "2 pommes" to IngredientWeight(300.0, PC)
    )

    @Test
    fun keptWhenRecipeEditedWithoutTouchingTheLine() {
        assertEquals(weights, IngredientWeights.keepForLines(weights, lines + "1 pincée de sel"))
    }

    @Test
    fun removedWhenLineTextChanges() {
        val kept = IngredientWeights.keepForLines(weights, listOf("2 noix de beurre", "2 pommes", "200 g de farine"))
        assertNull(kept["1 noix de beurre"])
        assertEquals(IngredientWeight(300.0, PC), kept["2 pommes"])
    }

    @Test
    fun neverOverwrittenByPc() {
        val merged = IngredientWeights.mergePc(weights, mapOf("1 noix de beurre" to 25.0, "2 pommes" to 320.0))
        assertEquals(IngredientWeight(10.0, USER), merged["1 noix de beurre"])
        assertEquals(IngredientWeight(320.0, PC), merged["2 pommes"])
    }

    @Test
    fun pcNullIsStoredAsAlreadySentMarker() {
        val merged = IngredientWeights.mergePc(emptyMap(), mapOf("1 pincée de safran" to null, "1 botte de radis" to 99999.0))
        assertEquals(IngredientWeight(null, PC), merged["1 pincée de safran"])
        assertEquals(IngredientWeight(null, PC), merged["1 botte de radis"])  // hors bornes → null
    }

    @Test
    fun userCorrectionReplacesPcAndIsUsedByCalculator() {
        val corrected = IngredientWeights.withUser(weights, "2 pommes", 250.0)
        assertEquals(IngredientWeight(250.0, USER), corrected["2 pommes"])
        val e = NutritionCalculator(TestFoods.table).estimate(Recipe(id = "r", title = "R", ingredients = lines, ingredientWeights = corrected))
        val apples = e.counted.first { it.parsed.line == "2 pommes" }
        assertEquals(GramsOrigin.USER, apples.origin)
        assertEquals(250.0, apples.grams, 0.0)
    }
}
