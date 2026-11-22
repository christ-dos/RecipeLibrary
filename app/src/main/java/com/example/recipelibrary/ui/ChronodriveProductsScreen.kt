package com.example.recipelibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipelibrary.data.chronodrive.ChoiceWithProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct

private val ProdViolet = Color(0xFF7A2CF6)
private val ProdVioletLight = Color(0xFFF3ECFF)
private val ProdGrey = Color(0xFF6B7280)

/**
 * « Mes produits Chronodrive » (spec 003, US1 et US3) : import du fichier de favoris, produit
 * retenu pour chaque article (changer, oublier), favoris par rayon. Sans état : tout passe par
 * les callbacks, l'écran ne touche jamais aux recettes.
 */
@Composable
fun ChronodriveProductsScreen(
    products: List<ChronodriveProduct>,
    choices: List<ChoiceWithProduct>,
    /** Libellé lisible d'une clé d'article (ChoiceLabel). */
    labelOf: (String) -> String,
    /** Dernier message d'import (« 124 produits Chronodrive importés », fichier invalide…). */
    message: String?,
    onImport: () -> Unit,
    onChange: (itemKey: String, productId: Long) -> Unit,
    onForget: (itemKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var changeKey by remember { mutableStateOf<String?>(null) }
    changeKey?.let { key ->
        FavoritesPickerDialog(
            title = "Produit pour « ${labelOf(key)} »",
            favorites = products,
            onPick = { onChange(key, it.productId); changeKey = null },
            onCancel = { changeKey = null }
        )
    }

    LazyColumn(modifier.fillMaxSize().testTag("products_list"), contentPadding = PaddingValues(16.dp)) {
        item(key = "import") {
            Column {
                Text(
                    if (products.isEmpty()) "Importe le fichier de tes favoris Chronodrive : l'app ouvrira ensuite directement la fiche de tes produits habituels."
                    else "${products.size} favoris importés. Réimporter le fichier ajoute les nouveaux sans rien perdre.",
                    color = ProdGrey, fontSize = 13.sp, modifier = Modifier.testTag("products_intro")
                )
                Spacer(Modifier.padding(4.dp))
                Button(
                    onClick = onImport, colors = ButtonDefaults.buttonColors(backgroundColor = ProdViolet),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.testTag("import_favorites")
                ) { Text("Importer mes favoris", color = Color.White) }
                if (message != null) {
                    Text(
                        message, color = ProdViolet, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            .background(ProdVioletLight, RoundedCornerShape(8.dp)).padding(10.dp).testTag("import_message")
                    )
                }
            }
        }

        item(key = "choices_title") { SectionTitle("Mes choix") }
        if (choices.isEmpty()) {
            item(key = "choices_empty") {
                Text(
                    "Aucun choix pour l'instant : touche « Chrono » sur un article de la liste de courses.",
                    color = ProdGrey, fontSize = 13.sp, modifier = Modifier.testTag("choices_empty")
                )
            }
        }
        items(choices, key = { "choice_${it.choice.itemKey}" }) { c ->
            val key = c.choice.itemKey
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                .semantics(mergeDescendants = true) {}.testTag("choice_$key"), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(labelOf(key), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    val p = c.product
                    Text(
                        if (p == null) "→ produit n° ${c.choice.productId} (plus dans tes favoris importés)"
                        else "→ ${p.name}" + (p.brand?.let { " · $it" } ?: "") + " · n° ${p.productId}" +
                            (if (!p.stillFavorite) " · plus en favori" else ""),
                        fontSize = 12.sp, color = ProdGrey
                    )
                }
                TextButton(onClick = { changeKey = key }, modifier = Modifier.testTag("change_$key")) { Text("Changer", color = ProdViolet) }
                TextButton(onClick = { onForget(key) }, modifier = Modifier.testTag("forget_$key")) { Text("Oublier", color = ProdGrey) }
            }
            Divider()
        }

        item(key = "favorites_title") { SectionTitle("Mes favoris") }
        products.groupBy { it.aisle }.toSortedMap().forEach { (aisle, ps) ->
            item(key = "fav_aisle_$aisle") {
                Text(aisle, color = ProdViolet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp).testTag("fav_aisle_$aisle"))
            }
            items(ps, key = { "fav_${it.productId}" }) { p ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .semantics(mergeDescendants = true) {}.testTag("fav_${p.productId}")) {
                    Text(p.name, fontSize = 14.sp)
                    Text(
                        listOfNotNull(p.brand, "n° ${p.productId}", if (!p.stillFavorite) "plus en favori" else null).joinToString(" · "),
                        fontSize = 12.sp, color = ProdGrey
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(Modifier.padding(top = 18.dp, bottom = 4.dp)) {
        Spacer(Modifier.width(0.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
    }
}
