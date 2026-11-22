package com.example.recipelibrary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.AppDatabase
import com.example.recipelibrary.data.FoodSubstitution
import com.example.recipelibrary.data.Recipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Aliments de remplacement (spec 001, story 6, T046) : base en mémoire, jamais celle du téléphone. */
@RunWith(AndroidJUnit4::class)
class FoodSubstitutionDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsert_list_replace_delete() = runBlocking {
        val dao = db.foodSubstitutionDao()
        dao.upsert(FoodSubstitution("skyr", "19590", createdAt = 1L))
        dao.upsert(FoodSubstitution("creme d'avoine", "19410", createdAt = 2L))
        assertEquals(listOf("creme d'avoine", "skyr"), dao.getAll().map { it.textKey })

        // Un nouveau choix pour le même texte remplace l'ancien.
        dao.upsert(FoodSubstitution("skyr", "19410", createdAt = 3L))
        assertEquals(2, dao.getAll().size)
        assertEquals("19410", dao.getAll().single { it.textKey == "skyr" }.foodCode)

        dao.delete("skyr")
        assertEquals(listOf("creme d'avoine"), dao.observeAll().first().map { it.textKey })
    }

    @Test
    fun cancellingNeverTouchesRecipes() = runBlocking {
        val recipe = Recipe(id = "r", title = "Bowl", ingredients = listOf("150 g de skyr"))
        db.recipeDao().upsert(recipe)
        val dao = db.foodSubstitutionDao()
        dao.upsert(FoodSubstitution("skyr", "19590"))
        dao.delete("skyr")
        assertEquals(recipe, db.recipeDao().getById("r"))
        assertEquals(0, dao.getAll().size)
    }
}
