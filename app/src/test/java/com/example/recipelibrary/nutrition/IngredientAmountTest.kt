package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.nutrition.Amount
import com.example.recipelibrary.data.nutrition.AmountKind.MASS
import com.example.recipelibrary.data.nutrition.AmountKind.PIECE
import com.example.recipelibrary.data.nutrition.AmountKind.SACHET
import com.example.recipelibrary.data.nutrition.AmountKind.VOLUME
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.nutrition.LooseQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Quantités pour la liste de courses (spec 002, T001-T002). */
class IngredientAmountTest {

    private val parser = IngredientParser(TestFoods.table)
    private fun amount(line: String) = parser.parse(line).amount
    private fun loose(line: String) = parser.parse(line).looseQuantity

    @Test fun masses() {
        assertEquals(Amount(MASS, 250.0), amount("250 g de farine"))
        assertEquals(Amount(MASS, 1500.0), amount("1,5 kg de pommes de terre"))
        assertEquals(Amount(MASS, 200.0), amount("Farine : 200 g"))
    }

    @Test fun volumesStayVolumes() {
        assertEquals(Amount(VOLUME, 500.0), amount("50 cl de lait"))
        assertEquals(Amount(VOLUME, 200.0), amount("200 ml de lait"))
        assertEquals(Amount(VOLUME, 30.0), amount("2 c. à soupe d'huile"))
        assertEquals(Amount(VOLUME, 5.0), amount("1 c. à café de cannelle"))
    }

    @Test fun pieces() {
        assertEquals(Amount(PIECE, 3.0), amount("3 œufs"))
        assertEquals(Amount(PIECE, 2.5), amount("2 à 3 pommes"))
        assertEquals(Amount(PIECE, 0.5), amount("1/2 citron"))
        assertEquals(Amount(PIECE, 2.0), amount("2 gros oignons"))
    }

    @Test fun sachets() {
        assertEquals(Amount(SACHET, 1.0), amount("1 sachet de levure chimique"))
    }

    @Test fun containerWithWrittenWeightIsMass() {
        assertEquals(Amount(MASS, 400.0), amount("1 boîte de 400 g de tomates"))
        assertNull(loose("1 boîte de 400 g de tomates"))
        assertEquals(Amount(MASS, 400.0), amount("1 boîte (400 g) de tomates concassées"))
    }

    @Test fun vagueQuantitiesKeptAsWritten() {
        assertNull(amount("1 boîte de tomates"))
        assertEquals(LooseQuantity(1.0, "boite"), loose("1 boîte de tomates"))
        assertEquals(LooseQuantity(1.0, "botte"), loose("1 botte de radis"))
        assertEquals(LooseQuantity(2.0, "gousse"), loose("2 gousses d'ail"))
        assertEquals(LooseQuantity(1.0, "pincee"), loose("1 pincée de sel"))
        assertEquals(LooseQuantity(1.0, "noix"), loose("1 noix de beurre"))
    }

    /** « c. à café » est une unité : un aliment inconnu derrière ne doit pas devenir du café. */
    @Test fun unitTextIsNeverTheFood() {
        assertNull(parser.parse("1 c. à café de sumac").food)
        assertEquals("cafe", parser.parse("2 c. à soupe de café soluble").food?.aliases?.first())
        assertEquals("cannelle", parser.parse("1 c. à café de cannelle").food?.aliases?.first())
    }

    @Test fun noQuantity() {
        assertNull(amount("du sucre"))
        assertNull(loose("du sucre"))
        assertNull(amount("sel, poivre"))
        assertNull(amount("quelques feuilles de basilic"))
    }
}
