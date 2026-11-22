package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.shopping.QuantityFormat
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.data.shopping.ShoppingListBuilder
import com.example.recipelibrary.data.shopping.ShoppingSource
import com.example.recipelibrary.nutrition.TestFoods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Calcul de la liste de courses avec la vraie table d'aliments (spec 002, T018). */
class ShoppingListBuilderTest {

    private val parser = IngredientParser(TestFoods.table)

    private fun build(vararg pairs: Pair<Recipe, MenuEntry>): List<ShoppingItem> =
        ShoppingListBuilder.build(pairs.map { it.second }, pairs.associate { it.first.id to it.first }, parser) { it.servings }

    private fun List<ShoppingItem>.item(key: String) = single { it.key == key }
    private fun List<ShoppingItem>.text(key: String) = QuantityFormat.of(item(key))

    private val crepes = Recipe(
        id = "crepes", title = "Crêpes", servings = 4, servingsSource = Recipe.SERVINGS_USER,
        ingredients = listOf("Pour la pâte :", "250 g de farine", "4 œufs", "50 cl de lait", "1 pincée de sel", "")
    )
    private val quiche = Recipe(
        id = "quiche", title = "Quiche", // sans parts
        ingredients = listOf("1 pâte brisée", "3 oeufs", "200 ml de lait", "200 g de lardons", "sel, poivre")
    )

    @Test fun servingsFactor_fourToEight_isDouble() {
        val l = build(crepes to MenuEntry(recipeId = "crepes", servings = 8))
        assertEquals("500 g", l.text("food:9440"))
        assertEquals("8", l.text("food:22000"))
        assertEquals("1 l", l.text("food:19042"))
    }

    @Test fun recipeWithoutServings_usesTimes() {
        assertEquals("3", build(quiche to MenuEntry(recipeId = "quiche")).text("food:22000"))
        val x3 = build(quiche to MenuEntry(recipeId = "quiche", times = 3))
        assertEquals("9", x3.text("food:22000"))
        assertEquals("600 g", x3.text("food:28720"))
    }

    @Test fun groupedByFood_andSummedByKind() {
        val l = build(crepes to MenuEntry(recipeId = "crepes", servings = 4), quiche to MenuEntry(recipeId = "quiche"))
        // « œufs » et « oeufs » : un seul article
        assertEquals(1, l.count { it.key == "food:22000" })
        assertEquals("7", l.text("food:22000"))
        // 50 cl + 200 ml = 70 cl
        assertEquals("70 cl", l.text("food:19042"))
        assertEquals("oeuf", l.item("food:22000").label)
    }

    @Test fun singularAndPlural_sameItem() {
        val a = Recipe(id = "a", title = "A", ingredients = listOf("2 tomates", "300 g de tomate"))
        val l = build(a to MenuEntry(recipeId = "a"))
        assertEquals("300 g + 2", l.text("food:20047"))
    }

    @Test fun differentKinds_sideBySide_andLooseSummed() {
        val a = Recipe(id = "a", title = "Sauce", ingredients = listOf("400 g de tomates", "1 boîte de tomates"))
        val b = Recipe(id = "b", title = "Chili", ingredients = listOf("1 boîte de tomates", "1 botte de radis"))
        val l = build(a to MenuEntry(recipeId = "a"), b to MenuEntry(recipeId = "b"))
        assertEquals("400 g + 2 boîtes", l.text("food:20047"))
        assertEquals("1 botte", l.text("food:20045"))
    }

    @Test fun vagueAndUnknown_keptAsWritten() {
        val a = Recipe(id = "a", title = "A", ingredients = listOf("du sucre", "1 c. à café de Sumac de Turquie", "200 g de fonio", "100 g de Fonio"))
        val l = build(a to MenuEntry(recipeId = "a"))
        assertEquals("", l.text("food:31016"))           // sans quantité : l'article existe, sans quantité
        // Aliments absents de la table (vérifié) : gardés tels qu'écrits, casse comprise
        val sumac = l.single { it.label == "Sumac de Turquie" }
        assertTrue(sumac.key.startsWith("txt:"))
        assertEquals("5 ml", QuantityFormat.of(sumac))
        assertEquals("Sumac de Turquie", sumac.searchTerm)
        val fonio = l.single { it.key == "txt:fonio" }  // « fonio » et « Fonio » regroupés
        assertEquals("300 g", QuantityFormat.of(fonio))
    }

    @Test fun headersAndToCompleteIgnored() {
        val a = Recipe(id = "a", title = "A", ingredients = listOf("Pour la sauce :", "", "À compléter", "250 g de farine"))
        assertEquals(listOf("food:9440"), build(a to MenuEntry(recipeId = "a")).map { it.key })
    }

    @Test fun seasonings_onceWithoutQuantity() {
        val l = build(crepes to MenuEntry(recipeId = "crepes", servings = 4), quiche to MenuEntry(recipeId = "quiche"))
        val sel = l.item("food:11017")
        assertTrue(sel.seasoning)
        assertEquals("", QuantityFormat.of(sel))
        assertNull(sel.loose["pincee"])
        assertEquals(listOf("Crêpes", "Quiche"), sel.sources.map { it.title })
    }

    @Test fun sources_sameRecipeTwiceCountedTwice() {
        val l = build(
            crepes to MenuEntry(recipeId = "crepes", servings = 4),
            quiche to MenuEntry(recipeId = "quiche"),
            crepes to MenuEntry(recipeId = "crepes", servings = 4)
        )
        assertEquals(listOf(ShoppingSource("crepes", "Crêpes", 2), ShoppingSource("quiche", "Quiche", 1)), l.item("food:22000").sources)
        assertEquals("500 g", l.text("food:9440"))
    }

    @Test fun entryWithMissingRecipe_ignored() {
        val l = ShoppingListBuilder.build(
            listOf(MenuEntry(recipeId = "disparue"), MenuEntry(recipeId = "crepes", servings = 4)),
            mapOf("crepes" to crepes), parser
        ) { it.servings }
        assertEquals("250 g", l.text("food:9440"))
        assertTrue(l.all { s -> s.sources.all { it.recipeId == "crepes" } })
    }

    @Test fun estimatedServings_usedForFactor() {
        val l = ShoppingListBuilder.build(listOf(MenuEntry(recipeId = "quiche", servings = 12)), mapOf("quiche" to quiche), parser) { 6 }
        assertEquals("6", l.text("food:22000"))
    }

    @Test fun emptyMenu_emptyList() {
        assertEquals(emptyList<ShoppingItem>(), build())
    }
}
