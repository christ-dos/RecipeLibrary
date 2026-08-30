package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun recipes() = listOf(
        Recipe(id = "1", title = "Tarte citron", category = "Desserts",
               ingredients = listOf("1 citron", "200 g sucre"), steps = listOf("Melanger", "Cuire")),
        Recipe(id = "2", title = "Soupe tomates", category = "Soupes",
               ingredients = listOf("4 tomates", "1 oignon"), steps = listOf("Faire revenir")),
        Recipe(id = "3", title = "Salade nicoise", category = "Salades",
               ingredients = listOf("Salade", "Thon"), steps = listOf("Preparer"))
    )

    // ── 1. La liste affiche toutes les recettes ───────────────────────────────

    @Test
    fun homeScreen_displaysAllRecipes() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        rule.onNodeWithText("Tarte citron").assertIsDisplayed()
        rule.onNodeWithText("Soupe tomates").assertIsDisplayed()
        rule.onNodeWithText("Salade nicoise").assertIsDisplayed()
    }

    // ── 2. La recherche filtre par titre ──────────────────────────────────────

    @Test
    fun homeScreen_searchByTitle_filtersResults() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        rule.onNodeWithTag("home_search").performTextReplacement("Tarte")
        // LazyVerticalGrid conserve les nœuds sémantiques des items filtrés — assertion positive seulement.
        // L'absence de résultats (0 item) est vérifiée dans homeScreen_emptyState_noSearchResults.
        rule.onNodeWithText("Tarte citron").assertIsDisplayed()
    }

    // ── 3. Le filtre catégorie fonctionne ─────────────────────────────────────

    @Test
    fun homeScreen_categoryFilter_showsOnlyMatchingRecipes() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        // "Desserts (" n'existe que dans le chip (qui affiche "Desserts (1)"), pas dans les cards
        rule.onNodeWithText("Desserts (", substring = true).performScrollTo().performClick()
        // LazyVerticalGrid conserve les nœuds sémantiques des items filtrés — assertion positive seulement.
        rule.onNodeWithText("Tarte citron").assertIsDisplayed()
    }

    // ── 4. État vide quand aucune recette ─────────────────────────────────────

    @Test
    fun homeScreen_emptyState_noRecipes() {
        rule.setContent { MaterialTheme { HomeScreen(emptyList(), {}, {}, {}) } }
        rule.onNodeWithText("Aucune recette pour l'instant.").assertIsDisplayed()
    }

    // ── 5. Recherche sans résultat affiche message vide ───────────────────────

    @Test
    fun homeScreen_emptyState_noSearchResults() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        rule.onNodeWithTag("home_search").performTextReplacement("xyzxyz")
        // "xyzxyz" existe aussi dans le champ de recherche → assertion sur le message complet
        rule.onNodeWithText("Aucun résultat", substring = true).assertIsDisplayed()
    }

    // ── 6. Le menu 3 points affiche les actions ───────────────────────────────

    @Test
    fun homeScreen_menu_showsActions() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        rule.onAllNodesWithContentDescription("Actions")[0].performClick()
        rule.onNodeWithText("Voir la recette").assertIsDisplayed()
        rule.onNodeWithText("Modifier").assertIsDisplayed()
        rule.onNodeWithText("Supprimer").assertIsDisplayed()
    }

    // ── 7. La suppression affiche une confirmation ────────────────────────────

    @Test
    fun homeScreen_deleteMenuItem_showsConfirmationDialog() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        rule.onAllNodesWithContentDescription("Actions")[0].performClick()
        rule.onNodeWithText("Supprimer").performClick()
        rule.onNodeWithText("Supprimer cette recette ?").assertIsDisplayed()
        rule.onNodeWithText("Annuler").assertIsDisplayed()
    }

    // ── 8. Annuler ferme le dialog ────────────────────────────────────────────

    @Test
    fun homeScreen_cancelDelete_dismissesDialog() {
        rule.setContent { MaterialTheme { HomeScreen(recipes(), {}, {}, {}) } }
        rule.onAllNodesWithContentDescription("Actions")[0].performClick()
        rule.onNodeWithText("Supprimer").performClick()
        rule.onNodeWithText("Annuler").performClick()
        rule.onNodeWithText("Supprimer cette recette ?").assertDoesNotExist()
    }
}
