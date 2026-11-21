package com.example.recipelibrary

import com.example.recipelibrary.data.ImportQueue
import com.example.recipelibrary.data.ImportQueueProcessor
import com.example.recipelibrary.data.PendingImport
import com.example.recipelibrary.data.PendingImportDao
import com.example.recipelibrary.data.RecipeCategories
import com.example.recipelibrary.data.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

/** File en mémoire qui reproduit les requêtes du vrai DAO. */
private class FakeQueueDao(vararg items: PendingImport) : PendingImportDao {
    val rows = items.toMutableList()
    override fun observeAll(): Flow<List<PendingImport>> = MutableStateFlow(rows.toList())
    override suspend fun nextWaiting() = rows.filter { it.status == PendingImport.WAITING }.minByOrNull { it.createdAt }
    override suspend fun countWaiting() = rows.count { it.status == PendingImport.WAITING }
    override suspend fun findByUrl(url: String) = rows.firstOrNull { it.url == url }
    override suspend fun insert(item: PendingImport): Long { rows += item; return item.id }
    override suspend fun update(item: PendingImport) { rows.replaceAll { if (it.id == item.id) item else it } }
    override suspend fun deleteById(id: Long) { rows.removeAll { it.id == id } }
    fun get(id: Long) = rows.first { it.id == id }
}

class ImportQueueProcessorTest {

    private fun item(id: Long) = PendingImport(id = id, url = "https://insta/reel/$id", createdAt = id)

    private fun processor(
        dao: PendingImportDao,
        server: String? = "http://192.168.1.44:18642",
        readable: (String) -> Boolean = { true },
        run: suspend (String, String) -> Result<String>
    ) = ImportQueueProcessor(dao, { server }, run, readable)

    @Test
    fun allExtracted_becomeReady_inOrder() = runBlocking {
        val dao = FakeQueueDao(item(1), item(2))
        val order = mutableListOf<String>()
        val report = processor(dao) { url, _ -> order += url; Result.success("""{"title":"$url"}""") }.processAll()

        assertFalse(report.retryLater)
        assertEquals(2, report.readyCount)
        assertEquals(listOf("https://insta/reel/1", "https://insta/reel/2"), order)
        assertEquals(PendingImport.READY, dao.get(1).status)
        assertEquals("""{"title":"https://insta/reel/1"}""", dao.get(1).recipeJson)
    }

    @Test
    fun pcUnreachable_stopsAndRetriesLater_keepingItemsWaiting() = runBlocking {
        val dao = FakeQueueDao(item(1), item(2))
        var calls = 0
        val report = processor(dao) { _, _ -> calls++; Result.failure(ConnectException("ETIMEDOUT")) }.processAll()

        assertTrue(report.retryLater)
        assertEquals(1, calls)
        assertEquals(PendingImport.WAITING, dao.get(1).status)
        assertEquals(PendingImport.WAITING, dao.get(2).status)
    }

    @Test
    fun connectTimeoutOnMobileData_retriesLater() = runBlocking {
        val dao = FakeQueueDao(item(1))
        val report = processor(dao) { _, _ ->
            Result.failure(SocketTimeoutException("failed to connect to /192.168.1.44 (port 18642) after 15000ms"))
        }.processAll()
        assertTrue(report.retryLater)
        assertEquals(PendingImport.WAITING, dao.get(1).status)
    }

    @Test
    fun slowServer_retriesLater() = runBlocking {
        val dao = FakeQueueDao(item(1))
        val report = processor(dao) { _, _ -> Result.failure(SocketTimeoutException("Read timed out")) }.processAll()
        assertTrue(report.retryLater)
        assertEquals(PendingImport.WAITING, dao.get(1).status)
    }

    @Test
    fun extractionError_marksFailedWithReason_andContinues() = runBlocking {
        val dao = FakeQueueDao(item(1), item(2))
        val report = processor(dao) { url, _ ->
            if (url.endsWith("1")) Result.failure(Exception("Pas de recette dans cette vidéo"))
            else Result.success("""{"title":"ok"}""")
        }.processAll()

        assertFalse(report.retryLater)
        assertEquals(1, report.readyCount)
        assertEquals(PendingImport.FAILED, dao.get(1).status)
        assertEquals("Pas de recette dans cette vidéo", dao.get(1).error)
        assertEquals(PendingImport.READY, dao.get(2).status)
    }

    @Test
    fun unreadableRecipe_marksFailed() = runBlocking {
        val dao = FakeQueueDao(item(1))
        val report = processor(dao, readable = { false }) { _, _ -> Result.success("""{"x":1}""") }.processAll()
        assertEquals(0, report.readyCount)
        assertEquals(PendingImport.FAILED, dao.get(1).status)
        assertEquals("Impossible de lire la recette reçue.", dao.get(1).error)
    }

    @Test
    fun noServerConfigured_retriesLaterWithoutCalling() = runBlocking {
        val dao = FakeQueueDao(item(1))
        var calls = 0
        val report = processor(dao, server = null) { _, _ -> calls++; Result.success("{}") }.processAll()
        assertTrue(report.retryLater)
        assertEquals(0, calls)
        assertEquals(PendingImport.WAITING, dao.get(1).status)
    }

    @Test
    fun readyAndFailedItems_areNotReprocessed() = runBlocking {
        val dao = FakeQueueDao(
            item(1).copy(status = PendingImport.READY, recipeJson = "{}"),
            item(2).copy(status = PendingImport.FAILED, error = "x")
        )
        var calls = 0
        val report = processor(dao) { _, _ -> calls++; Result.success("{}") }.processAll()
        assertEquals(0, calls)
        assertEquals(0, report.readyCount)
    }

    @Test
    fun readyMessage_singularAndPlural() {
        assertEquals("1 recette importée, à valider", ImportQueue.readyMessage(1))
        assertEquals("3 recettes importées, à valider", ImportQueue.readyMessage(3))
    }

    @Test
    fun queueSummary_listsOnlyNonEmptyGroups() {
        val ready = item(1).copy(status = PendingImport.READY)
        val waiting = item(2)
        val failed = item(3).copy(status = PendingImport.FAILED)
        assertEquals("1 à valider, 1 en attente, 1 échec", com.example.recipelibrary.ui.queueSummary(listOf(ready, waiting, failed)))
        assertEquals("2 en attente", com.example.recipelibrary.ui.queueSummary(listOf(item(4), item(5))))
        assertEquals("2 échecs", com.example.recipelibrary.ui.queueSummary(listOf(failed, failed.copy(id = 9))))
        assertEquals("", com.example.recipelibrary.ui.queueSummary(emptyList()))
    }

    @Test
    fun categories_hermesProposalIsMatchedToAppList() {
        assertEquals("Desserts", RecipeCategories.match("Desserts"))
        assertEquals("Desserts", RecipeCategories.match(" dessert "))
        assertEquals("Plats principaux", RecipeCategories.match("plat principal"))
        assertEquals("Gâteaux", RecipeCategories.match("gâteau"))
        assertNull(RecipeCategories.match("Cuisine du monde"))
        assertNull(RecipeCategories.match(null))
        assertNull(RecipeCategories.match(""))
    }

    @Test
    fun categories_unknownCategoryIsClearedOnImport() {
        val recipe = Recipe(id = "1", title = "T", category = "Cuisine du monde")
        assertNull(RecipeCategories.keepKnown(recipe).category)
        assertEquals("Soupes", RecipeCategories.keepKnown(recipe.copy(category = "soupe")).category)
    }
}
