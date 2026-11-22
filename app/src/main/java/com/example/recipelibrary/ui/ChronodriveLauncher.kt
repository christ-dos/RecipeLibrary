package com.example.recipelibrary.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.recipelibrary.data.shopping.ChronodriveSearch

/**
 * Bouton « Chronodrive » (contracts/chronodrive-search.md) : copie le terme, ouvre l'application
 * Chronodrive sur les favoris de l'utilisatrice (sinon sur l'accueil), sinon le site dans le navigateur.
 * Pas d'ouverture directe sur la recherche : essayé le 2026-11-22, l'application affiche
 * « Cette page s'est volatilisée… » pour ses propres liens chronodrive.com/search/….
 * Aucune donnée personnelle envoyée, aucune action sur le panier.
 */
object ChronodriveLauncher {

    fun open(context: Context, term: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Chronodrive", term))
        Toast.makeText(context, "« $term » copié : regarde dans tes favoris ou colle-le dans la recherche 🔍", Toast.LENGTH_LONG).show()

        // Favoris d'abord (produits habituels, loupe en haut pour chercher le reste), sinon l'accueil
        val favorites = Intent(Intent.ACTION_VIEW, Uri.parse(ChronodriveSearch.FAVORITES_URL))
            .setPackage(ChronodriveSearch.PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val app = context.packageManager.getLaunchIntentForPackage(ChronodriveSearch.PACKAGE)
        val opened = start(context, favorites) ||
            app != null && start(context, app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) ||
            start(context, Intent(Intent.ACTION_VIEW, Uri.parse(ChronodriveSearch.searchUrl(term))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (!opened) Toast.makeText(context, "Impossible d'ouvrir Chronodrive", Toast.LENGTH_SHORT).show()
    }

    /**
     * Spec 003 (contracts/chrono-link.md) : copie le nom de l'article puis ouvre la fiche du produit
     * retenu dans l'application, sinon sur le site, sinon retombe sur [open]. Ne touche jamais au panier.
     */
    fun openProduct(context: Context, productId: Long, productName: String, itemName: String) {
        val appUri = ChronodriveSearch.productAppUri(productId)
        val webUrl = ChronodriveSearch.productWebUrl(productId)
        if (appUri == null || webUrl == null) { open(context, itemName); return }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Chronodrive", itemName))
        val opened = start(context, Intent(Intent.ACTION_VIEW, Uri.parse(appUri))
                .setPackage(ChronodriveSearch.PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) ||
            start(context, Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (opened) Toast.makeText(context, "Fiche de « $productName » : appuie sur +", Toast.LENGTH_LONG).show()
        else open(context, itemName)
    }

    private fun start(context: Context, intent: Intent): Boolean =
        try { context.startActivity(intent); true } catch (_: ActivityNotFoundException) { false }
}
