package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.shopping.ShoppingListBuilder
import com.example.recipelibrary.nutrition.TestFoods
import org.junit.Assert.assertTrue
import org.junit.Test

/** Durée du calcul (marge pour SC-003, T020) : 30 recettes de 12 ingrédients en moins de 300 ms. */
class ShoppingListPerfTest {

    @Test fun thirtyRecipes_under300ms() {
        val parser = IngredientParser(TestFoods.table)
        val lines = listOf(
            "250 g de farine", "4 œufs", "50 cl de lait", "50 g de beurre", "1 pincée de sel", "2 oignons",
            "2 gousses d'ail", "1 boîte de tomates concassées", "200 g de lardons", "2 c. à soupe d'huile d'olive",
            "1 c. à café de sumac", "20 cl de crème fraîche"
        )
        val recipes = (1..30).map { i -> Recipe(id = "r$i", title = "Recette $i", servings = 4, ingredients = lines.map { "$it $i".substringBeforeLast(' ') }) }
        val entries = recipes.map { MenuEntry(recipeId = it.id, servings = 6) }
        val byId = recipes.associateBy { it.id }
        ShoppingListBuilder.build(entries, byId, parser) { it.servings } // échauffement (chargement de la table)
        val t0 = System.nanoTime()
        val list = ShoppingListBuilder.build(entries, byId, parser) { it.servings }
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("liste vide", list.isNotEmpty())
        assertTrue("calcul en $ms ms", ms < 300)
    }
}
