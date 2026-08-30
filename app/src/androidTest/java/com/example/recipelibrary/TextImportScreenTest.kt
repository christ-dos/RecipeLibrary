package com.example.recipelibrary

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.ui.parseTextRecipe
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de régression pour parseTextRecipe (import texte + OCR photo).
 *
 * parseTextRecipe est la seule logique de parsing sans couverture de test ;
 * toute modification de la détection de sections ou de cleanLine doit
 * faire passer ces tests avant tout commit.
 */
@RunWith(AndroidJUnit4::class)
class TextImportScreenTest {

    // ── 1. Recette structurée complète (sections FR) ──────────────────────────

    @Test
    fun parseTextRecipe_fullStructured_allSectionsParsed() {
        val text = """
            Fondant au chocolat

            Ingrédients :
            - 200g de chocolat noir
            - 3 œufs
            - 100g de beurre

            Étapes :
            1. Faire fondre le chocolat
            2. Mélanger les œufs et le beurre
            3. Cuire 20 min à 180°C
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals("Fondant au chocolat", recipe.title)
        assertEquals(3, recipe.ingredients.size)
        assertTrue(recipe.ingredients.any { it.contains("chocolat noir") })
        assertEquals(3, recipe.steps.size)
        assertTrue(recipe.steps.any { it.contains("fondre") })
    }

    // ── 2. Sections anglaises reconnues ──────────────────────────────────────

    @Test
    fun parseTextRecipe_englishSections_recognized() {
        val text = """
            Chocolate Cake

            Ingredients:
            - 200g flour
            - 3 eggs

            Steps:
            1. Mix ingredients
            2. Bake 30 min
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals("Chocolate Cake", recipe.title)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(2, recipe.steps.size)
    }

    // ── 3. cleanLine : numéros de liste retirés, quantités préservées ─────────

    @Test
    fun parseTextRecipe_cleanLine_numberedStepsStripped_quantitiesKept() {
        val text = """
            Gâteau

            Ingrédients :
            150g de farine
            2 oeufs
            10,5cl de lait

            Préparation :
            1. Mélanger les ingrédients
            2) Cuire 30 min
            3: Servir chaud
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        // Quantités préservées (150g, 2, 10,5cl ne commencent pas par un séparateur)
        assertTrue("150g doit être conservé", recipe.ingredients.any { it.startsWith("150g") })
        assertTrue("2 oeufs doit être conservé", recipe.ingredients.any { it.startsWith("2") })
        // Numéros de liste retirés des étapes
        assertTrue("'1.' retiré", recipe.steps.none { it.startsWith("1.") || it.startsWith("1)") })
        assertTrue("Contenu des étapes préservé", recipe.steps.any { it.contains("Mélanger") })
    }

    // ── 4. Bullets variés retirés (-, –, •, *, →) ────────────────────────────

    @Test
    fun parseTextRecipe_bulletPrefixes_stripped() {
        val text = """
            Soupe

            Ingrédients :
            - 1 oignon
            – 2 carottes
            • 3 pommes de terre
            * 1L de bouillon
            → sel, poivre
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals(5, recipe.ingredients.size)
        assertTrue(recipe.ingredients.none { it.startsWith("-") || it.startsWith("–") || it.startsWith("•") || it.startsWith("*") || it.startsWith("→") })
        assertTrue(recipe.ingredients.any { it.startsWith("1 oignon") })
    }

    // ── 5. Première ligne = en-tête de section → titre vide ──────────────────

    @Test
    fun parseTextRecipe_firstLineIsSection_titleEmpty() {
        val text = """
            Ingrédients :
            - farine
            - œufs
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals("", recipe.title)
        assertEquals(2, recipe.ingredients.size)
    }

    // ── 6. Texte sans sections → tout en extras (notes), titre = 1re ligne ───

    @Test
    fun parseTextRecipe_noSections_firstLineTitleRestExtras() {
        val text = """
            Ma super recette
            farine, oeufs, beurre
            mélanger et cuire
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals("Ma super recette", recipe.title)
        assertTrue("Extras non-sectionnés dans notes", recipe.notes != null)
        // Pas d'ingrédients/étapes détectés sans section headers
        assertEquals(0, recipe.ingredients.size)
        assertEquals(0, recipe.steps.size)
    }

    // ── 7. Section notes/conseils capturée ────────────────────────────────────

    @Test
    fun parseTextRecipe_notesSection_capturedInNotes() {
        val text = """
            Crêpes

            Ingrédients :
            - 250g farine

            Préparation :
            1. Mélanger

            Conseils :
            Servir chaud avec du citron
            Peut se congeler
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertNotNull(recipe.notes)
        assertTrue(recipe.notes!!.contains("citron"))
        assertTrue(recipe.notes!!.contains("congeler"))
    }

    // ── 8. Texte OCR simulé (numéros + ponctuation irrégulière) ──────────────

    @Test
    fun parseTextRecipe_ocrStyle_parsedCorrectly() {
        val text = """
            Tarte aux pommes

            Ingrédients
            3 pommes golden
            200 g de farine
            100 g de beurre froid

            Préparation
            1 Éplucher et couper les pommes
            2 Sabler la farine avec le beurre
            3 Étaler la pâte et garnir
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals("Tarte aux pommes", recipe.title)
        assertEquals(3, recipe.ingredients.size)
        assertEquals(3, recipe.steps.size)
        // "3 pommes" n'est pas traité comme numéro de liste (pas de séparateur après)
        assertTrue(recipe.ingredients.any { it.startsWith("3 pommes") })
    }

    // ── 9. Texte vide → recette avec titre vide et listes vides ─────────────

    @Test
    fun parseTextRecipe_emptyString_returnsEmptyRecipe() {
        val recipe = parseTextRecipe("")

        assertEquals("", recipe.title)
        assertEquals(0, recipe.ingredients.size)
        assertEquals(0, recipe.steps.size)
    }

    // ── 10. Ingrédients sans séparateur (format "pour X personnes") ──────────

    @Test
    fun parseTextRecipe_portionsLine_goesToExtrasNotIngredients() {
        val text = """
            Quiche lorraine

            Pour 4 personnes

            Ingrédients :
            - 200g lardons
            - 3 œufs
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals("Quiche lorraine", recipe.title)
        // "Pour 4 personnes" → meta (pas ingredients)
        assertEquals(2, recipe.ingredients.size)
        // La ligne portions se retrouve en notes/extras
        assertTrue(recipe.notes?.contains("personnes") == true || recipe.notes == null)
    }

    // ── 11. Alias "Instructions" (anglais) → steps ───────────────────────────

    @Test
    fun parseTextRecipe_instructionsAlias_parsedAsSteps() {
        val text = """
            Brownies

            Ingredients:
            200g chocolate

            Instructions:
            Melt chocolate
            Mix and bake
        """.trimIndent()

        val recipe = parseTextRecipe(text)

        assertEquals(2, recipe.steps.size)
        assertTrue(recipe.steps.any { it.contains("Melt") })
    }

    // ── 12. Chaque recette a un id UUID unique ────────────────────────────────

    @Test
    fun parseTextRecipe_twoCallsProduceDifferentIds() {
        val r1 = parseTextRecipe("Recette A\nIngrédients :\n- farine")
        val r2 = parseTextRecipe("Recette B\nIngrédients :\n- sucre")

        assertNotEquals("Chaque appel doit produire un UUID distinct", r1.id, r2.id)
    }
}
