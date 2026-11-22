package com.example.recipelibrary.data.shopping

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.MenuLogic
import com.example.recipelibrary.data.nutrition.AmountKind
import com.example.recipelibrary.data.nutrition.FoodKind
import com.example.recipelibrary.data.nutrition.IngredientIssue
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.nutrition.Normalize
import com.example.recipelibrary.data.nutrition.NutritionCalculator

/** Recette d'où vient un article, avec le nombre d'entrées du menu qui la visent. */
data class ShoppingSource(val recipeId: String, val title: String, val count: Int)

/** Article de la liste de courses (calculé, jamais stocké). */
data class ShoppingItem(
    /** `food:<code Ciqual>` ou `txt:<texte normalisé>`. */
    val key: String,
    val label: String,
    /** Totaux par nature (g, ml, pièces, sachets) ; null si aucune quantité de cette nature. */
    val mass: Double? = null,
    val volume: Double? = null,
    val pieces: Double? = null,
    val sachets: Double? = null,
    /** Quantités vagues par unité (« boite » → 2.0). */
    val loose: Map<String, Double> = emptyMap(),
    /** Sel, poivre, épices : affiché sans quantité. */
    val seasoning: Boolean = false,
    val sources: List<ShoppingSource> = emptyList(),
    /** Terme copié pour la recherche Chronodrive. */
    val searchTerm: String = label,
    val checked: Boolean = false,
    /** Rayon (story 4) : celui de l'aliment reconnu, sinon « Autres ». */
    val aisle: String = Aisles.OTHER
)

/**
 * Calcul de la liste de courses à partir du menu (spec 002, research R3). Kotlin pur.
 * Les volumes ne sont jamais convertis en grammes : la liste montre ce qu'on achète.
 */
object ShoppingListBuilder {

    private class Acc(val key: String, var label: String, var seasoning: Boolean, val aisle: String = Aisles.OTHER) {
        var mass: Double? = null
        var volume: Double? = null
        var pieces: Double? = null
        var sachets: Double? = null
        val loose = LinkedHashMap<String, Double>()
        val sources = LinkedHashMap<String, Pair<String, Int>>()
    }

    /**
     * @param servingsOf parts de la recette : indiquées, sinon estimées par le calcul des calories,
     *   sinon null (calculées une fois par recette par l'app, fournies directement en test).
     */
    fun build(
        entries: List<MenuEntry>,
        recipesById: Map<String, Recipe>,
        parser: IngredientParser,
        servingsOf: (Recipe) -> Int?
    ): List<ShoppingItem> {
        val items = LinkedHashMap<String, Acc>()
        val servingsCache = HashMap<String, Int?>()
        for (entry in entries) {
            val recipe = recipesById[entry.recipeId] ?: continue // recette introuvable : ignorée
            val servings = servingsCache.getOrPut(recipe.id) { servingsOf(recipe) }
            val factor = MenuLogic.factor(entry, servings)
            val seenInEntry = HashSet<String>()
            for (line in recipe.ingredients) {
                if (NutritionCalculator.isHeader(line)) continue
                val p = parser.parse(line)
                if (p.issue == IngredientIssue.TO_COMPLETE) continue
                val food = p.food
                val seasoning = food?.kind == FoodKind.SEASONING
                val key: String
                val label: String
                if (food != null && p.substitutionKey != null) {
                    // Aliment de remplacement (spec 001, T046) : on achète ce qui est écrit (« skyr »),
                    // seul le rayon vient de l'aliment choisi (décision du 2026-11-22)
                    key = "sub:${p.substitutionKey}"
                    label = displayText(line, p.foodText)
                } else if (food != null) {
                    key = "food:${food.code}"
                    label = food.aliases.firstOrNull() ?: food.name
                } else {
                    val text = Normalize.text(p.foodText).ifEmpty { Normalize.text(line) }
                    if (text.isEmpty()) continue
                    key = "txt:$text"
                    label = displayText(line, p.foodText)
                }
                val acc = items.getOrPut(key) { Acc(key, label, seasoning, Aisles.of(food?.aisle)) }
                if (seenInEntry.add(key)) {
                    val prev = acc.sources[recipe.id]
                    acc.sources[recipe.id] = recipe.title to ((prev?.second ?: 0) + 1)
                }
                if (seasoning) continue
                val amount = p.amount
                if (amount != null) {
                    val v = amount.value * factor
                    when (amount.kind) {
                        AmountKind.MASS -> acc.mass = (acc.mass ?: 0.0) + v
                        AmountKind.VOLUME -> acc.volume = (acc.volume ?: 0.0) + v
                        AmountKind.PIECE -> acc.pieces = (acc.pieces ?: 0.0) + v
                        AmountKind.SACHET -> acc.sachets = (acc.sachets ?: 0.0) + v
                    }
                } else {
                    p.looseQuantity?.let { lq -> acc.loose[lq.unit] = (acc.loose[lq.unit] ?: 0.0) + lq.count * factor }
                }
            }
        }
        return items.values
            .map { a ->
                ShoppingItem(
                    key = a.key, label = a.label, mass = a.mass, volume = a.volume, pieces = a.pieces,
                    sachets = a.sachets, loose = a.loose.toMap(), seasoning = a.seasoning,
                    sources = a.sources.map { (id, v) -> ShoppingSource(id, v.first, v.second) },
                    searchTerm = searchTerm(a.label), aisle = a.aisle
                )
            }
            .sortedBy { Normalize.plain(it.label) }
    }

    /**
     * Texte d'un aliment non reconnu, tel qu'écrit dans la ligne (casse et accents gardés),
     * sans la quantité ni l'unité de tête : « 1 c. à café de piment d'Espelette » → « piment d'Espelette ».
     */
    fun displayText(line: String, foodText: String): String {
        val first = Normalize.plain(foodText).let { WORD.find(it)?.value } ?: return line.trim()
        val start = WORD.findAll(line).firstOrNull { Normalize.plain(it.value) == first }?.range?.first
            ?: return foodText.trim()
        return PAREN.replace(line.substring(start), " ").replace(SPACES, " ").trim().trimEnd(',', ';', '.')
    }

    /** Terme Chronodrive : sans quantité, coupé avant une précision (« , coupé en dés »), 60 caractères au plus. */
    fun searchTerm(label: String): String {
        val cut = label.substringBefore(',').substringBefore('(').replace(SPACES, " ").trim()
        return cut.ifEmpty { label.trim() }.take(60).trim()
    }

    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val PAREN = Regex("\\([^)]*\\)")
    private val SPACES = Regex("\\s+")
}
