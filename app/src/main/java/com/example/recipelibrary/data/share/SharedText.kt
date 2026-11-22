package com.example.recipelibrary.data.share

/** Ce que contient un texte partagé vers l'app (feature 005, partage). */
sealed interface SharedText {
    /** Recette au format JSON (export de l'app, Telegram). */
    data class RecipeJson(val json: String) : SharedText
    /** Lien vers une image : à appliquer comme photo d'une recette. */
    data class ImageLink(val url: String) : SharedText
    /** Lien de page ou de vidéo (Instagram, TikTok, YouTube, site) : import par URL. */
    data class PageLink(val url: String) : SharedText
    /** Texte de recette sans lien (notes, message) : import texte. */
    data class RecipeText(val text: String) : SharedText
    /** Rien d'exploitable. */
    object Unknown : SharedText

    companion object {
        private val URL = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val IMAGE_EXT = Regex(""".*\.(jpg|jpeg|png|webp|gif)(\?.*)?""", RegexOption.IGNORE_CASE)
        private val IMAGE_HOSTS = listOf("googleusercontent", "wikimedia", "unsplash", "pexels")
        /** Au-delà de ce nombre de lignes hors lien, le texte est une recette et le lien n'est qu'une source. */
        private const val RECIPE_TEXT_LINES = 5
        /** Sans lien, il faut au moins ce nombre de lignes pour y voir une recette. */
        private const val MIN_TEXT_LINES = 3

        fun classify(raw: String?): SharedText {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return Unknown
            if (text.startsWith("{")) return RecipeJson(text)
            val match = URL.find(text)
            if (match != null) {
                val url = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
                if (url.matches(IMAGE_EXT) || IMAGE_HOSTS.any { url.contains(it, ignoreCase = true) }) return ImageLink(url)
                val rest = text.removeRange(match.range).lines().count { it.isNotBlank() }
                return if (rest >= RECIPE_TEXT_LINES) RecipeText(text) else PageLink(url)
            }
            return if (text.lines().count { it.isNotBlank() } >= MIN_TEXT_LINES) RecipeText(text) else Unknown
        }
    }
}
