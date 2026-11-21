package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.IngredientWeights
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.ui.NutritionDetailSheet
import com.example.recipelibrary.ui.kcalLabel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Story 2 : détail des calories et correction d'un poids. */
@RunWith(AndroidJUnit4::class)
class NutritionDetailSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val table: FoodTable by lazy { FoodTable.load(InstrumentationRegistry.getInstrumentation().targetContext) }

    private val base = Recipe(
        id = "d", title = "Crepes", category = "Desserts", servings = 4, servingsSource = Recipe.SERVINGS_USER,
        ingredients = listOf("250 g de farine", "4 œufs", "50 cl de lait", "1 noix de beurre", "1 pincée de sel", "2 pommes"),
        ingredientWeights = mapOf("2 pommes" to IngredientWeight(280.0, IngredientWeight.USER))
    )

    /** Affiche la feuille ; une correction met la recette à jour comme le fait l'app. */
    private fun show(start: Recipe = base, onChange: (String, IngredientWeight) -> Unit = { _, _ -> }) {
        rule.setContent {
            var recipe by remember { mutableStateOf(start) }
            val calc = remember { NutritionCalculator(table) }
            MaterialTheme {
                NutritionDetailSheet(
                    estimate = calc.estimate(recipe),
                    onDismiss = {},
                    onWeightChange = { line, w ->
                        onChange(line, w)
                        recipe = recipe.copy(ingredientWeights = IngredientWeights.withUser(recipe.ingredientWeights, line, w.g!!))
                    }
                )
            }
        }
    }

    @Test
    fun countedLinesShowWeightOriginAndKcal() {
        show()
        rule.onNodeWithTag("counted_250 g de farine").assertTextContains("250 g (écrit)", substring = true)
        rule.onNodeWithTag("counted_2 pommes").assertTextContains("280 g (corrigé)", substring = true)
        rule.onNodeWithTag("counted_250 g de farine").assertTextContains("kcal", substring = true)
    }

    @Test
    fun macrosPerServingShown() {
        show()
        rule.onNodeWithTag("detail_macros").assertTextContains("protéines", substring = true)
        rule.onNodeWithTag("detail_macros").assertTextContains("glucides", substring = true)
        rule.onNodeWithTag("detail_macros").assertTextContains("lipides", substring = true)
    }

    @Test
    fun uncountedLinesWithReadableReason() {
        show()
        rule.onNodeWithTag("ignored_1 noix de beurre").assertTextContains("quantité imprécise", substring = true)
    }

    @Test
    fun setting10gCallsCallbackAndUpdatesTotalImmediately() {
        var changed: Pair<String, IngredientWeight>? = null
        show { line, w -> changed = line to w }
        val calc = NutritionCalculator(table)
        val before = kcalLabel(calc.estimate(base).total!!.kcal)
        val after = kcalLabel(calc.estimate(base.copy(ingredientWeights = IngredientWeights.withUser(base.ingredientWeights, "1 noix de beurre", 10.0))).total!!.kcal)
        rule.onNodeWithTag("detail_recipe_total").assertTextContains("≈ $before kcal", substring = true)

        rule.onNodeWithTag("set_weight_1 noix de beurre").performClick()
        rule.onNodeWithTag("weight_field").performTextReplacement("10")
        rule.onNodeWithTag("weight_ok").performClick()
        rule.waitForIdle()

        assertEquals("1 noix de beurre" to IngredientWeight(10.0, IngredientWeight.USER), changed)
        // Le nouveau total apparaît dès la validation, sans autre écran (SC-005)
        rule.onNodeWithTag("detail_recipe_total").assertTextContains("≈ $after kcal", substring = true)
        rule.onNodeWithTag("counted_1 noix de beurre").assertTextContains("10 g (corrigé)", substring = true)
    }

    @Test
    fun invalidWeightRefused() {
        show()
        rule.onNodeWithTag("set_weight_1 noix de beurre").performClick()
        rule.onNodeWithTag("weight_field").performTextReplacement("0")
        rule.onNodeWithText("Enregistrer").assertExists()
        rule.onNodeWithTag("weight_ok").assertIsNotEnabled()
    }

    @Test
    fun sourceMentionPresent() {
        show()
        rule.onNodeWithTag("detail_list").performScrollToNode(hasTestTag("detail_source"))
        rule.onNode(hasTestTag("detail_source"), useUnmergedTree = true).assertTextContains("Ciqual 2020", substring = true)
    }

}
