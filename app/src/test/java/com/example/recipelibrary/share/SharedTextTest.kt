package com.example.recipelibrary.share

import com.example.recipelibrary.data.share.SharedText
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedTextTest {

    @Test
    fun instagramReel_isPageLink() {
        val shared = "https://www.instagram.com/reel/DAbc123/?igsh=MWx4"
        assertEquals(SharedText.PageLink(shared), SharedText.classify(shared))
    }

    @Test
    fun tiktokTextWithLinkInside_isPageLink() {
        val shared = "Regarde cette recette #cuisine https://vm.tiktok.com/ZMabc/ via TikTok"
        assertEquals(SharedText.PageLink("https://vm.tiktok.com/ZMabc/"), SharedText.classify(shared))
    }

    @Test
    fun chromeTitleThenLink_isPageLink() {
        val shared = "Quiche lorraine facile\nhttps://www.marmiton.org/recettes/quiche.aspx"
        assertEquals(SharedText.PageLink("https://www.marmiton.org/recettes/quiche.aspx"), SharedText.classify(shared))
    }

    @Test
    fun trailingPunctuation_removedFromLink() {
        assertEquals(SharedText.PageLink("https://youtu.be/xyz"), SharedText.classify("Vu ici (https://youtu.be/xyz)."))
    }

    @Test
    fun imageLink_isImageLink() {
        assertEquals(SharedText.ImageLink("https://site.fr/photo.JPG?w=400"), SharedText.classify("https://site.fr/photo.JPG?w=400"))
        assertEquals(
            SharedText.ImageLink("https://lh3.googleusercontent.com/abc"),
            SharedText.classify("https://lh3.googleusercontent.com/abc")
        )
    }

    @Test
    fun json_isRecipeJson() {
        val json = """{"title":"Crêpes","ingredients":[]}"""
        assertEquals(SharedText.RecipeJson(json), SharedText.classify("  $json \n"))
    }

    @Test
    fun recipeTextWithoutLink_isRecipeText() {
        val text = "Crêpes\nIngrédients\n250 g farine\n4 oeufs\nPréparation\nMélanger"
        assertEquals(SharedText.RecipeText(text), SharedText.classify(text))
    }

    @Test
    fun longRecipeTextWithSourceLink_isRecipeText() {
        val text = "Crêpes\n250 g farine\n4 oeufs\n50 cl lait\nMélanger\nCuire\nSource : https://site.fr/crepes"
        assertEquals(SharedText.RecipeText(text), SharedText.classify(text))
    }

    @Test
    fun shortTextWithoutLink_isUnknown() {
        assertEquals(SharedText.Unknown, SharedText.classify("coucou"))
        assertEquals(SharedText.Unknown, SharedText.classify("   "))
        assertEquals(SharedText.Unknown, SharedText.classify(null))
    }
}
