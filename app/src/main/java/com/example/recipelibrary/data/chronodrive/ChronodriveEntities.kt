package com.example.recipelibrary.data.chronodrive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Produit des favoris Chronodrive de l'utilisatrice (spec 003, data-model.md). Jamais supprimé
 * par un import : absent du dernier fichier, il passe à [stillFavorite] = false.
 */
@Entity(tableName = "chronodrive_products")
data class ChronodriveProduct(
    /** Numéro du site et de l'application (ex. 365920) ; deux produits peuvent avoir le même nom. */
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Long,
    /** Nom sans la marque, déduit du slug (« viande hachee pur boeuf 5 mg »). */
    val name: String,
    /** Marque (« auchan ») ; null pour un produit sans marque (« banane »). */
    val brand: String? = null,
    /** Titre du rayon dans le fichier (« Epicerie salée »). */
    val aisle: String,
    @ColumnInfo(name = "still_favorite", defaultValue = "1") val stillFavorite: Boolean = true,
    @ColumnInfo(name = "imported_at") val importedAt: Long
)

/**
 * Produit retenu pour un article de la liste de courses. Clé = `food:<code>`, `sub:<texte>` ou
 * `txt:<texte normalisé>` (research R4) ; pas de clé étrangère : le choix survit même si le
 * produit n'est plus en favori.
 */
@Entity(
    tableName = "product_choices",
    indices = [Index(value = ["product_id"], name = "index_product_choices_product_id")]
)
data class ProductChoice(
    @PrimaryKey @ColumnInfo(name = "item_key") val itemKey: String,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "chosen_at") val chosenAt: Long = System.currentTimeMillis()
)
