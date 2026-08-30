package com.example.recipelibrary

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeJsonParserTest {

    // ── 1. Recette complète → tous les champs parsés ──────────────────────────

    @Test
    fun parseRecipeJson_fullJson_allFieldsParsed() {
        val json = """
            {
              "title": "Tarte aux pommes",
              "description": "Une tarte classique",
              "ingredients": ["3 pommes", "200g de farine"],
              "steps": ["Éplucher les pommes", "Cuire 30 min"],
              "category": "Desserts",
              "tags": ["fruits", "facile"],
              "notes": "Servir tiède"
            }
        """.trimIndent()

        val recipe = parseRecipeJson(json)

        assertNotNull("La recette ne doit pas être null", recipe)
        assertEquals("Tarte aux pommes", recipe!!.title)
        assertEquals("Une tarte classique", recipe.description)
        assertEquals(listOf("3 pommes", "200g de farine"), recipe.ingredients)
        assertEquals(listOf("Éplucher les pommes", "Cuire 30 min"), recipe.steps)
        assertEquals("Desserts", recipe.category)
        assertEquals(listOf("fruits", "facile"), recipe.tags)
        assertEquals("Servir tiède", recipe.notes)
    }

    // ── 2. title: null → "Recette importée" (régression clé) ─────────────────

    @Test
    fun parseRecipeJson_nullTitle_returnsDefaultTitle() {
        val json = """{"title": null, "ingredients": ["100g de farine"], "steps": ["Mélanger"]}"""

        val recipe = parseRecipeJson(json)

        assertNotNull("Doit retourner une recette même si title est null", recipe)
        assertEquals(
            "Quand title=null, le titre doit être 'Recette importée'",
            "Recette importée",
            recipe!!.title
        )
    }

    // ── 3. title absent → "Recette importée" ─────────────────────────────────

    @Test
    fun parseRecipeJson_missingTitle_returnsDefaultTitle() {
        val json = """{"ingredients": ["1 oeuf"], "steps": ["Battre"]}"""

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals("Recette importée", recipe!!.title)
    }

    // ── 4. title vide → "Recette importée" ───────────────────────────────────

    @Test
    fun parseRecipeJson_emptyTitle_returnsDefaultTitle() {
        val json = """{"title": "", "ingredients": ["1 oeuf"]}"""

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals("Recette importée", recipe!!.title)
    }

    // ── 5. Balises markdown ``` supprimées ────────────────────────────────────

    @Test
    fun parseRecipeJson_markdownFences_stripped() {
        val json = "```json\n{\"title\": \"Crêpes\", \"ingredients\": [], \"steps\": []}\n```"

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals("Crêpes", recipe!!.title)
    }

    // ── 6. JSON invalide (pas d'accolades) → null ─────────────────────────────

    @Test
    fun parseRecipeJson_noBraces_returnsNull() {
        val result = parseRecipeJson("Voici une recette sans JSON du tout")
        assertNull("Doit retourner null si aucun JSON trouvé", result)
    }

    // ── 7. Chaîne vide → null ────────────────────────────────────────────────

    @Test
    fun parseRecipeJson_emptyString_returnsNull() {
        assertNull(parseRecipeJson(""))
        assertNull(parseRecipeJson("   "))
    }

    // ── 8. Ingrédient sous forme d'objet → chaîne aplatie ────────────────────

    @Test
    fun parseRecipeJson_ingredientAsObject_flattensToString() {
        val json = """
            {
              "title": "Test",
              "ingredients": [
                {"quantity": "200", "unit": "g", "name": "farine"},
                {"quantity": "3", "unit": "", "name": "oeufs", "note": "battus"}
              ],
              "steps": []
            }
        """.trimIndent()

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertTrue("Ingrédient avec unité", recipe!!.ingredients[0].contains("farine"))
        assertTrue("Ingrédient avec note", recipe.ingredients[1].contains("battus"))
    }

    // ── 9. Champ "instructions" accepté comme alias de "steps" ───────────────

    @Test
    fun parseRecipeJson_instructionsAlias_parsedAsSteps() {
        val json = """
            {
              "title": "Test",
              "ingredients": [],
              "instructions": ["Étape 1", "Étape 2"]
            }
        """.trimIndent()

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals(listOf("Étape 1", "Étape 2"), recipe!!.steps)
    }

    // ── 10. JSON malformé → null (pas de crash) ───────────────────────────────

    @Test
    fun parseRecipeJson_malformedJson_returnsNullWithoutCrash() {
        val result = parseRecipeJson("""{"title": "Test", "ingredients": [""")
        assertNull("JSON malformé doit retourner null sans crash", result)
    }

    // ── 11. sourceUrl préservée dans la recette ───────────────────────────────

    @Test
    fun parseRecipeJson_sourceUrl_preserved() {
        val json = """{"title": "Test", "sourceUrl": "https://youtu.be/abc123", "ingredients": [], "steps": []}"""

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals("https://youtu.be/abc123", recipe!!.sourceUrl)
    }

    // ── 12. sourceUrl depuis objet "source" { "url": "..." } ─────────────────

    @Test
    fun parseRecipeJson_sourceAsObject_urlExtracted() {
        val json = """{"title": "Test", "source": {"url": "https://example.com/recette"}, "ingredients": [], "steps": []}"""

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals("https://example.com/recette", recipe!!.sourceUrl)
    }

    // ── 13. Ingrédient objet avec quantité plage min/max ─────────────────────

    @Test
    fun parseRecipeJson_ingredientQuantityMinMax_flattenedToString() {
        val json = """
            {
              "title": "Test",
              "ingredients": [
                {"quantity": {"min": "2", "max": "4"}, "unit": "oeufs", "name": "œufs"},
                {"quantity": {"min": "100", "max": "150"}, "unit": "g", "name": "sucre"}
              ],
              "steps": []
            }
        """.trimIndent()

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals(2, recipe!!.ingredients.size)
        assertTrue("Min/max doit apparaître dans l'ingrédient", recipe.ingredients[0].contains("œufs"))
        assertTrue("Le nom doit être présent", recipe.ingredients[1].contains("sucre"))
    }

    // ── 14. imageUrl — fallback chain : image → thumbnail → photo → coverImage

    @Test
    fun parseRecipeJson_imageUrlFromImageField() {
        val json = """{"title": "Test", "image": "https://example.com/img.jpg", "ingredients": [], "steps": []}"""
        val recipe = parseRecipeJson(json)
        assertEquals("https://example.com/img.jpg", recipe!!.imageUrl)
    }

    @Test
    fun parseRecipeJson_imageUrlFromThumbnailField() {
        val json = """{"title": "Test", "thumbnail": "https://example.com/thumb.jpg", "ingredients": [], "steps": []}"""
        val recipe = parseRecipeJson(json)
        assertEquals("https://example.com/thumb.jpg", recipe!!.imageUrl)
    }

    @Test
    fun parseRecipeJson_imageUrlFromCoverImageField() {
        val json = """{"title": "Test", "coverImage": "https://example.com/cover.jpg", "ingredients": [], "steps": []}"""
        val recipe = parseRecipeJson(json)
        assertEquals("https://example.com/cover.jpg", recipe!!.imageUrl)
    }

    // ── 15. notes sous forme de tableau JSON → joinToString("\n") ────────────

    @Test
    fun parseRecipeJson_notesAsArray_joinedWithNewline() {
        val json = """
            {
              "title": "Test",
              "ingredients": [],
              "steps": [],
              "notes": ["Servir tiède", "Se congèle bien"]
            }
        """.trimIndent()

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        val notes = recipe!!.notes
        assertNotNull("notes ne doit pas être null", notes)
        assertTrue("Premier conseil présent", notes!!.contains("Servir tiède"))
        assertTrue("Deuxième conseil présent", notes.contains("Se congèle bien"))
        assertTrue("Séparés par un saut de ligne", notes.contains("\n"))
    }

    // ── 16. Titre TOUT MAJUSCULE → Capitalisé ─────────────────────────────────

    @Test
    fun parseRecipeJson_allCapsTitle_lowercasedWithFirstUppercase() {
        val json = """{"title": "TARTE AUX POMMES", "ingredients": [], "steps": []}"""

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals(
            "Titre tout-majuscule doit être normalisé",
            "Tarte aux pommes",
            recipe!!.title
        )
    }

    // ── 17. Titre mixte → inchangé ────────────────────────────────────────────

    @Test
    fun parseRecipeJson_mixedCaseTitle_unchanged() {
        val json = """{"title": "Tarte Aux Pommes", "ingredients": [], "steps": []}"""

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals("Tarte Aux Pommes", recipe!!.title)
    }

    // ── 18. Étape sous forme d'objet { "description": "..." } ────────────────

    @Test
    fun parseRecipeJson_stepAsObject_descriptionExtracted() {
        val json = """
            {
              "title": "Test",
              "ingredients": [],
              "steps": [
                {"description": "Préchauffer le four"},
                {"text": "Enfourner 30 min"}
              ]
            }
        """.trimIndent()

        val recipe = parseRecipeJson(json)

        assertNotNull(recipe)
        assertEquals(2, recipe!!.steps.size)
        assertEquals("Préchauffer le four", recipe.steps[0])
        assertEquals("Enfourner 30 min", recipe.steps[1])
    }
}
