package com.example.recipelibrary.data.nutrition

import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.Recipe
import kotlin.math.roundToInt

/** Origine du poids retenu pour une ligne. */
enum class GramsOrigin { WRITTEN, PC, USER }

/** Origine du nombre de parts retenu. */
enum class ServingsOrigin { USER, SOURCE, ESTIMATED }

enum class NutritionWarning {
    /** Plus de la moitié des lignes (hors assaisonnements) ne sont pas comptées (FR-013). */
    PARTIAL,
    /** Plus de 3 000 kcal par part, ou plus de 30 parts estimées (FR-013). */
    CHECK_VALUE,
    /** Huile de friture : quantité absorbée inconnue. */
    FRYING
}

/** Énergie et macronutriments. */
data class Nutrients(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(o: Nutrients) = Nutrients(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)
    operator fun div(n: Int) = Nutrients(kcal / n, protein / n, carbs / n, fat / n)

    /** kcal arrondis à la dizaine, nutriments au gramme (affichage indicatif). */
    fun rounded() = Nutrients(
        (kcal / 10).roundToInt() * 10.0,
        protein.roundToInt().toDouble(),
        carbs.roundToInt().toDouble(),
        fat.roundToInt().toDouble()
    )

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0)
        fun of(food: Food, grams: Double) = Nutrients(
            food.kcal * grams / 100, food.protein * grams / 100, food.carbs * grams / 100, food.fat * grams / 100
        )
    }
}

/** Ligne comptée dans le total. */
data class CountedLine(
    val parsed: ParsedIngredient,
    val food: Food,
    val grams: Double,
    val origin: GramsOrigin,
    val nutrients: Nutrients
)

/** Ligne non comptée, avec sa raison. */
data class IgnoredLine(val parsed: ParsedIngredient, val issue: IngredientIssue)

/** Estimation calculée à l'affichage (jamais stockée). */
data class NutritionEstimate(
    val counted: List<CountedLine>,
    val ignored: List<IgnoredLine>,
    /** Sel, poivre, épices… : ignorés sans avertissement. */
    val seasonings: List<ParsedIngredient>,
    /** null si aucune ligne n'est comptée. */
    val total: Nutrients?,
    val servings: Int?,
    val servingsOrigin: ServingsOrigin?,
    /** Valeur par part arrondie ; null sans nombre de parts. */
    val perServing: Nutrients?,
    val warnings: Set<NutritionWarning>
) {
    val countedGrams: Double get() = counted.sumOf { it.grams }
}

/** Calcul des calories d'une recette (fonction pure). */
class NutritionCalculator(private val table: FoodTable) {

    private val parser = IngredientParser(table)

    fun parse(line: String): ParsedIngredient = parser.parse(line)

    fun estimate(recipe: Recipe): NutritionEstimate {
        val counted = mutableListOf<CountedLine>()
        val ignored = mutableListOf<IgnoredLine>()
        val seasonings = mutableListOf<ParsedIngredient>()
        val warnings = mutableSetOf<NutritionWarning>()

        for (line in recipe.ingredients) {
            if (isHeader(line)) continue
            val p = parser.parse(line)
            val food = p.food
            if (food?.kind == FoodKind.SEASONING) { seasonings += p; continue }
            val entry = recipe.ingredientWeights[line]?.takeIf { IngredientWeight.isValid(it) }
            // Priorité : corrigé à la main > quantité écrite > estimé par le PC (FR-008).
            val chosen: Pair<Double, GramsOrigin>? = when {
                entry?.src == IngredientWeight.USER && entry.g != null -> entry.g to GramsOrigin.USER
                p.grams != null && p.issue == null -> p.grams to GramsOrigin.WRITTEN
                entry?.src == IngredientWeight.PC && entry.g != null -> entry.g to GramsOrigin.PC
                else -> null
            }
            if (food == null || chosen == null || p.issue == IngredientIssue.TO_COMPLETE) {
                ignored += IgnoredLine(p, p.issue ?: IngredientIssue.VAGUE)
                continue
            }
            if (food.kind == FoodKind.FRYING) warnings += NutritionWarning.FRYING
            counted += CountedLine(p, food, chosen.first, chosen.second, Nutrients.of(food, chosen.first))
        }

        val lines = counted.size + ignored.size
        val tooManyUncounted = lines > 0 && ignored.size * 2 > lines
        if (tooManyUncounted) warnings += NutritionWarning.PARTIAL
        val total = if (counted.isEmpty()) null else counted.fold(Nutrients.ZERO) { acc, c -> acc + c.nutrients }

        val stored = recipe.servings?.takeIf { it in Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS }
        val (servings, origin) = when {
            stored != null && recipe.servingsSource == Recipe.SERVINGS_SOURCE -> stored to ServingsOrigin.SOURCE
            stored != null -> stored to ServingsOrigin.USER
            else -> {
                val est = ServingsEstimator.estimate(recipe.category, counted.sumOf { it.grams }, tooManyUncounted)
                est to est?.let { ServingsOrigin.ESTIMATED }
            }
        }
        val perServing = if (total != null && servings != null) (total / servings).rounded() else null
        if (perServing != null && perServing.kcal > 3000) warnings += NutritionWarning.CHECK_VALUE
        if (origin == ServingsOrigin.ESTIMATED && servings != null && servings > 30) warnings += NutritionWarning.CHECK_VALUE

        return NutritionEstimate(counted, ignored, seasonings, total, servings, origin, perServing, warnings)
    }

    companion object {
        /** Ligne vide ou titre de section (« Pour la pâte : »). */
        fun isHeader(line: String): Boolean {
            val t = line.trim()
            return t.isEmpty() || (t.endsWith(":") && t.none { it.isDigit() })
        }
    }
}
