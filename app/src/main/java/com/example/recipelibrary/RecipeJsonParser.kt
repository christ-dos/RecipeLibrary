package com.example.recipelibrary

import com.example.recipelibrary.data.Recipe
import com.google.gson.stream.JsonReader
import java.io.StringReader
import java.util.UUID

internal fun parseRecipeJson(rawText: String): Recipe? {
    return try {
        val cleaned = rawText.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val start = cleaned.indexOf('{')
        val end   = cleaned.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        val json = cleaned.substring(start, end + 1)

        val jsonReader = JsonReader(StringReader(json))
        jsonReader.isLenient = true
        val obj = com.google.gson.JsonParser.parseReader(jsonReader).asJsonObject

        fun safeStr(el: com.google.gson.JsonElement?): String? = when {
            el == null || el.isJsonNull -> null
            el.isJsonPrimitive -> el.asString.trim().ifEmpty { null }
            else -> null
        }

        val rawTitle = safeStr(obj.get("title")) ?: "Recette importée"
        val title = if (rawTitle == rawTitle.uppercase()) rawTitle.lowercase().replaceFirstChar { it.uppercase() } else rawTitle

        val ingredients = obj.getAsJsonArray("ingredients")?.mapNotNull { el ->
            if (el.isJsonObject) {
                val o = el.asJsonObject
                val name = safeStr(o.get("name")) ?: return@mapNotNull null
                val unit = safeStr(o.get("unit")) ?: ""
                val qtyEl = o.get("quantity")
                val qty = when {
                    qtyEl == null || qtyEl.isJsonNull -> safeStr(o.get("originalQuantity"))
                    qtyEl.isJsonObject -> {
                        val min = safeStr(qtyEl.asJsonObject.get("min")) ?: ""
                        val max = safeStr(qtyEl.asJsonObject.get("max")) ?: ""
                        safeStr(o.get("originalQuantity")) ?: "$min-$max".trimEnd('-').ifEmpty { null }
                    }
                    else -> safeStr(qtyEl)
                }
                buildString {
                    if (!qty.isNullOrBlank()) { append(qty); if (unit.isNotBlank()) append(" $unit"); append(" ") }
                    append(name)
                    safeStr(o.get("note"))?.let { append(" ($it)") }
                }.trim()
            } else safeStr(el)
        } ?: emptyList()

        val steps = (obj.getAsJsonArray("instructions") ?: obj.getAsJsonArray("steps"))
            ?.mapNotNull { el ->
                if (el.isJsonObject) {
                    val o = el.asJsonObject
                    safeStr(o.get("description")) ?: safeStr(o.get("text"))
                } else safeStr(el)
            } ?: emptyList()

        val tags      = obj.getAsJsonArray("tags")?.mapNotNull { safeStr(it)?.lowercase() } ?: emptyList()
        val sourceUrl = safeStr(obj.get("sourceUrl")) ?: safeStr(obj.getAsJsonObject("source")?.get("url"))
        val imageUrl  = safeStr(obj.get("imageUrl")) ?: safeStr(obj.get("image")) ?: safeStr(obj.get("thumbnail"))
                     ?: safeStr(obj.get("photo")) ?: safeStr(obj.get("coverImage"))
        val notes = obj.get("notes")?.let { n ->
            when {
                n.isJsonNull  -> null
                n.isJsonArray -> n.asJsonArray.mapNotNull { safeStr(it) }.joinToString("\n").ifEmpty { null }
                else          -> safeStr(n)
            }
        }

        val now = System.currentTimeMillis()
        Recipe(
            id          = UUID.randomUUID().toString(),
            title       = title,
            sourceUrl   = sourceUrl,
            description = safeStr(obj.get("description")),
            ingredients = ingredients,
            steps       = steps,
            category    = safeStr(obj.get("category")),
            tags        = tags,
            notes       = notes,
            imageUrl    = imageUrl,
            createdAt   = now,
            updatedAt   = now
        )
    } catch (e: Exception) {
        null
    }
}
