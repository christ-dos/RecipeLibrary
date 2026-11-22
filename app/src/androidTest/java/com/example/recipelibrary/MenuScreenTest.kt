package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.RecipeEdits
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.ui.MenuActions
import com.example.recipelibrary.ui.MenuScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Écran du menu (spec 002, T011). Le menu est tenu en mémoire par le test, comme le ferait la base. */
@RunWith(AndroidJUnit4::class)
class MenuScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val crepes = Recipe(id = "crepes", title = "Crepes", servings = 4, servingsSource = Recipe.SERVINGS_USER, ingredients = listOf("250 g de farine"))
    private val quiche = Recipe(id = "quiche", title = "Quiche lorraine", ingredients = listOf("3 oeufs"))
    private val tarte = Recipe(id = "tarte", title = "Tarte aux pommes", servings = 6, servingsSource = Recipe.SERVINGS_USER)

    private val recipes = mutableStateListOf(crepes, quiche, tarte)
    private val entries = mutableStateListOf<MenuEntry>()
    /** Recettes enregistrées par l'écran : seule « Indiquer les parts de la recette » doit en produire. */
    private val savedRecipes = mutableListOf<Recipe>()
    private var n = 0

    private fun update(id: String, f: (MenuEntry) -> MenuEntry) {
        val i = entries.indexOfFirst { it.id == id }
        entries[i] = f(entries[i])
    }

    private fun show() {
        rule.setContent {
            MaterialTheme {
                MenuScreen(
                    entries = entries.toList(),
                    recipesById = recipes.associateBy { it.id },
                    allRecipes = recipes.toList(),
                    shopping = emptyList(),
                    actions = MenuActions(
                        onAdd = { r -> entries += MenuEntry(id = "e${n++}", recipeId = r.id, servings = r.servings, position = entries.size) },
                        onServings = { id, s -> update(id) { it.copy(servings = s) } },
                        onTimes = { id, t -> update(id) { it.copy(times = t) } },
                        onSetRecipeServings = { e, r, s ->
                            val saved = RecipeEdits.withServings(r, s, now = 1L)
                            savedRecipes += saved
                            recipes[recipes.indexOfFirst { it.id == r.id }] = saved
                            update(e.id) { it.copy(servings = s) }
                        },
                        onDay = { id, d -> update(id) { it.copy(day = d) } },
                        onRemove = { id -> entries.removeAll { it.id == id } },
                        onClear = { entries.clear() }
                    )
                )
            }
        }
    }

    private fun addBySearch(query: String, recipeId: String) {
        rule.onNodeWithTag("add_recipe").performClick()
        rule.onNodeWithTag("add_search").performTextInput(query)
        rule.onNodeWithTag("add_result_$recipeId").performClick()
        rule.waitForIdle()
    }

    @Test fun emptyMenu_showsInvitation() {
        show()
        rule.onNodeWithTag("menu_empty").assertIsDisplayed()
        rule.onNodeWithTag("clear_menu").assertDoesNotExist()
    }

    @Test fun addBySearch_thenChangeServings() {
        show()
        addBySearch("quiche", "quiche")
        assertEquals(listOf("quiche"), entries.map { it.recipeId })
        addBySearch("crep", "crepes")
        rule.onNodeWithTag("servings_e1_value").assertTextEquals("4 parts")
        rule.onNodeWithTag("servings_e1_plus").performClick()
        rule.onNodeWithTag("servings_e1_plus").performClick()
        rule.onNodeWithTag("servings_e1_value").assertTextEquals("6 parts")
        assertEquals(6, entries[1].servings)
        assertEquals(emptyList<Recipe>(), savedRecipes)
    }

    @Test fun recipeWithoutServings_timesThenSetRecipeServings() {
        show()
        addBySearch("quiche", "quiche")
        rule.onNodeWithTag("times_e0_value").assertTextEquals("× 1 recette")
        rule.onNodeWithTag("times_e0_minus").assertIsNotEnabled()
        rule.onNodeWithTag("times_e0_plus").performClick()
        rule.onNodeWithTag("times_e0_value").assertTextEquals("× 2 recette")
        assertEquals(2, entries[0].times)
        assertEquals(emptyList<Recipe>(), savedRecipes) // changer le nombre de fois n'écrit pas la recette

        rule.onNodeWithTag("set_recipe_servings_e0").performClick()
        rule.onNodeWithTag("number_field").performTextInput("6")
        rule.onNodeWithTag("number_ok").performClick()
        rule.waitForIdle()
        assertEquals(1, savedRecipes.size)
        assertEquals(6, savedRecipes[0].servings)
        assertEquals(Recipe.SERVINGS_USER, savedRecipes[0].servingsSource)
        assertEquals(quiche.copy(servings = 6, servingsSource = Recipe.SERVINGS_USER, updatedAt = 1L), savedRecipes[0])
        rule.onNodeWithTag("servings_e0_value").assertTextEquals("6 parts")
        rule.onNodeWithTag("set_recipe_servings_e0").assertDoesNotExist()
    }

    @Test fun setRecipeServings_outOfRangeRefused() {
        show()
        addBySearch("quiche", "quiche")
        rule.onNodeWithTag("set_recipe_servings_e0").performClick()
        rule.onNodeWithTag("number_field").performTextInput("0")
        rule.onNodeWithTag("number_ok").assertIsNotEnabled()
        rule.onNodeWithTag("number_field").performTextReplacement("101")
        rule.onNodeWithTag("number_ok").assertIsNotEnabled()
        assertEquals(emptyList<Recipe>(), savedRecipes)
    }

    @Test fun chooseDay_groupsByDay_thenRemoveDay() {
        show()
        addBySearch("tarte", "tarte")
        addBySearch("crep", "crepes")
        rule.onNodeWithTag("group_0").assertTextEquals("Sans jour")
        rule.onNodeWithTag("day_e1").performClick()
        rule.onNodeWithTag("day_option_3").performClick()
        rule.waitForIdle()
        assertEquals(3, entries[1].day)
        rule.onNodeWithTag("group_3").assertTextEquals("Mercredi")
        rule.onNodeWithTag("group_0").assertExists() // la tarte reste sans jour
        rule.onNodeWithTag("day_e1").performClick()
        rule.onNodeWithTag("day_option_0").performClick()
        rule.waitForIdle()
        assertEquals(null, entries[1].day)
        rule.onNodeWithTag("group_3").assertIsNotDisplayed()
    }

    @Test fun sameRecipeTwice_twoEntries_thenRemoveOne() {
        show()
        addBySearch("crep", "crepes")
        addBySearch("crep", "crepes")
        assertEquals(listOf("crepes", "crepes"), entries.map { it.recipeId })
        rule.onNodeWithTag("entry_e0").assertExists()
        rule.onNodeWithTag("entry_e1").assertExists()
        rule.onNodeWithTag("remove_e0").performClick()
        rule.waitForIdle()
        assertEquals(listOf("e1"), entries.map { it.id })
        rule.onNodeWithTag("entry_e0").assertIsNotDisplayed() // LazyColumn (Compose 1.5) garde un instant le nœud retiré
    }

    @Test fun clearMenu_asksConfirmation() {
        show()
        addBySearch("crep", "crepes")
        rule.onNodeWithTag("clear_menu").performClick()
        rule.onNodeWithText("Vider le menu ?").assertIsDisplayed()
        rule.onNodeWithText("Annuler").performClick()
        assertEquals(1, entries.size)
        rule.onNodeWithTag("clear_menu").performClick()
        rule.onNodeWithTag("clear_confirm").performClick()
        rule.waitForIdle()
        assertEquals(0, entries.size)
        rule.onNodeWithTag("menu_empty").assertIsDisplayed()
        assertEquals(emptyList<Recipe>(), savedRecipes)
    }
}
