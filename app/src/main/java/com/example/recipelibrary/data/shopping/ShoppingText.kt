package com.example.recipelibrary.data.shopping

import com.example.recipelibrary.data.menu.ShoppingExtra

/** Texte partagé de la liste de courses (contracts/shopping-text.md). */
object ShoppingText {

    /**
     * @param recipeCount nombre d'entrées du menu.
     * @return le texte, ou null s'il n'y a rien à partager (bouton désactivé).
     */
    fun build(items: List<ShoppingItem>, extras: List<ShoppingExtra>, recipeCount: Int): String? {
        val open = items.filter { !it.checked }
        val toBuy = open.filter { !it.seasoning }
        val seasonings = open.filter { it.seasoning }
        val openExtras = extras.filter { !it.checked }
        if (toBuy.isEmpty() && seasonings.isEmpty() && openExtras.isEmpty()) return null

        val lines = mutableListOf<String>()
        lines += "Liste de courses ($recipeCount recette${if (recipeCount > 1) "s" else ""})"
        lines += ""
        fun itemLine(item: ShoppingItem): String {
            val q = QuantityFormat.of(item)
            return if (q.isEmpty()) "- ${item.label}" else "- ${item.label} : $q"
        }
        val tail = openExtras.map { "- ${it.text}" } +
            listOfNotNull(seasonings.takeIf { it.isNotEmpty() }?.let { s -> "- ${s.joinToString(", ") { it.label }} (à vérifier)" })
        if (!Aisles.showHeaders(toBuy)) {
            // Aucun rayon connu : liste à plat, comme avant la story 4
            toBuy.forEach { lines += itemLine(it) }
            lines += tail
            return lines.joinToString("\n")
        }
        // Titres de rayon dans l'ordre fixe ; ajouts à la main et assaisonnements à la fin de « Autres »
        val sections = Aisles.group(toBuy).toMutableList()
        if (tail.isNotEmpty() && sections.none { it.first == Aisles.OTHER }) sections += Aisles.OTHER to emptyList()
        sections.forEachIndexed { i, (aisle, list) ->
            if (i > 0) lines += ""
            lines += aisle
            list.forEach { lines += itemLine(it) }
            if (aisle == Aisles.OTHER) lines += tail
        }
        return lines.joinToString("\n")
    }
}
