package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.nutrition.FoodTable
import java.io.File

/** Vraie table embarquée, lue depuis les assets (les tests JVM tournent dans le dossier `app`). */
object TestFoods {
    val file = File("src/main/assets/nutrition/foods.json")
    val table: FoodTable by lazy { FoodTable.fromJson(file.readText(Charsets.UTF_8)) }
}
