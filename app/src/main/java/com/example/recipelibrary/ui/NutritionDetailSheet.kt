package com.example.recipelibrary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.nutrition.GramsOrigin
import com.example.recipelibrary.data.nutrition.IngredientIssue
import com.example.recipelibrary.data.nutrition.FoodSubstitutions
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.NutritionEstimate
import com.example.recipelibrary.data.nutrition.ParsedIngredient
import kotlin.math.roundToInt

private val Violet = Color(0xFF7A2CF6)
private val Grey = Color(0xFF6B7280)

fun originLabel(o: GramsOrigin): String = when (o) {
    GramsOrigin.WRITTEN -> "écrit"
    GramsOrigin.PC -> "estimé"
    GramsOrigin.USER -> "corrigé"
}

fun issueLabel(i: IngredientIssue): String = when (i) {
    IngredientIssue.VAGUE -> "quantité imprécise"
    IngredientIssue.UNKNOWN_FOOD -> "aliment non reconnu"
    IngredientIssue.NO_PIECE_WEIGHT -> "poids d'une pièce inconnu"
    IngredientIssue.TO_COMPLETE -> "ligne à compléter"
}

/** Grammes saisis : nombre > 0 et ≤ 10 000 (virgule acceptée), sinon null. */
fun parseGrams(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= IngredientWeight.MAX_GRAMS }

private fun grams(g: Double): String = if (g >= 10) "${g.roundToInt()}" else "%.1f".format(g).replace('.', ',')

/**
 * Détail des calories (story 2) : lignes comptées avec poids, origine et kcal ; nutriments par
 * part ; lignes non comptées avec leur raison ; saisie ou correction d'un poids.
 */
@Composable
fun NutritionDetailSheet(
    estimate: NutritionEstimate,
    onDismiss: () -> Unit,
    onWeightChange: (line: String, weight: IngredientWeight) -> Unit,
    /** Table utilisée pour chercher un aliment de remplacement (story 6) ; null = bouton masqué. */
    foodTable: FoodTable? = null,
    /** Aliment de remplacement choisi : texte normalisé → code Ciqual, pour toutes les recettes. */
    onChooseFood: ((key: String, foodCode: String) -> Unit)? = null,
    /** Ouvre l'écran « Aliments de remplacement » ; null = lien masqué. */
    onOpenSubstitutions: (() -> Unit)? = null
) {
    var editing by remember { mutableStateOf<Pair<String, Double?>?>(null) }
    var choosing by remember { mutableStateOf<ParsedIngredient?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(12.dp).testTag("nutrition_detail")
        ) {
            LazyColumn(Modifier.padding(16.dp).testTag("detail_list"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Text("Détail des calories", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    val per = estimate.perServing
                    val total = estimate.total
                    when {
                        per != null && estimate.servings != null -> {
                            Text(
                                "≈ ${kcalLabel(per.kcal)} kcal / part · ${servingsLabel(estimate.servings, estimate.servingsOrigin)}",
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("detail_per_serving")
                            )
                            Text(
                                "Par part : protéines ${per.protein.roundToInt()} g · glucides ${per.carbs.roundToInt()} g · lipides ${per.fat.roundToInt()} g",
                                fontSize = 13.sp, color = Grey, modifier = Modifier.testTag("detail_macros")
                            )
                        }
                        total != null -> Text("≈ ${kcalLabel(total.kcal)} kcal pour la recette", fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("detail_total"))
                        else -> Text("Estimation impossible : aucun ingrédient compté.", fontWeight = FontWeight.SemiBold)
                    }
                    if (total != null) {
                        Text("Total recette : ≈ ${kcalLabel(total.kcal)} kcal", fontSize = 13.sp, color = Grey, modifier = Modifier.testTag("detail_recipe_total"))
                    }
                }

                if (estimate.counted.isNotEmpty()) {
                    item { SectionTitle("Comptés") }
                    items(estimate.counted) { c ->
                        Row(
                            Modifier.fillMaxWidth().testTag("counted_${c.parsed.line}").semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.parsed.line, fontSize = 14.sp)
                                Text(
                                    "${grams(c.grams)} g (${originLabel(c.origin)}) · ${c.nutrients.kcal.roundToInt()} kcal · ${c.food.name}",
                                    fontSize = 12.sp, color = Grey
                                )
                                c.parsed.substitutionKey?.let { key ->
                                    Text(
                                        "Aliment de remplacement pour « $key »",
                                        fontSize = 12.sp, color = Violet,
                                        modifier = Modifier.testTag("substituted_${c.parsed.line}")
                                    )
                                }
                            }
                            TextButton(onClick = { editing = c.parsed.line to c.grams }) { Text("Corriger", color = Violet, fontSize = 12.sp) }
                        }
                    }
                }

                if (estimate.ignored.isNotEmpty()) {
                    item { SectionTitle("Non comptés") }
                    items(estimate.ignored) { ig ->
                        Row(
                            Modifier.fillMaxWidth().testTag("ignored_${ig.parsed.line}").semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ig.parsed.line, fontSize = 14.sp)
                                Text(issueLabel(ig.issue), fontSize = 12.sp, color = Color(0xFF8A5A00))
                            }
                            // Sans aliment reconnu, un poids ne donne pas de calories : pas de saisie.
                            if (ig.parsed.food != null) {
                                TextButton(
                                    onClick = { editing = ig.parsed.line to null },
                                    modifier = Modifier.testTag("set_weight_${ig.parsed.line}")
                                ) { Text("Indiquer le poids", color = Violet, fontSize = 12.sp) }
                            } else if (ig.issue == IngredientIssue.UNKNOWN_FOOD && foodTable != null && onChooseFood != null) {
                                TextButton(
                                    onClick = { choosing = ig.parsed },
                                    modifier = Modifier.testTag("choose_food_${ig.parsed.line}")
                                ) { Text("Choisir l'aliment", color = Violet, fontSize = 12.sp) }
                            }
                        }
                    }
                }

                if (estimate.seasonings.isNotEmpty()) {
                    item {
                        SectionTitle("Ignorés")
                        Text(
                            estimate.seasonings.joinToString(" · ") { it.line } + " (sel, épices : quantités négligeables)",
                            fontSize = 12.sp, color = Grey
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Text(
                        "Estimation indicative (environ ±15 %). Source : Anses, table Ciqual 2020.",
                        fontSize = 11.sp, color = Grey, modifier = Modifier.padding(top = 6.dp).testTag("detail_source")
                    )
                    if (onOpenSubstitutions != null) {
                        TextButton(
                            onClick = onOpenSubstitutions,
                            modifier = Modifier.fillMaxWidth().testTag("open_substitutions")
                        ) { Text("Aliments de remplacement", color = Violet) }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Fermer", color = Violet) }
                }
            }
        }
    }

    editing?.let { (line, current) ->
        WeightDialog(
            line = line,
            initial = current,
            onConfirm = { g -> editing = null; onWeightChange(line, IngredientWeight(g, IngredientWeight.USER)) },
            onDismiss = { editing = null }
        )
    }

    val picking = choosing
    if (picking != null && foodTable != null && onChooseFood != null) {
        FoodPickerDialog(
            parsed = picking,
            table = foodTable,
            onChoose = { key, code -> choosing = null; onChooseFood(key, code) },
            onDismiss = { choosing = null }
        )
    }
}

/**
 * Choix d'un aliment de remplacement (story 6) : texte à reconnaître (prérempli, raccourcissable
 * pour viser d'autres recettes, ex. « skyr »), recherche dans la table ; toucher un aliment le retient.
 */
@Composable
fun FoodPickerDialog(
    parsed: ParsedIngredient,
    table: FoodTable,
    onChoose: (key: String, foodCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(FoodSubstitutions.defaultKey(parsed)) }
    var query by remember { mutableStateOf("") }
    val validKey = remember(key) { FoodSubstitutions.normalizedKey(key, parsed.line, table) }
    val results = remember(query) { FoodSubstitutions.search(table, query) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(12.dp).testTag("food_picker")
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Choisir l'aliment", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(parsed.line, fontSize = 13.sp, color = Grey)
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.take(80) },
                    singleLine = true,
                    label = { Text("Texte à reconnaître") },
                    isError = validKey == null,
                    modifier = Modifier.fillMaxWidth().testTag("substitution_key")
                )
                Text(
                    if (validKey == null) "Ce texte doit figurer dans l'ingrédient."
                    else "Le choix vaudra pour toutes les recettes contenant « $validKey ».",
                    fontSize = 12.sp, color = if (validKey == null) Color(0xFFB00020) else Grey,
                    modifier = Modifier.testTag("substitution_key_hint")
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    singleLine = true,
                    label = { Text("Rechercher un aliment") },
                    modifier = Modifier.fillMaxWidth().testTag("food_search")
                )
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("food_results")) {
                    items(results, key = { it.code }) { f ->
                        Column(
                            Modifier.fillMaxWidth()
                                .clickable(enabled = validKey != null) { validKey?.let { onChoose(it, f.code) } }
                                .padding(vertical = 8.dp)
                                .testTag("food_result_${f.code}")
                                .semantics(mergeDescendants = true) {}
                        ) {
                            Text(f.name, fontSize = 14.sp)
                            Text("${f.kcal.roundToInt()} kcal / 100 g", fontSize = 12.sp, color = Grey)
                        }
                        Divider()
                    }
                    if (query.isNotBlank() && results.isEmpty()) {
                        item { Text("Aucun aliment trouvé.", fontSize = 13.sp, color = Grey) }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Annuler", color = Violet) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun WeightDialog(line: String, initial: Double?, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial?.let { grams(it) } ?: "") }
    val value = parseGrams(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Poids en grammes") },
        text = {
            Column {
                Text(line, fontSize = 13.sp, color = Grey)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(8) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = text.isNotEmpty() && value == null,
                    modifier = Modifier.testTag("weight_field")
                )
                if (text.isNotEmpty() && value == null) Text("Entrez un poids supérieur à 0, jusqu'à 10 000 g.", color = Color(0xFFB00020), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null, modifier = Modifier.testTag("weight_ok")) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
