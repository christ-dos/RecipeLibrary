package com.example.recipelibrary.ui

import androidx.compose.foundation.layout.aspectRatio
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.R
import com.example.recipelibrary.data.HermesImporter
import com.example.recipelibrary.data.HermesNetwork
import com.example.recipelibrary.data.HermesPrefs
import com.example.recipelibrary.data.PendingImport
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.extractorDisplayAddress
import com.example.recipelibrary.data.isConnectionError
import com.example.recipelibrary.data.normalizeExtractorBaseUrl
import com.example.recipelibrary.data.resolveStoredServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Message affiché quand un lien part en file d'attente. */
const val QUEUED_MESSAGE =
    "Lien mis en file d'attente\n\nLe PC est injoignable. La recette sera importée automatiquement " +
        "dès que le téléphone sera sur le Wi-Fi de la maison. Tu la retrouveras dans la file d'attente, à valider."

@Composable
fun UrlImportScreen(
    onImport: (Recipe) -> Unit,
    onBack: () -> Unit,
    parseJson: (String) -> Recipe?,
    extract: suspend (String, String) -> Result<String> = HermesNetwork::extractRecipe,
    discover: suspend () -> String? = HermesNetwork::discover,
    isUp: suspend (String) -> Boolean = HermesNetwork::isUp,
    wake: suspend (String, String) -> Unit = HermesNetwork::wake,
    learnMac: suspend (String) -> String? = HermesNetwork::fetchMac,
    onLocalNetwork: suspend () -> Boolean = HermesNetwork::onWifi,
    /** Si fourni, un lien dont l'import échoue faute de joindre le PC est mis en file d'attente. */
    onQueue: ((String) -> Unit)? = null,
    /** Contenu de la file, pour afficher le lien vers l'écran de la file d'attente. */
    pending: List<PendingImport> = emptyList(),
    onOpenQueue: () -> Unit = {},
    /** Lien partagé vers l'app (Instagram, Chrome…) : collé et importé tout de suite. */
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(HermesPrefs.NAME, Context.MODE_PRIVATE)
    }
    val importer = remember(prefs) { HermesImporter(prefs, extract, discover, isUp, wake, learnMac, onLocalNetwork) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("") }
    var serverUrlInput by rememberSaveable {
        mutableStateOf(resolveStoredServerUrl(prefs))
    }
    var loading by remember { mutableStateOf(false) }
    var detecting by remember { mutableStateOf(false) }
    var waking by remember { mutableStateOf(false) }
    var serverMac by remember { mutableStateOf(prefs.getString(HermesPrefs.SERVER_MAC, null)) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var detectError by remember { mutableStateOf<String?>(null) }

    val normalizedServerUrl = remember(serverUrlInput) { normalizeExtractorBaseUrl(serverUrlInput) }
    val serverAddress = normalizedServerUrl?.let(::extractorDisplayAddress) ?: "adresse invalide"

    fun startImport() {
        val activeServerUrl = normalizedServerUrl
        if (activeServerUrl == null) {
            error = "Adresse Hermes invalide."
            return
        }
        val videoUrl = url.trim()

        error = null
        info = null
        loading = true
        scope.launch(Dispatchers.IO) {
            val outcome = importer.run(videoUrl, activeServerUrl) { isWaking ->
                withContext(Dispatchers.Main) { waking = isWaking }
            }

            withContext(Dispatchers.Main) {
                loading = false
                // L'importeur a pu changer l'adresse du serveur ou apprendre l'adresse MAC du PC
                if (outcome.successUrl != null && outcome.successUrl != activeServerUrl) {
                    serverUrlInput = outcome.successUrl
                }
                serverMac = prefs.getString(HermesPrefs.SERVER_MAC, null)

                outcome.result.onSuccess { json ->
                    val recipe = parseJson(json)
                    if (recipe != null) {
                        onImport(recipe)
                    } else {
                        error = "Impossible de parser la recette recue."
                    }
                }.onFailure { e ->
                    if (onQueue != null && isConnectionError(e)) {
                        onQueue(videoUrl)
                        url = ""
                        info = QUEUED_MESSAGE
                        return@onFailure
                    }
                    val triedAddresses = outcome.triedUrls.joinToString(", ") { extractorDisplayAddress(it) }
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
    }

    // Lien partagé depuis une autre app : on le colle et on lance l'import directement
    LaunchedEffect(sharedUrl) {
        val shared = sharedUrl ?: return@LaunchedEffect
        onSharedUrlConsumed()
        if (loading) return@LaunchedEffect
        url = shared
        startImport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Haut à 32 dp comme les pages Fichier et Photo : l'icône est au même niveau
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val headerIcon = painterResource(R.drawable.ic_nav_url)
        Image(
            painter = headerIcon,
            contentDescription = null,
            // Emblème avec un nœud en bas : 128 dp pour que son cadre ait la taille des tuiles
            // Fichier et Photo (96 dp) ; largeur selon le dessin
            modifier = Modifier.height(128.dp)
                .aspectRatio(headerIcon.intrinsicSize.width / headerIcon.intrinsicSize.height)
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
                onValueChange = { url = it; error = null; info = null },
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
                        info = null
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
                    prefs.edit().putString(HermesPrefs.SERVER_URL, it).apply()
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
                            prefs.edit().putString(HermesPrefs.SERVER_URL, found).apply()
                            // Memorise l'adresse MAC du PC pour pouvoir le reveiller plus tard
                            val mac = withContext(Dispatchers.IO) { runCatching { learnMac(found) }.getOrNull() }
                            if (mac != null) {
                                prefs.edit().putString(HermesPrefs.SERVER_MAC, mac).apply()
                                serverMac = mac
                            }
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
            MessageCard(detectError!!, isError = true)
        }

        Text(
            "Adresse IP du PC (ipconfig dans cmd). Exemple: 192.168.1.X:18642",
            fontSize = 11.sp,
            color = Color(0xFF625B70),
            textAlign = TextAlign.Center
        )

        if (error != null) {
            MessageCard(error!!, isError = true)
            // Extraction impossible : copier le lien pour la faire à la main (navigateur, import texte)
            if (url.isNotBlank()) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(url.trim()))
                    Toast.makeText(context, "Lien copié", Toast.LENGTH_SHORT).show()
                }) {
                    Text("📋 Copier le lien", color = Color(0xFF7A2CF6), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (info != null) {
            MessageCard(info!!, isError = false)
        }

        Button(
            onClick = { startImport() },
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
                        if (waking) "Reveil du PC..." else "Extraction en cours... (1-2 min)",
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
            else if (serverMac != null) "Serveur : $serverAddress — meme WiFi. Le PC en veille sera reveille automatiquement."
            else "Serveur : $serverAddress — PC allume, meme WiFi, Hermes lance.",
            fontSize = 11.sp,
            color = Color(0xFF625B70),
            textAlign = TextAlign.Center
        )

        if (pending.isNotEmpty()) {
            TextButton(onClick = onOpenQueue) {
                Text(
                    "Voir la file d'attente (${queueSummary(pending)})",
                    color = Color(0xFF7A2CF6),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MessageCard(text: String, isError: Boolean) {
    Card(
        backgroundColor = if (isError) Color(0xFFFFEEEE) else Color(0xFFF3ECFF),
        elevation = 0.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(10.dp),
            color = if (isError) Color(0xFFCC0000) else Color(0xFF4A16C6),
            fontSize = 13.sp
        )
    }
}
