package com.example.recipelibrary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.data.FoodSubstitution
import com.example.recipelibrary.data.nutrition.FoodTable

/**
 * Aliments de remplacement (spec 001, story 6) : liste des équivalences « texte → aliment »
 * retenues pour toutes les recettes, chacune annulable. Annuler ne modifie aucune recette.
 */
@Composable
fun FoodSubstitutionsScreen(
    substitutions: List<FoodSubstitution>,
    /** Table des aliments ; null tant qu'elle n'est pas chargée (codes affichés tels quels). */
    foodTable: FoodTable?,
    onCancel: (textKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        if (substitutions.isEmpty()) {
            Text(
                "Aucun aliment de remplacement. Dans le détail des calories, « Choisir l'aliment » " +
                    "sur un ingrédient non reconnu en ajoute un, valable pour toutes les recettes.",
                fontSize = 14.sp, color = Color(0xFF6B7280),
                modifier = Modifier.padding(16.dp).testTag("substitutions_empty")
            )
        } else {
        // Pas de `return@Column` ici : un retour anticipé dans un Column casse Compose (SlotTable)
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("substitutions_list"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    "Valables pour toutes les recettes, existantes et futures.",
                    fontSize = 13.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(substitutions, key = { it.textKey }) { s ->
                val food = foodTable?.food(s.foodCode)
                Row(
                    Modifier.fillMaxWidth().testTag("substitution_${s.textKey}").semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("« ${s.textKey} »", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                food != null -> "→ ${food.name}"
                                foodTable == null -> "→ aliment ${s.foodCode}"
                                else -> "→ aliment introuvable dans la table (ignoré)"
                            },
                            fontSize = 13.sp, color = if (food == null && foodTable != null) Color(0xFF8A5A00) else Color(0xFF6B7280)
                        )
                    }
                    TextButton(
                        onClick = { onCancel(s.textKey) },
                        modifier = Modifier.testTag("cancel_substitution_${s.textKey}")
                    ) { Text("Annuler", color = Color(0xFF7A2CF6)) }
                }
                Divider()
            }
        }
        }
    }
}
