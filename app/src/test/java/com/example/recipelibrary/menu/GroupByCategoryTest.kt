package com.example.recipelibrary.menu

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.groupByCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/** Fenêtre « Ajouter au menu » rangée par catégorie (demande de l'utilisatrice). */
class GroupByCategoryTest {

    @Test fun alphabeticalCategories_noCategoryLast_titlesSorted() {
        val r = listOf(
            Recipe(id = "1", title = "Tarte", category = "Desserts"),
            Recipe(id = "2", title = "Soupe", category = null),
            Recipe(id = "3", title = "Éclair", category = "Desserts"),
            Recipe(id = "4", title = "Quiche", category = "Entrées"),
            Recipe(id = "5", title = "Pain", category = "  ")
        )
        val g = groupByCategory(r)
        assertEquals(listOf("Desserts", "Entrées", "Sans catégorie"), g.map { it.first })
        assertEquals(listOf("Éclair", "Tarte"), g[0].second.map { it.title })
        assertEquals(listOf("Pain", "Soupe"), g[2].second.map { it.title })
    }
}
