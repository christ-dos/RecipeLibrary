package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.RecipeDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** « Ajouter au menu » et avertissement avant suppression d'une recette au menu (spec 002, T015-T016). */
@RunWith(AndroidJUnit4::class)
class MenuDeleteWarningTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val recipe = Recipe(id = "r1", title = "Poulet roti", ingredients = listOf("1 poulet"))

    private fun openDelete(menuCount: Int) {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(recipe, onBack = {}, onEdit = {}, onDelete = {}, menuCount = menuCount) }
        }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.onNodeWithText("Supprimer").performClick()
    }

    @Test fun deleteDialog_warnsWhenInMenu() {
        openDelete(menuCount = 2)
        rule.onNodeWithTag("delete_text").assertTextContains("Elle est au menu (2 fois) et en sera retirée.", substring = true)
    }

    @Test fun deleteDialog_noWarningWhenNotInMenu() {
        openDelete(menuCount = 0)
        rule.onNodeWithTag("delete_text").assertTextEquals("Cette action est définitive.")
    }

    @Test fun addToMenu_visibleButtonUnderTitle() {
        var added = 0
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(recipe, onBack = {}, onEdit = {}, onDelete = {}, onAddToMenu = { added++ }, menuCount = 2) }
        }
        rule.onNodeWithTag("detail_list").performScrollToNode(hasTestTag("add_to_menu"))
        rule.onNodeWithTag("add_to_menu").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(1, added)
        rule.onNodeWithTag("in_menu").assertTextEquals("Au menu (2 fois)")
    }

    @Test fun addToMenu_hiddenWithoutCallback() {
        rule.setContent {
            MaterialTheme { RecipeDetailScreen(recipe, onBack = {}, onEdit = {}, onDelete = {}) }
        }
        rule.onNodeWithTag("add_to_menu").assertDoesNotExist()
    }
}
