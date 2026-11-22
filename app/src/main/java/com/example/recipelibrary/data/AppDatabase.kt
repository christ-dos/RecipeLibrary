package com.example.recipelibrary.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Recipe::class, PendingImport::class,
        com.example.recipelibrary.data.menu.MenuEntry::class,
        com.example.recipelibrary.data.menu.ShoppingCheck::class,
        com.example.recipelibrary.data.menu.ShoppingExtra::class,
        FoodSubstitution::class
    ],
    version = 8
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun pendingImportDao(): PendingImportDao
    abstract fun menuDao(): com.example.recipelibrary.data.menu.MenuDao
    abstract fun foodSubstitutionDao(): FoodSubstitutionDao
}
