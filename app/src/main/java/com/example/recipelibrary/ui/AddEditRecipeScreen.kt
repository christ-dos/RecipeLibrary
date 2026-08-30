package com.example.recipelibrary.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.data.Recipe
import java.util.UUID

private val CATEGORIES = listOf(
    "Apéritifs", "Biscuits et petits gâteaux", "Boissons",
    "Accompagnements", "Desserts", "Entrées", "Gâteaux", "Goûters",
    "Pâtes et riz", "Petits-déjeuners", "Plats principaux", "Salades",
    "Sandwichs et wraps", "Sauces", "Soupes", "Tartes et quiches"
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddEditRecipeScreen(
    existing: Recipe?,
    onSave: (Recipe) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var title       by remember { mutableStateOf(existing?.title ?: "") }
    var sourceUrl   by remember { mutableStateOf(existing?.sourceUrl ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var ingredients by remember { mutableStateOf(existing?.ingredients?.joinToString("\n") ?: "") }
    var steps       by remember { mutableStateOf(existing?.steps?.joinToString("\n") ?: "") }
    var category    by remember { mutableStateOf(existing?.category ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var tags  by remember { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    var onSpeechResult by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!recognized.isNullOrBlank()) onSpeechResult?.invoke(recognized)
            else Toast.makeText(context, "Dictée non reconnue", Toast.LENGTH_SHORT).show()
        }
        onSpeechResult = null
    }

    fun startDictation(prompt: String, onResult: (String) -> Unit) {
        onSpeechResult = onResult
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try { speechLauncher.launch(intent) } catch (_: Exception) {
            Toast.makeText(context, "Dictée non disponible", Toast.LENGTH_SHORT).show()
            onSpeechResult = null
        }
    }

    val titleError       = when {
        title.isBlank()     -> "Donnez un titre à la recette."
        title.length > 256  -> "Le titre ne doit pas dépasser 256 caractères."
        else -> null
    }
    val ingredientsError = if (ingredients.isBlank()) "Ajoutez au moins un ingrédient." else null
    val stepsError       = if (steps.isBlank()) "Ajoutez au moins une étape." else null
    val urlError         = sourceUrl.trim().let { u ->
        if (u.isNotEmpty() && !u.startsWith("http://") && !u.startsWith("https://"))
            "L'URL doit commencer par http:// ou https://"
        else null
    }
    val categoryError = if (category.isBlank()) "Choisissez une catégorie." else null
    val isValid = titleError == null && ingredientsError == null && stepsError == null &&
                  urlError == null && categoryError == null

    Scaffold(
        backgroundColor = ColorScreenBg,
        topBar = {
            AppTopBar(
                title = if (existing == null) "Nouvelle recette" else "Modifier la recette",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Titre
                FormCard {
                    FieldLabel(
                        label = "Titre *",
                        onMicClick = {
                            startDictation("Dites le titre") { r ->
                                title = if (title.isBlank()) r else "$title $r"
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = title,
                        onValueChange = { if (it.length <= 256) title = it },
                        placeholder = "Donnez un nom appétissant à votre recette",
                        singleLine = true,
                        isError = titleError != null,
                        modifier = Modifier.semantics(mergeDescendants = true) { testTag = "form_title" }
                    )
                    if (titleError != null) ErrorText(titleError)
                }

                // Catégorie
                FormCard {
                    FieldLabel(label = "Catégorie *")
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (category.isEmpty()) "" else "${categoryEmoji(category)} $category",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Choisir une catégorie", color = ColorInactiveText, fontSize = 14.sp) },
                            isError = categoryError != null,
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ColorTitleViolet)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = ColorTitleViolet,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                cursorColor = ColorTitleViolet,
                                backgroundColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            CATEGORIES.forEach { cat ->
                                DropdownMenuItem(onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }) {
                                    Text("${categoryEmoji(cat)} $cat")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("≡ ", color = ColorTitleViolet, fontSize = 12.sp)
                        Text("Liste triée par ordre alphabétique", fontSize = 12.sp, color = ColorInactiveText)
                    }
                    if (categoryError != null) ErrorText(categoryError)
                }

                // Ingrédients
                FormCard {
                    FieldLabel(
                        label = "Ingrédients *",
                        subtitle = "Un ingrédient par ligne",
                        onMicClick = {
                            startDictation("Dites un ingrédient") { r ->
                                ingredients = if (ingredients.isBlank()) r else "$ingredients\n$r"
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        placeholder = "Ex. : 200 g de farine\n2 œufs\n1 pincée de sel…",
                        isError = ingredientsError != null,
                        modifier = Modifier.height(140.dp).semantics(mergeDescendants = true) { testTag = "form_ingredients" }
                    )
                    if (ingredientsError != null) ErrorText(ingredientsError)
                }

                // Étapes
                FormCard {
                    FieldLabel(
                        label = "Étapes de préparation *",
                        subtitle = "Une étape par ligne",
                        onMicClick = {
                            startDictation("Dites une étape") { r ->
                                steps = if (steps.isBlank()) r else "$steps\n$r"
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = steps,
                        onValueChange = { steps = it },
                        placeholder = "Décrivez chaque étape en détail…",
                        isError = stepsError != null,
                        modifier = Modifier.height(180.dp).semantics(mergeDescendants = true) { testTag = "form_steps" }
                    )
                    if (stepsError != null) ErrorText(stepsError)
                }

                // URL source
                FormCard {
                    FieldLabel(
                        label = "URL source",
                        optional = true,
                        subtitle = "Indiquez l'URL si la recette provient d'une vidéo (YouTube, Instagram, etc.)"
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        placeholder = "https://…",
                        singleLine = true,
                        isError = urlError != null,
                        modifier = Modifier.semantics(mergeDescendants = true) { testTag = "form_source_url" }
                    )
                    if (urlError != null) ErrorText(urlError)
                }

                // Tags
                FormCard {
                    FieldLabel(label = "Tags", optional = true)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        placeholder = "Ajoutez des tags (ex. goûter, chocolat, vegan)",
                        singleLine = true
                    )
                }

                // Notes
                FormCard {
                    FieldLabel(
                        label = "Notes",
                        optional = true,
                        onMicClick = {
                            startDictation("Dites vos notes") { r ->
                                notes = if (notes.isBlank()) r else "$notes\n$r"
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "Ajoutez des notes utiles (astuces, variantes, conseils…)",
                        modifier = Modifier.height(100.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            // Save button — pinned at bottom
            GradientButton(
                text = "Enregistrer",
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        Recipe(
                            id          = existing?.id ?: UUID.randomUUID().toString(),
                            title       = title.trim(),
                            sourceUrl   = sourceUrl.trim().ifEmpty { null },
                            description = description.trim().ifEmpty { null },
                            ingredients = ingredients.lines().map { it.trim() }.filter { it.isNotEmpty() },
                            steps       = steps.lines().map { it.trim() }.filter { it.isNotEmpty() },
                            category    = category.trim().ifEmpty { null },
                            tags        = tags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() },
                            notes       = notes.trim().ifEmpty { null },
                            imageUrl    = existing?.imageUrl,
                            createdAt   = existing?.createdAt ?: now,
                            updatedAt   = now
                        )
                    )
                },
                enabled = isValid,
                leadingIcon = { Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun FieldLabel(
    label: String,
    optional: Boolean = false,
    subtitle: String? = null,
    onMicClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                if (optional) {
                    Text(" (optionnel)", fontSize = 12.sp, color = ColorInactiveText)
                }
            }
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = ColorInactiveText)
            }
        }
        if (onMicClick != null) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onMicClick,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(40.dp),
                border = BorderStroke(1.5.dp, ColorTitleViolet),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.White,
                    contentColor = ColorTitleViolet
                )
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Dicter", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = ColorInactiveText, fontSize = 14.sp) },
        isError = isError,
        singleLine = singleLine,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = ColorTitleViolet,
            unfocusedBorderColor = Color(0xFFE2E8F0),
            cursorColor = ColorTitleViolet,
            backgroundColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ErrorText(message: String) {
    Spacer(Modifier.height(4.dp))
    Text(message, color = MaterialTheme.colors.error, fontSize = 12.sp)
}
