package com.example.recipelibrary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.recipelibrary.data.Recipe
import java.util.UUID

@Composable
fun TextImportScreen(
    onImport: (Recipe) -> Unit,
    onBack: () -> Unit,
    initialText: String = ""
) {
    var text by remember { mutableStateOf(initialText) }
    val isOcr = initialText.isNotBlank()

    Scaffold(
        backgroundColor = ColorScreenBg,
        topBar = { AppTopBar(if (isOcr) "Texte reconnu (photo)" else "Importer du texte", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Zone de texte scrollable — prend tout l'espace disponible
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (isOcr)
                        "Texte extrait de la photo — fais défiler pour tout voir, corrige si besoin."
                    else
                        "Colle ou tape le texte de la recette. L'app pré-remplira le formulaire.",
                    style = MaterialTheme.typography.body2
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Texte de la recette") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Titre\n\nIngrédients :\n- ...\n\nÉtapes :\n1. ...") }
                )
            }

            // Bouton toujours visible en bas, au-dessus de la barre système
            Button(
                onClick = { onImport(parseTextRecipe(text)) },
                enabled = text.trim().isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text("Importer dans le formulaire")
            }
        }
    }
}

internal fun parseTextRecipe(raw: String): Recipe {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

    // Section keywords — French + English + common OCR variants
    val ingredientKeywords = listOf(
        "ingrédient", "ingredient", "ingrédients", "ingredients",
        "il vous faut", "il faut", "vous aurez besoin", "pour cette recette",
        "liste des courses", "courses", "produits", "what you need",
        "you will need", "you'll need", "shopping list"
    )
    val stepKeywords = listOf(
        "étape", "etape", "étapes", "etapes",
        "préparation", "preparation", "instructions", "instruction",
        "méthode", "methode", "procédé", "procede",
        "déroulement", "comment faire", "marche à suivre",
        "mode d'emploi", "directions", "method",
        "steps", "how to", "procedure"
    )
    val noteKeywords = listOf(
        "note", "notes", "conseil", "conseils", "astuce", "astuces",
        "tips", "tip", "remarque", "remarques", "variante", "variantes",
        "suggestion", "suggestions", "info", "information",
        "à savoir", "bon à savoir", "le saviez-vous", "important"
    )
    val servingsKeywords = listOf(
        "portion", "portions", "personne", "personnes", "serve", "serves",
        "pour", "yield", "quantité"
    )
    val timeKeywords = listOf(
        "temps", "durée", "duree", "cuisson", "repos", "total",
        "préparation :", "time", "prep", "cook"
    )

    // Detect section headings — line must START WITH a keyword (never just contain it)
    // This avoids false positives like "Mélanger les ingrédients" triggering ingredients mode
    fun detectSection(line: String): String? {
        val l = line.lowercase().trim().trimEnd(':', '.', ' ')
        fun startsWithAny(kw: List<String>) = kw.any { l.startsWith(it) }
        return when {
            startsWithAny(ingredientKeywords) -> "ingredients"
            startsWithAny(stepKeywords)       -> "steps"
            startsWithAny(noteKeywords)       -> "notes"
            servingsKeywords.any { l == it || l.startsWith("$it ") || l.startsWith("$it:") } -> "meta"
            timeKeywords.any { l.startsWith(it) } -> "meta"
            else -> null
        }
    }

    // Strip common list prefixes: bullets, numbers, dashes
    // Only removes leading digits when followed by a separator (". ", ") ", ": ") —
    // preserves quantities like "150g", "2 oignons", "10,5cl"
    fun cleanLine(line: String): String {
        var s = line.trimStart('-', '–', '—', '•', '·', '*', '▪', '◦', '○', '●', '→', '>')
        // "1. ", "2) ", "3: " → strip — but "150g", "2 oignons" → keep (no separator after digits)
        s = s.replace(Regex("^\\d+[.):] +"), "")
        return s.trim()
    }

    // First non-empty line = title (skip if it looks like a section heading)
    val titleLine = lines.firstOrNull() ?: ""
    val title = if (detectSection(titleLine) != null) "" else titleLine

    val startIndex = if (title.isNotEmpty()) 1 else 0
    var mode = "none"
    val ingredients = mutableListOf<String>()
    val steps = mutableListOf<String>()
    val notes = mutableListOf<String>()
    val extras = mutableListOf<String>()

    for (line in lines.drop(startIndex)) {
        val section = detectSection(line)
        if (section != null) {
            mode = section
            continue
        }
        val cleaned = cleanLine(line)
        if (cleaned.isBlank()) continue
        when (mode) {
            "ingredients" -> ingredients.add(cleaned)
            "steps" -> steps.add(cleaned)
            "notes" -> notes.add(cleaned)
            "meta" -> extras.add(line) // keep timing/servings info in extras
            else -> extras.add(line)
        }
    }

    val allNotes = (notes + extras).filter { it.isNotBlank() }
    val now = System.currentTimeMillis()
    return Recipe(
        id = UUID.randomUUID().toString(),
        title = title,
        sourceUrl = null,
        description = null,
        ingredients = ingredients,
        steps = steps,
        category = null,
        tags = emptyList(),
        notes = allNotes.joinToString("\n").ifEmpty { null },
        createdAt = now,
        updatedAt = now
    )
}
