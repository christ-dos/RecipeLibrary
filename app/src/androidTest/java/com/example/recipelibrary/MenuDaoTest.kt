package com.example.recipelibrary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.AppDatabase
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.RecipeRepository
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.MenuRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Menu, coches, ajouts à la main et suppression de recette (spec 002, T009). Base en mémoire. */
@RunWith(AndroidJUnit4::class)
class MenuDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var menu: MenuRepository
    private lateinit var recipes: RecipeRepository

    private val crepes = Recipe(id = "crepes", title = "Crêpes", servings = 4, servingsSource = Recipe.SERVINGS_USER, ingredients = listOf("250 g de farine"))
    private val quiche = Recipe(id = "quiche", title = "Quiche", ingredients = listOf("3 œufs"))

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        menu = MenuRepository(db.menuDao())
        recipes = RecipeRepository(db.recipeDao(), db)
        db.recipeDao().upsert(crepes)
        db.recipeDao().upsert(quiche)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun entriesKeepInsertionOrder_andSameRecipeTwice() = runBlocking {
        menu.add(crepes, 4)
        menu.add(quiche, null)
        menu.add(crepes, 8)
        val e = db.menuDao().entries()
        assertEquals(listOf("crepes", "quiche", "crepes"), e.map { it.recipeId })
        assertEquals(listOf(0, 1, 2), e.map { it.position })
        assertEquals(listOf(4, null, 8), e.map { it.servings })
        assertEquals(listOf(1, 1, 1), e.map { it.times })
        assertEquals(2, db.menuDao().countForRecipe("crepes"))
    }

    @Test
    fun servingsTimesAndDay_validatedAndStored() = runBlocking {
        menu.add(quiche, null)
        val id = db.menuDao().entries().single().id
        menu.setTimes(id, 3)
        menu.setTimes(id, 11)       // hors bornes : ignoré
        menu.setDay(id, 5)
        menu.setDay(id, 8)          // hors bornes : ignoré
        menu.setServings(id, 0)     // hors bornes : ignoré
        val e = db.menuDao().entries().single()
        assertEquals(MenuEntry(id = id, recipeId = "quiche", servings = null, times = 3, day = 5, position = 0, createdAt = e.createdAt), e)
        menu.setDay(id, null)
        assertEquals(null, db.menuDao().entries().single().day)
    }

    @Test
    fun clearMenu_removesEntriesAndChecks_keepsExtras() = runBlocking {
        menu.add(crepes, 4)
        menu.check("food:9410")
        menu.addExtra("  papier toilette  ")
        menu.addExtra("   ")       // vide : ignoré
        menu.clear()
        assertEquals(0, db.menuDao().entries().size)
        assertEquals(emptyList<String>(), db.menuDao().observeCheckedKeys().first())
        assertEquals(listOf("papier toilette"), db.menuDao().observeExtras().first().map { it.text })
    }

    @Test
    fun checkAndUncheck() = runBlocking {
        menu.check("food:22000")
        menu.check("food:22000")
        menu.check("txt:piment d espelette")
        assertEquals(setOf("food:22000", "txt:piment d espelette"), db.menuDao().observeCheckedKeys().first().toSet())
        menu.uncheck("food:22000")
        assertEquals(listOf("txt:piment d espelette"), db.menuDao().observeCheckedKeys().first())
    }

    @Test
    fun deletingRecipe_removesItsEntriesOnly_inOneTransaction() = runBlocking {
        menu.add(crepes, 4)
        menu.add(quiche, null)
        menu.add(crepes, 8)
        assertEquals(2, recipes.menuCount("crepes"))

        recipes.delete("crepes")

        assertEquals(null, db.recipeDao().getById("crepes"))
        assertEquals(quiche, db.recipeDao().getById("quiche"))
        assertEquals(listOf("quiche"), db.menuDao().entries().map { it.recipeId })
        assertEquals(0, recipes.menuCount("crepes"))
    }

    @Test
    fun extras_editCheckDelete() = runBlocking {
        menu.addExtra("éponges")
        val id = db.menuDao().observeExtras().first().single().id
        menu.setExtraText(id, "2 éponges")
        menu.setExtraChecked(id, true)
        val x = db.menuDao().observeExtras().first().single()
        assertEquals("2 éponges", x.text)
        assertEquals(true, x.checked)
        menu.removeExtra(id)
        assertEquals(0, db.menuDao().observeExtras().first().size)
    }

    @Test
    fun checksOlderThanThreeDays_expire_recentKept() = runBlocking {
        val now = 10_000_000_000L
        val day = 24 * 3_600_000L
        db.menuDao().check("food:old", at = now - 3 * day - 1)
        db.menuDao().check("food:recent", at = now - 2 * day)
        menu.expireChecks(now)
        assertEquals(listOf("food:recent"), db.menuDao().observeCheckedKeys().first())
    }
}
