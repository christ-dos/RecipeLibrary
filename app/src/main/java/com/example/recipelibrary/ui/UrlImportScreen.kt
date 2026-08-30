package com.example.recipelibrary.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.R
import com.example.recipelibrary.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val PREFS_NAME = "url_import"
private const val PREF_SERVER_URL = "server_url"
private const val PREF_FALLBACK_URLS = "fallback_urls"
private const val MAX_FALLBACKS = 5

private fun normalizeExtractorBaseUrl(raw: String): String? {
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

private fun extractorEndpoint(baseUrl: String): String = "${baseUrl.trimEnd('/')}/extract"

private fun extractorDisplayAddress(baseUrl: String): String =
    baseUrl.removePrefix("http://").removePrefix("https://").trimEnd('/')

private fun resolveStoredServerUrl(prefs: android.content.SharedPreferences): String {
    val stored = prefs.getString(PREF_SERVER_URL, null)?.trim()?.let(::normalizeExtractorBaseUrl)
    // "192.168.0.21" was an incorrect hardcoded default — clear it so the user enters the real IP
    if (stored == "http://192.168.0.21:18642") {
        prefs.edit().remove(PREF_SERVER_URL).apply()
        return ""
    }
    return stored ?: ""
}

private fun loadFallbackUrls(prefs: android.content.SharedPreferences): List<String> {
    val json = prefs.getString(PREF_FALLBACK_URLS, null) ?: return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).mapNotNull { normalizeExtractorBaseUrl(arr.getString(it)) }
    } catch (e: Exception) { emptyList() }
}

private fun saveFallbackUrls(prefs: android.content.SharedPreferences, urls: List<String>) {
    val arr = org.json.JSONArray(urls.take(MAX_FALLBACKS))
    prefs.edit().putString(PREF_FALLBACK_URLS, arr.toString()).apply()
}

@Composable
fun UrlImportScreen(
    onImport: (Recipe) -> Unit,
    onBack: () -> Unit,
    parseJson: (String) -> Recipe?,
    extract: suspend (String, String) -> Result<String> = ::extractRecipe,
    discover: suspend () -> String? = ::discoverHermesServer
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("") }
    var serverUrlInput by rememberSaveable {
        mutableStateOf(resolveStoredServerUrl(prefs))
    }
    var loading by remember { mutableStateOf(false) }
    var detecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detectError by remember { mutableStateOf<String?>(null) }

    val normalizedServerUrl = remember(serverUrlInput) { normalizeExtractorBaseUrl(serverUrlInput) }
    val serverAddress = normalizedServerUrl?.let(::extractorDisplayAddress) ?: "adresse invalide"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_nav_url),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Text(
            "Importer depuis une URL",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A16C6)
        )
        Text(
            "Collez le lien d'une video ou d'une page pour extraire la recette automatiquement.",
            fontSize = 14.sp,
            color = Color(0xFF625B70),
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                label = { Text("URL") },
                placeholder = { Text("https://www.instagram.com/reel/...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !loading,
                shape = RoundedCornerShape(12.dp)
            )
            IconButton(
                onClick = {
                    val clip = clipboard.getText()?.text?.trim() ?: ""
                    if (clip.startsWith("http")) {
                        url = clip
                        error = null
                    }
                },
                enabled = !loading
            ) {
                Text("Coller", fontSize = 12.sp)
            }
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = {
                    serverUrlInput = it
                    prefs.edit().putString(PREF_SERVER_URL, it).apply()
                    error = null
                    detectError = null
                },
                label = { Text("Serveur Hermes") },
                placeholder = { Text("192.168.X.X:18642") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !loading && !detecting,
                shape = RoundedCornerShape(12.dp),
                isError = normalizedServerUrl == null && serverUrlInput.isNotBlank()
            )
            Button(
                onClick = {
                    detectError = null
                    detecting = true
                    scope.launch {
                        val found = withContext(Dispatchers.IO) { discover() }
                        detecting = false
                        if (found != null) {
                            serverUrlInput = found
                            prefs.edit().putString(PREF_SERVER_URL, found).apply()
                        } else {
                            detectError = "Aucun serveur Hermes trouve. Verifie que le PC est allume et sur le meme WiFi."
                        }
                    }
                },
                enabled = !loading && !detecting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6750A4)),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (detecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Detecter", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (detectError != null) {
            Card(
                backgroundColor = Color(0xFFFFEEEE),
                elevation = 0.dp,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    detectError!!,
                    modifier = Modifier.padding(10.dp),
                    color = Color(0xFFCC0000),
                    fontSize = 13.sp
                )
            }
        }

        Text(
            "Adresse IP du PC (ipconfig dans cmd). Exemple: 192.168.1.X:18642",
            fontSize = 11.sp,
            color = Color(0xFF625B70),
            textAlign = TextAlign.Center
        )

        if (error != null) {
            Card(
                backgroundColor = Color(0xFFFFEEEE),
                elevation = 0.dp,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    error!!,
                    modifier = Modifier.padding(10.dp),
                    color = Color(0xFFCC0000),
                    fontSize = 13.sp
                )
            }
        }

        Button(
            onClick = {
                val activeServerUrl = normalizedServerUrl
                if (activeServerUrl == null) {
                    error = "Adresse Hermes invalide."
                    return@Button
                }

                // Build list: configured URL first, then previously-successful fallbacks
                val urlsToTry = listOf(activeServerUrl) +
                    loadFallbackUrls(prefs).filter { it != activeServerUrl }

                error = null
                loading = true
                scope.launch(Dispatchers.IO) {
                    var lastResult: Result<String> = Result.failure(Exception("Aucune adresse disponible"))
                    var successUrl: String? = null

                    for (candidateUrl in urlsToTry) {
                        val result = extract(url.trim(), candidateUrl)
                        lastResult = result
                        if (result.isSuccess) {
                            successUrl = candidateUrl
                            break
                        }
                        // Only try next IP on connection errors; stop on extraction errors
                        val ex = result.exceptionOrNull()
                        val isConnErr = ex is java.net.ConnectException ||
                            ex?.message?.let {
                                it.contains("ECONNREFUSED") || it.contains("ETIMEDOUT") ||
                                it.contains("ENETUNREACH") || it.contains("Connection refused")
                            } == true
                        if (!isConnErr) break
                    }

                    withContext(Dispatchers.Main) {
                        loading = false
                        // If a fallback URL worked, promote it and keep old one as fallback
                        if (successUrl != null && successUrl != activeServerUrl) {
                            val oldFallbacks = loadFallbackUrls(prefs).filter { it != successUrl }
                            saveFallbackUrls(prefs, listOf(activeServerUrl) + oldFallbacks)
                            serverUrlInput = successUrl
                            prefs.edit().putString(PREF_SERVER_URL, successUrl).apply()
                        }
                        lastResult.onSuccess { json ->
                            val recipe = parseJson(json)
                            if (recipe != null) {
                                onImport(recipe)
                            } else {
                                error = "Impossible de parser la recette recue."
                            }
                        }.onFailure { e ->
                            val triedAddresses = urlsToTry.joinToString(", ") { extractorDisplayAddress(it) }
                            val msg = e.message ?: ""
                            error = when {
                                e is java.net.ConnectException &&
                                    (msg.contains("ECONNREFUSED") || msg.contains("Connection refused")) ->
                                    "Connexion refusee ($triedAddresses)\n\nHermes n'est pas demarre sur le PC.\nLance hermes.py sur ton PC et reessaie."

                                e is java.net.ConnectException &&
                                    (msg.contains("ETIMEDOUT") || msg.contains("timed out") || msg.contains("ENETUNREACH")) ->
                                    "PC injoignable ($triedAddresses)\n\nVerifie que :\n- Le telephone est bien sur le WiFi du PC\n- Le PC n'est pas en veille\n- Le pare-feu Windows autorise Hermes"

                                e is java.net.ConnectException ->
                                    "Connexion impossible ($triedAddresses)\n\n${msg.substringAfterLast(":").trim().ifBlank { msg }}\n\nVerifie le WiFi et que Hermes tourne sur le PC."

                                e is java.net.SocketTimeoutException ->
                                    "Hermes ne repond pas ($triedAddresses)\n\nLa connexion a ete etablie mais l'extraction depasse le delai. Reessaie ou redemarre hermes.py."

                                else -> e.message ?: "Erreur inconnue"
                            }
                        }
                    }
                }
            },
            enabled = url.trim().startsWith("http") && normalizedServerUrl != null && !loading && !detecting,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7A2CF6)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (loading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Text(
                        "Extraction en cours... (1-2 min)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text("Importer", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            if (serverAddress == "adresse invalide") "Entre l'adresse IP du PC ci-dessus."
            else "Serveur : $serverAddress — PC allume, meme WiFi, Hermes lance.",
            fontSize = 11.sp,
            color = Color(0xFF625B70),
            textAlign = TextAlign.Center
        )
    }
}

private suspend fun extractRecipe(videoUrl: String, extractorBaseUrl: String): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBaseUrl = normalizeExtractorBaseUrl(extractorBaseUrl)
                ?: error("Adresse Hermes invalide.")
            val endpoint = extractorEndpoint(normalizedBaseUrl)

            Log.d("UrlImport", "Connexion a $endpoint")
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 180_000
            conn.doOutput = true

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

private suspend fun discoverHermesServer(): String? = withContext(Dispatchers.IO) {
    val subnet = getLocalSubnet() ?: return@withContext null
    Log.d("UrlImport", "Scan reseau sur $subnet.1-254:18642")
    coroutineScope {
        (1..254).map { i ->
            async {
                val ip = "$subnet.$i"
                try {
                    val conn = URL("http://$ip:18642/health").openConnection() as HttpURLConnection
                    conn.connectTimeout = 300
                    conn.readTimeout = 300
                    conn.requestMethod = "GET"
                    if (conn.responseCode == 200) "http://$ip:18642" else null
                } catch (_: Exception) { null }
            }
        }.awaitAll().firstOrNull { it != null }
    }
}
