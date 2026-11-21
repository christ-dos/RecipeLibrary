package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.nutrition.Food
import com.example.recipelibrary.data.nutrition.FoodTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodTableTest {

    private val table = TestFoods.table
    private fun code(text: String) = table.match(text)?.code

    @Test fun longestVariantWins() {
        assertEquals("16402", code("beurre demi-sel"))   // beurre demi-sel avant beurre
        assertEquals("16400", code("beurre"))
    }

    @Test fun eggVariants() {
        assertEquals("22000", code("Oeufs"))
        assertEquals("22000", code("œuf"))
        assertEquals("22000", code("3 œufs frais"))
        assertEquals("22001", code("blancs d'œufs"))
    }

    @Test fun wholeWordsOnly() {
        // « riz » ne doit pas trouver un aliment dont le nom contient « ris »
        val small = FoodTable(listOf(
            Food("1", "Ris de veau", listOf("ris de veau"), 100.0, 0.0, 0.0, 0.0),
            Food("2", "Riz", listOf("riz"), 350.0, 0.0, 0.0, 0.0)
        ))
        assertEquals("2", small.match("200 g de riz")?.code)
        assertEquals("1", small.match("ris de veau")?.code)
        assertNull(small.match("risotto"))
        assertEquals("9100", code("riz"))
    }

    @Test fun compositeNames() {
        assertEquals("4008", code("pommes de terre"))
        assertEquals("13620", code("pommes"))
        assertEquals("17270", code("huile d'olive"))
        assertEquals("17440", code("huile"))
        assertEquals("9440", code("farine de blé"))
    }

    @Test fun skyrCountsAsFatFreeFromageBlanc() {
        // Pas de skyr dans Ciqual 2020 : le plus proche est le fromage blanc 0 % (19644).
        assertEquals("19644", code("150 g de skyr"))
        assertEquals("19644", code("2 pots de skyr nature"))
    }

    @Test fun unknownTextGivesNull() {
        assertNull(code("xyzzy"))
        assertNull(code(""))
    }
}
