package com.example.recipelibrary.data.menu

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Recette prévue au menu (spec 002). Ne contient qu'une référence à la recette : la recette
 * n'est jamais copiée. Plusieurs entrées peuvent viser la même recette.
 */
@Entity(tableName = "menu_entries", indices = [Index(value = ["recipe_id"], name = "index_menu_entries_recipe_id")])
data class MenuEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    /** Parts prévues (1 à 100) ; null si la recette n'a ni parts ni estimation : [times] s'applique. */
    val servings: Int? = null,
    /** Nombre de fois la recette (1 à 10), utilisé seulement quand [servings] est null. */
    @ColumnInfo(defaultValue = "1") val times: Int = 1,
    /** 1 = lundi … 7 = dimanche ; null = « Sans jour ». */
    val day: Int? = null,
    val position: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MIN_TIMES = 1
        const val MAX_TIMES = 10
    }
}

/** Article calculé coché (déjà à la maison ou mis au panier), repéré par sa clé. */
@Entity(tableName = "shopping_checks")
data class ShoppingCheck(
    @PrimaryKey @ColumnInfo(name = "item_key") val itemKey: String,
    @ColumnInfo(name = "checked_at") val checkedAt: Long = System.currentTimeMillis()
)

/** Article ajouté à la main, indépendant du menu. */
@Entity(tableName = "shopping_extras")
data class ShoppingExtra(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 1 à 200 caractères ; la quantité éventuelle fait partie du texte. */
    val text: String,
    @ColumnInfo(defaultValue = "0") val checked: Boolean = false,
    val position: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_LENGTH = 200
    }
}
