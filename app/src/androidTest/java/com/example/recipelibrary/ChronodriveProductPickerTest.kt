package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.ui.FavoritesPickerDialog
import com.example.recipelibrary.ui.ProductPickerDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Boîtes de choix de produit (spec 003, T018 et T033). */
@RunWith(AndroidJUnit4::class)
class ChronodriveProductPickerTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val bananeA = ChronodriveProduct(365920, "banane", null, "Fruits et légumes", true, 0)
    private val bananeB = ChronodriveProduct(305254, "banane", "auchan", "Fruits et légumes", true, 0)
    private val hache = ChronodriveProduct(111111, "viande hachee pur boeuf", "charal", "Boucherie", true, 0)

    private val picked = mutableListOf<ChronodriveProduct>()
    private var none = 0
    private var cancel = 0

    private fun showPicker(candidates: List<ChronodriveProduct> = listOf(bananeA, bananeB)) {
        rule.setContent {
            MaterialTheme {
                ProductPickerDialog("banane", candidates, onPick = { picked += it }, onNone = { none++ }, onCancel = { cancel++ })
            }
        }
    }

    private fun showFavorites(favorites: List<ChronodriveProduct>) {
        rule.setContent {
            MaterialTheme {
                FavoritesPickerDialog("Mon produit", favorites, onPick = { picked += it }, onCancel = { cancel++ })
            }
        }
    }

    @Test fun twoBananas_distinguishedByNumber_andTitleHasItemName() {
        showPicker()
        rule.onNodeWithText("Quel produit pour « banane » ?").assertIsDisplayed()
        rule.onNodeWithTag("product_365920").assertIsDisplayed().assertTextContains("365920", substring = true)
        rule.onNodeWithTag("product_305254").assertIsDisplayed().assertTextContains("305254", substring = true)
            .assertTextContains("auchan", substring = true)
    }

    @Test fun tapProduct_callsOnPick_withThatProduct() {
        showPicker()
        rule.onNodeWithTag("product_305254").performClick()
        rule.waitForIdle()
        assertEquals(listOf(bananeB), picked)
        assertEquals(0, none)
        assertEquals(0, cancel)
    }

    @Test fun none_callsOnNone_only() {
        showPicker()
        rule.onNodeWithTag("picker_none").performClick()
        rule.waitForIdle()
        assertEquals(1, none)
        assertEquals(emptyList<ChronodriveProduct>(), picked)
        assertEquals(0, cancel)
    }

    @Test fun cancel_callsOnCancel_only() {
        showPicker()
        rule.onNodeWithTag("picker_cancel").performClick()
        rule.waitForIdle()
        assertEquals(1, cancel)
        assertEquals(0, none)
        assertEquals(emptyList<ChronodriveProduct>(), picked)
    }

    @Test fun favoritesPicker_search_filtersByText() {
        showFavorites(listOf(bananeA, bananeB, hache))
        rule.onNodeWithTag("product_111111").assertIsDisplayed()
        rule.onNodeWithTag("favorites_search").performTextInput("viande")
        rule.waitForIdle()
        rule.onNodeWithTag("product_111111").assertIsDisplayed()
        // LazyColumn (Compose 1.5) peut garder des nœuds retirés : on vérifie qu'ils ne sont plus affichés
        rule.onNodeWithTag("product_365920").assertIsNotDisplayed()
        rule.onNodeWithTag("product_305254").assertIsNotDisplayed()
    }

    @Test fun favoritesPicker_tapProduct_callsOnPick() {
        showFavorites(listOf(bananeA, hache))
        rule.onNodeWithTag("product_365920").performClick()
        rule.waitForIdle()
        assertEquals(listOf(bananeA), picked)
    }

    @Test fun favoritesPicker_cancel_callsOnCancel() {
        showFavorites(listOf(bananeA))
        rule.onNodeWithTag("favorites_cancel").performClick()
        rule.waitForIdle()
        assertEquals(1, cancel)
    }

    @Test fun favoritesPicker_withoutFavorites_showsEmptyMessage() {
        showFavorites(emptyList())
        rule.onNodeWithTag("favorites_empty").assertIsDisplayed()
        rule.onNodeWithTag("favorites_search").assertDoesNotExist()
    }
}
