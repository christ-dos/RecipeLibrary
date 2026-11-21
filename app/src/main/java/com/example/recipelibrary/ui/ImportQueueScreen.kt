package com.example.recipelibrary.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.data.PendingImport
import com.example.recipelibrary.data.Recipe

private val Violet = Color(0xFF4A16C6)
private val Grey = Color(0xFF625B70)
private val Red = Color(0xFFCC0000)
private val Green = Color(0xFF2E7D32)

/** Résumé court de la file, pour le bandeau de l'accueil et le lien de l'écran URL. */
fun queueSummary(pending: List<PendingImport>): String {
    val ready = pending.count { it.status == PendingImport.READY }
    val waiting = pending.count { it.status == PendingImport.WAITING }
    val failed = pending.count { it.status == PendingImport.FAILED }
    return listOfNotNull(
        ready.takeIf { it > 0 }?.let { "$it à valider" },
        waiting.takeIf { it > 0 }?.let { "$it en attente" },
        failed.takeIf { it > 0 }?.let { if (it == 1) "1 échec" else "$it échecs" }
    ).joinToString(", ")
}

/**
 * Écran de la file d'attente : liens mis de côté quand le PC était injoignable.
 * Trois groupes : à valider (recette extraite), en attente du PC, échecs.
 */
@Composable
fun ImportQueueScreen(
    pending: List<PendingImport>,
    parseJson: (String) -> Recipe?,
    onOpen: (PendingImport) -> Unit,
    onRetry: (PendingImport) -> Unit,
    onDelete: (PendingImport) -> Unit,
    modifier: Modifier = Modifier,
    /** Relance tout de suite l'import des liens en attente (sans attendre le prochain essai). */
    onRunNow: () -> Unit = {},
    /** Copie un lien (pour faire l'extraction à la main). Par défaut : presse-papiers + message. */
    onCopy: ((String) -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copy: (String) -> Unit = onCopy ?: { link ->
        clipboard.setText(AnnotatedString(link))
        Toast.makeText(context, "Lien copié", Toast.LENGTH_SHORT).show()
    }
    val ready = pending.filter { it.status == PendingImport.READY }
    val waiting = pending.filter { it.status == PendingImport.WAITING }
    val failed = pending.filter { it.status == PendingImport.FAILED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pending.isEmpty()) {
            Text(
                "Aucun lien en attente.\n\nQuand le PC est injoignable, les liens importés depuis l'écran URL " +
                    "arrivent ici, puis sont importés automatiquement au retour sur le Wi-Fi de la maison.",
                fontSize = 14.sp,
                color = Grey,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
            )
        }
        // Pas de "return@Column" ici : une sortie anticipée d'un bloc Column corrompt l'état de Compose
        // (plantage ArrayIndexOutOfBoundsException dans SlotTable). Les groupes vides ne s'affichent pas.

        Group(
            title = "À valider (${ready.size})",
            hint = "Recettes extraites : vérifie la catégorie puis enregistre.",
            items = ready
        ) { item ->
            QueueItem(
                label = "✅ Prête",
                labelColor = Green,
                text = item.recipeJson?.let { runCatching { parseJson(it)?.title }.getOrNull() } ?: item.url,
                error = null
            ) {
                TextButton(onClick = { onOpen(item) }) { Text("Valider") }
                TextButton(onClick = { onDelete(item) }) { Text("Supprimer", color = Grey) }
            }
        }

        Group(
            title = "En attente du PC (${waiting.size})",
            hint = "Importés automatiquement dès que le téléphone est sur le Wi-Fi de la maison.",
            items = waiting,
            action = {
                TextButton(onClick = onRunNow) {
                    Text("▶ Lancer maintenant", color = Color(0xFF7A2CF6), fontWeight = FontWeight.SemiBold)
                }
            }
        ) { item ->
            QueueItem(label = "⏳ En attente", labelColor = Grey, text = item.url, error = null) {
                TextButton(onClick = { copy(item.url) }) { Text("Copier le lien") }
                TextButton(onClick = { onDelete(item) }) { Text("Supprimer", color = Grey) }
            }
        }

        Group(
            title = "Échecs (${failed.size})",
            hint = "Si la recette n'est pas dans la vidéo, copie le lien et passe par l'import texte.",
            items = failed
        ) { item ->
            QueueItem(label = "⚠️ Échec", labelColor = Red, text = item.url, error = item.error) {
                TextButton(onClick = { onRetry(item) }) { Text("Réessayer") }
                TextButton(onClick = { copy(item.url) }) { Text("Copier le lien") }
                TextButton(onClick = { onDelete(item) }) { Text("Supprimer", color = Grey) }
            }
        }
    }
}

@Composable
private fun Group(
    title: String,
    hint: String,
    items: List<PendingImport>,
    action: (@Composable () -> Unit)? = null,
    row: @Composable (PendingImport) -> Unit
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Violet)
        Text(hint, fontSize = 12.sp, color = Grey)
        action?.invoke()
        items.forEach { row(it) }
    }
}

@Composable
private fun QueueItem(
    label: String,
    labelColor: Color,
    text: String,
    error: String?,
    actions: @Composable () -> Unit
) {
    Card(
        backgroundColor = Color.White,
        elevation = 1.dp,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
            Text(text, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (error != null) Text(error, fontSize = 12.sp, color = Red)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { actions() }
        }
    }
}
