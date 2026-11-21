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
