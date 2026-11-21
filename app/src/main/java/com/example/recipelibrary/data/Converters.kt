package com.example.recipelibrary.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type
    private val weightsType = object : TypeToken<Map<String, IngredientWeight?>>() {}.type

    @TypeConverter
    @JvmStatic
    fun fromList(value: List<String>?): String? = if (value == null) null else gson.toJson(value)

    @TypeConverter
    @JvmStatic
    fun toList(value: String?): List<String>? = if (value == null) null else gson.fromJson(value, listType)

    /** Carte vide → « {} » (la colonne est NOT NULL DEFAULT '{}'). */
    @TypeConverter
    @JvmStatic
    fun fromWeights(value: Map<String, IngredientWeight>?): String = gson.toJson(value ?: emptyMap<String, IngredientWeight>())

    /** NULL, JSON invalide ou entrées hors règles → ignorés, jamais d'exception. */
    @TypeConverter
    @JvmStatic
    fun toWeights(value: String?): Map<String, IngredientWeight> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            val raw: Map<String, IngredientWeight?>? = gson.fromJson(value, weightsType)
            raw.orEmpty().filter { (k, w) -> k.isNotBlank() && IngredientWeight.isValid(w) }
                .mapValues { it.value!! }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
