package com.example.recipelibrary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.ProductMatcher

private val PickerViolet = Color(0xFF7A2CF6)
private val PickerGrey = Color(0xFF6B7280)

/** Une ligne de produit : nom, marque, rayon et numéro (deux « banane » se distinguent par leur numéro). */
@Composable
internal fun ProductLine(product: ChronodriveProduct, onClick: () -> Unit, showAisle: Boolean = true) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics(mergeDescendants = true) {}.testTag("product_${product.productId}")
    ) {
        Text(product.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        val details = listOfNotNull(product.brand, if (showAisle) product.aisle else null, "n° ${product.productId}") +
            (if (!product.stillFavorite) listOf("plus en favori") else emptyList())
        Text(details.joinToString(" · "), fontSize = 12.sp, color = PickerGrey)
    }
}

/**
 * Plusieurs favoris correspondent à l'article (FR-004) : l'utilisatrice choisit, rien n'est
 * choisi au hasard. « Aucun de ceux-là » garde le comportement de la feature 002.
 */
@Composable
fun ProductPickerDialog(
    itemName: String,
    candidates: List<ChronodriveProduct>,
    onPick: (ChronodriveProduct) -> Unit,
    onNone: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Quel produit pour « $itemName » ?") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp).testTag("product_picker")) {
                items(candidates, key = { it.productId }) { p ->
                    ProductLine(p, onClick = { onPick(p) })
                    Divider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNone, modifier = Modifier.testTag("picker_none")) { Text("Aucun de ceux-là", color = PickerViolet) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("picker_cancel")) { Text("Annuler", color = PickerGrey) }
        }
    )
}

/**
 * Tous les favoris importés, par rayon, avec recherche par texte (US3) : sert à attribuer ou
 * changer à la main le produit d'un article. N'ouvre pas Chronodrive.
 */
@Composable
fun FavoritesPickerDialog(
    title: String,
    favorites: List<ChronodriveProduct>,
    onPick: (ChronodriveProduct) -> Unit,
    onCancel: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query, favorites) {
        val words = ProductMatcher.significantWords(query)
        favorites.filter { p -> words.isEmpty() || ProductMatcher.candidates(query, listOf(p)).isNotEmpty() ||
            ProductMatcher.significantWords(listOfNotNull(p.brand, p.name).joinToString(" ")).containsAll(words) }
            .sortedWith(compareBy({ it.aisle }, { it.name }, { it.productId }))
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column {
                if (favorites.isEmpty()) {
                    Text(
                        "Importe d'abord tes favoris Chronodrive (menu ⋮ de l'onglet Courses).",
                        color = PickerGrey, modifier = Modifier.testTag("favorites_empty")
                    )
                } else {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        placeholder = { Text("Rechercher") },
                        modifier = Modifier.fillMaxWidth().testTag("favorites_search")
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp).testTag("favorites_picker")) {
                        shown.groupBy { it.aisle }.forEach { (aisle, ps) ->
                            item(key = "aisle_$aisle") {
                                Text(aisle, color = PickerViolet, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 10.dp))
                            }
                            items(ps, key = { it.productId }) { p ->
                                ProductLine(p, onClick = { onPick(p) }, showAisle = false)
                                Divider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("favorites_cancel")) { Text("Annuler", color = PickerGrey) }
        }
    )
}
