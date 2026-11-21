package com.example.recipelibrary

import com.example.recipelibrary.data.ImportedRecipe
import com.example.recipelibrary.data.ImportedRecipe.TO_COMPLETE
import com.example.recipelibrary.data.Recipe
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportedRecipeTest {

    private val base = Recipe(id = "1", title = "Crumble", ingredients = listOf("pommes"), steps = listOf("cuire"), category = "Desserts")

    @Test
    fun fillMissing_completeRecipe_unchanged() {
        assertEquals(base, ImportedRecipe.fillMissing(base))
    }

    @Test
    fun fillMissing_blankTitle_becomesToComplete() {
        assertEquals(TO_COMPLETE, ImportedRecipe.fillMissing(base.copy(title = "  ")).title)
    }

    @Test
    fun fillMissing_noIngredientsNorSteps_becomeToComplete() {
        val filled = ImportedRecipe.fillMissing(base.copy(ingredients = emptyList(), steps = listOf("", " ")))
        assertEquals(listOf(TO_COMPLETE), filled.ingredients)
        assertEquals(listOf(TO_COMPLETE), filled.steps)
    }

    @Test
    fun fillMissing_dropsBlankLinesButKeepsRealOnes() {
        val filled = ImportedRecipe.fillMissing(base.copy(steps = listOf("", "cuire", " ")))
        assertEquals(listOf("cuire"), filled.steps)
    }

    @Test
    fun fillMissing_keepsCategoryEmpty() {
        // La catégorie reste à choisir : « À compléter » n'est pas une catégorie
        assertEquals(null, ImportedRecipe.fillMissing(base.copy(category = null)).category)
    }

    @Test
    fun missingFields_listsOnlyFieldsStillToComplete() {
        assertEquals(emptyList<String>(), ImportedRecipe.missingFields("Crumble", "pommes", "cuire"))
        assertEquals(listOf("titre", "étapes"), ImportedRecipe.missingFields(TO_COMPLETE, "pommes", "cuire\n$TO_COMPLETE"))
        assertEquals(listOf("ingrédients"), ImportedRecipe.missingFields("Crumble", TO_COMPLETE, "cuire"))
    }
}
