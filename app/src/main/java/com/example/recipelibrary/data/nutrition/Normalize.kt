package com.example.recipelibrary.data.nutrition

import java.text.Normalizer

/**
 * Normalisation commune aux noms d'aliments et aux lignes de recette (même règles que
 * `tools/ciqual/build_foods.py`) : minuscules, accents retirés, `œ`→`oe`, `æ`→`ae`,
 * découpage en mots (lettres, chiffres, %), pluriels simples retirés mot par mot.
 */
object Normalize {
    private val WORD = Regex("[a-z0-9%]+")
    private val MARKS = Regex("\\p{Mn}+")

    /** Minuscules sans accents, ponctuation conservée (utile à l'analyseur). */
    fun plain(text: String): String {
        val s = text.lowercase()
            .replace("œ", "oe").replace("æ", "ae")
            .replace('’', '\'').replace('`', '\'').replace('ʼ', '\'')
            .replace(' ', ' ').replace(' ', ' ')
        return MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "")
    }

    /** Mots normalisés, pluriels retirés. */
    fun words(text: String): List<String> =
        WORD.findAll(plain(text)).map { singular(it.value) }.toList()

    /** Texte normalisé : mots séparés par une espace. */
    fun text(text: String): String = words(text).joinToString(" ")

    fun singular(w: String): String = when {
        w.length > 3 && w.endsWith("aux") -> w.dropLast(3) + "al"
        w.length > 2 && (w.endsWith("s") || w.endsWith("x")) -> w.dropLast(1)
        else -> w
    }
}
