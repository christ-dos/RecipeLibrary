package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.RecipeEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Modifications enregistrées depuis la fiche (ce que MainActivity passe à upsert). */
class RecipeEditsTest {

    private val recipe = Recipe(
        id = "r", title = "Tarte", category = "Desserts",
        ingredients = listOf("1 noix de beurre", "2 pommes"),
        servings = 6, servingsSource = Recipe.SERVINGS_SOURCE,
        ingredientWeights = mapOf("2 pommes" to IngredientWeight(300.0, IngredientWeight.PC)),
        createdAt = 1L, updatedAt = 2L
    )

    @Test
    fun servingsFromDetailAreUserAndTouchUpdatedAtOnly() {
        val out = RecipeEdits.withServings(recipe, 4, now = 99L)
        assertEquals(recipe.copy(servings = 4, servingsSource = Recipe.SERVINGS_USER, updatedAt = 99L), out)
    }

    @Test
    fun servingsOutOfRangeIgnored() {
        assertSame(recipe, RecipeEdits.withServings(recipe, 0))
        assertSame(recipe, RecipeEdits.withServings(recipe, 101))
    }

    @Test
    fun userWeightAddedAndOtherEntriesKept() {
        val out = RecipeEdits.withUserWeight(recipe, "1 noix de beurre", IngredientWeight(10.0, IngredientWeight.USER), now = 99L)
        assertEquals(
            mapOf(
                "2 pommes" to IngredientWeight(300.0, IngredientWeight.PC),
                "1 noix de beurre" to IngredientWeight(10.0, IngredientWeight.USER)
            ),
            out.ingredientWeights
        )
        assertEquals(99L, out.updatedAt)
        assertEquals(recipe.copy(ingredientWeights = out.ingredientWeights, updatedAt = 99L), out)
    }

    @Test
    fun userWeightReplacesPcEstimate() {
        val out = RecipeEdits.withUserWeight(recipe, "2 pommes", IngredientWeight(250.0, IngredientWeight.USER))
        assertEquals(IngredientWeight(250.0, IngredientWeight.USER), out.ingredientWeights["2 pommes"])
    }

    @Test
    fun invalidOrUnknownLineIgnored() {
        assertSame(recipe, RecipeEdits.withUserWeight(recipe, "ligne absente", IngredientWeight(10.0, IngredientWeight.USER)))
        assertSame(recipe, RecipeEdits.withUserWeight(recipe, "2 pommes", IngredientWeight(null, IngredientWeight.USER)))
        assertSame(recipe, RecipeEdits.withUserWeight(recipe, "2 pommes", IngredientWeight(20000.0, IngredientWeight.USER)))
        assertSame(recipe, RecipeEdits.withUserWeight(recipe, "2 pommes", IngredientWeight(0.0, IngredientWeight.USER)))
    }
}
