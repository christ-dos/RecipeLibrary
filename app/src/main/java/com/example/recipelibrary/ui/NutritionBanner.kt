package com.example.recipelibrary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.NutritionEstimate
import com.example.recipelibrary.data.nutrition.NutritionWarning
import com.example.recipelibrary.data.nutrition.ServingsOrigin
import kotlin.math.roundToInt

private val BannerBg = Color(0xFFF0FAF3)
private val BannerText = Color(0xFF1B5E37)
private val WarnText = Color(0xFF8A5A00)

/** Texte du nombre de parts : « 4 parts », « ≈ 6 parts (estimé) ». */
fun servingsLabel(servings: Int, origin: ServingsOrigin?): String {
    val n = if (servings > 1) "$servings parts" else "1 part"
    return if (origin == ServingsOrigin.ESTIMATED) "≈ $n (estimé)" else n
}

/** kcal arrondis à la dizaine. */
fun kcalLabel(kcal: Double): String = "${(kcal / 10).roundToInt() * 10}"

/**
 * Bandeau « ≈ X kcal / part » sous le titre de la fiche (story 1).
 *
 * @param onOpenDetail ouvre le détail par ingrédient (story 2) ; null = bandeau non cliquable.
 * @param onServingsChange enregistre un nombre de parts saisi ici (story 5) ; null = pas de saisie.
 */
@Composable
fun NutritionBanner(
    estimate: NutritionEstimate,
    onOpenDetail: (() -> Unit)? = null,
    onServingsChange: ((Int) -> Unit)? = null
) {
    var askServings by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("nutrition_banner")
            .let { m -> if (onOpenDetail != null) m.clickable { onOpenDetail() } else m },
        shape = RoundedCornerShape(14.dp),
        backgroundColor = BannerBg,
        elevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            val total = estimate.total
            val per = estimate.perServing
            when {
                total == null -> {
                    Text("🔥 Estimation impossible", fontWeight = FontWeight.SemiBold, color = BannerText, modifier = Modifier.testTag("nutrition_impossible"))
                    Text("Aucun ingrédient n'a pu être compté.", fontSize = 12.sp, color = BannerText)
                }
                per != null && estimate.servings != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "🔥 ≈ ${kcalLabel(per.kcal)} kcal / part",
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BannerText,
                            modifier = Modifier.testTag("nutrition_per_serving")
                        )
                        val label = servingsLabel(estimate.servings, estimate.servingsOrigin)
                        Text(
                            label,
                            fontSize = 13.sp, color = BannerText,
                            modifier = Modifier.testTag("nutrition_servings")
                                .let { m -> if (onServingsChange != null && estimate.servingsOrigin == ServingsOrigin.ESTIMATED) m.clickable { askServings = true } else m }
                        )
                    }
                }
                else -> {
                    Text(
                        "🔥 ≈ ${kcalLabel(total.kcal)} kcal pour la recette",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BannerText,
                        modifier = Modifier.testTag("nutrition_total")
                    )
                    if (onServingsChange != null) {
                        TextButton(onClick = { askServings = true }, modifier = Modifier.testTag("nutrition_set_servings")) {
                            Text("Indiquer le nombre de parts", color = Color(0xFF7A2CF6), fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text("Indique le nombre de parts pour avoir la valeur par part.", fontSize = 12.sp, color = BannerText)
                    }
                }
            }
            if (NutritionWarning.PARTIAL in estimate.warnings) {
                Text("⚠ Estimation partielle : plusieurs ingrédients ne sont pas comptés.", fontSize = 12.sp, color = WarnText, modifier = Modifier.testTag("nutrition_warn_partial"))
            }
            if (NutritionWarning.CHECK_VALUE in estimate.warnings) {
                Text("⚠ Valeur à vérifier : regarde les quantités et le nombre de parts.", fontSize = 12.sp, color = WarnText, modifier = Modifier.testTag("nutrition_warn_check"))
            }
            if (NutritionWarning.FRYING in estimate.warnings) {
                Text("Friture : la quantité d'huile absorbée est approximative.", fontSize = 12.sp, color = WarnText)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (onOpenDetail != null) "Estimation indicative · touche pour le détail" else "Estimation indicative",
                fontSize = 11.sp, color = BannerText.copy(alpha = 0.7f)
            )
        }
    }

    if (askServings && onServingsChange != null) {
        ServingsDialog(
            initial = estimate.servings,
            onConfirm = { askServings = false; onServingsChange(it) },
            onDismiss = { askServings = false }
        )
    }
}

/** Valide une saisie de nombre de parts : entier de 1 à 100, sinon null. */
fun parseServings(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS }

@Composable
fun ServingsDialog(initial: Int?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial?.toString() ?: "") }
    val value = parseServings(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nombre de parts") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && value == null,
                    modifier = Modifier.testTag("servings_dialog_field")
                )
                if (text.isNotEmpty() && value == null) {
                    Text("Entre un nombre de 1 à 100.", color = Color(0xFFB00020), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null, modifier = Modifier.testTag("servings_dialog_ok")) {
                Text("Enregistrer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
