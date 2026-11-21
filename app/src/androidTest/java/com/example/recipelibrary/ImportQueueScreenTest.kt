package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.PendingImport
import com.example.recipelibrary.ui.ImportQueueScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportQueueScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val ready = PendingImport(id = 1, url = "https://insta/reel/1", status = PendingImport.READY, recipeJson = """{"title":"Crumble"}""")
    private val waiting = PendingImport(id = 2, url = "https://insta/reel/2")
    private val failed = PendingImport(id = 3, url = "https://insta/reel/3", status = PendingImport.FAILED, error = "Pas de recette dans cette video")

    private fun show(
        pending: List<PendingImport>,
        onOpen: (PendingImport) -> Unit = {},
        onRetry: (PendingImport) -> Unit = {},
        onDelete: (PendingImport) -> Unit = {}
    ) {
        rule.setContent {
            MaterialTheme {
                ImportQueueScreen(
                    pending = pending,
                    parseJson = ::parseRecipeJson,
                    onOpen = onOpen,
                    onRetry = onRetry,
                    onDelete = onDelete
                )
            }
        }
    }

    @Test
    fun queue_groupsItemsByStatus() {
        show(listOf(waiting, failed, ready))
        rule.onNodeWithText("À valider (1)").assertIsDisplayed()
        rule.onNodeWithText("Crumble").assertIsDisplayed()
        rule.onNodeWithText("En attente du PC (1)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("https://insta/reel/2").assertExists()
        rule.onNodeWithText("Échecs (1)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Pas de recette dans cette video").assertExists()
    }

    @Test
    fun queue_validateOpensReadyRecipe() {
        var opened: PendingImport? = null
        show(listOf(waiting, ready), onOpen = { opened = it })
        rule.onNodeWithText("Valider").performClick()
        assertEquals(1L, opened?.id)
    }

    @Test
    fun queue_retryOnlyOnFailedItems() {
        var retried: PendingImport? = null
        show(listOf(ready, waiting, failed), onRetry = { retried = it })
        rule.onAllNodesWithText("Réessayer").fetchSemanticsNodes().size.let { assertEquals(1, it) }
        rule.onNodeWithText("Réessayer").performScrollTo().performClick()
        assertEquals(3L, retried?.id)
    }

    @Test
    fun queue_deleteOnEveryItem() {
        val deleted = mutableListOf<Long>()
        show(listOf(ready, waiting, failed), onDelete = { deleted += it.id })
        assertEquals(3, rule.onAllNodesWithText("Supprimer").fetchSemanticsNodes().size)
    }

    @Test
    fun queue_runNowButton_onlyWithWaitingItems() {
        var runs = 0
        rule.setContent {
            MaterialTheme {
                ImportQueueScreen(
                    pending = listOf(ready, waiting),
                    parseJson = ::parseRecipeJson,
                    onOpen = {}, onRetry = {}, onDelete = {},
                    onRunNow = { runs++ }
                )
            }
        }
        rule.onNodeWithText("▶ Lancer maintenant").performScrollTo().performClick()
        assertEquals(1, runs)
    }

    @Test
    fun queue_noWaitingItems_noRunNowButton() {
        show(listOf(ready, failed))
        assertEquals(0, rule.onAllNodesWithText("▶ Lancer maintenant").fetchSemanticsNodes().size)
    }

    @Test
    fun queue_copyLink_onWaitingAndFailedItems() {
        val copied = mutableListOf<String>()
        rule.setContent {
            MaterialTheme {
                ImportQueueScreen(
                    pending = listOf(ready, waiting, failed),
                    parseJson = ::parseRecipeJson,
                    onOpen = {}, onRetry = {}, onDelete = {},
                    onCopy = { copied += it }
                )
            }
        }
        val buttons = rule.onAllNodesWithText("Copier le lien")
        assertEquals(2, buttons.fetchSemanticsNodes().size)
        buttons[0].performScrollTo().performClick()
        buttons[1].performScrollTo().performClick()
        assertEquals(listOf("https://insta/reel/2", "https://insta/reel/3"), copied)
    }

    @Test
    fun queue_empty_showsExplanation() {
        show(emptyList())
        rule.onNodeWithText("Aucun lien en attente", substring = true).assertIsDisplayed()
    }
}
