package com.example.recipelibrary.data

import com.example.recipelibrary.data.Recipe

object RecipeUtils {
    fun normalizeTags(tags: List<String>): List<String> = tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    fun nowMillis(): Long = System.currentTimeMillis()
}
