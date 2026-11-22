package com.example.recipelibrary.chronodrive

import com.example.recipelibrary.data.chronodrive.ChoiceLabel
import com.example.recipelibrary.data.shopping.ChronodriveSearch
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.nutrition.TestFoods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Liens vers la fiche d'un produit et clés de choix (spec 003, contracts/chrono-link.md, research R4). */
class ChronodriveLinksTest {

    @Test fun productLinks() {
        assertEquals("chronodrive://product/122974", ChronodriveSearch.productAppUri(122974))
        assertEquals("https://www.chronodrive.com/product/122974", ChronodriveSearch.productWebUrl(122974))
        assertNull(ChronodriveSearch.productAppUri(0))
        assertNull(ChronodriveSearch.productWebUrl(-3))
    }

    @Test fun choiceKey_isTheItemKey() {
        assertEquals("food:13005", ChronodriveSearch.choiceKey(ShoppingItem(key = "food:13005", label = "banane")))
        assertEquals("txt:sumac", ChronodriveSearch.choiceKey(ShoppingItem(key = "txt:sumac", label = "sumac")))
    }

    @Test fun extraKey_withoutQuantity_sharedWithComputedItem() {
        assertEquals("txt:papier toilette", ChronodriveSearch.choiceKeyForExtra("2 papier toilette"))
        assertEquals(ChronodriveSearch.choiceKeyForExtra("papier toilette"), ChronodriveSearch.choiceKeyForExtra("2 papier toilette"))
        assertFalse(ChronodriveSearch.choiceKeyForExtra("5 bananes").contains("5"))
        assertEquals("txt:banane", ChronodriveSearch.choiceKeyForExtra("5 bananes"))
    }

    // ── Libellé d'un choix (US3) ───────────────────────────────────────────

    @Test fun choiceLabel() {
        val farine = TestFoods.table.match("farine")!!
        assertEquals(farine.name, ChoiceLabel.of("food:${farine.code}", foods = TestFoods.table))
        assertEquals("papier toilette", ChoiceLabel.of("txt:papier toilette"))
        assertEquals("skyr", ChoiceLabel.of("sub:skyr"))
        assertEquals("zzz:inconnu", ChoiceLabel.of("zzz:inconnu"))
        assertEquals("farine", ChoiceLabel.of("food:${farine.code}", mapOf("food:${farine.code}" to "farine"), TestFoods.table))
    }
}
