package com.example.recipelibrary.data.nutrition

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Table des aliments de référence et appariement « texte d'un ingrédient → aliment ».
 *
 * Appariement (R3) : la variante de nom la plus longue (en nombre de mots) contenue dans le
 * texte, en mots entiers ; à égalité, la plus à gauche, puis la plus longue en caractères.
 */
class FoodTable(
    val foods: List<Food>,
    val source: String = "",
    /**
     * Aliments de remplacement choisis par l'utilisatrice (story 6) : texte normalisé → code Ciqual.
     * Utilisés seulement quand la table ne reconnaît aucun aliment ; un code absent de la table est ignoré.
     */
    val substitutions: Map<String, String> = emptyMap()
) {

    private class Alias(val words: List<String>, val chars: Int, val food: Food, val key: String = "")

    /** Variantes indexées par leur premier mot. */
    private val byFirstWord: Map<String, List<Alias>> = foods
        .flatMap { f -> f.aliases.map { a -> Normalize.words(a) }.filter { it.isNotEmpty() }.distinct().map { Alias(it, it.sumOf { w -> w.length }, f) } }
        .groupBy { it.words.first() }

    private val byCode: Map<String, Food> by lazy { foods.associateBy { it.code } }

    /** Remplacements indexés comme les variantes de nom (même règle d'appariement). */
    private val substitutionsByFirstWord: Map<String, List<Alias>> by lazy {
        substitutions.mapNotNull { (key, code) ->
            val food = byCode[code] ?: return@mapNotNull null
            val words = Normalize.words(key).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Alias(words, words.sumOf { it.length }, food, words.joinToString(" "))
        }.groupBy { it.words.first() }
    }

    val size: Int get() = foods.size

    /** Aliment de code [code], ou null. */
    fun food(code: String): Food? = byCode[code]

    /** Même table avec ces remplacements (texte normalisé → code). */
    fun withSubstitutions(map: Map<String, String>): FoodTable =
        if (map == substitutions) this else FoodTable(foods, source, map)

    /** Aliment reconnu dans [text], ou null. */
    fun match(text: String): Food? = best(byFirstWord, text)?.food

    /** Remplacement qui s'applique à [text] : texte retenu et aliment, ou null. */
    fun matchSubstitution(text: String): Pair<String, Food>? =
        if (substitutions.isEmpty()) null else best(substitutionsByFirstWord, text)?.let { it.key to it.food }

    private fun best(index: Map<String, List<Alias>>, text: String): Alias? {
        val words = Normalize.words(text)
        var best: Alias? = null
        var bestPos = 0
        for (i in words.indices) {
            val candidates = index[words[i]] ?: continue
            for (a in candidates) {
                if (i + a.words.size > words.size) continue
                if (!a.words.indices.all { k -> words[i + k] == a.words[k] }) continue
                val b = best
                val better = b == null ||
                    a.words.size > b.words.size ||
                    (a.words.size == b.words.size && i < bestPos) ||
                    (a.words.size == b.words.size && i == bestPos && a.chars > b.chars)
                if (better) { best = a; bestPos = i }
            }
        }
        return best
    }

    private class FileJson(
        @SerializedName("source") val source: String?,
        @SerializedName("foods") val foods: List<Food>?
    )

    companion object {
        const val ASSET_PATH = "nutrition/foods.json"

        @Volatile
        private var cached: FoodTable? = null

        /** Construit la table depuis le texte de `foods.json` (testable en JVM). */
        fun fromJson(json: String): FoodTable {
            val file = Gson().fromJson(json, FileJson::class.java)
            val foods = file.foods.orEmpty().map { f ->
                // Gson ne passe pas par le constructeur : valeurs par défaut rétablies ici.
                @Suppress("SENSELESS_COMPARISON")
                if (f.kind == null || f.aliases == null) f.copy(kind = f.kind ?: FoodKind.NORMAL, aliases = f.aliases ?: emptyList()) else f
            }
            return FoodTable(foods, file.source.orEmpty())
        }

        /** Table chargée une seule fois par lancement (à appeler hors du fil principal). */
        fun load(context: Context): FoodTable = cached ?: synchronized(this) {
            cached ?: context.applicationContext.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8)
                .use { fromJson(it.readText()) }
                .also { cached = it }
        }

        /** Table déjà chargée, ou null. */
        fun loadedOrNull(): FoodTable? = cached
    }
}
