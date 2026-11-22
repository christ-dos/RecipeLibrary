package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.ui.RecipeDetailScreen
import com.example.recipelibrary.ui.kcalLabel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Story 1 et 5 : bandeau « ≈ X kcal / part » sur la fiche. */
@RunWith(AndroidJUnit4::class)
class RecipeDetailNutritionTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val table: FoodTable by lazy {
        FoodTable.load(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    private fun show(recipe: Recipe, onServings: ((Int) -> Unit)? = null) {
        rule.setContent {
            MaterialTheme {
                RecipeDetailScreen(recipe, onBack = {}, onEdit = {}, onDelete = {}, foodTable = table, onServingsChange = onServings)
            }
        }
    }

    private fun scrollToBanner() {
        rule.onNodeWithTag("detail_list").performScrollToNode(hasTestTag("nutrition_banner"))
    }

    @Test
    fun banner_appearsInLessThan500ms_for20Ingredients() {
        val lines = listOf(
            "250 g de farine", "4 œufs", "50 cl de lait", "50 g de beurre", "200 g de sucre", "1 sachet de levure chimique",
            "3 pommes", "100 g de chocolat noir", "20 cl de crème fraîche", "2 c. à soupe d'huile", "1 citron",
            "100 g de poudre d'amande", "50 g de noisettes", "1 yaourt nature", "30 g de cacao", "2 bananes",
            "1 c. à café de cannelle", "1 pincée de sel", "100 g de raisins secs", "1 c. à soupe de miel"
        )
        val recipe = Recipe(id = "t20", title = "Gateau complet", category = "Gâteaux", ingredients = lines, servings = 8, servingsSource = Recipe.SERVINGS_USER)
        table.size // table déjà chargée, comme dans l'app
        val start = System.nanoTime()
        show(recipe)
        rule.waitUntil(timeoutMillis = 500) {
            rule.onAllNodes(hasTestTag("nutrition_banner")).fetchSemanticsNodes().isNotEmpty()
        }
        val ms = (System.nanoTime() - start) / 1_000_000
        assert(ms < 500) { "bandeau affiché en $ms ms" }
    }

    @Test
    fun banner_perServingWith4Servings() {
        val recipe = Recipe(
            id = "c", title = "Pate a crepes", category = "Desserts", servings = 4, servingsSource = Recipe.SERVINGS_USER,
            ingredients = listOf("250 g de farine", "4 œufs", "50 cl de lait", "50 g de beurre")
        )
        val expected = kcalLabel(NutritionCalculator(table).estimate(recipe).perServing!!.kcal)
        show(recipe)
        scrollToBanner()
        rule.onNodeWithTag("nutrition_per_serving").assertIsDisplayed().assertTextContains("≈ $expected kcal / part", substring = true)
        rule.onNodeWithTag("nutrition_servings").assertTextContains("4 parts")
    }

    @Test
    fun banner_estimatedServingsForCake600g() {
        val recipe = Recipe(id = "g", title = "Gateau", category = "Gâteaux", ingredients = listOf("400 g de farine", "200 g de sucre"))
        show(recipe)
        scrollToBanner()
        rule.onNodeWithTag("nutrition_servings").assertTextContains("≈ 6 parts (estimé)")
    }

    @Test
    fun banner_totalAndInvitationWhenServingsUnknown() {
        var saved: Int? = null
        val recipe = Recipe(id = "n", title = "Sans categorie", ingredients = listOf("200 g de farine"))
        show(recipe) { saved = it }
        scrollToBanner()
        rule.onNodeWithTag("nutrition_total").assertTextContains("kcal pour la recette", substring = true)
        rule.onNodeWithTag("nutrition_set_servings").assertIsDisplayed().performClick()
        rule.onNodeWithTag("servings_dialog_field").performTextInput("4")
        rule.onNodeWithTag("servings_dialog_ok").performClick()
        rule.waitForIdle()
        assertEquals(4, saved)
    }

    @Test
    fun banner_impossibleWhenNothingCounted() {
        val recipe = Recipe(id = "i", title = "Vague", category = "Desserts", ingredients = listOf("1 noix de beurre", "du sucre"))
        show(recipe)
        scrollToBanner()
        rule.onNodeWithTag("nutrition_impossible").assertIsDisplayed()
        rule.onNodeWithText("Estimation impossible", substring = true).assertIsDisplayed()
    }

    @Test
    fun banner_absentWithoutTable() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(Recipe(id = "x", title = "X", ingredients = listOf("200 g de farine")), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(hasTestTag("nutrition_banner")).fetchSemanticsNodes().size)
    }

    // ── Fiche complète : bandeau → détail → correction, comme dans l'app ──────

    @Test
    fun bannerClick_opensDetail_andCorrectionUpdatesBanner() {
        var received: Pair<String, com.example.recipelibrary.data.IngredientWeight>? = null
        val start = Recipe(
            id = "f", title = "Crepes", category = "Desserts", servings = 4, servingsSource = Recipe.SERVINGS_USER,
            ingredients = listOf("250 g de farine", "4 œufs", "50 cl de lait", "1 noix de beurre")
        )
        val calc = NutritionCalculator(table)
        val before = kcalLabel(calc.estimate(start).perServing!!.kcal)
        val corrected = com.example.recipelibrary.data.RecipeEdits.withUserWeight(
            start, "1 noix de beurre", com.example.recipelibrary.data.IngredientWeight(40.0, com.example.recipelibrary.data.IngredientWeight.USER)
        )
        val after = kcalLabel(calc.estimate(corrected).perServing!!.kcal)
        assert(before != after)

        rule.setContent {
            var recipe by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(start) }
            MaterialTheme {
                RecipeDetailScreen(
                    recipe, onBack = {}, onEdit = {}, onDelete = {}, foodTable = table,
                    onServingsChange = { recipe = com.example.recipelibrary.data.RecipeEdits.withServings(recipe, it) },
                    onWeightChange = { line, w ->
                        received = line to w
                        recipe = com.example.recipelibrary.data.RecipeEdits.withUserWeight(recipe, line, w)
                    }
                )
            }
        }
        scrollToBanner()
        rule.onNodeWithTag("nutrition_per_serving", useUnmergedTree = true).assertTextContains("≈ $before kcal / part", substring = true)
        rule.onNodeWithTag("nutrition_banner").performClick()
        rule.onNodeWithTag("nutrition_detail").assertIsDisplayed()

        rule.onNodeWithTag("set_weight_1 noix de beurre").performClick()
        rule.onNodeWithTag("weight_field").performTextReplacement("40")
        rule.onNodeWithTag("weight_ok").performClick()
        rule.waitForIdle()

        assertEquals("1 noix de beurre" to com.example.recipelibrary.data.IngredientWeight(40.0, com.example.recipelibrary.data.IngredientWeight.USER), received)
        // Retour du téléphone = fermeture de la feuille (onDismissRequest). Le bouton « Fermer »
        // apparaît en double dans l'arbre de test : Compose 1.5 garde une ancienne ligne de liste.
        androidx.test.espresso.Espresso.pressBack()
        rule.waitForIdle()
        // La fermeture de la fenêtre peut arriver un peu après le retour : on l'attend (5 s au plus).
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("nutrition_detail")).fetchSemanticsNodes().isEmpty() }
        assertEquals(0, rule.onAllNodes(hasTestTag("nutrition_detail")).fetchSemanticsNodes().size)
        rule.waitForIdle()
        scrollToBanner()
        rule.onNodeWithTag("nutrition_per_serving", useUnmergedTree = true).assertTextContains("≈ $after kcal / part", substring = true)
    }

    @Test
    fun bannerNotClickableWithoutCorrectionCallback() {
        show(Recipe(id = "nc", title = "X", category = "Desserts", servings = 2, servingsSource = Recipe.SERVINGS_USER, ingredients = listOf("200 g de farine")))
        scrollToBanner()
        rule.onNodeWithTag("nutrition_banner").performClick()
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(hasTestTag("nutrition_detail")).fetchSemanticsNodes().size)
    }

    @Test
    fun estimatedServings_clickToSetServings() {
        var saved: Int? = null
        show(Recipe(id = "es", title = "Gateau", category = "Gâteaux", ingredients = listOf("400 g de farine", "200 g de sucre"))) { saved = it }
        scrollToBanner()
        rule.onNodeWithTag("nutrition_servings").performClick()
        rule.onNodeWithTag("servings_dialog_field").performTextReplacement("8")
        rule.onNodeWithTag("servings_dialog_ok").performClick()
        rule.waitForIdle()
        assertEquals(8, saved)
    }

    @Test
    fun foodTable_loadedOnceAndComplete() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val first = FoodTable.load(ctx)
        assert(first === FoodTable.load(ctx)) { "la table doit être chargée une seule fois" }
        assert(first === FoodTable.loadedOrNull())
        assert(first.size >= 300) { "${first.size} aliments" }
        assertEquals("22000", first.match("3 œufs")?.code)
    }
}
