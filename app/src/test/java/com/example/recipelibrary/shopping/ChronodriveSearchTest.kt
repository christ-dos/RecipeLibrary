package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.shopping.ChronodriveSearch
import com.example.recipelibrary.data.shopping.ShoppingListBuilder
import com.example.recipelibrary.nutrition.TestFoods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Terme et adresse du bouton « Chronodrive » (contracts/chronodrive-search.md, T026). */
class ChronodriveSearchTest {

    private val parser = IngredientParser(TestFoods.table)

    private fun termOf(line: String): String {
        val r = Recipe(id = "r", title = "R", ingredients = listOf(line))
        return ChronodriveSearch.term(ShoppingListBuilder.build(listOf(MenuEntry(recipeId = "r")), mapOf("r" to r), parser) { null }.single())
    }

    @Test fun recognizedFood_firstAlias_neverQuantity() {
        assertEquals("farine", termOf("450 g de farine"))
        assertEquals("creme fraiche", termOf("20 cl de crème fraîche"))
        assertEquals("oeuf", termOf("6 œufs"))
        assertEquals("fromage blanc 0%", termOf("500 g de skyr"))
    }

    @Test fun unknownFood_textWithoutQuantityUnitOrArticle() {
        assertEquals("Sumac de Turquie", termOf("1 c. à café de Sumac de Turquie"))
        assertEquals("fonio", termOf("200 g de fonio, rincé"))
    }

    @Test fun extra_withoutLeadingQuantity() {
        assertEquals("papier toilette", ChronodriveSearch.termForExtra("papier toilette"))
        assertEquals("papier toilette", ChronodriveSearch.termForExtra("2 papier toilette"))
        assertEquals("éponges", ChronodriveSearch.termForExtra("  3   éponges "))
        assertEquals("12", ChronodriveSearch.termForExtra("12"))
    }

    @Test fun termLength_1to60() {
        val t = ChronodriveSearch.termForExtra("a".repeat(100))
        assertEquals(60, t.length)
        assertTrue(ChronodriveSearch.termForExtra("x").isNotEmpty())
    }

    @Test fun searchUrl_encoded() {
        assertEquals("https://www.chronodrive.com/search/creme%20fraiche", ChronodriveSearch.searchUrl("creme fraiche"))
        assertEquals("https://www.chronodrive.com/search/cr%C3%A8me%20fra%C3%AEche", ChronodriveSearch.searchUrl("crème fraîche"))
    }
}
