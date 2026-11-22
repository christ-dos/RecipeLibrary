package com.example.recipelibrary.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE recipes ADD COLUMN source_url TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN description TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN ingredients TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN category TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN tags TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN notes TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN updated_at INTEGER DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE recipes ADD COLUMN image_url TEXT")
    }
}

// Version bump sans changement de schéma (hash Room interne)
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {}
}

// File d'attente des liens à importer plus tard. Ajout d'une table seulement :
// la table recipes n'est pas touchée.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_imports` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`url` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`recipe_json` TEXT, " +
                "`error` TEXT, " +
                "`created_at` INTEGER NOT NULL)"
        )
    }
}

// Calories par part : ajout de colonnes seulement, aucune donnée existante modifiée.
// ingredient_weights est NOT NULL DEFAULT '{}' (le champ Kotlin n'est pas nullable) :
// les recettes existantes reçoivent « {} », c'est-à-dire aucune entrée.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE recipes ADD COLUMN servings INTEGER")
        database.execSQL("ALTER TABLE recipes ADD COLUMN servings_source TEXT")
        database.execSQL("ALTER TABLE recipes ADD COLUMN ingredient_weights TEXT NOT NULL DEFAULT '{}'")
    }
}

// Menus et liste de courses : trois nouvelles tables, aucune table existante touchée.
// Schéma identique à celui que Room génère pour MenuEntry, ShoppingCheck et ShoppingExtra.
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `menu_entries` (`id` TEXT NOT NULL, `recipe_id` TEXT NOT NULL, " +
                "`servings` INTEGER, `times` INTEGER NOT NULL DEFAULT 1, `day` INTEGER, `position` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_entries_recipe_id` ON `menu_entries` (`recipe_id`)")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `shopping_checks` (`item_key` TEXT NOT NULL, `checked_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`item_key`))"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `shopping_extras` (`id` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                "`checked` INTEGER NOT NULL DEFAULT 0, `position` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

// Aliments de remplacement (spec 001, story 6) : une nouvelle table, aucune table existante touchée.
// Schéma identique à celui que Room génère pour FoodSubstitution.
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_substitutions` (`text_key` TEXT NOT NULL, `food_code` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, PRIMARY KEY(`text_key`))"
        )
    }
}

/** Spec 003 : favoris Chronodrive et produit retenu par article ; aucune table existante touchée. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `chronodrive_products` (`product_id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                "`brand` TEXT, `aisle` TEXT NOT NULL, `still_favorite` INTEGER NOT NULL DEFAULT 1, " +
                "`imported_at` INTEGER NOT NULL, PRIMARY KEY(`product_id`))"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `product_choices` (`item_key` TEXT NOT NULL, `product_id` INTEGER NOT NULL, " +
                "`chosen_at` INTEGER NOT NULL, PRIMARY KEY(`item_key`))"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_product_choices_product_id` ON `product_choices` (`product_id`)"
        )
    }
}
