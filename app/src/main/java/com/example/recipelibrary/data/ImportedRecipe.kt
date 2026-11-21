package com.example.recipelibrary.data

/**
 * Recette importée (Hermes, fichier, texte, partage) : un champ obligatoire vide ne doit pas
 * bloquer l'enregistrement. On le remplit avec « À compléter » pour le reprendre plus tard.
 */
object ImportedRecipe {
    const val TO_COMPLETE = "À compléter"

    fun fillMissing(recipe: Recipe): Recipe = recipe.copy(
        title = recipe.title.ifBlank { TO_COMPLETE },
        ingredients = recipe.ingredients.filter { it.isNotBlank() }.ifEmpty { listOf(TO_COMPLETE) },
        steps = recipe.steps.filter { it.isNotBlank() }.ifEmpty { listOf(TO_COMPLETE) }
    )

    /** Noms des champs encore marqués « À compléter » (pour le prévenir dans le formulaire). */
    fun missingFields(title: String, ingredients: String, steps: String): List<String> = listOfNotNull(
        "titre".takeIf { title.contains(TO_COMPLETE) },
        "ingrédients".takeIf { ingredients.contains(TO_COMPLETE) },
        "étapes".takeIf { steps.contains(TO_COMPLETE) }
    )
}
