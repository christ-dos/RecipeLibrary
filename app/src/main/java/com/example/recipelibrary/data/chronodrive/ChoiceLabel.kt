package com.example.recipelibrary.data.chronodrive

import com.example.recipelibrary.data.nutrition.FoodTable

/**
 * Libellé lisible d'une clé de choix (écran « Mes choix ») : article de la liste courante s'il y
 * est, sinon nom de l'aliment pour `food:<code>`, sinon le texte de `txt:` / `sub:`, sinon la clé.
 */
object ChoiceLabel {
    fun of(itemKey: String, currentLabels: Map<String, String> = emptyMap(), foods: FoodTable? = null): String {
        currentLabels[itemKey]?.let { return it }
        return when {
            itemKey.startsWith("food:") -> foods?.food(itemKey.removePrefix("food:"))?.name ?: itemKey
            itemKey.startsWith("txt:") -> itemKey.removePrefix("txt:")
            itemKey.startsWith("sub:") -> itemKey.removePrefix("sub:")
            else -> itemKey
        }
    }
}
