package com.example.recipelibrary.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recipe entity v2
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "ingredients")
    val ingredients: List<String> = emptyList(),

    @ColumnInfo(name = "steps")
    val steps: List<String> = emptyList(),

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /** Nombre de parts, 1 à 100 ; null = inconnu. */
    @ColumnInfo(name = "servings")
    val servings: Int? = null,

    /** Origine de [servings] : [SERVINGS_USER] ou [SERVINGS_SOURCE] ; null si [servings] est null. */
    @ColumnInfo(name = "servings_source")
    val servingsSource: String? = null,

    /** Poids par ligne d'ingrédients (clé = texte exact de la ligne). */
    @ColumnInfo(name = "ingredient_weights")
    val ingredientWeights: Map<String, IngredientWeight> = emptyMap()
) {
    companion object {
        const val SERVINGS_USER = "USER"
        const val SERVINGS_SOURCE = "SOURCE"
        const val MIN_SERVINGS = 1
        const val MAX_SERVINGS = 100
    }
}
