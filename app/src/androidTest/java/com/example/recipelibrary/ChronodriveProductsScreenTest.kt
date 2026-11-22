package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.chronodrive.ChoiceWithProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.ProductChoice
import com.example.recipelibrary.ui.ChronodriveProductsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Écran « Mes produits Chronodrive » (spec 003, T017, T024, T025, T032) : écran sans état, callbacks enregistrés par le test. */
@RunWith(AndroidJUnit4::class)
class ChronodriveProductsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val bananeBio = ChronodriveProduct(365920, "banane", null, "Fruits et légumes", true, 0)
    private val bananeAuchan = ChronodriveProduct(305254, "banane", "auchan", "Fruits et légumes", true, 0)
    private val hache = ChronodriveProduct(111111, "viande hachee pur boeuf", "charal", "Boucherie", true, 0)
    private val ancien = ChronodriveProduct(222222, "yaourt nature", "danone", "Crèmerie", false, 0)

    private val imports = mutableListOf<Unit>()
    private val changes = mutableListOf<Pair<String, Long>>()
    private val forgets = mutableListOf<String>()

    private fun show(
        products: List<ChronodriveProduct> = emptyList(),
        choices: List<ChoiceWithProduct> = emptyList(),
        message: String? = null
    ) {
        rule.setContent {
            MaterialTheme {
                ChronodriveProductsScreen(
                    products = products, choices = choices,
                    labelOf = { k -> if (k == "food:1") "banane" else k.substringAfter(':') },
                    message = message,
                    onImport = { imports += Unit },
                    onChange = { k, id -> changes += k to id },
                    onForget = { forgets += it }
                )
            }
        }
    }

    private fun choice(key: String, p: ChronodriveProduct?, id: Long = p?.productId ?: 999) =
        ChoiceWithProduct(ProductChoice(key, id, 0), p)

    @Test fun emptyList_showsInvitation_andImportButtonCallsOnImport() {
        show()
        rule.onNodeWithTag("products_intro").assertIsDisplayed().assertTextContains("Importe le fichier", substring = true)
        rule.onNodeWithTag("import_favorites").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(1, imports.size)
    }

    @Test fun products_groupedByAisle_withNameBrandAndNumber() {
        show(products = listOf(bananeBio, bananeAuchan, hache))
        rule.onNodeWithTag("products_intro").assertTextContains("3 favoris", substring = true)
        rule.onNodeWithTag("fav_aisle_Fruits et légumes").assertIsDisplayed()
        rule.onNodeWithTag("fav_365920").assertIsDisplayed().assertTextContains("banane", substring = true)
            .assertTextContains("365920", substring = true)
        rule.onNodeWithTag("fav_305254").assertIsDisplayed().assertTextContains("auchan", substring = true)
            .assertTextContains("305254", substring = true)
        rule.onNodeWithTag("products_list").performScrollToNode(hasTestTag("fav_111111"))
        rule.onNodeWithTag("fav_aisle_Boucherie").assertIsDisplayed()
        rule.onNodeWithTag("fav_111111").assertIsDisplayed().assertTextContains("charal", substring = true)
    }

    @Test fun product_notStillFavorite_showsNoLongerFavorite() {
        show(products = listOf(ancien))
        rule.onNodeWithTag("fav_222222").assertIsDisplayed().assertTextContains("plus en favori", substring = true)
    }

    @Test fun importMessage_isDisplayed() {
        show(message = "124 produits Chronodrive importés")
        rule.onNodeWithTag("import_message").assertIsDisplayed().assertTextContains("124 produits Chronodrive importés")
    }

    @Test fun noMessage_noImportMessageNode() {
        show()
        rule.onNodeWithTag("import_message").assertDoesNotExist()
    }

    @Test fun choices_oneRowPerChoice_withLabelProductAndNumber() {
        show(
            products = listOf(bananeBio, hache),
            choices = listOf(choice("food:1", bananeBio), choice("txt:boeuf", hache))
        )
        rule.onNodeWithTag("choice_food:1").assertIsDisplayed()
            .assertTextContains("banane", substring = true).assertTextContains("365920", substring = true)
        rule.onNodeWithTag("choice_txt:boeuf").assertIsDisplayed()
            .assertTextContains("boeuf", substring = true).assertTextContains("charal", substring = true)
            .assertTextContains("111111", substring = true)
    }

    @Test fun choice_productNoLongerFavorite_isFlagged() {
        show(products = listOf(ancien), choices = listOf(choice("txt:yaourt", ancien)))
        rule.onNodeWithTag("choice_txt:yaourt").assertTextContains("plus en favori", substring = true)
    }

    @Test fun noChoices_showsEmptyMessage() {
        show(products = listOf(bananeBio))
        rule.onNodeWithTag("choices_empty").assertIsDisplayed()
    }

    @Test fun choiceWithMissingProduct_showsReadableMention() {
        show(choices = listOf(choice("txt:cumin", null, id = 777777)))
        rule.onNodeWithTag("choice_txt:cumin").assertIsDisplayed()
            .assertTextContains("777777", substring = true)
            .assertTextContains("plus dans tes favoris", substring = true)
    }

    @Test fun forget_callsOnForget_withKey() {
        show(products = listOf(bananeBio), choices = listOf(choice("food:1", bananeBio)))
        rule.onNodeWithTag("forget_food:1").performClick()
        rule.waitForIdle()
        assertEquals(listOf("food:1"), forgets)
        assertEquals(emptyList<Pair<String, Long>>(), changes)
    }

    @Test fun change_opensFavoritesList_searchFilters_pickCallsOnChange() {
        show(
            products = listOf(bananeBio, bananeAuchan, hache),
            choices = listOf(choice("food:1", bananeBio))
        )
        rule.onNodeWithTag("change_food:1").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("favorites_picker").assertIsDisplayed()
        rule.onNodeWithTag("favorites_search").performTextInput("viande")
        rule.waitForIdle()
        rule.onNodeWithTag("product_111111").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(listOf("food:1" to 111111L), changes)
        assertEquals(emptyList<String>(), forgets)
    }

    @Test fun change_cancel_doesNotCallOnChange() {
        show(products = listOf(bananeBio), choices = listOf(choice("food:1", bananeBio)))
        rule.onNodeWithTag("change_food:1").performClick()
        rule.onNodeWithTag("favorites_cancel").performClick()
        rule.waitForIdle()
        assertEquals(emptyList<Pair<String, Long>>(), changes)
    }
}
