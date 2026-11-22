package com.example.recipelibrary.data.shopping

import kotlin.math.ceil
import kotlin.math.roundToLong

/** Affichage des quantités de la liste de courses (spec 002, research R3). */
object QuantityFormat {

    private const val EPS = 1e-6

    /** « 250 g », « 1,5 kg ». */
    fun mass(g: Double): String {
        val rounded = if (g < 10) round1(g) else g.roundToLong().toDouble()
        return if (rounded < 1000) "${num(rounded)} g" else "${num(round1(g / 1000))} kg"
    }

    /** « 30 ml », « 70 cl », « 1,5 l ». */
    fun volume(ml: Double): String {
        if (ml < 100) return "${num(if (ml < 10) round1(ml) else ml.roundToLong().toDouble())} ml"
        val cl = round1(ml / 10)
        return if (cl < 100) "${num(cl)} cl" else "${num(round1(ml / 1000))} l"
    }

    /** Nombre de pièces arrondi au supérieur (1,5 citron → 2). */
    fun pieces(n: Double): String = ceilInt(n).toString()

    fun sachets(n: Double): String = ceilInt(n).let { if (it > 1) "$it sachets" else "$it sachet" }

    /** « 1 boîte », « 2 boîtes », « 3 pincées ». */
    fun loose(count: Double, unit: String): String {
        val n = ceilInt(count)
        val (singular, plural) = UNIT_NAMES[unit] ?: (unit to unit)
        return "$n ${if (n > 1) plural else singular}"
    }

    /** Toutes les quantités d'un article, jointes par « + » ; chaîne vide si aucune. */
    fun of(item: ShoppingItem): String {
        if (item.seasoning) return ""
        val parts = mutableListOf<String>()
        item.mass?.let { parts += mass(it) }
        item.volume?.let { parts += volume(it) }
        item.pieces?.let { parts += pieces(it) }
        item.sachets?.let { parts += sachets(it) }
        item.loose.forEach { (u, c) -> parts += loose(c, u) }
        return parts.joinToString(" + ")
    }

    private fun ceilInt(x: Double): Long = maxOf(1L, ceil(x - EPS).toLong())
    private fun round1(x: Double): Double = (x * 10).roundToLong() / 10.0

    /** 1.0 → « 1 », 1.5 → « 1,5 ». */
    private fun num(x: Double): String {
        val r = round1(x)
        return if (r == r.roundToLong().toDouble()) r.roundToLong().toString() else r.toString().replace('.', ',')
    }

    private val UNIT_NAMES = mapOf(
        "boite" to ("boîte" to "boîtes"), "botte" to ("botte" to "bottes"), "pincee" to ("pincée" to "pincées"),
        "gousse" to ("gousse" to "gousses"), "pot" to ("pot" to "pots"), "tranche" to ("tranche" to "tranches"),
        "feuille" to ("feuille" to "feuilles"), "cube" to ("cube" to "cubes"), "poignee" to ("poignée" to "poignées"),
        "brin" to ("brin" to "brins"), "morceau" to ("morceau" to "morceaux"), "trait" to ("trait" to "traits"),
        "carre" to ("carré" to "carrés"), "boule" to ("boule" to "boules"), "noix" to ("noix" to "noix")
    )
}
