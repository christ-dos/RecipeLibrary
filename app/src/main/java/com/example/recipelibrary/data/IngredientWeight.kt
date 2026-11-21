package com.example.recipelibrary.data

/**
 * Poids d'une ligne d'ingrédients, stocké dans `recipes.ingredient_weights` sous la clé
 * « texte exact de la ligne ».
 *
 * - [src] = [USER] : saisi à la main, prioritaire sur tout, jamais remplacé par le PC.
 * - [src] = [PC] : estimé par Hermes ; [g] = null signifie « le PC n'a pas su » et sert de
 *   marqueur « déjà envoyé ».
 */
data class IngredientWeight(val g: Double?, val src: String) {
    companion object {
        const val PC = "PC"
        const val USER = "USER"
        const val MAX_GRAMS = 10_000.0

        /** Vrai si l'entrée respecte les règles du modèle de données. */
        fun isValid(w: IngredientWeight?): Boolean {
            if (w == null) return false
            if (w.src != PC && w.src != USER) return false
            val g = w.g ?: return w.src == PC
            return g > 0 && g <= MAX_GRAMS
        }
    }
}

/** Règles de mise à jour de `ingredient_weights` (FR-008, FR-021). */
object IngredientWeights {

    /** Garde les seules entrées dont la ligne existe encore à l'identique. */
    fun keepForLines(weights: Map<String, IngredientWeight>, lines: List<String>): Map<String, IngredientWeight> {
        val set = lines.toSet()
        return weights.filterKeys { it in set }
    }

    /** Poids saisi à la main pour [line] (toujours prioritaire). */
    fun withUser(weights: Map<String, IngredientWeight>, line: String, grams: Double): Map<String, IngredientWeight> =
        weights + (line to IngredientWeight(grams, IngredientWeight.USER))

    /**
     * Ajoute les réponses du PC (grammes ou null = « n'a pas su ») sans jamais remplacer une
     * entrée [IngredientWeight.USER].
     */
    fun mergePc(weights: Map<String, IngredientWeight>, pc: Map<String, Double?>): Map<String, IngredientWeight> {
        val out = weights.toMutableMap()
        for ((line, g) in pc) {
            if (out[line]?.src == IngredientWeight.USER) continue
            val valid = g?.takeIf { it > 0 && it <= IngredientWeight.MAX_GRAMS }
            out[line] = IngredientWeight(valid, IngredientWeight.PC)
        }
        return out
    }
}
