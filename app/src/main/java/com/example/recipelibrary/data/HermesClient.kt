package com.example.recipelibrary.data

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Réglages du serveur Hermes, partagés entre l'écran d'import et la file d'attente. */
object HermesPrefs {
    const val NAME = "url_import"
    const val SERVER_URL = "server_url"
    const val FALLBACK_URLS = "fallback_urls"
    /** Adresse MAC du PC, apprise via /health, pour le réveiller quand il est en veille. */
    const val SERVER_MAC = "server_mac"
    const val MAX_FALLBACKS = 5
}

fun normalizeExtractorBaseUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null

    val withScheme = when {
        trimmed.startsWith("http://", ignoreCase = true) -> "http://${trimmed.removePrefix("http://")}"
        trimmed.startsWith("https://", ignoreCase = true) -> "https://${trimmed.removePrefix("https://")}"
        else -> "http://$trimmed"
    }.trimEnd('/')

    return if (withScheme.lowercase().endsWith("/extract")) {
        withScheme.dropLast("/extract".length)
    } else {
        withScheme
    }
}

fun isConnectionError(ex: Throwable?): Boolean =
    ex is java.net.ConnectException ||
        // Android signale un délai de connexion dépassé par une SocketTimeoutException
        // "failed to connect to ..." : le PC n'a jamais été joint, ce n'est pas une extraction lente.
        (ex is java.net.SocketTimeoutException && ex.message?.contains("failed to connect") == true) ||
        ex?.message?.let {
            it.contains("ECONNREFUSED") || it.contains("ETIMEDOUT") ||
                it.contains("ENETUNREACH") || it.contains("Connection refused")
        } == true

fun extractorDisplayAddress(baseUrl: String): String =
    baseUrl.removePrefix("http://").removePrefix("https://").trimEnd('/')

fun resolveStoredServerUrl(prefs: SharedPreferences): String {
    val stored = prefs.getString(HermesPrefs.SERVER_URL, null)?.trim()?.let(::normalizeExtractorBaseUrl)
    // "192.168.0.21" was an incorrect hardcoded default — clear it so the user enters the real IP
    if (stored == "http://192.168.0.21:18642") {
        prefs.edit().remove(HermesPrefs.SERVER_URL).apply()
        return ""
    }
    return stored ?: ""
}

fun loadFallbackUrls(prefs: SharedPreferences): List<String> {
    val json = prefs.getString(HermesPrefs.FALLBACK_URLS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { normalizeExtractorBaseUrl(arr.getString(it)) }
    } catch (e: Exception) { emptyList() }
}

fun saveFallbackUrls(prefs: SharedPreferences, urls: List<String>) {
    val arr = JSONArray(urls.take(HermesPrefs.MAX_FALLBACKS))
    prefs.edit().putString(HermesPrefs.FALLBACK_URLS, arr.toString()).apply()
}

/**
 * Import d'une recette via Hermes : réveil du PC s'il dort, essai des adresses connues,
 * nouvelle recherche sur le réseau si l'IP a changé, puis mémorisation de l'adresse qui marche.
 * Utilisé par l'écran d'import et par la file d'attente en arrière-plan.
 */
class HermesImporter(
    private val prefs: SharedPreferences,
    private val extract: suspend (String, String) -> Result<String> = HermesNetwork::extractRecipe,
    private val discover: suspend () -> String? = HermesNetwork::discover,
    private val isUp: suspend (String) -> Boolean = HermesNetwork::isUp,
    private val wake: suspend (String, String) -> Unit = HermesNetwork::wake,
    private val learnMac: suspend (String) -> String? = HermesNetwork::fetchMac,
    /** Le téléphone est-il sur un Wi-Fi ? Sans Wi-Fi (4G), réveil et recherche du PC sont impossibles. */
    private val onLocalNetwork: suspend () -> Boolean = HermesNetwork::onWifi,
    private val pollMs: Long = WAKE_POLL_MS
) {
    data class Outcome(val result: Result<String>, val triedUrls: List<String>, val successUrl: String?)

    suspend fun run(
        videoUrl: String,
        configuredUrl: String,
        onWaking: suspend (Boolean) -> Unit = {}
    ): Outcome {
        val url = videoUrl.trim()
        val mac = prefs.getString(HermesPrefs.SERVER_MAC, null)
        val local = onLocalNetwork()

        // Le PC ne répond pas mais on connaît son adresse MAC : il est sans doute en veille.
        // On envoie le signal de réveil et on attend que Hermes réponde. Si l'IP du PC a changé,
        // une recherche sur le réseau en cours de route évite d'attendre pour rien sur l'ancienne.
        var rediscovered: String? = null
        if (local && mac != null && !isUp(configuredUrl)) {
            onWaking(true)
            try {
                for (attempt in 1..WAKE_ATTEMPTS) {
                    wake(mac, configuredUrl)
                    if (isUp(configuredUrl)) break
                    if (attempt == WAKE_DISCOVER_AFTER) {
                        val found = discover()
                        if (found != null && found != configuredUrl) {
                            rediscovered = found
                            break
                        }
                    }
                    delay(pollMs)
                }
            } finally {
                onWaking(false)
            }
        }

        // Adresse configurée d'abord (ou celle retrouvée pendant le réveil), puis les anciennes qui ont marché
        val urlsToTry = (listOfNotNull(rediscovered) + configuredUrl + loadFallbackUrls(prefs)).distinct()

        var lastResult: Result<String> = Result.failure(Exception("Aucune adresse disponible"))
        var successUrl: String? = null

        for (candidateUrl in urlsToTry) {
            val result = extract(url, candidateUrl)
            lastResult = result
            if (result.isSuccess) {
                successUrl = candidateUrl
                break
            }
            // Only try next IP on connection errors; stop on extraction errors
            if (!isConnectionError(result.exceptionOrNull())) break
        }

        // Every known address failed to connect: the PC's IP probably changed.
        // Rescan the local network and retry once with the address found.
        if (local && successUrl == null && isConnectionError(lastResult.exceptionOrNull())) {
            val found = discover()
            if (found != null && found !in urlsToTry) {
                val result = extract(url, found)
                lastResult = result
                if (result.isSuccess) successUrl = found
            }
        }

        if (successUrl != null) {
            // If another URL worked, promote it and keep the old one as fallback
            if (successUrl != configuredUrl) {
                val oldFallbacks = loadFallbackUrls(prefs).filter { it != successUrl }
                saveFallbackUrls(prefs, listOf(configuredUrl) + oldFallbacks)
                prefs.edit().putString(HermesPrefs.SERVER_URL, successUrl).apply()
            }
            // Remember the PC's MAC address to be able to wake it up next time
            val learned = runCatching { learnMac(successUrl) }.getOrNull()
            if (learned != null) prefs.edit().putString(HermesPrefs.SERVER_MAC, learned).apply()
        }

        return Outcome(lastResult, urlsToTry, successUrl)
    }

    companion object {
        // Réveil : jusqu'à 15 essais (signal + test du serveur), soit environ 50 s au maximum
        const val WAKE_ATTEMPTS = 15
        const val WAKE_DISCOVER_AFTER = 5
        const val WAKE_POLL_MS = 2000L
    }
}

/** Appels réseau réels vers le serveur Hermes. */
object HermesNetwork {

    suspend fun extractRecipe(videoUrl: String, extractorBaseUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedBaseUrl = normalizeExtractorBaseUrl(extractorBaseUrl)
                    ?: error("Adresse Hermes invalide.")
                val endpoint = "${normalizedBaseUrl.trimEnd('/')}/extract"

                Log.d("UrlImport", "Connexion a $endpoint")
                val conn = LanNetwork.open(URL(endpoint)) as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                // Sur le réseau local, le PC répond en moins d'une seconde : 5 s suffisent largement
                conn.connectTimeout = 5_000
                conn.readTimeout = 180_000
                conn.doOutput = true
                try {
                    conn.connect()
                } catch (e: java.net.SocketTimeoutException) {
                    // Délai de connexion dépassé = PC injoignable (pas une extraction trop lente)
                    throw java.net.ConnectException("ETIMEDOUT (connexion impossible au PC) ${e.message}")
                }

                val escaped = videoUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                conn.outputStream.bufferedWriter().use { it.write("""{"url":"$escaped"}""") }

                Log.d("UrlImport", "Requete envoyee, attente reponse...")
                val code = conn.responseCode
                Log.d("UrlImport", "HTTP $code recu")

                val body = runCatching {
                    (if (code == 200) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()
                        ?.readText()
                        ?: ""
                }.getOrDefault("")

                Log.d("UrlImport", "Body (${body.length} chars): ${body.take(200)}")
                if (code != 200) {
                    val msg = runCatching { JSONObject(body).getString("error") }
                        .getOrDefault("Serveur inaccessible (HTTP $code).")
                    Log.e("UrlImport", "Erreur serveur: $msg")
                    error(msg)
                }

                val parsed = runCatching { JSONObject(body) }.getOrNull()
                val serverError = parsed?.optString("error")?.takeIf { it.isNotBlank() }
                if (serverError != null) {
                    Log.e("UrlImport", "Erreur extraction: $serverError")
                    error(serverError)
                }

                Log.d("UrlImport", "Succes")
                body
            }.onFailure { e ->
                Log.e("UrlImport", "Exception: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

    private fun openHealth(baseUrl: String): HttpURLConnection {
        val conn = LanNetwork.open(URL("${baseUrl.trimEnd('/')}/health")) as HttpURLConnection
        conn.connectTimeout = 1500
        conn.readTimeout = 1500
        conn.requestMethod = "GET"
        return conn
    }

    suspend fun isUp(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { openHealth(baseUrl).responseCode == 200 }.getOrDefault(false)
    }

    /** Lit l'adresse MAC que Hermes annonce dans /health (absente sur les anciennes versions du serveur). */
    suspend fun fetchMac(baseUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = openHealth(baseUrl)
            if (conn.responseCode != 200) return@runCatching null
            val mac = JSONObject(conn.inputStream.bufferedReader().readText()).optString("mac")
            mac.takeIf { WakeOnLan.parseMac(it) != null }
        }.getOrNull()
    }

    suspend fun wake(mac: String, baseUrl: String) {
        withContext(Dispatchers.IO) {
            val host = runCatching { URL(baseUrl).host }.getOrNull() ?: return@withContext
            val sent = WakeOnLan.send(mac, host, bind = LanNetwork::bind)
            Log.d("UrlImport", "Signal de reveil vers $host : ${if (sent) "envoye" else "echec"}")
        }
    }

    /** Vrai si le téléphone a une adresse IPv4 sur une interface Wi-Fi ou Ethernet. */
    suspend fun onWifi(): Boolean = withContext(Dispatchers.IO) {
        // Dans l'app : le système dit directement s'il y a un Wi-Fi connecté
        if (LanNetwork.isAvailable()) return@withContext LanNetwork.wifi() != null
        runCatching {
            java.net.NetworkInterface.getNetworkInterfaces()?.asSequence().orEmpty()
                .filter { it.isUp && !it.isLoopback && (it.name.startsWith("wlan") || it.name.startsWith("eth")) }
                .any { iface -> iface.inetAddresses.asSequence().any { it is java.net.Inet4Address } }
        }.getOrDefault(true)
    }

    private fun getLocalSubnet(): String? = try {
        java.net.NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.filter { iface -> !iface.isLoopback && iface.isUp }
            ?.filter { iface -> iface.name.startsWith("wlan") || iface.name.startsWith("eth") || iface.name.startsWith("en") || iface.name.startsWith("rmnet") }
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.filterIsInstance<java.net.Inet4Address>()
            ?.firstOrNull()
            ?.hostAddress
            ?.substringBeforeLast(".")
    } catch (_: Exception) { null }

    suspend fun discover(): String? = withContext(Dispatchers.IO) {
        val subnet = getLocalSubnet() ?: return@withContext null
        Log.d("UrlImport", "Scan reseau sur $subnet.1-254:18642")
        coroutineScope {
            (1..254).map { i ->
                async {
                    val ip = "$subnet.$i"
                    try {
                        val conn = LanNetwork.open(URL("http://$ip:18642/health")) as HttpURLConnection
                        conn.connectTimeout = 300
                        conn.readTimeout = 300
                        conn.requestMethod = "GET"
                        if (conn.responseCode == 200) "http://$ip:18642" else null
                    } catch (_: Exception) { null }
                }
            }.awaitAll().firstOrNull { it != null }
        }
    }
}
