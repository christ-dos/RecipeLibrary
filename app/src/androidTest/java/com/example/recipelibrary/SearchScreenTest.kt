package com.example.recipelibrary

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.SearchScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun recipes() = listOf(
        Recipe(id = "1", title = "Mousse au chocolat", category = "Desserts",
               tags = listOf("chocolat", "rapide"),
               ingredients = listOf("200 g chocolat", "4 œufs"), steps = listOf("Fondre", "Mélanger")),
        Recipe(id = "2", title = "Soupe de légumes", category = "Soupes",
               tags = listOf("healthy", "végétarien"),
               ingredients = listOf("Carottes", "Poireaux"), steps = listOf("Couper", "Cuire")),
        Recipe(id = "3", title = "Tarte aux fraises", category = "Desserts",
               ingredients = listOf("500 g fraises", "Pâte"), steps = listOf("Garnir"))
    )

    // ── 1. État inactif : sections visibles ───────────────────────────────────

    @Test
    fun searchScreen_idle_showsCategoriesSection() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("Parcourir par catégorie").assertIsDisplayed()
    }

    @Test
    fun searchScreen_idle_showsQuickFiltersSection() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("Filtres rapides").assertIsDisplayed()
    }

    @Test
    fun searchScreen_idle_showsCategoryTiles() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("Desserts").assertIsDisplayed()
        rule.onNodeWithText("Soupes").assertIsDisplayed()
    }

    @Test
    fun searchScreen_idle_noRecentSearchSection() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("Recherches récentes").assertDoesNotExist()
        rule.onNodeWithText("Aucune recette ne correspond à votre recherche.").assertDoesNotExist()
    }

    // ── 2. Recherche par titre ────────────────────────────────────────────────

    @Test
    fun searchScreen_typing_showsMatchingResults() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("Mousse")
        rule.onNodeWithText("Mousse au chocolat").assertIsDisplayed()
        rule.onNodeWithText("Soupe de légumes").assertDoesNotExist()
    }

    @Test
    fun searchScreen_typing_searchesInIngredients() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("carottes")
        rule.onNodeWithText("Soupe de légumes").assertIsDisplayed()
        rule.onNodeWithText("Mousse au chocolat").assertDoesNotExist()
    }

    @Test
    fun searchScreen_typing_searchesInCategory() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("Soupes")
        rule.onNodeWithText("Soupe de légumes").assertIsDisplayed()
    }

    // ── 3. État vide si aucun résultat ────────────────────────────────────────

    @Test
    fun searchScreen_noMatch_showsEmptyMessage() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("xyzinconnu")
        rule.onNodeWithText("Aucune recette ne correspond à votre recherche.").assertIsDisplayed()
    }

    // ── 4. Cliquer une catégorie lance la recherche ───────────────────────────

    @Test
    fun searchScreen_categoryTile_triggersSearch() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("Desserts").performClick()
        rule.onNodeWithText("Mousse au chocolat").assertIsDisplayed()
        rule.onNodeWithText("Tarte aux fraises").assertIsDisplayed()
        rule.onNodeWithText("Soupe de légumes").assertDoesNotExist()
    }

    // ── 5. Les filtres rapides viennent des tags des recettes ─────────────────

    @Test
    fun searchScreen_quickFilters_showsActualTags() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("chocolat").assertIsDisplayed()
        rule.onNodeWithText("healthy").assertIsDisplayed()
    }

    @Test
    fun searchScreen_quickFilter_click_triggersSearch() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), {}, {}) } }
        rule.onNodeWithText("chocolat").performClick()
        rule.onNodeWithText("Mousse au chocolat").assertIsDisplayed()
        rule.onNodeWithText("Soupe de légumes").assertDoesNotExist()
    }

    // ── 6. Les recherches récentes s'accumulent après un clic ─────────────────

    @Test
    fun searchScreen_recentSearches_appearsAfterClickingResult() {
        rule.setContent { MaterialTheme { SearchScreen(recipes(), onRecipeClick = {}, onBack = {}) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("Mousse")
        rule.onNodeWithText("Mousse au chocolat").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("")
        rule.onNodeWithText("Recherches récentes").assertIsDisplayed()
        rule.onNodeWithText("Mousse").assertIsDisplayed()
    }

    // ── 7. Fallback tags quand les recettes n'ont pas de tags ─────────────────

    @Test
    fun searchScreen_fallbackFilters_whenNoTags() {
        val noTagRecipes = listOf(
            Recipe(id = "1", title = "Soupe", category = "Soupes",
                   ingredients = listOf("Légumes"), steps = listOf("Cuire"))
        )
        rule.setContent { MaterialTheme { SearchScreen(noTagRecipes, {}, {}) } }
        rule.onNodeWithText("Filtres rapides").assertIsDisplayed()
        rule.onNodeWithText("Chocolat").assertIsDisplayed()
        rule.onNodeWithText("Rapide").assertIsDisplayed()
    }
}
