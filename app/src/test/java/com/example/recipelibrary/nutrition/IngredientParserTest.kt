package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.nutrition.IngredientIssue
import com.example.recipelibrary.data.nutrition.IngredientIssue.NO_PIECE_WEIGHT
import com.example.recipelibrary.data.nutrition.IngredientIssue.TO_COMPLETE
import com.example.recipelibrary.data.nutrition.IngredientIssue.UNKNOWN_FOOD
import com.example.recipelibrary.data.nutrition.IngredientIssue.VAGUE
import com.example.recipelibrary.data.nutrition.IngredientParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientParserTest {

    private val parser = IngredientParser(TestFoods.table)

    /** Vérifie l'aliment (code Ciqual), le poids (± 0,5 g) et l'absence de problème. */
    private fun grams(line: String, code: String, expected: Double) {
        val p = parser.parse(line)
        assertEquals("aliment de « $line »", code, p.food?.code)
        assertNull("problème de « $line » : ${p.issue}", p.issue)
        assertEquals("grammes de « $line »", expected, p.grams!!, 0.5)
    }

    private fun issue(line: String, expected: IngredientIssue, code: String? = null) {
        val p = parser.parse(line)
        assertEquals("problème de « $line »", expected, p.issue)
        if (code != null) assertEquals("aliment de « $line »", code, p.food?.code)
    }

    // ── Masses ──
    @Test fun mass_grams() = grams("200 g de farine", "9440", 200.0)
    @Test fun mass_gramsNoSpace() = grams("200g de sucre", "31016", 200.0)
    @Test fun mass_grammesWord() = grams("150 grammes de beurre", "16400", 150.0)
    @Test fun mass_kgDecimalComma() = grams("1,5 kg de pommes de terre", "4008", 1500.0)
    @Test fun mass_kgDecimalPoint() = grams("1.2 kg de poulet", "36017", 1200.0)
    @Test fun mass_inline() = grams("Farine : 250 g", "9440", 250.0)
    @Test fun mass_inlineNoColon() = grams("chocolat noir 100g", "31074", 100.0)
    @Test fun mass_range() = grams("100-150 g de lardons", "28720", 125.0)

    // ── Volumes ──
    @Test fun volume_cl() = grams("25 cl de lait", "19042", 257.5)            // 250 ml × 1,03
    @Test fun volume_ml() = grams("100 ml de crème liquide", "19415", 100.0)
    @Test fun volume_litreFraction() = grams("1/2 litre de crème", "19410", 500.0)
    @Test fun volume_unicodeFraction() = grams("½ l de lait", "19042", 515.0)
    @Test fun volume_dl() = grams("2 dl d'eau", "18066", 200.0)
    @Test fun volume_soupSpoon() = grams("2 c. à soupe d'huile", "17440", 27.6)  // 30 ml × 0,92
    @Test fun volume_soupSpoonShort() = grams("1 càs de miel", "31008", 21.0)    // 15 ml × 1,4
    @Test fun volume_soupSpoonCs() = grams("3 cs de sucre", "31016", 38.25)      // 45 ml × 0,85
    @Test fun volume_soupSpoonWords() = grams("2 cuillères à soupe de farine", "9440", 16.5)
    @Test fun volume_coffeeSpoon() = grams("1 c. à café de cannelle", "11025", 2.5)
    @Test fun volume_coffeeSpoonCc() = grams("2 cc de vanille liquide", "11057", 10.0)
    @Test fun volume_glass() = grams("1 verre de lait", "19042", 206.0)
    @Test fun volume_cup() = grams("1 tasse de riz", "9100", 212.5)

    // ── Pièces ──
    @Test fun piece_eggs() = grams("3 œufs", "22000", 165.0)
    @Test fun piece_eggsLigatureless() = grams("4 oeufs", "22000", 220.0)
    @Test fun piece_wordNumber() = grams("deux oignons", "20034", 200.0)
    @Test fun piece_une() = grams("une pomme", "13620", 150.0)
    @Test fun piece_range() = grams("2 à 3 pommes", "13620", 375.0)
    @Test fun piece_garlic() = grams("2 gousses d'ail", "11000", 10.0)
    @Test fun piece_slices() = grams("4 tranches de jambon", "28900", 180.0)
    @Test fun piece_bigFactor() = grams("1 gros oignon", "20034", 150.0)
    @Test fun piece_parenGramsWin() = grams("1 oignon (120 g)", "20034", 120.0)
    @Test fun piece_eggYolks() = grams("2 jaunes d'œufs", "22002", 36.0)
    @Test fun piece_bulletAndHalf() = grams("- 1/2 citron", "13009", 60.0)
    @Test fun piece_dozen() = grams("une douzaine d'œufs", "22000", 660.0)

    // ── Sachets ──
    @Test fun sachet_baking() = grams("1 sachet de levure chimique", "11046", 11.0)
    @Test fun sachet_vanilla() = grams("2 sachets de sucre vanillé", "31044", 15.0)
    @Test fun box_innerWeight() = grams("1 boîte de 400 g de pois chiches", "20532", 400.0)

    // ── Lignes vagues ──
    @Test fun vague_pinch() = issue("une pincée de sel", VAGUE, "11017")
    @Test fun vague_saltPepper() = issue("sel, poivre", VAGUE, "11017")
    @Test fun vague_noixDeBeurre() = issue("1 noix de beurre", VAGUE, "16400")
    @Test fun vague_noQuantity() = issue("beurre pour le moule", VAGUE, "16400")
    @Test fun vague_selonLeGout() = issue("sucre selon le goût", VAGUE, "31016")
    @Test fun vague_unPeu() = issue("un peu de lait", VAGUE, "19042")
    @Test fun vague_bunch() = issue("1 botte de persil", VAGUE, "11014")
    @Test fun vague_box() = issue("1 boîte de thon", VAGUE, "26039")

    // ── Pièces sans poids, aliments inconnus, « À compléter » ──
    @Test fun noPieceWeight() = issue("2 filets d'huile d'olive", NO_PIECE_WEIGHT, "17270")
    @Test fun noPieceWeight_pot() = issue("1 pot de crème fraîche", NO_PIECE_WEIGHT, "19410")
    @Test fun noixDeSaintJacques_isAPiece() = grams("6 noix de saint-jacques", "10045", 150.0)
    @Test fun unknownFood() = issue("200 g de xyzzy", UNKNOWN_FOOD)
    @Test fun unknownFood_keepsGrams() = assertEquals(200.0, parser.parse("200 g de xyzzy").grams!!, 0.01)
    @Test fun toComplete() = issue("À compléter", TO_COMPLETE)
}
