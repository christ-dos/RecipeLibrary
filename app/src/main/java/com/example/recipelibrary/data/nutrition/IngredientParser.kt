package com.example.recipelibrary.data.nutrition

/** Raison pour laquelle une ligne n'a pas de poids exploitable. */
enum class IngredientIssue {
    /** Quantité vague (pincée, noix de beurre, « selon le goût », pas de quantité). */
    VAGUE,
    /** Aliment absent de la table. */
    UNKNOWN_FOOD,
    /** Quantité en pièces pour un aliment sans poids moyen par pièce. */
    NO_PIECE_WEIGHT,
    /** Ligne « À compléter » laissée par un import incomplet. */
    TO_COMPLETE
}

/** Ligne d'ingrédient analysée (calculée, jamais stockée). */
data class ParsedIngredient(
    val line: String,
    val quantity: Double?,
    /** Unité reconnue : « g », « ml », « c. a soupe », « piece », « pincee »… ; null si aucune. */
    val unit: String?,
    val foodText: String,
    val food: Food?,
    /** Poids lu dans le texte, en grammes. */
    val grams: Double?,
    val issue: IngredientIssue?,
    /** Quantité additionnable pour la liste de courses (g, ml, pièces, sachets) ; null si vague ou absente. */
    val amount: Amount? = null,
    /** Quantité vague telle qu'écrite (« 1 boîte », « 2 brins ») ; null si `amount` est rempli ou sans quantité. */
    val looseQuantity: LooseQuantity? = null,
    /**
     * Texte normalisé de l'aliment de remplacement appliqué (story 6) : la table ne reconnaissait
     * rien et l'utilisatrice a choisi [food] pour ce texte ; null sinon.
     */
    val substitutionKey: String? = null
)

/** Nature d'une quantité additionnable. */
enum class AmountKind { MASS, VOLUME, PIECE, SACHET }

/** Quantité additionnable : `value` en g (MASS), ml (VOLUME), pièces ou sachets. */
data class Amount(val kind: AmountKind, val value: Double)

/**
 * Quantité vague gardée telle qu'écrite : nombre et unité (« boite », « botte »…, sans accent).
 * Deux quantités de même unité s'additionnent (« 1 boîte » + « 1 boîte » → « 2 boîtes »).
 */
data class LooseQuantity(val count: Double, val unit: String)

/**
 * Lecture des quantités par règles fixes (R2) : quantité, unité, aliment, conversion en grammes.
 * Fonction pure, sans dépendance Android.
 */
class IngredientParser(private val table: FoodTable) {

    private enum class Kind { MASS, VOLUME, PIECE, SACHET, VAGUE }

    private class UnitDef(val label: String, val kind: Kind, val factor: Double, pattern: String) {
        val regex = Regex("^(?:$pattern)(?![a-z])\\.?\\s*")
    }

    fun parse(line: String): ParsedIngredient {
        val trimmed = line.trim()
        if (Normalize.text(trimmed) == TO_COMPLETE_NORMALIZED) {
            return ParsedIngredient(line, null, null, "", null, null, IngredientIssue.TO_COMPLETE)
        }
        var s = fractions(Normalize.plain(trimmed)).replace(BULLET, "").trim()

        // Parenthèses : retirées du texte principal, gardées pour un poids éventuel « (200 g) ».
        val parenGrams = PAREN.findAll(s).mapNotNull { m -> massOrVolumeGrams(m.groupValues[1]) }.firstOrNull()
        s = PAREN.replace(s, " ").replace(SPACES, " ").trim()
        s = s.replace(APPROX, "")

        var quantity: Double? = null
        var unit: UnitDef? = null
        var rest = s

        val lead = leadingQuantity(s)
        if (lead != null) {
            quantity = lead.first
            rest = lead.second.trimStart()
            unit = UNITS.firstOrNull { it.regex.containsMatchIn(rest) }
            if (unit != null) rest = unit.regex.replaceFirst(rest, "")
            // « 1 noix de beurre » : noix est une unité vague ; « 3 noix de saint-jacques » non.
            if (unit == null) {
                val m = NOIX_FAT.find(rest)
                if (m != null) { unit = NOIX_UNIT; rest = rest.substring(m.range.last + 1) }
            }
        } else {
            // « Farine : 200 g » : quantité et unité ailleurs dans la ligne.
            val m = INLINE_QTY.find(s)
            if (m != null) {
                quantity = number(m.groupValues[1])
                unit = UNITS.first { it.regex.containsMatchIn(m.groupValues[2]) }
                rest = (s.substring(0, m.range.first) + " " + s.substring(m.range.last + 1)).trim()
            }
        }
        rest = rest.replace(LEADING_DE, "").trim()

        // Sans aliment dans le reste, relire la ligne, mais jamais le texte de l'unité (« c. à café » n'est pas du café).
        val direct = table.match(rest) ?: if (unit == null) table.match(s) else null
        // Aliment de remplacement choisi par l'utilisatrice : seulement si la table ne reconnaît rien.
        val substitution = if (direct != null) null
            else table.matchSubstitution(rest) ?: if (unit == null) table.matchSubstitution(s) else null
        val food = direct ?: substitution?.second
        val vagueWords = VAGUE_WORDS.containsMatchIn(s)

        var grams: Double? = null
        var issue: IngredientIssue? = null
        val q = quantity
        when {
            q == null || q <= 0 -> issue = IngredientIssue.VAGUE
            unit?.kind == Kind.MASS -> grams = q * unit.factor
            unit?.kind == Kind.VOLUME -> grams = q * unit.factor * (food?.densityOrDefault ?: 1.0)
            unit?.kind == Kind.VAGUE -> {
                val per = innerGrams(rest)
                if (per != null) grams = q * per else issue = IngredientIssue.VAGUE
            }
            unit?.kind == Kind.SACHET -> {
                val per = innerGrams(rest) ?: food?.let { SACHET_G[it.code] }
                if (per != null) grams = q * per else issue = IngredientIssue.VAGUE
            }
            vagueWords -> issue = IngredientIssue.VAGUE
            else -> { // pièces (« 3 œufs », « 2 gousses d'ail », « 1 boîte de 400 g »)
                val per = innerGrams(rest) ?: food?.pieceG?.let { it * sizeFactor(rest, food) }
                if (per != null) grams = q * per else issue = IngredientIssue.NO_PIECE_WEIGHT
            }
        }
        if (parenGrams != null && (grams == null || unit?.kind != Kind.MASS && unit?.kind != Kind.VOLUME)) {
            grams = parenGrams
            issue = null
        }
        if (food == null) issue = IngredientIssue.UNKNOWN_FOOD
        val unitLabel = unit?.label ?: if (q != null && q > 0) "piece" else null
        val (amount, loose) = shoppingAmount(q, unit, rest, parenGrams, vagueWords)
        return ParsedIngredient(line, quantity, unitLabel, rest, food, grams, issue, amount, loose, substitution?.first)
    }

    /**
     * Quantité pour la liste de courses (spec 002) : ce qu'on achète, sans conversion en grammes
     * d'un volume ou d'une pièce. Un contenant avec un poids écrit (« 1 boîte de 400 g ») devient une masse.
     */
    private fun shoppingAmount(
        q: Double?, unit: UnitDef?, rest: String, parenGrams: Double?, vagueWords: Boolean
    ): Pair<Amount?, LooseQuantity?> {
        if (q == null || q <= 0) return null to null
        val inner = innerGrams(rest)
        return when {
            unit?.kind == Kind.MASS -> Amount(AmountKind.MASS, q * unit.factor) to null
            unit?.kind == Kind.VOLUME -> Amount(AmountKind.VOLUME, q * unit.factor) to null
            inner != null -> Amount(AmountKind.MASS, q * inner) to null
            parenGrams != null -> Amount(AmountKind.MASS, parenGrams) to null
            unit?.kind == Kind.SACHET -> Amount(AmountKind.SACHET, q) to null
            unit == null && vagueWords -> null to null
            unit == null || unit.label == "piece" -> Amount(AmountKind.PIECE, q) to null
            else -> null to LooseQuantity(q, unit.label) // gousse, pot, boîte, botte, pincée…
        }
    }

    /** « 200 g », « 25 cl »… → grammes (eau : 1 g/ml) ; sinon null. */
    private fun massOrVolumeGrams(text: String): Double? {
        val m = INLINE_QTY.find(fractions(text.trim())) ?: return null
        val u = UNITS.first { it.regex.containsMatchIn(m.groupValues[2]) }
        return number(m.groupValues[1])?.times(u.factor)
    }

    /** « boîte de 400 g », « sachet de 11 g » → poids d'une unité. */
    private fun innerGrams(rest: String): Double? {
        val m = INNER_QTY.find(rest) ?: return null
        return massOrVolumeGrams(m.groupValues[1])
    }

    private fun sizeFactor(rest: String, food: Food): Double {
        val w = Normalize.words(rest)
        val aliasWords = food.aliases.flatMap { Normalize.words(it) }.toSet()
        return when {
            w.any { it in BIG && it !in aliasWords } -> 1.5
            w.any { it in SMALL && it !in aliasWords } -> 0.6
            else -> 1.0
        }
    }

    /** Quantité en tête de ligne et reste du texte, ou null. */
    private fun leadingQuantity(s: String): Pair<Double, String>? {
        MIXED.find(s)?.let { m ->
            val v = m.groupValues[1].toDouble() + m.groupValues[2].toDouble() / m.groupValues[3].toDouble()
            return v to s.substring(m.range.last + 1)
        }
        FRACTION.find(s)?.let { m ->
            val d = m.groupValues[2].toDouble()
            if (d > 0) return m.groupValues[1].toDouble() / d to s.substring(m.range.last + 1)
        }
        RANGE.find(s)?.let { m ->
            val a = number(m.groupValues[1]); val b = number(m.groupValues[2])
            if (a != null && b != null) return (a + b) / 2 to s.substring(m.range.last + 1)
        }
        NUMBER.find(s)?.let { m ->
            val v = number(m.groupValues[1]) ?: return null
            var rest = s.substring(m.range.last + 1)
            HALF_AFTER.find(rest)?.let { h -> rest = rest.substring(h.range.last + 1); return v + 0.5 to rest }
            return v to rest
        }
        WORD_NUMBER.find(s)?.let { m ->
            val w = m.groupValues[1]
            var v = WORDS[w] ?: return null
            var rest = s.substring(m.range.last + 1)
            // « un demi litre », « une demi-botte »
            DEMI.find(rest)?.let { d -> if (w == "un" || w == "une") { v = 0.5; rest = rest.substring(d.range.last + 1) } }
            return v to rest
        }
        DEMI.find(s)?.let { d -> return 0.5 to s.substring(d.range.last + 1) }
        return null
    }

    private fun number(t: String): Double? = t.replace(',', '.').toDoubleOrNull()

    private fun fractions(s: String): String = FRACTION_CHARS.entries.fold(s) { acc, (c, f) ->
        acc.replace(Regex("(\\d)$c"), "$1 $f").replace(c, f)
    }

    companion object {
        private const val TO_COMPLETE_NORMALIZED = "a completer"

        private const val NUM = "(\\d+(?:[.,]\\d+)?)"
        private val BULLET = Regex("^[-–—•*·]+\\s*")
        private val PAREN = Regex("\\(([^)]*)\\)")
        private val SPACES = Regex("\\s+")
        private val APPROX = Regex("^(?:environ|env\\.?|~|±|a peu pres)\\s*")
        private val MIXED = Regex("^(\\d+)\\s+(\\d+)\\s*/\\s*(\\d+)")
        private val FRACTION = Regex("^(\\d+)\\s*/\\s*(\\d+)")
        private val RANGE = Regex("^$NUM\\s*(?:a|-|–|ou)\\s*$NUM(?![\\d/])")
        private val NUMBER = Regex("^$NUM")
        private val HALF_AFTER = Regex("^\\s*(?:et\\s+)?demie?\\b")
        private val DEMI = Regex("^\\s*demie?[- ]?")
        private val LEADING_DE = Regex("^(?:de |d'|d |du |des )+")
        private val NOIX_FAT = Regex("^noix\\s+(?:de\\s+|d')(?=beurre|margarine|saindoux)")
        private val VAGUE_WORDS = Regex("\\b(?:selon (?:le |votre |vos )?go[uû]ts?|au go[uû]t|facultatif|q\\.?s\\.?|quelques|un peu|peu de|a volonte)\\b")
        private val BIG = setOf("gro", "grosse", "grand", "grande")
        private val SMALL = setOf("petit", "petite")

        private val WORDS = mapOf(
            "un" to 1.0, "une" to 1.0, "deux" to 2.0, "trois" to 3.0, "quatre" to 4.0, "cinq" to 5.0,
            "six" to 6.0, "sept" to 7.0, "huit" to 8.0, "neuf" to 9.0, "dix" to 10.0, "onze" to 11.0,
            "douze" to 12.0, "une douzaine" to 12.0, "une dizaine" to 10.0, "une demi-douzaine" to 6.0
        )
        private val WORD_NUMBER = Regex("^(une demi-douzaine|une douzaine|une dizaine|un|une|deux|trois|quatre|cinq|six|sept|huit|neuf|dix|onze|douze)\\b")

        private val FRACTION_CHARS = mapOf("½" to "1/2", "¼" to "1/4", "¾" to "3/4", "⅓" to "1/3", "⅔" to "2/3")

        /** Poids d'un sachet selon l'aliment (codes Ciqual). */
        private val SACHET_G = mapOf("11046" to 11.0, "31044" to 7.5, "11010" to 7.0)

        private val UNITS = listOf(
            UnitDef("kg", Kind.MASS, 1000.0, "kg|kilos?|kilogrammes?"),
            UnitDef("mg", Kind.MASS, 0.001, "mg|milligrammes?"),
            UnitDef("g", Kind.MASS, 1.0, "g|gr|grs|grammes?"),
            UnitDef("ml", Kind.VOLUME, 1.0, "ml|millilitres?"),
            UnitDef("cl", Kind.VOLUME, 10.0, "cl|centilitres?"),
            UnitDef("dl", Kind.VOLUME, 100.0, "dl|decilitres?"),
            UnitDef("l", Kind.VOLUME, 1000.0, "l|lt|litres?"),
            UnitDef("c. a soupe", Kind.VOLUME, 15.0, "(?:cuill?\\w*|c)\\.?\\s*(?:a|de)?\\.?\\s*(?:soupe|s)"),
            UnitDef("c. a cafe", Kind.VOLUME, 5.0, "(?:cuill?\\w*|c)\\.?\\s*(?:a|de)?\\.?\\s*(?:cafe|the|c)"),
            UnitDef("c. a soupe", Kind.VOLUME, 15.0, "cuill?\\w*"),
            UnitDef("verre", Kind.VOLUME, 200.0, "verres?"),
            UnitDef("tasse", Kind.VOLUME, 250.0, "tasses?"),
            UnitDef("bol", Kind.VOLUME, 350.0, "bols?"),
            UnitDef("sachet", Kind.SACHET, 1.0, "sachets?"),
            UnitDef("gousse", Kind.PIECE, 1.0, "gousses?"),
            UnitDef("tranche", Kind.PIECE, 1.0, "tranches?"),
            UnitDef("feuille", Kind.PIECE, 1.0, "feuilles?"),
            UnitDef("pot", Kind.PIECE, 1.0, "pots?"),
            UnitDef("cube", Kind.PIECE, 1.0, "cubes?"),
            UnitDef("piece", Kind.PIECE, 1.0, "pieces?|unites?"),
            UnitDef("pincee", Kind.VAGUE, 1.0, "pincees?"),
            UnitDef("botte", Kind.VAGUE, 1.0, "bottes?|bouquets?"),
            UnitDef("poignee", Kind.VAGUE, 1.0, "poignees?"),
            UnitDef("brin", Kind.VAGUE, 1.0, "brins?"),
            UnitDef("morceau", Kind.VAGUE, 1.0, "morceaux?"),
            UnitDef("boite", Kind.VAGUE, 1.0, "boites?|conserves?|briques?"),
            UnitDef("trait", Kind.VAGUE, 1.0, "traits?"),
            UnitDef("carre", Kind.VAGUE, 1.0, "carres?"),
            UnitDef("boule", Kind.VAGUE, 1.0, "boules?")
        )
        private val NOIX_UNIT = UnitDef("noix", Kind.VAGUE, 1.0, "noix")

        private val INLINE_QTY = Regex("$NUM\\s*(kg|kilos?|g|gr|grammes?|mg|ml|cl|dl|l|litres?)(?![a-z])")
        private val INNER_QTY = Regex("(?:^|\\bde\\s+)(\\d+(?:[.,]\\d+)?\\s*(?:kg|g|gr|grammes?|ml|cl|dl|l|litres?))(?![a-z])")
    }
}
