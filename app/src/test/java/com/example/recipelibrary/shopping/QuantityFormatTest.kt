package com.example.recipelibrary.shopping

import com.example.recipelibrary.data.shopping.QuantityFormat
import com.example.recipelibrary.data.shopping.ShoppingItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Affichage des quantités (spec 002, T017). */
class QuantityFormatTest {

    @Test fun mass() {
        assertEquals("250 g", QuantityFormat.mass(250.0))
        assertEquals("999 g", QuantityFormat.mass(999.0))
        assertEquals("1 kg", QuantityFormat.mass(1000.0))
        assertEquals("1,5 kg", QuantityFormat.mass(1500.0))
        assertEquals("1,3 kg", QuantityFormat.mass(1250.0 + 30.0))
        assertEquals("2,5 g", QuantityFormat.mass(2.5))
        assertEquals("13 g", QuantityFormat.mass(12.6))
    }

    @Test fun volume() {
        assertEquals("30 ml", QuantityFormat.volume(30.0))
        assertEquals("5 ml", QuantityFormat.volume(5.0))
        assertEquals("10 cl", QuantityFormat.volume(100.0))
        assertEquals("70 cl", QuantityFormat.volume(700.0))
        assertEquals("12,5 cl", QuantityFormat.volume(125.0))
        assertEquals("99 cl", QuantityFormat.volume(990.0))
        assertEquals("1 l", QuantityFormat.volume(1000.0))
        assertEquals("1,5 l", QuantityFormat.volume(1500.0))
    }

    @Test fun piecesAndSachetsRoundedUp() {
        assertEquals("2", QuantityFormat.pieces(1.5))
        assertEquals("3", QuantityFormat.pieces(3.0))
        assertEquals("3", QuantityFormat.pieces(2.5 * 1.2)) // 3,0000000004 n'arrondit pas à 4
        assertEquals("1", QuantityFormat.pieces(0.5))
        assertEquals("1 sachet", QuantityFormat.sachets(1.0))
        assertEquals("2 sachets", QuantityFormat.sachets(1.5))
    }

    @Test fun looseWithPlural() {
        assertEquals("1 boîte", QuantityFormat.loose(1.0, "boite"))
        assertEquals("2 boîtes", QuantityFormat.loose(2.0, "boite"))
        assertEquals("3 pincées", QuantityFormat.loose(3.0, "pincee"))
        assertEquals("2 morceaux", QuantityFormat.loose(2.0, "morceau"))
    }

    @Test fun severalKindsJoinedByPlus() {
        val item = ShoppingItem(key = "food:20047", label = "tomate", mass = 400.0, pieces = 3.0, loose = mapOf("boite" to 1.0))
        assertEquals("400 g + 3 + 1 boîte", QuantityFormat.of(item))
        assertEquals("", QuantityFormat.of(ShoppingItem(key = "food:11017", label = "sel", seasoning = true, mass = 5.0)))
        assertEquals("", QuantityFormat.of(ShoppingItem(key = "food:31016", label = "sucre")))
    }
}
