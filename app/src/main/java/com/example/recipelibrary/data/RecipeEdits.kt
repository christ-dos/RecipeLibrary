package com.example.recipelibrary.data

/** Modifications faites depuis la fiche d'une recette (stories 2 et 5). */
object RecipeEdits {

    /** Nombre de parts saisi par l'utilisatrice : origine USER (FR-018). Hors 1 à 100 → recette inchangée. */
    fun withServings(recipe: Recipe, servings: Int, now: Long = System.currentTimeMillis()): Recipe {
        if (servings !in Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS) return recipe
        return recipe.copy(servings = servings, servingsSource = Recipe.SERVINGS_USER, updatedAt = now)
    }

    /**
     * Poids corrigé à la main pour une ligne : prioritaire sur tout (FR-008).
     * Ligne absente de la recette ou poids hors bornes → recette inchangée.
     */
    fun withUserWeight(recipe: Recipe, line: String, weight: IngredientWeight, now: Long = System.currentTimeMillis()): Recipe {
        val g = weight.g ?: return recipe
        if (line !in recipe.ingredients || !IngredientWeight.isValid(IngredientWeight(g, IngredientWeight.USER))) return recipe
        return recipe.copy(ingredientWeights = IngredientWeights.withUser(recipe.ingredientWeights, line, g), updatedAt = now)
    }
}
