package com.example.recipelibrary

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.AppDatabase
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.chronodrive.ChronodriveRepository
import com.example.recipelibrary.data.chronodrive.ChronodriveResolution
import com.example.recipelibrary.data.chronodrive.ImportResult
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.ShoppingExtra
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Favoris et choix Chronodrive en base (spec 003, T016) : import atomique, jamais de suppression. */
@RunWith(AndroidJUnit4::class)
class ChronodriveDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ChronodriveRepository
    private var now = 1000L

    @Before
    fun createDb() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = ChronodriveRepository(db) { now }
        runBlocking {
            db.recipeDao().upsert(Recipe(id = "r1", title = "Banana bread", ingredients = listOf("3 bananes"), steps = emptyList(), createdAt = 1, updatedAt = 2))
            db.menuDao().appendEntry(MenuEntry(id = "m1", recipeId = "r1", servings = 4))
            db.menuDao().check("food:13005", 3)
            db.menuDao().appendExtra(ShoppingExtra(id = "x1", text = "Papier toilette"))
        }
    }

    @After
    fun closeDb() = db.close()

    private val first = "## Fruits & Légumes (3)\n365920 banane;305254 banane;3416 daregal--ciboulette\n"

    private fun message(r: ImportResult) = (r as ImportResult.Done).report.message

    @Test fun import_thenReimport_addsUpdatesAndMarksGone_neverDeletes() = runBlocking {
        assertEquals("3 produits Chronodrive importés", message(repo.importFavorites(first)))
        now = 2000
        val second = "## Fruits (2)\n365920 banane;3416 daregal--ciboulette-fraiche\n## Epicerie (1)\n392862 auchan--lentilles-blondes"
        assertEquals("1 ajouté, 2 déjà connus, 1 plus en favori", message(repo.importFavorites(second)))
        val dao = db.chronodriveDao()
        assertEquals(4, dao.products().size)
        assertFalse(dao.getById(305254)!!.stillFavorite)
        assertEquals("ciboulette fraiche", dao.getById(3416)!!.name)
        assertEquals("Fruits", dao.getById(3416)!!.aisle)
        assertEquals(2000L, dao.getById(3416)!!.importedAt)
    }

    @Test fun invalidFile_changesNothing() = runBlocking {
        repo.importFavorites(first)
        val before = db.chronodriveDao().products()
        assertEquals(ImportResult.Invalid, repo.importFavorites("ceci n'est pas un relevé"))
        assertEquals(ImportResult.Invalid, repo.importFavorites(""))
        assertEquals(before, db.chronodriveDao().products())
    }

    @Test fun choice_survivesReimportWhereItsProductDisappeared() = runBlocking {
        repo.importFavorites(first)
        repo.setChoice("food:13005", 305254)
        repo.importFavorites("## Fruits (1)\n365920 banane")
        assertEquals(305254L, db.chronodriveDao().getChoice("food:13005")?.productId)
        val r = repo.resolveChrono("food:13005", "banane")
        assertEquals(305254L, (r as ChronodriveResolution.Retained).product.productId)
    }

    @Test fun replaceForgetAndCount() = runBlocking {
        repo.importFavorites(first)
        repo.setChoice("food:13005", 365920)
        repo.setChoice("txt:banane", 365920)
        assertEquals(2, db.chronodriveDao().countForProduct(365920))
        repo.setChoice("food:13005", 305254)
        assertEquals(305254L, db.chronodriveDao().getChoice("food:13005")?.productId)
        assertEquals(1, db.chronodriveDao().countForProduct(365920))
        repo.forgetChoice("food:13005")
        assertNull(db.chronodriveDao().getChoice("food:13005"))
        assertEquals(3, db.chronodriveDao().products().size)
    }

    @Test fun resolve_singleIsSaved_severalIsNot() = runBlocking {
        repo.importFavorites(first)
        assertTrue(repo.resolveChrono("txt:ciboulette", "ciboulette") is ChronodriveResolution.Single)
        assertEquals(3416L, db.chronodriveDao().getChoice("txt:ciboulette")?.productId)
        assertTrue(repo.resolveChrono("food:13005", "bananes") is ChronodriveResolution.Several)
        assertNull(db.chronodriveDao().getChoice("food:13005"))
        assertTrue(repo.resolveChrono("txt:ciboulette", "ciboulette") is ChronodriveResolution.Retained)
    }

    @Test fun choicesFlow_joinsProduct_unknownTolerated() = runBlocking {
        repo.importFavorites(first)
        repo.setChoice("food:13005", 365920)
        repo.setChoice("txt:x", 999)
        val choices = repo.choices.first().associateBy { it.choice.itemKey }
        assertEquals("banane", choices["food:13005"]?.product?.name)
        assertNull(choices["txt:x"]?.product)
    }

    @Test fun failureMidWrite_rollsBackEverything() = runBlocking {
        repo.importFavorites(first)
        val before = db.chronodriveDao().products()
        try {
            db.withTransaction {
                db.chronodriveDao().upsertProducts(listOf(before.first().copy(name = "modifié")))
                error("panne")
            }
        } catch (_: IllegalStateException) { }
        assertEquals(before, db.chronodriveDao().products())
    }

    @Test fun existingTablesUntouched() = runBlocking {
        repo.importFavorites(first)
        repo.importFavorites("## Fruits (1)\n365920 banane")
        repo.setChoice("food:13005", 365920)
        repo.forgetChoice("food:13005")
        assertEquals("Banana bread", db.recipeDao().getById("r1")?.title)
        assertEquals(listOf("m1"), db.menuDao().entries().map { it.id })
        assertEquals(listOf("food:13005"), db.menuDao().observeCheckedKeys().first())
        assertEquals(listOf("Papier toilette"), db.menuDao().observeExtras().first().map { it.text })
    }
}
