package com.example.recipelibrary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.AppDatabase
import com.example.recipelibrary.data.MIGRATION_1_2
import com.example.recipelibrary.data.MIGRATION_2_3
import com.example.recipelibrary.data.MIGRATION_3_4
import com.example.recipelibrary.data.MIGRATION_4_5
import com.example.recipelibrary.data.MIGRATION_5_6
import com.example.recipelibrary.data.IngredientWeight
import com.example.recipelibrary.data.PendingImport
import com.example.recipelibrary.data.Recipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de non-régression base de données.
 *
 * Ces tests vérifient que :
 * 1. La DB s'ouvre sans erreur au schéma v4
 * 2. Une recette insérée survit à la fermeture/réouverture (pas de wipe silencieux)
 * 3. Tous les champs sont conservés fidèlement (converters, nullables)
 *
 * Pour des tests de migration pas-à-pas (v1→v2→v3→v4), ajouter dans build.gradle.kts :
 *   ksp { arg("room.schemaLocation", "$projectDir/schemas") }
 * puis utiliser MigrationTestHelper après le premier build qui génère les JSON de schéma.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    // ── 1. Le schéma v4 s'ouvre et accepte une recette complète ──────────────

    @Test
    fun schema_v4_insertsAndReadsFullRecipe() = runBlocking {
        val recipe = Recipe(
            id          = "test-001",
            title       = "Tarte aux pommes",
            sourceUrl   = "https://youtu.be/abc",
            description = "Classique",
            ingredients = listOf("3 pommes", "200g farine"),
            steps       = listOf("Éplucher", "Cuire 30 min"),
            category    = "Desserts",
            tags        = listOf("fruits", "facile"),
            notes       = "Servir tiède",
            imageUrl    = "https://example.com/img.jpg",
            createdAt   = 1000L,
            updatedAt   = 2000L
        )
        db.recipeDao().upsert(recipe)

        val loaded = db.recipeDao().getById("test-001")
        assertNotNull("La recette doit exister après insertion", loaded)
        assertEquals("Tarte aux pommes",        loaded!!.title)
        assertEquals("https://youtu.be/abc",    loaded.sourceUrl)
        assertEquals("Classique",               loaded.description)
        assertEquals(listOf("3 pommes", "200g farine"), loaded.ingredients)
        assertEquals(listOf("Éplucher", "Cuire 30 min"), loaded.steps)
        assertEquals("Desserts",                loaded.category)
        assertEquals(listOf("fruits", "facile"), loaded.tags)
        assertEquals("Servir tiède",            loaded.notes)
        assertEquals("https://example.com/img.jpg", loaded.imageUrl)
        assertEquals(1000L,                     loaded.createdAt)
    }

    // ── 2. Les listes vides sont conservées (pas null après Converters) ───────

    @Test
    fun schema_v4_emptyListsRoundtrip() = runBlocking {
        val recipe = Recipe(
            id          = "test-002",
            title       = "Vide",
            ingredients = emptyList(),
            steps       = emptyList(),
            tags        = emptyList()
        )
        db.recipeDao().upsert(recipe)

        val loaded = db.recipeDao().getById("test-002")
        assertNotNull(loaded)
        assertEquals(emptyList<String>(), loaded!!.ingredients)
        assertEquals(emptyList<String>(), loaded.steps)
        assertEquals(emptyList<String>(), loaded.tags)
    }

    // ── 3. Les champs nullable retournent null (pas une chaîne vide) ──────────

    @Test
    fun schema_v4_nullableFieldsAreNull() = runBlocking {
        val recipe = Recipe(id = "test-003", title = "Minimal")
        db.recipeDao().upsert(recipe)

        val loaded = db.recipeDao().getById("test-003")
        assertNotNull(loaded)
        assertEquals(null, loaded!!.sourceUrl)
        assertEquals(null, loaded.description)
        assertEquals(null, loaded.category)
        assertEquals(null, loaded.notes)
        assertEquals(null, loaded.imageUrl)
    }

    // ── 4. Plusieurs recettes — getAll retourne toutes (flux) ────────────────

    @Test
    fun schema_v4_getAll_returnsAllRecipes() = runBlocking {
        db.recipeDao().upsert(Recipe(id = "r1", title = "Recette 1"))
        db.recipeDao().upsert(Recipe(id = "r2", title = "Recette 2"))
        db.recipeDao().upsert(Recipe(id = "r3", title = "Recette 3"))

        val all = db.recipeDao().getAll().first()
        assertEquals(3, all.size)
    }

    // ── 5. Suppression — deleteById retire la recette ────────────────────────

    @Test
    fun schema_v4_delete_removesRecipe() = runBlocking {
        db.recipeDao().upsert(Recipe(id = "r-del", title = "À supprimer"))
        db.recipeDao().deleteById("r-del")

        assertEquals(null, db.recipeDao().getById("r-del"))
    }

    // ── 7. Migration 4 → 5 : les recettes existantes survivent, la file d'attente apparaît ──

    @Test
    fun migration_4_to_5_keepsRecipes_andCreatesQueueTable() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test-v4.db"
        ctx.deleteDatabase(name)

        // Base au format v4, telle que Room la créait avant la file d'attente
        ctx.openOrCreateDatabase(name, android.content.Context.MODE_PRIVATE, null).use { raw ->
            raw.execSQL("CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `source_url` TEXT, `description` TEXT, `ingredients` TEXT NOT NULL, `steps` TEXT NOT NULL, `category` TEXT, `tags` TEXT NOT NULL, `notes` TEXT, `image_url` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            raw.execSQL(
                "INSERT INTO recipes (id, title, ingredients, steps, category, tags, created_at, updated_at) " +
                    "VALUES ('keep-1', 'Quiche lorraine', '[\"3 oeufs\"]', '[\"Cuire\"]', 'Tartes et quiches', '[]', 1000, 2000)"
            )
            raw.version = 4
        }

        val migrated = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            val kept = migrated.recipeDao().getById("keep-1")
            assertNotNull("La recette existante doit survivre à la migration", kept)
            assertEquals("Quiche lorraine", kept!!.title)
            assertEquals(listOf("3 oeufs"), kept.ingredients)
            assertEquals("Tartes et quiches", kept.category)

            val dao = migrated.pendingImportDao()
            val id = dao.insert(PendingImport(url = "https://insta/reel/1"))
            assertEquals(1, dao.countWaiting())
            assertEquals("https://insta/reel/1", dao.nextWaiting()?.url)
            dao.update(dao.nextWaiting()!!.copy(status = PendingImport.READY, recipeJson = "{}"))
            assertEquals(0, dao.countWaiting())
            dao.deleteById(id)
            assertEquals(null, dao.findByUrl("https://insta/reel/1"))
        } finally {
            migrated.close()
            ctx.deleteDatabase(name)
        }
    }

    // ── 8. Migration 5 → 6 : recettes relues à l'identique, nouvelles colonnes vides ──

    @Test
    fun migration_5_to_6_keepsRecipesIdentical_newColumnsEmpty() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test-v5.db"
        ctx.deleteDatabase(name)

        // Base au format v5, telle que Room la créait avant les calories
        ctx.openOrCreateDatabase(name, android.content.Context.MODE_PRIVATE, null).use { raw ->
            raw.execSQL("CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `source_url` TEXT, `description` TEXT, `ingredients` TEXT NOT NULL, `steps` TEXT NOT NULL, `category` TEXT, `tags` TEXT NOT NULL, `notes` TEXT, `image_url` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            raw.execSQL("CREATE TABLE IF NOT EXISTS `pending_imports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `status` TEXT NOT NULL, `recipe_json` TEXT, `error` TEXT, `created_at` INTEGER NOT NULL)")
            raw.execSQL(
                "INSERT INTO recipes VALUES ('v5-full', 'Tarte aux pommes', 'https://youtu.be/abc', 'Classique', " +
                    "'[\"3 pommes\",\"200 g de farine\"]', '[\"Éplucher\",\"Cuire 30 min\"]', 'Desserts', " +
                    "'[\"fruits\",\"facile\"]', 'Servir tiède', 'https://example.com/img.jpg', 1000, 2000)"
            )
            raw.execSQL(
                "INSERT INTO recipes (id, title, ingredients, steps, tags, created_at, updated_at) " +
                    "VALUES ('v5-min', 'Minimal', '[]', '[]', '[]', 3000, 4000)"
            )
            raw.execSQL("INSERT INTO pending_imports (url, status, created_at) VALUES ('https://insta/reel/2', 'WAITING', 5000)")
            raw.version = 5
        }

        val migrated = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            val full = migrated.recipeDao().getById("v5-full")
            assertEquals(
                Recipe(
                    id = "v5-full", title = "Tarte aux pommes", sourceUrl = "https://youtu.be/abc",
                    description = "Classique", ingredients = listOf("3 pommes", "200 g de farine"),
                    steps = listOf("Éplucher", "Cuire 30 min"), category = "Desserts",
                    tags = listOf("fruits", "facile"), notes = "Servir tiède",
                    imageUrl = "https://example.com/img.jpg", createdAt = 1000L, updatedAt = 2000L,
                    servings = null, servingsSource = null, ingredientWeights = emptyMap()
                ),
                full
            )
            val min = migrated.recipeDao().getById("v5-min")
            assertNotNull(min)
            assertEquals("Minimal", min!!.title)
            assertEquals(3000L, min.createdAt)
            assertEquals(null, min.servings)
            assertEquals(emptyMap<String, IngredientWeight>(), min.ingredientWeights)
            assertEquals(2, migrated.recipeDao().getAll().first().size)
            assertEquals("https://insta/reel/2", migrated.pendingImportDao().nextWaiting()?.url)
        } finally {
            migrated.close()
            ctx.deleteDatabase(name)
        }
    }

    // ── 9. Parts et poids : écrits puis relus à l'identique ─────────────────

    @Test
    fun schema_v6_servingsAndWeightsRoundtrip() = runBlocking {
        val weights = mapOf(
            "1 noix de beurre" to IngredientWeight(10.0, IngredientWeight.USER),
            "2 pommes" to IngredientWeight(300.0, IngredientWeight.PC),
            "1 pincée de safran" to IngredientWeight(null, IngredientWeight.PC)
        )
        db.recipeDao().upsert(
            Recipe(id = "w1", title = "Avec poids", servings = 6, servingsSource = Recipe.SERVINGS_SOURCE, ingredientWeights = weights)
        )
        val loaded = db.recipeDao().getById("w1")!!
        assertEquals(6, loaded.servings)
        assertEquals(Recipe.SERVINGS_SOURCE, loaded.servingsSource)
        assertEquals(weights, loaded.ingredientWeights)
    }

    // ── 6. Upsert (REPLACE) — mise à jour préserve l'id ─────────────────────

    @Test
    fun schema_v4_upsert_updatesExistingRecipe() = runBlocking {
        db.recipeDao().upsert(Recipe(id = "r-upd", title = "Avant"))
        db.recipeDao().upsert(Recipe(id = "r-upd", title = "Après", notes = "modifié"))

        val loaded = db.recipeDao().getById("r-upd")
        assertEquals("Après",   loaded!!.title)
        assertEquals("modifié", loaded.notes)
    }
}
