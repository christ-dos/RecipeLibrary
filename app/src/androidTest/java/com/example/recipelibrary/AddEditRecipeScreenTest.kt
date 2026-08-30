package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.AddEditRecipeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEditRecipeScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // ── 1. Nouveau formulaire : bouton Enregistrer désactivé ──────────────────

    @Test
    fun addRecipe_initialState_saveButtonDisabled() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onNodeWithText("Enregistrer").assertIsNotEnabled()
    }

    // ── 2. Les champs obligatoires du haut sont visibles ─────────────────────

    @Test
    fun addRecipe_topRequiredFields_areVisible() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onNodeWithText("Titre *").assertIsDisplayed()
        rule.onNodeWithText("Catégorie *").assertIsDisplayed()
    }

    // ── 3. Les champs obligatoires du bas existent (scrollable) ───────────────

    @Test
    fun addRecipe_bottomRequiredFields_existInForm() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onNodeWithText("Ingrédients *").assertExists()
        rule.onNodeWithText("Étapes de préparation *").assertExists()
    }

    // ── 4. Les champs optionnels existent dans le formulaire ──────────────────

    @Test
    fun addRecipe_optionalFields_existInForm() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onNodeWithText("URL source").assertExists()
        rule.onNodeWithText("Tags").assertExists()
        rule.onNodeWithText("Notes").assertExists()
    }

    // ── 5. Titre seul → bouton reste désactivé ────────────────────────────────

    @Test
    fun addRecipe_fillTitleOnly_saveStillDisabled() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        // performTextReplacement appelle SetText directement — ne passe pas par l'IME
        rule.onNodeWithTag("form_title").performTextReplacement("Tarte citron")
        rule.onNodeWithText("Enregistrer").assertIsNotEnabled()
    }

    // ── 6. Les 4 champs obligatoires remplis activent le bouton ───────────────

    @Test
    fun addRecipe_allRequiredFields_saveEnabled() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onNodeWithTag("form_title").performTextReplacement("Tarte citron")
        rule.onNodeWithText("Choisir une catégorie").performClick()
        rule.onNodeWithText("Desserts", substring = true).performClick()
        rule.onNodeWithTag("form_ingredients").performTextReplacement("1 citron")
        rule.onNodeWithTag("form_steps").performTextReplacement("Cuire 30 min")
        rule.onNodeWithText("Enregistrer").assertIsEnabled()
    }

    // ── 7. Enregistrer appelle onSave avec le bon titre ───────────────────────

    @Test
    fun addRecipe_save_callsOnSaveWithCorrectTitle() {
        var savedTitle = ""
        rule.setContent {
            MaterialTheme {
                AddEditRecipeScreen(
                    existing = null,
                    onSave = { recipe -> savedTitle = recipe.title },
                    onBack = {}
                )
            }
        }
        rule.onNodeWithTag("form_title").performTextReplacement("Gateau chocolat")
        rule.onNodeWithText("Choisir une catégorie").performClick()
        rule.onNodeWithText("Gâteaux", substring = true).performClick()
        rule.onNodeWithTag("form_ingredients").performTextReplacement("200 g chocolat")
        rule.onNodeWithTag("form_steps").performTextReplacement("Fondre le chocolat")
        rule.onNodeWithText("Enregistrer").performClick()
        assert(savedTitle == "Gateau chocolat") {
            "Titre attendu 'Gateau chocolat', obtenu '$savedTitle'"
        }
    }

    // ── 8. Formulaire pré-rempli en mode édition ──────────────────────────────

    @Test
    fun editRecipe_existingData_preFillsFields() {
        val existing = Recipe(
            id = "42", title = "Quiche lorraine", category = "Tartes et quiches",
            ingredients = listOf("Pate brisee", "Lardons", "3 oeufs"),
            steps = listOf("Foncer le moule", "Cuire 35 min")
        )
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = existing, onSave = {}, onBack = {}) } }
        rule.onNodeWithText("Modifier la recette").assertIsDisplayed()
        rule.onNodeWithText("Quiche lorraine").assertIsDisplayed()
        rule.onNodeWithText("Enregistrer").assertIsEnabled()
    }

    // ── 9. Les boutons microphone sont présents ────────────────────────────────

    @Test
    fun addRecipe_micButtons_arePresent() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onAllNodesWithContentDescription("Dicter")[0].assertIsDisplayed()
    }

    // ── 10. URL invalide bloque la sauvegarde ─────────────────────────────────

    @Test
    fun addRecipe_invalidUrl_showsError() {
        rule.setContent { MaterialTheme { AddEditRecipeScreen(existing = null, onSave = {}, onBack = {}) } }
        rule.onNodeWithTag("form_title").performTextReplacement("Tarte")
        rule.onNodeWithText("Choisir une catégorie").performClick()
        rule.onNodeWithText("Desserts", substring = true).performClick()
        rule.onNodeWithTag("form_ingredients").performTextReplacement("1 citron")
        rule.onNodeWithTag("form_steps").performTextReplacement("Cuire")
        // Scroll jusqu'au champ URL et saisir une URL invalide
        rule.onNodeWithTag("form_source_url").performScrollTo()
        rule.onNodeWithTag("form_source_url").performTextReplacement("pas-une-url")
        rule.onNodeWithText("L'URL doit commencer par http:// ou https://").assertExists()
        rule.onNodeWithText("Enregistrer").assertIsNotEnabled()
    }
}
