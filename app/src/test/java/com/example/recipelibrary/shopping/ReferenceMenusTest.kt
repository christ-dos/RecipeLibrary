package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.nutrition.Normalize
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.data.shopping.QuantityFormat
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.data.shopping.ShoppingListBuilder
import com.example.recipelibrary.nutrition.TestFoods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 10 menus de référence (SC-002, T019) : recettes réalistes, sommes attendues écrites à la main.
 * Vérifie que 100 % des lignes d'ingrédients sont dans la liste et qu'au moins 90 % des
 * aliments communs à plusieurs recettes sont regroupés avec la bonne somme.
 */
class ReferenceMenusTest {

    private val parser = IngredientParser(TestFoods.table)

    private class Menu(val name: String, val plan: List<Pair<Recipe, MenuEntry>>, val expected: Map<String, String>)

    private fun r(id: String, servings: Int?, vararg lines: String) =
        Recipe(id = id, title = id, servings = servings, servingsSource = servings?.let { Recipe.SERVINGS_USER }, ingredients = lines.toList())

    private fun e(recipe: Recipe, servings: Int? = recipe.servings, times: Int = 1) =
        recipe to MenuEntry(recipeId = recipe.id, servings = servings, times = times)

    // Recettes
    private val crepes = r("crepes", 4, "250 g de farine", "4 œufs", "50 cl de lait", "50 g de beurre", "1 pincée de sel")
    private val quiche = r("quiche", 6, "1 pâte brisée", "3 oeufs", "20 cl de crème fraîche", "200 g de lardons", "100 g d'emmental", "sel, poivre")
    private val gateau = r("gateau", 8, "200 g de chocolat noir", "150 g de beurre", "4 œufs", "100 g de sucre", "50 g de farine")
    private val bolo = r("bolo", 4, "500 g de pâtes", "1 boîte de tomates concassées", "2 oignons", "2 gousses d'ail", "2 c. à soupe d'huile d'olive")
    private val ratatouille = r("ratatouille", 4, "2 courgettes", "4 tomates", "1 oignon", "3 gousses d'ail", "4 c. à soupe d'huile d'olive", "1 c. à café d'herbes de Provence")
    private val pouletRiz = r("pouletRiz", 2, "2 blancs de poulet", "150 g de riz", "1 oignon", "20 cl de lait de coco", "1 c. à café de curry")
    private val gratin = r("gratin", 4, "1 kg de pommes de terre", "50 cl de lait", "20 cl de crème fraîche", "1 gousse d'ail", "100 g d'emmental")
    private val salade = r("salade", 2, "Pour la vinaigrette :", "3 c. à soupe d'huile d'olive", "1 citron", "Salade :", "4 tomates", "1 botte de radis")
    private val compote = r("compote", null, "6 pommes", "1 sachet de sucre vanillé", "1 c. à café de cannelle")
    private val pancakes = r("pancakes", 4, "200 g de farine", "2 œufs", "30 cl de lait", "1 sachet de levure chimique", "40 g de sucre")

    private val menus = listOf(
        Menu("crêpes + gâteau", listOf(e(crepes), e(gateau)),
            mapOf("food:9440" to "300 g", "food:22000" to "8", "food:16400" to "200 g")),
        Menu("crêpes ×2 parts + quiche", listOf(e(crepes, servings = 8), e(quiche)),
            mapOf("food:9440" to "500 g", "food:22000" to "11", "food:19042" to "1 l")),
        Menu("bolo + ratatouille", listOf(e(bolo), e(ratatouille)),
            mapOf("food:20034" to "3", "food:11000" to "5 gousses", "food:17270" to "90 ml")),
        Menu("poulet riz double + bolo", listOf(e(pouletRiz, servings = 4), e(bolo)),
            mapOf("food:20034" to "4", "food:9100" to "300 g")),
        Menu("gratin + quiche", listOf(e(gratin), e(quiche)),
            mapOf("food:19410" to "40 cl", "food:12115" to "200 g")),
        Menu("salade + ratatouille", listOf(e(salade), e(ratatouille)),
            mapOf("food:20047" to "8", "food:17270" to "10,5 cl")),
        Menu("compote ×2 + crêpes", listOf(e(compote, times = 2), e(crepes)),
            mapOf("food:13620" to "12", "food:31044" to "2 sachets")),
        Menu("pancakes + crêpes + gâteau", listOf(e(pancakes), e(crepes), e(gateau)),
            mapOf("food:9440" to "500 g", "food:22000" to "10", "food:19042" to "80 cl", "food:31016" to "140 g")),
        Menu("gratin moitié + crêpes", listOf(e(gratin, servings = 2), e(crepes)),
            mapOf("food:19042" to "75 cl", "food:4008" to "500 g")),
        Menu("semaine complète", listOf(e(crepes), e(quiche), e(bolo), e(ratatouille), e(gratin), e(salade)),
            mapOf("food:22000" to "7", "food:20034" to "3", "food:11000" to "6 gousses", "food:17270" to "13,5 cl", "food:19042" to "1 l",
                "food:19410" to "40 cl", "food:12115" to "200 g", "food:20047" to "8"))
    )

    private fun build(m: Menu): List<ShoppingItem> =
        ShoppingListBuilder.build(m.plan.map { it.second }, m.plan.associate { it.first.id to it.first }, parser) { it.servings }

    @Test fun everyIngredientLineIsInTheList() {
        for (m in menus) {
            val list = build(m)
            val keys = list.map { it.key }.toSet()
            for ((recipe, _) in m.plan) for (line in recipe.ingredients) {
                if (NutritionCalculator.isHeader(line)) continue
                val p = parser.parse(line)
                val key = p.food?.let { "food:${it.code}" } ?: "txt:${Normalize.text(p.foodText)}"
                assertTrue("${m.name} : « $line » absente de la liste", key in keys)
            }
        }
    }

    @Test fun commonFoodsGroupedWithRightSum_atLeast90Percent() {
        var ok = 0
        var total = 0
        val misses = mutableListOf<String>()
        for (m in menus) {
            val list = build(m)
            for ((key, want) in m.expected) {
                total++
                val got = list.filter { it.key == key }
                if (got.size == 1 && QuantityFormat.of(got.single()) == want) ok++
                else misses += "${m.name} / $key : attendu « $want », obtenu ${got.map { QuantityFormat.of(it) }}"
            }
        }
        assertTrue("Regroupements corrects : $ok / $total\n" + misses.joinToString("\n"), ok * 10 >= total * 9)
        // Objectif réel : tout juste sur ces menus
        assertEquals(misses.joinToString("\n"), total, ok)
    }
}
