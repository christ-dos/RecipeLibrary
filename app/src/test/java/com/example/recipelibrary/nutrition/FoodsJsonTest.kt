package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.nutrition.FoodKind
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contrôle du fichier généré `assets/nutrition/foods.json` (contracts/foods-json.md). */
class FoodsJsonTest {

    private val json = JSONObject(TestFoods.file.readText(Charsets.UTF_8))
    private val foods = json.getJSONArray("foods").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

    @Test fun atLeast300Foods() = assertTrue("${foods.size} aliments", foods.size >= 300)

    @Test fun sourceMentionsCiqual() = assertTrue(json.getString("source").contains("Ciqual 2020"))

    @Test fun codesAreUnique() {
        val codes = foods.map { it.getString("code") }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test fun aliasesAreUniqueLowercaseWithoutAccents() {
        val all = foods.flatMap { f -> f.getJSONArray("aliases").let { a -> (0 until a.length()).map { a.getString(it) } } }
        assertEquals("alias en double : ${all.groupBy { it }.filter { it.value.size > 1 }.keys}", all.size, all.toSet().size)
        for (a in all) {
            assertEquals("minuscules : $a", a.lowercase(), a)
            assertTrue("sans accents ni œ : $a", a.none { it in "àâäéèêëîïôöùûüçœæÿ" })
            assertTrue("non vide", a.isNotBlank())
        }
        foods.forEach { assertTrue("au moins un alias : ${it.getString("name")}", it.getJSONArray("aliases").length() > 0) }
    }

    @Test fun nutrientsInRange() {
        for (f in foods) {
            val name = f.getString("name")
            // 9 kcal/g de lipides × 100 g = 900, plus l'arrondi de Ciqual (huile de tournesol : 901)
            assertTrue("kcal $name", f.getDouble("kcal") in 0.0..910.0)
            for (k in listOf("protein", "carbs", "fat")) assertTrue("$k $name", f.getDouble(k) in 0.0..100.0)
        }
    }

    @Test fun pieceAndDensityInRange() {
        for (f in foods) {
            val name = f.getString("name")
            if (!f.isNull("pieceG")) assertTrue("pieceG $name", f.getDouble("pieceG") > 0)
            if (!f.isNull("density")) assertTrue("density $name", f.getDouble("density") in 0.1..2.0)
        }
    }

    @Test fun kindIsKnown() {
        val kinds = FoodKind.values().map { it.name }.toSet()
        foods.forEach { assertTrue(it.getString("kind"), it.getString("kind") in kinds) }
    }

    @Test fun loadsIntoFoodTable() = assertEquals(foods.size, TestFoods.table.size)
}
