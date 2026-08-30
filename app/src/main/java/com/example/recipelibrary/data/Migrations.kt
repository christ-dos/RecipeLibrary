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
