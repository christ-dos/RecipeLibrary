package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.ShoppingExtra
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.shopping.Aisles
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.data.shopping.ShoppingText
import com.example.recipelibrary.nutrition.TestFoods
import com.example.recipelibrary.ui.MenuViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Liste rangée par rayon (spec 002, story 4, T035). */
class AislesTest {

    @Test fun unknownOrMissingAisle_isOther() {
        assertEquals("Autres", Aisles.of(null))
        assertEquals("Autres", Aisles.of("Rayon inventé"))
        assertEquals("Crèmerie", Aisles.of("Crèmerie"))
    }

    @Test fun group_followsFixedOrder_andOmitsEmptyAisles() {
        val items = listOf(
            ShoppingItem(key = "a", label = "sucre", aisle = "Épicerie sucrée"),
            ShoppingItem(key = "b", label = "pomme", aisle = "Fruits et légumes"),
            ShoppingItem(key = "c", label = "sumac"),
            ShoppingItem(key = "d", label = "lait", aisle = "Crèmerie"),
            ShoppingItem(key = "e", label = "beurre", aisle = "Crèmerie")
        )
        val groups = Aisles.group(items)
        assertEquals(listOf("Fruits et légumes", "Crèmerie", "Épicerie sucrée", "Autres"), groups.map { it.first })
        assertEquals(listOf("lait", "beurre"), groups[1].second.map { it.label })
    }

    @Test fun headersOnlyWhenAKnownAisleExists() {
        assertFalse(Aisles.showHeaders(listOf(ShoppingItem(key = "c", label = "sumac"))))
        assertTrue(Aisles.showHeaders(listOf(ShoppingItem(key = "d", label = "lait", aisle = "Crèmerie"))))
    }

    @Test fun realList_farineLaitPommePoulet_fourAisles() {
        val r = Recipe(
            id = "r", title = "Test", servings = 4, servingsSource = Recipe.SERVINGS_USER,
            ingredients = listOf("250 g de farine", "50 cl de lait", "2 pommes", "1 poulet")
        )
        val list = MenuViewModel.buildList(listOf(MenuEntry(id = "e", recipeId = "r", servings = 4)), mapOf("r" to r), TestFoods.table)
        val byLabel = list.associate { it.label to it.aisle }
        assertEquals("Épicerie sucrée", byLabel["farine"])
        assertEquals("Crèmerie", byLabel["lait"])
        assertEquals("Fruits et légumes", byLabel["pomme"])
        assertEquals("Boucherie et poisson", byLabel["poulet"])
    }

    @Test fun sharedText_withAisleTitles_extrasAndSeasoningsInOther() {
        val text = ShoppingText.build(
            listOf(
                ShoppingItem(key = "food:9440", label = "farine", mass = 700.0, aisle = "Épicerie sucrée"),
                ShoppingItem(key = "food:13039", label = "pomme", pieces = 6.0, aisle = "Fruits et légumes"),
                ShoppingItem(key = "food:19042", label = "lait", volume = 700.0, aisle = "Crèmerie"),
                ShoppingItem(key = "txt:sumac", label = "sumac"),
                ShoppingItem(key = "food:11017", label = "sel", seasoning = true, aisle = "Épicerie salée")
            ),
            listOf(ShoppingExtra(text = "papier toilette")),
            recipeCount = 2
        )
        assertEquals(
            """
            Liste de courses (2 recettes)

            Fruits et légumes
            - pomme : 6

            Crèmerie
            - lait : 70 cl

            Épicerie sucrée
            - farine : 700 g

            Autres
            - sumac
            - papier toilette
            - sel (à vérifier)
            """.trimIndent(),
            text
        )
    }

    @Test fun sharedText_onlyExtrasWithKnownAisles_addsOtherSection() {
        val text = ShoppingText.build(
            listOf(ShoppingItem(key = "food:19042", label = "lait", volume = 700.0, aisle = "Crèmerie")),
            listOf(ShoppingExtra(text = "éponges")),
            recipeCount = 1
        )
        assertEquals("Liste de courses (1 recette)\n\nCrèmerie\n- lait : 70 cl\n\nAutres\n- éponges", text)
    }

    @Test fun boursin_isRecognised_inCreamery() {
        // Signalé par l'utilisatrice le 2026-11-22 : Boursin tombait dans « Autres »
        val food = TestFoods.table.match("1 boursin cuisine ail et fines herbes")
        assertEquals("19530", food?.code)
        assertEquals("Crèmerie", food?.aisle)
    }
}
