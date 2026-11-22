package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.menu.ShoppingExtra
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.data.shopping.ShoppingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Texte partagé (contracts/shopping-text.md, T025). */
class ShoppingTextTest {

    private val farine = ShoppingItem(key = "food:9440", label = "farine", mass = 700.0)
    private val lait = ShoppingItem(key = "food:19042", label = "lait", volume = 700.0)
    private val tomate = ShoppingItem(key = "food:20047", label = "tomate", mass = 400.0, loose = mapOf("boite" to 1.0))
    private val sucre = ShoppingItem(key = "food:31016", label = "sucre")
    private val sel = ShoppingItem(key = "food:11017", label = "sel", seasoning = true)
    private val poivre = ShoppingItem(key = "food:11015", label = "poivre", seasoning = true)

    @Test fun fullText() {
        val text = ShoppingText.build(
            listOf(farine, lait.copy(checked = true), tomate, sucre, sel, poivre),
            listOf(ShoppingExtra(text = "papier toilette"), ShoppingExtra(text = "éponges", checked = true)),
            recipeCount = 5
        )
        assertEquals(
            """
            Liste de courses (5 recettes)

            - farine : 700 g
            - tomate : 400 g + 1 boîte
            - sucre
            - papier toilette
            - sel, poivre (à vérifier)
            """.trimIndent(),
            text
        )
    }

    @Test fun singularHeader() {
        assertEquals("Liste de courses (1 recette)\n\n- lait : 70 cl", ShoppingText.build(listOf(lait), emptyList(), 1))
    }

    @Test fun nothingToShare_null() {
        assertNull(ShoppingText.build(emptyList(), emptyList(), 0))
        assertNull(ShoppingText.build(listOf(farine.copy(checked = true)), listOf(ShoppingExtra(text = "x", checked = true)), 2))
    }

    @Test fun onlyExtras_menuEmpty() {
        assertEquals("Liste de courses (0 recette)\n\n- 2 papier toilette", ShoppingText.build(emptyList(), listOf(ShoppingExtra(text = "2 papier toilette")), 0))
    }
}
