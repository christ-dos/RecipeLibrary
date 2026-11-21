package com.example.recipelibrary

import android.content.SharedPreferences
import com.example.recipelibrary.data.HermesImporter
import com.example.recipelibrary.data.HermesPrefs
import com.example.recipelibrary.data.isConnectionError
import com.example.recipelibrary.data.loadFallbackUrls
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

/** SharedPreferences en mémoire, suffisantes pour l'importeur. */
private class FakePrefs : SharedPreferences {
    val values = mutableMapOf<String, Any?>()

    override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue
    override fun contains(key: String) = key in values
    override fun getAll(): MutableMap<String, *> = values
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
    override fun getInt(key: String, defValue: Int) = defValue
    override fun getLong(key: String, defValue: Long) = defValue
    override fun getFloat(key: String, defValue: Float) = defValue
    override fun getBoolean(key: String, defValue: Boolean) = defValue
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?) = apply { values[key] = value }
        override fun remove(key: String) = apply { values.remove(key) }
        override fun putStringSet(key: String, values: MutableSet<String>?) = this
        override fun putInt(key: String, value: Int) = this
        override fun putLong(key: String, value: Long) = this
        override fun putFloat(key: String, value: Float) = this
        override fun putBoolean(key: String, value: Boolean) = this
        override fun clear() = apply { values.clear() }
        override fun commit() = true
        override fun apply() {}
    }
}

class HermesImporterTest {

    private val home = "http://192.168.1.44:18642"
    private val newIp = "http://192.168.1.60:18642"
    private val mac = "04:33:C2:B9:44:3E"
    private val recipeJson = """{"title":"Tarte"}"""
    private val refused = ConnectException("ECONNREFUSED (Connection refused)")

    @Test
    fun awakePc_noWakeSignal_importsAndLearnsMac() = runBlocking {
        val prefs = FakePrefs()
        var wakes = 0
        val importer = HermesImporter(
            prefs,
            extract = { _, _ -> Result.success(recipeJson) },
            discover = { null },
            isUp = { true },
            wake = { _, _ -> wakes++ },
            learnMac = { mac },
            onLocalNetwork = { true },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home)
        assertEquals(recipeJson, outcome.result.getOrNull())
        assertEquals(home, outcome.successUrl)
        assertEquals(0, wakes)
        assertEquals(mac, prefs.values[HermesPrefs.SERVER_MAC])
    }

    @Test
    fun sleepingPc_knownMac_wakesThenImports() = runBlocking {
        val prefs = FakePrefs().apply { values[HermesPrefs.SERVER_MAC] = mac }
        var awake = false
        val wakeCalls = mutableListOf<Pair<String, String>>()
        val waking = mutableListOf<Boolean>()
        val importer = HermesImporter(
            prefs,
            extract = { _, _ -> if (awake) Result.success(recipeJson) else Result.failure(refused) },
            discover = { null },
            isUp = { awake },
            wake = { m, s -> wakeCalls += m to s; if (wakeCalls.size == 3) awake = true },
            learnMac = { null },
            onLocalNetwork = { true },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home) { waking += it }
        assertTrue(outcome.result.isSuccess)
        assertEquals(3, wakeCalls.size)
        assertEquals(mac to home, wakeCalls.first())
        assertEquals(listOf(true, false), waking)
    }

    @Test
    fun sleepingPc_ipChanged_rediscoversDuringWake() = runBlocking {
        val prefs = FakePrefs().apply { values[HermesPrefs.SERVER_MAC] = mac }
        val tried = mutableListOf<String>()
        var wakes = 0
        val importer = HermesImporter(
            prefs,
            extract = { _, s -> tried += s; if (s == newIp) Result.success(recipeJson) else Result.failure(refused) },
            discover = { newIp },
            isUp = { it == newIp },
            wake = { _, _ -> wakes++ },
            learnMac = { mac },
            onLocalNetwork = { true },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home)
        assertTrue(outcome.result.isSuccess)
        // Recherche lancée après 5 réveils sans réponse, sans attendre les 15 essais
        assertEquals(HermesImporter.WAKE_DISCOVER_AFTER, wakes)
        // La nouvelle adresse est essayée en premier et devient l'adresse du serveur
        assertEquals(listOf(newIp), tried)
        assertEquals(newIp, prefs.values[HermesPrefs.SERVER_URL])
        assertEquals(listOf(home), loadFallbackUrls(prefs))
    }

    @Test
    fun unknownMac_neverWakes() = runBlocking {
        val prefs = FakePrefs()
        var probes = 0
        val importer = HermesImporter(
            prefs,
            extract = { _, _ -> Result.failure(refused) },
            discover = { null },
            isUp = { probes++; false },
            wake = { _, _ -> probes++ },
            learnMac = { null },
            onLocalNetwork = { true },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home)
        assertTrue(outcome.result.exceptionOrNull() is ConnectException)
        assertEquals(0, probes)
        assertNull(outcome.successUrl)
    }

    @Test
    fun pcNeverWakes_givesUpAfterMaxAttempts() = runBlocking {
        val prefs = FakePrefs().apply { values[HermesPrefs.SERVER_MAC] = mac }
        var wakes = 0
        val importer = HermesImporter(
            prefs,
            extract = { _, _ -> Result.failure(refused) },
            discover = { null },
            isUp = { false },
            wake = { _, _ -> wakes++ },
            learnMac = { null },
            onLocalNetwork = { true },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home)
        assertEquals(HermesImporter.WAKE_ATTEMPTS, wakes)
        assertTrue(outcome.result.exceptionOrNull() is ConnectException)
    }

    @Test
    fun mobileData_noWakeNoScan_failsFastAsConnectionError() = runBlocking {
        // Cas réel du 15/11 : téléphone en 4G, le PC n'est pas joignable
        val prefs = FakePrefs().apply { values[HermesPrefs.SERVER_MAC] = mac }
        var probes = 0
        var extracts = 0
        val importer = HermesImporter(
            prefs,
            extract = { _, _ -> extracts++; Result.failure(ConnectException("ETIMEDOUT (connexion impossible au PC)")) },
            discover = { probes++; null },
            isUp = { probes++; false },
            wake = { _, _ -> probes++ },
            learnMac = { null },
            onLocalNetwork = { false },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home)
        assertEquals("ni réveil, ni test, ni scan hors Wi-Fi", 0, probes)
        assertEquals(1, extracts)
        assertTrue(isConnectionError(outcome.result.exceptionOrNull()))
    }

    @Test
    fun connectTimeout_isAConnectionError_butReadTimeoutIsNot() {
        assertTrue(isConnectionError(SocketTimeoutException(
            "failed to connect to /192.168.1.44 (port 18642) from /192.0.0.2 (port 42696) after 15000ms"
        )))
        assertFalse(isConnectionError(SocketTimeoutException("Read timed out")))
        assertTrue(isConnectionError(ConnectException("ECONNREFUSED (Connection refused)")))
        assertFalse(isConnectionError(Exception("Pas de recette dans cette vidéo")))
    }

    @Test
    fun extractionError_doesNotTryOtherAddresses() = runBlocking {
        val prefs = FakePrefs()
        val tried = mutableListOf<String>()
        prefs.values[HermesPrefs.FALLBACK_URLS] = """["$newIp"]"""
        val importer = HermesImporter(
            prefs,
            extract = { _, s -> tried += s; Result.failure(Exception("Pas de recette dans cette vidéo")) },
            discover = { error("ne doit pas scanner") },
            isUp = { true },
            wake = { _, _ -> },
            learnMac = { null },
            onLocalNetwork = { true },
            pollMs = 0
        )
        val outcome = importer.run("https://insta/reel/1", home)
        assertEquals("Pas de recette dans cette vidéo", outcome.result.exceptionOrNull()?.message)
        assertEquals(listOf(home), tried)
    }
}
