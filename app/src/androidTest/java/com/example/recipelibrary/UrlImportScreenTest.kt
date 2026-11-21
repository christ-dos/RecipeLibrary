package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.ui.UrlImportScreen
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ConnectException
import java.net.SocketTimeoutException

@RunWith(AndroidJUnit4::class)
class UrlImportScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val validUrl = "https://youtu.be/test123"

    private val prefs
        get() = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("url_import", Context.MODE_PRIVATE)

    // Sans adresse MAC memorisee, l'ecran ne tente aucun reveil : les tests qui n'en parlent pas
    // gardent exactement le comportement d'avant (aucun appel reseau reel).
    @Before
    @After
    fun clearLearnedMac() {
        prefs.edit().remove("server_mac").commit()
    }

    private fun urlField() = rule.onNode(hasText("URL") and hasSetTextAction())

    private fun serverField() = rule.onNode(hasText("Serveur Hermes") and hasSetTextAction())

    private fun setUrl(url: String = validUrl) {
        urlField().performTextReplacement(url)
    }

    private fun setServer(address: String = "192.168.1.36:18642") {
        serverField().performTextReplacement(address)
    }

    @Test
    fun urlImport_importButton_disabledWithoutUrl() {
        rule.setContent {
            MaterialTheme { UrlImportScreen(onImport = {}, onBack = {}, parseJson = { null }) }
        }
        setServer()
        rule.onNodeWithText("Importer").assertIsNotEnabled()
    }

    @Test
    fun urlImport_importButton_enabledWithValidUrl() {
        rule.setContent {
            MaterialTheme { UrlImportScreen(onImport = {}, onBack = {}, parseJson = { null }) }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").assertIsEnabled()
    }

    @Test
    fun urlImport_showsServerAddress_inHint() {
        rule.setContent {
            MaterialTheme { UrlImportScreen(onImport = {}, onBack = {}, parseJson = { null }) }
        }
        // Le texte d'aide sous le champ (Text normal, visible dans l'arbre semantique)
        rule.onAllNodesWithText("192.168.1.X:18642", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_connectRefused_showsHermesNotStartedMessage() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> Result.failure(ConnectException("ECONNREFUSED (Connection refused)")) }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Hermes n'est pas demarre", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Hermes n'est pas demarre", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("hermes.py", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_networkTimeout_showsPcUnreachableMessage() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> Result.failure(ConnectException("ETIMEDOUT (Connection timed out)")) }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("PC injoignable", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("PC injoignable", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("WiFi du PC", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_socketTimeout_showsTimeoutMessage() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> Result.failure(SocketTimeoutException("Read timed out")) }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Hermes ne repond pas", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Hermes ne repond pas", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_success_callsOnImportWithParsedRecipe() {
        var imported: Recipe? = null
        val fakeRecipe = Recipe(
            id = "r1",
            title = "Gateau test",
            category = "Desserts",
            ingredients = listOf("farine"),
            steps = listOf("cuire")
        )
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = { fakeRecipe },
                    extract = { _, _ -> Result.success("""{"title":"Gateau test"}""") }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assert(imported?.title == "Gateau test")
    }

    @Test
    fun urlImport_nullTitle_importsWithDefaultTitle() {
        var imported: Recipe? = null
        val rawJson = """{"title": null, "ingredients": ["100g de farine"], "steps": ["Melanger"]}"""
        val expectedTitle = parseRecipeJson(rawJson)?.title
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ -> Result.success(rawJson) }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals(expectedTitle, imported?.title)
    }

    @Test
    fun urlImport_serverNoSubtitlesError_showsErrorMessage() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ ->
                        Result.failure(Exception("Pas de sous-titres et description insuffisante pour cette video."))
                    }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("sous-titres", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("sous-titres", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_unparseableResponse_showsParseError() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> Result.success("""{"no_recipe": true}""") }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Impossible de parser", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Impossible de parser", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_instagramFallback_fullRecipeImported() {
        var imported: Recipe? = null
        val json = """{"title":"Biscuits cacahuete","ingredients":["100g flocons d'avoine","90g pois chiche","60g sucre"],"steps":["Melanger tous les ingredients","Former des boules","Cuire 12 min a 180"],"sourceUrl":"https://www.instagram.com/reel/test123/"}"""
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ -> Result.success(json) }
                )
            }
        }
        setUrl("https://www.instagram.com/reel/test123/")
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals("Biscuits cacahuete", imported?.title)
        assert((imported?.ingredients?.size ?: 0) >= 3)
        assert((imported?.steps?.size ?: 0) >= 3)
    }

    @Test
    fun urlImport_serverIpChanged_rediscoversAndImports() {
        var imported: Recipe? = null
        val newServer = "http://192.168.1.99:18642"
        val json = """{"title":"Tarte","ingredients":["200g farine"],"steps":["Cuire"]}"""
        val triedServers = mutableListOf<String>()
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, server ->
                        triedServers += server
                        if (server == newServer) Result.success(json)
                        else Result.failure(ConnectException("ECONNREFUSED (Connection refused)"))
                    },
                    discover = { newServer },
                    onLocalNetwork = { true } // la recherche du PC n'a lieu que sur Wi-Fi
                )
            }
        }
        setUrl()
        setServer("192.168.1.36:18642") // ancienne IP
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals("Tarte", imported?.title)
        assertEquals(newServer, triedServers.last())
    }

    // --- Tests reveil du PC en veille (Wake-on-LAN) ---

    @Test
    fun urlImport_pcAsleep_sendsWakeSignalThenImports() {
        prefs.edit().putString("server_mac", "04:33:C2:B9:44:3E").commit()
        var imported: Recipe? = null
        var awake = false
        val wakeCalls = mutableListOf<Pair<String, String>>()
        val json = """{"title":"Tarte","ingredients":["200g farine"],"steps":["Cuire"]}"""
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ ->
                        if (awake) Result.success(json)
                        else Result.failure(ConnectException("ETIMEDOUT (Connection timed out)"))
                    },
                    isUp = { awake },
                    wake = { mac, server -> wakeCalls += mac to server; awake = true },
                    learnMac = { null },
                    onLocalNetwork = { true }
                )
            }
        }
        setUrl()
        setServer("192.168.1.44:18642")
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals("Tarte", imported?.title)
        assertEquals(listOf("04:33:C2:B9:44:3E" to "http://192.168.1.44:18642"), wakeCalls)
    }

    @Test
    fun urlImport_pcAlreadyAwake_doesNotSendWakeSignal() {
        prefs.edit().putString("server_mac", "04:33:C2:B9:44:3E").commit()
        var imported: Recipe? = null
        var wakeCalls = 0
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ -> Result.success("""{"title":"Tarte","ingredients":["a"],"steps":["b"]}""") },
                    isUp = { true },
                    wake = { _, _ -> wakeCalls++ },
                    learnMac = { null },
                    onLocalNetwork = { true }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals(0, wakeCalls)
    }

    @Test
    fun urlImport_unknownMac_neverTriesToWake() {
        var done = false
        var probes = 0
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> done = true; Result.failure(ConnectException("ETIMEDOUT (Connection timed out)")) },
                    discover = { null },
                    isUp = { probes++; false },
                    wake = { _, _ -> probes++ }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { done }
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("PC injoignable", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, probes)
    }

    @Test
    fun urlImport_successfulImport_remembersServerMac() {
        var imported: Recipe? = null
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ -> Result.success("""{"title":"Tarte","ingredients":["a"],"steps":["b"]}""") },
                    learnMac = { "04:33:C2:B9:44:3E" }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        rule.waitUntil(timeoutMillis = 5000) { prefs.getString("server_mac", null) != null }
        assertEquals("04:33:C2:B9:44:3E", prefs.getString("server_mac", null))
    }

    // --- Tests file d'attente ---

    @Test
    fun urlImport_pcUnreachable_withQueue_queuesLinkAndShowsMessage() {
        var queued: String? = null
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> Result.failure(ConnectException("ETIMEDOUT (Connection timed out)")) },
                    discover = { null },
                    onQueue = { queued = it }
                )
            }
        }
        setUrl("https://www.instagram.com/reel/abc/")
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { queued != null }
        assertEquals("https://www.instagram.com/reel/abc/", queued)
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Lien mis en file d'attente", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("PC injoignable (", substring = true).assertCountEquals(0)
    }

    @Test
    fun urlImport_mobileDataConnectTimeout_withQueue_queuesInsteadOfSlowExtractionMessage() {
        var queued: String? = null
        var wakes = 0
        prefs.edit().putString("server_mac", "04:33:C2:B9:44:3E").commit()
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ ->
                        Result.failure(SocketTimeoutException("failed to connect to /192.168.1.44 (port 18642) after 15000ms"))
                    },
                    discover = { null },
                    isUp = { false },
                    wake = { _, _ -> wakes++ },
                    learnMac = { null },
                    onLocalNetwork = { false },
                    onQueue = { queued = it }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { queued != null }
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Lien mis en file d'attente", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Hermes ne repond pas", substring = true).assertCountEquals(0)
        assertEquals(0, wakes)
    }

    @Test
    fun urlImport_extractionError_withQueue_isNotQueued() {
        var queued: String? = null
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ -> Result.failure(Exception("Pas de recette dans cette video")) },
                    onQueue = { queued = it }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Pas de recette", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(null, queued)
    }

    @Test
    fun urlImport_nonEmptyQueue_showsLinkThatOpensQueueScreen() {
        var openedQueue = false
        val pending = listOf(
            com.example.recipelibrary.data.PendingImport(id = 1, url = "https://insta/reel/1", status = "READY", recipeJson = "{}"),
            com.example.recipelibrary.data.PendingImport(id = 2, url = "https://insta/reel/2")
        )
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    pending = pending,
                    onOpenQueue = { openedQueue = true }
                )
            }
        }
        rule.onNodeWithText("Voir la file d'attente (1 à valider, 1 en attente)")
            .performScrollTo().assertIsDisplayed().performClick()
        assertEquals(true, openedQueue)
    }

    @Test
    fun urlImport_emptyQueue_showsNoQueueLink() {
        rule.setContent {
            MaterialTheme { UrlImportScreen(onImport = {}, onBack = {}, parseJson = { null }) }
        }
        rule.onAllNodesWithText("Voir la file d'attente", substring = true).assertCountEquals(0)
    }

    @Test
    fun urlImport_instagramBlocked_showsTextImportSuggestion() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ ->
                        Result.failure(
                            Exception(
                                "Instagram bloque l'acces sans connexion.\n\n" +
                                    "Copie le texte de la recette depuis Instagram sur ton telephone, " +
                                    "puis utilise Importer depuis texte dans l'app."
                            )
                        )
                    }
                )
            }
        }
        setUrl("https://www.instagram.com/reel/blocked/")
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Importer depuis texte", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Importer depuis texte", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_youtubeNoSubtitles_fallbackDescriptionWorks() {
        var imported: Recipe? = null
        val json = """{"title":"Mousse au chocolat","ingredients":["200g chocolat noir","4 oeufs","50g sucre"],"steps":["Faire fondre le chocolat","Separer les blancs des jaunes","Monter les blancs en neige","Incorporer delicatement"],"sourceUrl":"https://youtu.be/abc123"}"""
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ -> Result.success(json) }
                )
            }
        }
        setUrl("https://youtu.be/abc123")
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals("Mousse au chocolat", imported?.title)
        assert((imported?.ingredients?.size ?: 0) >= 3)
        assert((imported?.steps?.size ?: 0) >= 3)
    }

    @Test
    fun urlImport_sourceUrl_preservedInImportedRecipe() {
        var imported: Recipe? = null
        val testUrl = "https://www.instagram.com/reel/DaPiaafj7Tn/"
        val json = """{"title":"Tarte aux pommes","ingredients":["3 pommes","200g farine"],"steps":["Peler les pommes","Etaler la pate"],"sourceUrl":"$testUrl"}"""
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = { imported = it },
                    onBack = {},
                    parseJson = ::parseRecipeJson,
                    extract = { _, _ -> Result.success(json) }
                )
            }
        }
        setUrl(testUrl)
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { imported != null }
        assertEquals(testUrl, imported?.sourceUrl)
    }

    @Test
    fun urlImport_loading_showsProgressIndicator() {
        val latch = java.util.concurrent.CountDownLatch(1)
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ ->
                        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                        Result.failure(Exception("annule"))
                    }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.onNodeWithText("Extraction en cours", substring = true).assertIsDisplayed()
        latch.countDown()
    }

    @Test
    fun urlImport_customServerAddress_isUsedForExtraction() {
        var receivedServerUrl: String? = null
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, serverUrl ->
                        receivedServerUrl = serverUrl
                        Result.success("""{"no_recipe": true}""")
                    }
                )
            }
        }
        setUrl()
        serverField().performTextReplacement("10.0.0.12:18642")
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) { receivedServerUrl != null }
        assertEquals("http://10.0.0.12:18642", receivedServerUrl)
        rule.onAllNodesWithText("10.0.0.12:18642", substring = true).onFirst().assertIsDisplayed()
    }

    // --- Tests bouton Detecter ---

    @Test
    fun urlImport_detectButton_isDisplayed() {
        rule.setContent {
            MaterialTheme { UrlImportScreen(onImport = {}, onBack = {}, parseJson = { null }) }
        }
        rule.onNodeWithText("Detecter").assertIsDisplayed()
    }

    @Test
    fun urlImport_detectButton_fillsServerField() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    discover = { "http://192.168.99.42:18642" }
                )
            }
        }
        rule.onNodeWithText("Detecter").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("192.168.99.42", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("192.168.99.42", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_detectButton_showsErrorWhenNotFound() {
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    discover = { null }
                )
            }
        }
        rule.onNodeWithText("Detecter").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Aucun serveur Hermes", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Aucun serveur Hermes", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun urlImport_detectButton_disabledDuringExtraction() {
        val latch = java.util.concurrent.CountDownLatch(1)
        rule.setContent {
            MaterialTheme {
                UrlImportScreen(
                    onImport = {},
                    onBack = {},
                    parseJson = { null },
                    extract = { _, _ ->
                        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                        Result.failure(Exception("annule"))
                    }
                )
            }
        }
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.onNodeWithText("Detecter").assertIsNotEnabled()
        latch.countDown()
    }
    // ── Extraction impossible : copier le lien pour la faire à la main ──────────

    private class FakeClipboard : androidx.compose.ui.platform.ClipboardManager {
        var copied: androidx.compose.ui.text.AnnotatedString? = null
        override fun getText() = copied
        override fun setText(annotatedString: androidx.compose.ui.text.AnnotatedString) { copied = annotatedString }
    }

    @Test
    fun urlImport_extractionFails_copyLinkButtonCopiesUrl() {
        val clipboard = FakeClipboard()
        rule.setContent {
            MaterialTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalClipboardManager provides clipboard
                ) {
                    UrlImportScreen(
                        onImport = {}, onBack = {}, parseJson = { null },
                        extract = { _, _ -> Result.failure(Exception("Pas de recette dans cette video.")) }
                    )
                }
            }
        }
        rule.onAllNodesWithText("Copier le lien", substring = true).assertCountEquals(0)
        setUrl()
        setServer()
        rule.onNodeWithText("Importer").performClick()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Copier le lien", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Copier le lien", substring = true).performScrollTo().performClick()
        assertEquals(validUrl, clipboard.copied?.text)
    }
}
