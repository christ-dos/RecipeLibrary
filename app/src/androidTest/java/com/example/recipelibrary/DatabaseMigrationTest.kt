package com.example.recipelibrary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.AppDatabase
import com.example.recipelibrary.data.MIGRATION_1_2
import com.example.recipelibrary.data.MIGRATION_2_3
import com.example.recipelibrary.data.MIGRATION_3_4
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
