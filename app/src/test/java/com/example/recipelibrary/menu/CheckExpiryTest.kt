package com.example.recipelibrary.menu

import com.example.recipelibrary.data.menu.MenuLogic
import org.junit.Assert.assertEquals
import org.junit.Test

/** Coches « déjà à la maison ou dans le panier » effacées au bout de 3 jours (décision du 2026-11-22). */
class CheckExpiryTest {

    @Test fun cutoffIsThreeDaysBefore() {
        val now = 1_000_000_000_000L
        assertEquals(now - 3 * 24 * 3_600_000L, MenuLogic.checkCutoff(now))
    }
}
