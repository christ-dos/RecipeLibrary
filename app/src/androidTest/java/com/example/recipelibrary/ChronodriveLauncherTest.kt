package com.example.recipelibrary

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.ui.ChronodriveLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Ouverture de la fiche produit (spec 003, T026, contracts/chrono-link.md) : intents enregistrés, jamais lancés pour de vrai. */
@RunWith(AndroidJUnit4::class)
class ChronodriveLauncherTest {

    /** Enregistre les intents ; lève ActivityNotFoundException pour les [failFirst] premiers. */
    private class RecordingContext(base: Context, private val failFirst: Int) : ContextWrapper(base) {
        val intents = mutableListOf<Intent>()
        override fun startActivity(intent: Intent) {
            intents += intent
            if (intents.size <= failFirst) throw ActivityNotFoundException("test")
        }
    }

    /** Exécute sur le thread principal (presse-papiers et Toast), contexte basé sur le targetContext. */
    private fun launch(failFirst: Int, block: (Context) -> Unit): RecordingContext {
        val inst = InstrumentationRegistry.getInstrumentation()
        val ctx = RecordingContext(inst.targetContext, failFirst)
        inst.runOnMainSync { block(ctx) }
        return ctx
    }

    private fun assertNoCartOrAccount(intents: List<Intent>) {
        val forbidden = listOf("cart", "panier", "basket", "account", "compte", "login", "checkout")
        intents.forEach {
            val s = it.dataString.orEmpty().lowercase()
            assertFalse("intent vers panier/compte : $s", forbidden.any { w -> w in s })
        }
    }

    @Test fun openProduct_appAvailable_launchesAppLinkOnly() {
        val ctx = launch(failFirst = 0) { ChronodriveLauncher.openProduct(it, 365920, "banane", "banane") }
        val i = ctx.intents.single()
        assertEquals(Intent.ACTION_VIEW, i.action)
        assertEquals("chronodrive://product/365920", i.dataString)
        assertEquals("com.ikomobi.chronodrive", i.`package`)
        assertNoCartOrAccount(ctx.intents)
    }

    @Test fun openProduct_appRefuses_fallsBackToWebProductPage() {
        val ctx = launch(failFirst = 1) { ChronodriveLauncher.openProduct(it, 365920, "banane", "banane") }
        assertEquals(2, ctx.intents.size)
        assertEquals("chronodrive://product/365920", ctx.intents[0].dataString)
        assertEquals("com.ikomobi.chronodrive", ctx.intents[0].`package`)
        assertEquals("https://www.chronodrive.com/product/365920", ctx.intents[1].dataString)
        assertNull(ctx.intents[1].`package`)
        assertTrue(ctx.intents[1].flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertNoCartOrAccount(ctx.intents)
    }

    @Test fun openProduct_nothingWorks_fallsBackToFeature002_withoutCartIntent() {
        val ctx = launch(failFirst = 100) { ChronodriveLauncher.openProduct(it, 305254, "banane auchan", "banane") }
        // Fiche appli, fiche web, puis repli de la feature 002 : aucun ne vise un panier ou un compte
        assertTrue(ctx.intents.size >= 3)
        assertEquals("chronodrive://product/305254", ctx.intents[0].dataString)
        assertEquals("https://www.chronodrive.com/product/305254", ctx.intents[1].dataString)
        assertNoCartOrAccount(ctx.intents)
    }

    @Test fun openProduct_invalidId_usesFeature002Behaviour() {
        val ctx = launch(failFirst = 0) { ChronodriveLauncher.openProduct(it, 0, "x", "banane") }
        assertTrue(ctx.intents.isNotEmpty())
        assertTrue(ctx.intents.none { it.dataString.orEmpty().contains("/product/") })
        assertNoCartOrAccount(ctx.intents)
    }
}
