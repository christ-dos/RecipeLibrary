package com.example.recipelibrary.menu

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.MenuLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles du menu (spec 002, T010). */
class MenuLogicTest {

    private val withServings = Recipe(id = "a", title = "Crêpes", servings = 4, servingsSource = Recipe.SERVINGS_USER)
    private val noServings = Recipe(id = "b", title = "Quiche")

    @Test fun defaultServings_recipeThenEstimateThenNull() {
        assertEquals(4, MenuLogic.defaultServings(withServings, estimated = 6))
        assertEquals(6, MenuLogic.defaultServings(noServings, estimated = 6))
        assertNull(MenuLogic.defaultServings(noServings, estimated = null))
        assertNull(MenuLogic.defaultServings(noServings, estimated = 0))
        assertEquals(1, MenuEntry(recipeId = "b").times)
    }

    @Test fun factor_servingsRatioElseTimes() {
        assertEquals(2.0, MenuLogic.factor(MenuEntry(recipeId = "a", servings = 8), 4), 1e-9)
        assertEquals(0.5, MenuLogic.factor(MenuEntry(recipeId = "a", servings = 2), 4), 1e-9)
        assertEquals(1.0, MenuLogic.factor(MenuEntry(recipeId = "b", servings = null), null), 1e-9)
        assertEquals(3.0, MenuLogic.factor(MenuEntry(recipeId = "b", servings = null, times = 3), null), 1e-9)
        // Parts prévues mais recette sans parts : le nombre de fois s'applique
        assertEquals(2.0, MenuLogic.factor(MenuEntry(recipeId = "b", servings = 6, times = 2), null), 1e-9)
    }

    @Test fun bounds() {
        assertTrue(MenuLogic.isValidTimes(1)); assertTrue(MenuLogic.isValidTimes(10))
        assertFalse(MenuLogic.isValidTimes(0)); assertFalse(MenuLogic.isValidTimes(11))
        assertTrue(MenuLogic.isValidServings(1)); assertTrue(MenuLogic.isValidServings(100))
        assertFalse(MenuLogic.isValidServings(0)); assertFalse(MenuLogic.isValidServings(101)); assertFalse(MenuLogic.isValidServings(null))
    }

    @Test fun groupByDay_mondayToSundayThenNoDay() {
        val e = listOf(
            MenuEntry(id = "1", recipeId = "a", day = null, position = 0),
            MenuEntry(id = "2", recipeId = "a", day = 7, position = 1),
            MenuEntry(id = "3", recipeId = "b", day = 1, position = 2),
            MenuEntry(id = "4", recipeId = "b", day = 7, position = 3),
            MenuEntry(id = "5", recipeId = "b", day = null, position = 4)
        )
        val g = MenuLogic.groupByDay(e.shuffled(java.util.Random(1)))
        assertEquals(listOf(1, 7, null), g.map { it.day })
        assertEquals(listOf(listOf("3"), listOf("2", "4"), listOf("1", "5")), g.map { gr -> gr.entries.map { it.id } })
        assertEquals("Lundi", MenuLogic.dayName(1))
        assertEquals("Dimanche", MenuLogic.dayName(7))
        assertEquals("Sans jour", MenuLogic.dayName(null))
        assertEquals(emptyList<Any>(), MenuLogic.groupByDay(emptyList()))
    }
}
