package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.FoodSubstitution
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.Food
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.ui.FoodSubstitutionsScreen
import com.example.recipelibrary.ui.NutritionDetailSheet
import com.example.recipelibrary.ui.RecipeDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Story 6 (T046) : « Choisir l'aliment » dans le détail des calories et écran des remplacements. */
@RunWith(AndroidJUnit4::class)
class FoodSubstitutionUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // Petite table fixe : le test ne dépend pas du contenu de foods.json.
    private val fromageBlanc = Food(
        code = "19590", name = "Fromage blanc nature, 0 % MG", aliases = listOf("fromage blanc"),
        kcal = 45.0, protein = 7.5, carbs = 4.0, fat = 0.2
    )
    private val farine = Food(
        code = "9410", name = "Farine de blé", aliases = listOf("farine"),
        kcal = 340.0, protein = 10.0, carbs = 72.0, fat = 1.0
    )
    private val base = FoodTable(listOf(fromageBlanc, farine))
    private val recipe = Recipe(
        id = "s", title = "Bowl", servings = 2, servingsSource = Recipe.SERVINGS_USER,
        ingredients = listOf("200 g de farine", "150 g de skyr nature")
    )
    private val skyrLine = "150 g de skyr nature"

    /** Feuille de détail ; un choix met à jour les remplacements comme le fait l'app. */
    private fun showSheet(onChoose: (String, String) -> Unit = { _, _ -> }, withChooser: Boolean = true) {
        rule.setContent {
            var subs by remember { mutableStateOf(emptyMap<String, String>()) }
            val table = base.withSubstitutions(subs)
            MaterialTheme {
                NutritionDetailSheet(
                    estimate = NutritionCalculator(table).estimate(recipe),
                    onDismiss = {},
                    onWeightChange = { _, _ -> },
                    foodTable = table,
                    onChooseFood = if (withChooser) ({ key, code -> onChoose(key, code); subs = subs + (key to code) }) else null
                )
            }
        }
    }

    @Test
    fun unknownFood_chooseFood_countsLineWithReplacement() {
        var chosen: Pair<String, String>? = null
        showSheet(onChoose = { k, c -> chosen = k to c })
        rule.onNodeWithTag("ignored_$skyrLine").assertTextContains("aliment non reconnu", substring = true)

        rule.onNodeWithTag("choose_food_$skyrLine").performClick()
        rule.onNodeWithTag("substitution_key").assertTextContains("skyr nature", substring = true)
        rule.onNodeWithTag("food_search").performTextReplacement("fromage")
        rule.onNodeWithTag("food_result_19590").performClick()
        rule.waitForIdle()

        assertEquals("skyr nature" to "19590", chosen)
        assertEquals(0, rule.onAllNodes(hasTestTag("food_picker")).fetchSemanticsNodes().size)
        rule.onNodeWithTag("counted_$skyrLine").assertTextContains("150 g (écrit)", substring = true)
        rule.onNodeWithTag("counted_$skyrLine").assertTextContains("Fromage blanc", substring = true)
        rule.onNodeWithTag("substituted_$skyrLine", useUnmergedTree = true)
            .assertTextContains("« skyr nature »", substring = true)
    }

    @Test
    fun shortenedKey_isUsed() {
        var chosen: Pair<String, String>? = null
        showSheet(onChoose = { k, c -> chosen = k to c })
        rule.onNodeWithTag("choose_food_$skyrLine").performClick()
        rule.onNodeWithTag("substitution_key").performTextReplacement("Skyr")
        rule.onNodeWithTag("substitution_key_hint").assertTextContains("« skyr »", substring = true)
        rule.onNodeWithTag("food_search").performTextReplacement("blanc")
        rule.onNodeWithTag("food_result_19590").performClick()
        rule.waitForIdle()
        assertEquals("skyr" to "19590", chosen)
    }

    @Test
    fun keyNotInLine_isRefused() {
        var chosen: Pair<String, String>? = null
        showSheet(onChoose = { k, c -> chosen = k to c })
        rule.onNodeWithTag("choose_food_$skyrLine").performClick()
        rule.onNodeWithTag("substitution_key").performTextReplacement("yaourt")
        rule.onNodeWithTag("substitution_key_hint").assertTextContains("doit figurer", substring = true)
        rule.onNodeWithTag("food_search").performTextReplacement("fromage")
        rule.onNodeWithTag("food_result_19590").performClick()
        rule.waitForIdle()
        assertNull(chosen)
    }

    @Test
    fun noChooser_noButton() {
        showSheet(withChooser = false)
        assertEquals(0, rule.onAllNodes(hasTestTag("choose_food_$skyrLine")).fetchSemanticsNodes().size)
    }

    @Test
    fun recipeDetail_opensSubstitutionsScreen() {
        var opened = 0
        rule.setContent {
            MaterialTheme {
                RecipeDetailScreen(
                    recipe, onBack = {}, onEdit = {}, onDelete = {}, foodTable = base,
                    onWeightChange = { _, _ -> }, onChooseFood = { _, _ -> }, onOpenSubstitutions = { opened++ }
                )
            }
        }
        rule.onNodeWithTag("detail_list").performScrollToNode(hasTestTag("nutrition_banner"))
        rule.onNodeWithTag("nutrition_banner").performClick()
        rule.onNodeWithTag("choose_food_$skyrLine").assertExists()
        rule.onNodeWithTag("open_substitutions").performClick()
        rule.waitForIdle()
        assertEquals(1, opened)
        // La feuille se ferme pour laisser place à l'écran.
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("nutrition_detail")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun substitutionsScreen_listsAndCancels() {
        var cancelled: String? = null
        rule.setContent {
            var list by remember {
                mutableStateOf(listOf(FoodSubstitution("skyr", "19590"), FoodSubstitution("tofu soyeux", "00000")))
            }
            MaterialTheme {
                FoodSubstitutionsScreen(
                    substitutions = list, foodTable = base,
                    onCancel = { key -> cancelled = key; list = list.filterNot { it.textKey == key } }
                )
            }
        }
        rule.onNodeWithTag("substitution_skyr").assertTextContains("Fromage blanc nature, 0 % MG", substring = true)
        rule.onNodeWithTag("substitution_tofu soyeux").assertTextContains("introuvable", substring = true)
        rule.onNodeWithTag("cancel_substitution_skyr").performClick()
        rule.waitForIdle()
        assertEquals("skyr", cancelled)
        // LazyColumn (Compose 1.5) garde un nœud retiré : on vérifie qu'il n'est plus affiché, et que l'autre reste
        rule.onNodeWithTag("substitution_skyr").assertIsNotDisplayed()
        rule.onNodeWithTag("substitution_tofu soyeux").assertIsDisplayed()
    }

    @Test
    fun substitutionsScreen_empty() {
        rule.setContent { MaterialTheme { FoodSubstitutionsScreen(substitutions = emptyList(), foodTable = base, onCancel = {}) } }
        rule.onNodeWithTag("substitutions_empty").assertTextContains("Choisir l'aliment", substring = true)
    }
}
