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
    val updatedAt: Long = System.currentTimeMillis()
)
