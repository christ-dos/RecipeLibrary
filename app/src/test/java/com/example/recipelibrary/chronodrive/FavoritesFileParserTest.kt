package com.example.recipelibrary.chronodrive

import com.example.recipelibrary.data.chronodrive.FavoritesFileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * Lecture du fichier de favoris (spec 003, contracts/favorites-file.md).
 * Copie figée du relevé (2026-11-22, complété) : 124 produits, 13 rayons, premier 189100, dernier 558237.
 */
class FavoritesFileParserTest {

    private val releve = File("src/test/resources/chronodrive/favoris-releve.txt").readText(Charsets.UTF_8)

    @Test fun realFile_allProductsAndAisles() {
        val parsed = FavoritesFileParser.parse(releve)!!
        assertEquals(124, parsed.products.size)
        assertEquals(13, parsed.products.map { it.aisle }.distinct().size)
        assertEquals(189100L, parsed.products.first().productId)
        assertEquals("champignons bruns bio", parsed.products.first().name)
        assertEquals(558237L, parsed.products.last().productId)
        assertEquals(0, parsed.ignored)
    }

    @Test fun realFile_sameNameProductsStayDistinct() {
        val p = FavoritesFileParser.parse(releve)!!.products
        assertEquals(listOf(365920L, 305254L), p.filter { it.name == "banane" }.map { it.productId })
        assertEquals(listOf(368958L, 82295L), p.filter { it.name == "emmental rape" }.map { it.productId })
        assertEquals("president", p.first { it.productId == 368958L }.brand)
    }

    @Test fun aisleTitle_withoutCounter() {
        val p = FavoritesFileParser.parse(releve)!!.products
        assertEquals("Produits laitiers & Oeufs", p.first { it.productId == 55785L }.aisle)
        assertEquals("Fruits & Légumes", p.first { it.productId == 365920L }.aisle)
    }

    @Test fun slug_toBrandAndName() {
        assertEquals(null to "banane", FavoritesFileParser.brandAndName("banane"))
        assertEquals("lactel" to "matin leger - lait sans lactose 12 mg uht",
            FavoritesFileParser.brandAndName("lactel--matin-leger---lait-sans-lactose-12-mg-uht"))
        assertEquals("happy bio" to "graines de chia bio", FavoritesFileParser.brandAndName("happy-bio--graines-de-chia-bio"))
    }

    @Test fun titleWithoutParens_isAnAisleWithoutProducts() {
        val parsed = FavoritesFileParser.parse("## Restent ~5 : à relever\n## Fruits (1)\n365920 banane")!!
        assertEquals(1, parsed.products.size)
        assertEquals("Fruits", parsed.products.single().aisle)
    }

    @Test fun counters_neverChecked() {
        val parsed = FavoritesFileParser.parse("## Fruits (20 sur 21)\n365920 banane")!!
        assertEquals(1, parsed.products.size)
    }

    @Test fun commentsBlankLinesCrLfAndBom() {
        val parsed = FavoritesFileParser.parse("﻿# commentaire\r\n\r\n## Fruits (2)\r\n365920 banane;123953 butternut\r\n")!!
        assertEquals(listOf(365920L, 123953L), parsed.products.map { it.productId })
    }

    @Test fun productBeforeFirstAisle_goesToOther() {
        assertEquals("Autre", FavoritesFileParser.parse("365920 banane")!!.products.single().aisle)
    }

    @Test fun malformedItems_ignoredAndCounted() {
        val parsed = FavoritesFileParser.parse("## Fruits (3)\n365920 banane;abc pomme;0 citron;12 Mauvais Slug;123953 butternut")!!
        assertEquals(listOf(365920L, 123953L), parsed.products.map { it.productId })
        assertEquals(3, parsed.ignored)
    }

    @Test fun duplicateNumber_firstWins() {
        val parsed = FavoritesFileParser.parse("## A (1)\n365920 banane\n## B (1)\n365920 autre-nom")!!
        assertEquals("banane", parsed.products.single().name)
        assertEquals("A", parsed.products.single().aisle)
    }

    @Test fun emptyOrNoValidProduct_isInvalid() {
        assertNull(FavoritesFileParser.parse(""))
        assertNull(FavoritesFileParser.parse("# rien\n## Fruits (0)\n"))
        assertNull(FavoritesFileParser.parse("Bonjour, ceci n'est pas un fichier de favoris."))
        assertNotNull(FavoritesFileParser.parse("1 a"))
    }
}
