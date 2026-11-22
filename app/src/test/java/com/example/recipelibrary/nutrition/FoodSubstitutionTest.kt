package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.nutrition.Food
import com.example.recipelibrary.data.nutrition.FoodKind
import com.example.recipelibrary.data.nutrition.FoodSubstitutions
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.IngredientIssue
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.data.shopping.Aisles
import com.example.recipelibrary.data.shopping.ShoppingListBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aliment de remplacement (spec 001, story 6, T046) : appliqué partout, jamais à la place d'un aliment reconnu. */
class FoodSubstitutionTest {

    private val fromageBlanc = Food(
        code = "19590", name = "Fromage blanc nature, 0 % MG", aliases = listOf("fromage blanc"),
        kcal = 45.0, protein = 7.5, carbs = 4.0, fat = 0.2, pieceG = 100.0
    )
    private val yaourt = Food(
        code = "19410", name = "Yaourt nature", aliases = listOf("yaourt"),
        kcal = 60.0, protein = 4.0, carbs = 5.0, fat = 3.0, pieceG = 125.0
    )
    private val farine = Food(
        code = "9410", name = "Farine de blé", aliases = listOf("farine"),
        kcal = 340.0, protein = 10.0, carbs = 72.0, fat = 1.0, density = 0.55
    )
    private val sel = Food(
        code = "11058", name = "Sel", aliases = listOf("sel"),
        kcal = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0, kind = FoodKind.SEASONING
    )
    private val base = FoodTable(listOf(fromageBlanc, yaourt, farine, sel))

    private fun table(vararg subs: Pair<String, String>) = base.withSubstitutions(subs.toMap())

    @Test
    fun unknownFood_withoutSubstitution_staysUnknown() {
        val p = IngredientParser(base).parse("150 g de skyr nature")
        assertNull(p.food)
        assertEquals(IngredientIssue.UNKNOWN_FOOD, p.issue)
        assertNull(p.substitutionKey)
    }

    @Test
    fun substitution_replacesFood_quantityUnchanged() {
        val p = IngredientParser(table("skyr nature" to "19590")).parse("150 g de Skyr nature")
        assertEquals(fromageBlanc, p.food)
        assertEquals("skyr nature", p.substitutionKey)
        assertNull(p.issue)
        assertEquals(150.0, p.grams!!, 0.001)
        assertEquals("skyr nature", p.foodText)
    }

    @Test
    fun substitution_appliesToEveryRecipeContainingTheText() {
        val parser = IngredientParser(table("skyr" to "19590"))
        listOf("200 g de skyr", "1 pot de skyr à la vanille", "Skyrs : 300 g", "- 2 c. à soupe de skyr bien froid").forEach { line ->
            val p = parser.parse(line)
            assertEquals(line, fromageBlanc, p.food)
            assertEquals(line, "skyr", p.substitutionKey)
        }
    }

    @Test
    fun substitution_doesNotMatchPartOfAWord() {
        val p = IngredientParser(table("skyr" to "19590")).parse("100 g de skyrella")
        assertNull(p.food)
        assertEquals(IngredientIssue.UNKNOWN_FOOD, p.issue)
    }

    @Test
    fun recognisedFood_isNeverReplaced() {
        // Un choix dont le texte contient un aliment connu ne change pas les lignes reconnues.
        val p = IngredientParser(table("yaourt grec" to "19590", "farine" to "19590")).parse("125 g de yaourt grec")
        assertEquals(yaourt, p.food)
        assertNull(p.substitutionKey)
        assertEquals(farine, IngredientParser(table("farine" to "19590")).parse("200 g de farine").food)
    }

    @Test
    fun missingFoodCode_isIgnored() {
        val p = IngredientParser(table("skyr" to "00000")).parse("150 g de skyr")
        assertNull(p.food)
        assertEquals(IngredientIssue.UNKNOWN_FOOD, p.issue)
        assertNull(p.substitutionKey)
    }

    @Test
    fun longestText_wins() {
        val parser = IngredientParser(table("skyr" to "19590", "skyr vanille" to "19410"))
        assertEquals(yaourt, parser.parse("150 g de skyr vanille").food)
        assertEquals("skyr vanille", parser.parse("150 g de skyr vanille").substitutionKey)
        assertEquals(fromageBlanc, parser.parse("150 g de skyr nature").food)
    }

    @Test
    fun pieces_useTheReplacementPieceWeight() {
        // « 2 pots » : le nombre est lu comme avant ; le poids d'une pièce est celui de l'aliment choisi.
        val p = IngredientParser(table("skyr" to "19590")).parse("2 pots de skyr")
        assertEquals(200.0, p.grams!!, 0.001)
    }

    @Test
    fun calculator_countsTheSubstitutedLine() {
        val recipe = Recipe(id = "r", title = "Cheesecake", servings = 2, ingredients = listOf("200 g de farine", "150 g de skyr"))
        val without = NutritionCalculator(base).estimate(recipe)
        assertEquals(1, without.ignored.size)
        val with = NutritionCalculator(table("skyr" to "19590")).estimate(recipe)
        assertTrue(with.ignored.isEmpty())
        val skyr = with.counted.single { it.parsed.line == "150 g de skyr" }
        assertEquals(fromageBlanc, skyr.food)
        assertEquals(67.5, skyr.nutrients.kcal, 0.001)
        assertEquals(without.total!!.kcal + 67.5, with.total!!.kcal, 0.001)
    }

    @Test
    fun seasoningReplacement_isIgnoredLikeASeasoning() {
        val recipe = Recipe(id = "r", title = "R", ingredients = listOf("1 pincée de fleur de Guérande"))
        val e = NutritionCalculator(table("fleur de guerande" to "11058")).estimate(recipe)
        assertTrue(e.ignored.isEmpty())
        assertEquals(1, e.seasonings.size)
    }

    @Test
    fun shoppingList_keepsTheWrittenName_notMergedWithTheReplacementFood() {
        // Décision de l'utilisatrice (2026-11-22) : dans la liste de courses on garde « skyr »,
        // ce qu'elle achète vraiment ; le remplacement ne sert qu'aux calories et au rayon.
        val a = Recipe(id = "a", title = "Bowl", ingredients = listOf("200 g de skyr"))
        val c = Recipe(id = "c", title = "Smoothie", ingredients = listOf("100 g de skyr"))
        val b = Recipe(id = "b", title = "Dip", ingredients = listOf("100 g de fromage blanc"))
        val entries = listOf(MenuEntry(recipeId = "a"), MenuEntry(recipeId = "b"), MenuEntry(recipeId = "c"))
        val byId = mapOf("a" to a, "b" to b, "c" to c)

        val after = ShoppingListBuilder.build(entries, byId, IngredientParser(table("skyr" to "19590"))) { null }
        assertEquals(2, after.size)
        val skyr = after.single { it.key == "sub:skyr" }
        assertEquals("skyr", skyr.label)
        assertEquals("skyr", skyr.searchTerm)
        assertEquals(300.0, skyr.mass!!, 0.001)
        assertEquals(listOf("Bowl", "Smoothie"), skyr.sources.map { it.title })
        assertEquals(Aisles.of(base.food("19590")?.aisle), skyr.aisle)
        val fromageBlanc = after.single { it.key == "food:19590" }
        assertEquals(listOf("Dip"), fromageBlanc.sources.map { it.title })
    }

    @Test
    fun defaultKey_isTheNormalisedFoodText() {
        val p = IngredientParser(base).parse("150 g de Skyr nature")
        assertEquals("skyr nature", FoodSubstitutions.defaultKey(p))
        assertEquals("oeuf de caille", FoodSubstitutions.defaultKey(IngredientParser(base).parse("3 Œufs de cailles")))
    }

    @Test
    fun normalizedKey_mustApplyToTheLine() {
        val line = "150 g de Skyr nature"
        assertEquals("skyr", FoodSubstitutions.normalizedKey(" Skyr ", line, base))
        assertEquals("skyr nature", FoodSubstitutions.normalizedKey("skyr NATURE", line, base))
        assertNull(FoodSubstitutions.normalizedKey("skyr vanille", line, base))
        assertNull(FoodSubstitutions.normalizedKey("sky", line, base))
        assertNull(FoodSubstitutions.normalizedKey("  ", line, base))
        // Le texte de l'unité n'est jamais pris pour l'aliment.
        assertNull(FoodSubstitutions.normalizedKey("soupe", "2 c. à soupe de skyr", base))
    }

    @Test
    fun search_findsFoodsByNameOrAlias_ignoringAccentsAndCase() {
        assertEquals(listOf(fromageBlanc), FoodSubstitutions.search(base, "Fromage bl"))
        assertEquals(listOf(farine), FoodSubstitutions.search(base, "BLÉ"))
        assertEquals(listOf(yaourt), FoodSubstitutions.search(base, "yaou"))
        assertTrue(FoodSubstitutions.search(base, "").isEmpty())
        assertTrue(FoodSubstitutions.search(base, "chocolat").isEmpty())
    }

    @Test
    fun search_onRealTable_isLimitedAndStartsWithTheQuery() {
        val r = FoodSubstitutions.search(TestFoods.table, "fromage")
        assertTrue(r.isNotEmpty())
        assertTrue(r.size <= FoodSubstitutions.MAX_RESULTS)
        assertTrue(r.first().name.lowercase().startsWith("fromage"))
    }

    @Test
    fun withSubstitutions_sameMap_keepsTheTable() {
        val t = table("skyr" to "19590")
        assertSame(t, t.withSubstitutions(mapOf("skyr" to "19590")))
        assertSame(base, base.withSubstitutions(emptyMap()))
        assertEquals(fromageBlanc, t.food("19590"))
        assertNull(t.food("00000"))
    }
}
