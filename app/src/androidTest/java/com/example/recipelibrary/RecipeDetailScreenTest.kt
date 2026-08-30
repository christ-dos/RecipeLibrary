package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.RecipeDetailScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDetailScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // Recette de test avec titre ASCII pour éviter les problèmes Unicode
    private fun testRecipe() = Recipe(
        id = "r1",
        title = "Poulet roti",
        category = "Plats principaux",
        ingredients = listOf("1 poulet", "Herbes", "2 gousses ail"),
        steps = listOf(
            "Prechauffer le four",
            "Assaisonner le poulet",
            "Cuire 1h30 a 180 degres"
        ),
        notes = "Arroser toutes les 20 minutes."
    )

    // ── 1. Le titre de la recette est affiché ─────────────────────────────────

    @Test
    fun detail_showsRecipeTitle() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        // Le titre apparaît à la fois dans le TopBar et dans le body — les deux sont affichés,
        // on prend le premier nœud trouvé.
        rule.onAllNodesWithText("Poulet roti").onFirst().assertIsDisplayed()
    }

    // ── 2. La catégorie est affichée ──────────────────────────────────────────

    @Test
    fun detail_showsCategory() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        rule.onNodeWithTag("detail_list").performScrollToNode(hasText("Plats principaux", substring = true))
        rule.onNodeWithText("Plats principaux", substring = true).assertIsDisplayed()
    }

    // ── 3. La section Ingrédients est visible avec son contenu ────────────────

    @Test
    fun detail_showsIngredientsSection() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        // SectionCard affiche "🧑‍🍳 Ingrédients" → substring = true requis
        rule.onNodeWithTag("detail_list").performScrollToNode(hasText("Ingrédients", substring = true))
        rule.onNodeWithText("Ingrédients", substring = true).assertIsDisplayed()
        rule.onNodeWithText("1 poulet").assertExists()
    }

    // ── 4. La section Préparation est visible avec son contenu ────────────────

    @Test
    fun detail_showsPreparationSection() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        // SectionCard affiche "🍳 Préparation" → substring = true requis
        rule.onNodeWithTag("detail_list").performScrollToNode(hasText("Préparation", substring = true))
        rule.onNodeWithText("Préparation", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Prechauffer le four").assertExists()
    }

    // ── 5. La section Notes est visible quand les notes existent ─────────────

    @Test
    fun detail_showsNotesSection_whenNotesExist() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        // SectionCard affiche "📋 Notes" → substring = true requis
        rule.onNodeWithTag("detail_list").performScrollToNode(hasText("Notes", substring = true))
        rule.onNodeWithText("Notes", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Arroser toutes les 20 minutes.").assertExists()
    }

    // ── 6. La section Notes est absente quand pas de notes ────────────────────

    @Test
    fun detail_hidesNotesSection_whenNoNotes() {
        val recipe = testRecipe().copy(notes = null)
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(recipe, onBack = {}, onEdit = {}, onDelete = {}) }
        }
        rule.onNodeWithText("Notes").assertDoesNotExist()
    }

    // ── 7. Le menu 3 points s'ouvre avec les bonnes actions ───────────────────

    @Test
    fun detail_menu_opensWithCorrectItems() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.onNodeWithText("Modifier la recette").assertIsDisplayed()
        rule.onNodeWithText("Partager").assertIsDisplayed()
        rule.onNodeWithText("Dupliquer").assertIsDisplayed()
        rule.onNodeWithText("Supprimer").assertIsDisplayed()
    }

    // ── 8. Supprimer affiche la confirmation ──────────────────────────────────

    @Test
    fun detail_deleteMenu_showsConfirmationDialog() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = {}, onDelete = {}) }
        }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.onNodeWithText("Supprimer").performClick()
        rule.onNodeWithText("Supprimer la recette ?").assertIsDisplayed()
    }

    // ── 9. Modifier appelle onEdit ────────────────────────────────────────────

    @Test
    fun detail_editMenu_callsOnEdit() {
        var editCalled = false
        rule.setContent {
            MaterialTheme {
                RecipeDetailScreen(testRecipe(), onBack = {}, onEdit = { editCalled = true }, onDelete = {})
            }
        }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.onNodeWithText("Modifier la recette").performClick()
        assert(editCalled) { "onEdit n'a pas ete appele" }
    }

    // ── 10. La duplication appelle onDuplicate ────────────────────────────────

    @Test
    fun detail_duplicateMenu_callsOnDuplicate() {
        var duplicateCalled = false
        rule.setContent {
            MaterialTheme {
                RecipeDetailScreen(
                    testRecipe(), onBack = {}, onEdit = {}, onDelete = {},
                    onDuplicate = { duplicateCalled = true }
                )
            }
        }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.onNodeWithText("Dupliquer").performClick()
        assert(duplicateCalled) { "onDuplicate n'a pas ete appele" }
    }
}
