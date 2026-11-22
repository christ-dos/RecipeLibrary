package com.example.recipelibrary

import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.ShoppingExtra
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.ui.MenuActions
import com.example.recipelibrary.ui.MenuScreen
import com.example.recipelibrary.ui.MenuViewModel
import com.example.recipelibrary.ui.ShoppingActions
import com.example.recipelibrary.ui.ShoppingListView
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Vue « Courses » (spec 002, T024 et T032), calculée avec la vraie table d'aliments. Menu et coches tenus par le test. */
@RunWith(AndroidJUnit4::class)
class ShoppingListScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val table: FoodTable by lazy { FoodTable.load(InstrumentationRegistry.getInstrumentation().targetContext) }

    private val crepes = Recipe(
        id = "crepes", title = "Crepes", category = "Desserts", servings = 4, servingsSource = Recipe.SERVINGS_USER,
        ingredients = listOf("250 g de farine", "4 oeufs", "50 cl de lait", "1 c. a cafe de sumac")
    )
    private val quiche = Recipe(
        id = "quiche", title = "Quiche", category = "Plats", servings = 6, servingsSource = Recipe.SERVINGS_USER,
        ingredients = listOf("3 oeufs", "200 ml de lait", "200 g de lardons")
    )
    private val salade = Recipe(id = "salade", title = "Salade", category = null, ingredients = listOf("1 citron"))
    private val byId = mapOf(crepes.id to crepes, quiche.id to quiche, salade.id to salade)
    private val entries = mutableStateListOf<MenuEntry>()
    private val checked = mutableStateListOf<String>()
    private val extras = mutableStateListOf<ShoppingExtra>()
    private val chronoTerms = mutableListOf<String>()
    private val shared = mutableListOf<String>()

    private val content: @androidx.compose.runtime.Composable () -> Unit = {
        MaterialTheme {
            val e = entries.toList()
            val keys = checked.toSet()
            val list = remember(e, keys) { MenuViewModel.buildList(e, byId, table).map { it.copy(checked = it.key in keys) } }
            MenuScreen(
                entries = e, recipesById = byId, allRecipes = byId.values.toList(), shopping = list,
                actions = MenuActions(
                    onAdd = { r -> entries += MenuEntry(id = "e${entries.size}", recipeId = r.id, servings = r.servings) },
                    onClear = { entries.clear(); checked.clear() }
                ),
                extras = extras.toList(),
                shoppingActions = ShoppingActions(
                    onCheck = { k, c -> if (c) { if (k !in checked) checked += k } else checked.remove(k) },
                    onAddExtra = { t -> extras += ShoppingExtra(id = "x${extras.size}", text = t.trim()) },
                    onExtraChecked = { id, c -> val i = extras.indexOfFirst { it.id == id }; extras[i] = extras[i].copy(checked = c) },
                    onRemoveExtra = { id -> extras.removeAll { it.id == id } },
                    onShare = { shared += it },
                    onChronodrive = { chronoTerms += it }
                )
            )
        }
    }

    private fun show() {
        rule.setContent(content)
        rule.onNodeWithTag("courses_tab").performClick()
    }

    private fun menuOfTwo() {
        entries += MenuEntry(id = "a", recipeId = "crepes", servings = 4)
        entries += MenuEntry(id = "b", recipeId = "quiche", servings = 6)
    }

    @Test fun emptyMenu_showsMessage_shareDisabled() {
        show()
        rule.onNodeWithTag("shopping_empty").assertIsDisplayed()
        rule.onNodeWithTag("share").assertIsNotEnabled()
    }

    @Test fun groupedSums_sourcesAndUnknownLine() {
        menuOfTwo()
        show()
        rule.onNodeWithTag("label_food:22000").assertTextContains("oeuf").assertTextContains("7")
        rule.onNodeWithTag("label_food:19042").assertTextContains("70 cl")
        rule.onNodeWithTag("label_food:9440").assertTextContains("250 g")
        rule.onNodeWithTag("shopping_list").performScrollToNode(hasText("sumac", substring = true))
        rule.onNodeWithText("sumac", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("label_food:22000").performClick()
        rule.onNodeWithTag("sources_food:22000").assertTextEquals("Pour : Crepes, Quiche")
    }

    @Test fun addingRecipe_updatesList() {
        entries += MenuEntry(id = "a", recipeId = "crepes", servings = 4)
        show()
        rule.onNodeWithTag("label_food:22000").assertTextContains("4")
        rule.onNodeWithTag("label_food:28720").assertDoesNotExist()
        rule.onNodeWithTag("menu_tab").performClick()
        rule.onNodeWithTag("add_recipe").performClick()
        rule.onNodeWithTag("add_result_quiche").performClick()
        rule.onNodeWithTag("courses_tab").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("label_food:22000").assertTextContains("7")
        rule.onNodeWithTag("label_food:28720").assertTextContains("200 g")
    }

    @Test fun checkThenUncheck_checkKeptAfterAddingRecipe() {
        entries += MenuEntry(id = "a", recipeId = "crepes", servings = 4)
        show()
        rule.onNodeWithTag("check_food:9440").performClick()
        rule.waitForIdle()
        assertEquals(listOf("food:9440"), checked.toList())
        rule.onNodeWithTag("done_header").assertExists()
        rule.onNodeWithTag("chrono_food:9440").assertDoesNotExist() // pas de bouton Chronodrive sur un article coché
        // Recalcul (nouvelle recette) : la coche suit l'article
        entries += MenuEntry(id = "b", recipeId = "quiche", servings = 6)
        rule.waitForIdle()
        rule.onNodeWithTag("check_food:9440").assertIsOn()
        rule.onNodeWithTag("check_food:9440").performClick()
        rule.waitForIdle()
        assertEquals(emptyList<String>(), checked.toList())
        rule.onNodeWithTag("check_food:9440").assertIsOff()
    }

    @Test fun extraKeptAfterClearingMenu_andShared() {
        menuOfTwo()
        show()
        rule.onNodeWithTag("extra_field").performTextInput("2 papier toilette")
        rule.onNodeWithTag("extra_add").performClick()
        rule.waitForIdle()
        assertEquals(listOf("2 papier toilette"), extras.map { it.text })
        rule.onNodeWithTag("menu_tab").performClick()
        rule.onNodeWithTag("clear_menu").performClick()
        rule.onNodeWithTag("clear_confirm").performClick()
        rule.onNodeWithTag("courses_tab").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("label_extra:x0").assertTextContains("2 papier toilette")
        rule.onNodeWithTag("share").assertIsEnabled().performClick()
        assertEquals("Liste de courses (0 recette)\n\n- 2 papier toilette", shared.single())
    }

    @Test fun shareDisabled_whenEverythingChecked() {
        entries += MenuEntry(id = "s", recipeId = "salade")
        show()
        rule.onNodeWithTag("share").assertIsEnabled()
        rule.onNodeWithTag("check_food:13009").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("share").assertIsNotEnabled()
    }

    @Test fun chronodrive_termSent_itemHighlighted_thenMarkDone() {
        menuOfTwo()
        show()
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        assertEquals(listOf("farine"), chronoTerms)
        rule.onNodeWithTag("mark_done_food:9440").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(listOf("food:9440"), checked.toList())
        rule.onNodeWithTag("mark_done_food:9440").assertDoesNotExist()
    }

    @Test fun chronodrive_highlightAndScrollKeptAfterRecreation() {
        menuOfTwo()
        // Assez d'articles pour défiler
        repeat(25) { i -> extras += ShoppingExtra(id = "z$i", text = "article $i") }
        val restore = StateRestorationTester(rule)
        restore.setContent(content)
        rule.onNodeWithTag("courses_tab").performClick()
        rule.onNodeWithTag("shopping_list").performScrollToNode(hasTestTag("item_extra:z20"))
        rule.onNodeWithTag("chrono_extra:z20").performClick()
        rule.waitForIdle()
        assertEquals(listOf("article 20"), chronoTerms)

        restore.emulateSavedInstanceStateRestore() // comme au retour de Chronodrive si l'écran a été recréé
        rule.waitForIdle()
        rule.onNodeWithTag("mark_done_extra:z20").assertIsDisplayed() // même position, article en évidence
        assertNotNull(rule.onNodeWithTag("item_extra:z20").fetchSemanticsNode())
    }

    @Test fun addDialog_groupedByCategory_noCategoryLast() {
        show()
        rule.onNodeWithTag("menu_tab").performClick()
        rule.onNodeWithTag("add_recipe").performClick()
        val tops = listOf("add_cat_Desserts", "add_cat_Plats", "add_cat_Sans catégorie").map {
            rule.onNodeWithTag(it).assertExists().fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(tops.sorted(), tops)
        // La recherche garde le rangement
        rule.onNodeWithTag("add_search").performTextInput("qui")
        rule.waitForIdle()
        rule.onNodeWithTag("add_cat_Plats").assertIsDisplayed()
        rule.onNodeWithTag("add_result_quiche").assertIsDisplayed()
        // LazyColumn (Compose 1.5) garde des nœuds retirés : on vérifie qu'ils ne sont plus affichés
        rule.onNodeWithTag("add_cat_Desserts").assertIsNotDisplayed()
        rule.onNodeWithTag("add_result_crepes").assertIsNotDisplayed()
    }

    @Test fun groupedByAisle_unknownAndManualInOther() {
        menuOfTwo()
        extras += ShoppingExtra(id = "x0", text = "papier toilette")
        show()
        rule.onNodeWithTag("aisle_Crèmerie").assertIsDisplayed()
        rule.onNodeWithTag("aisle_Épicerie sucrée").assertIsDisplayed()
        rule.onNodeWithTag("aisle_Boucherie et poisson").assertIsDisplayed()
        // sumac (non reconnu) et papier toilette (ajout à la main) sont rangés dans « Autres », après les autres rayons
        val other = rule.onNodeWithTag("aisle_Autres").fetchSemanticsNode().boundsInRoot.top
        val creamery = rule.onNodeWithTag("aisle_Crèmerie").fetchSemanticsNode().boundsInRoot.top
        assert(other > creamery)
        // L'ajout à la main est sous le titre « Autres » (refonte 006 : lignes plus hautes, on fait défiler jusqu'à lui)
        rule.onNodeWithTag("shopping_list").performScrollToNode(hasTestTag("label_extra:x0"))
        val otherAfterScroll = rule.onNodeWithTag("aisle_Autres").fetchSemanticsNode().boundsInRoot.top
        val extra = rule.onNodeWithTag("label_extra:x0").fetchSemanticsNode().boundsInRoot.top
        assert(extra > otherAfterScroll)
    }

    // ── Spec 003 : « Mes produits Chronodrive » (T017, T018, T033) ──

    private val bananeA = ChronodriveProduct(365920, "banane", null, "Fruits et légumes", true, 0)
    private val bananeB = ChronodriveProduct(305254, "banane", "auchan", "Fruits et légumes", true, 0)
    private val farineP = ChronodriveProduct(123456, "farine de ble t55", "francine", "Epicerie", true, 0)
    private val openedProducts = mutableListOf<Pair<ChronodriveProduct, String>>()
    private val chosen = mutableListOf<Pair<String, Long>>()
    private val resolved = mutableListOf<Pair<String, String>>()
    private var productsOpened = 0

    /** Affiche directement la vue Courses (menu de deux recettes + extras éventuels) avec les actions données. */
    private fun showShopping(actions: ShoppingActions) {
        menuOfTwo()
        rule.setContent {
            MaterialTheme {
                val list = remember { MenuViewModel.buildList(entries.toList(), byId, table) }
                ShoppingListView(
                    items = list, extras = extras.toList(), menuEmpty = false, recipeCount = 2, actions = actions
                )
            }
        }
    }

    private fun chronoActions(
        resolution: ChronodriveResolution,
        withProducts: Boolean = true,
        favorites: List<ChronodriveProduct> = emptyList()
    ) = ShoppingActions(
        onChronodrive = { chronoTerms += it },
        onOpenProducts = if (withProducts) ({ productsOpened++ }) else null,
        resolveChrono = { k, t -> resolved += k to t; resolution },
        onOpenProduct = { p, n -> openedProducts += p to n },
        onChooseProduct = { k, id -> chosen += k to id },
        favorites = favorites
    )

    // Refonte 006 : le menu « ⋮ » est remplacé par la carte « Mes produits Chronodrive » (un seul appui)
    @Test fun productsCard_absentWhenOnOpenProductsNull() {
        showShopping(chronoActions(ChronodriveResolution.None, withProducts = false))
        rule.onNodeWithTag("open_products").assertDoesNotExist()
    }

    @Test fun productsCard_present_andCallsCallback() {
        showShopping(chronoActions(ChronodriveResolution.None))
        rule.onNodeWithTag("open_products").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(1, productsOpened)
    }

    @Test fun chrono_retained_opensProduct_noDialog() {
        showShopping(chronoActions(ChronodriveResolution.Retained(farineP)))
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        assertEquals(listOf(farineP), openedProducts.map { it.first })
        assertEquals(listOf("food:9440"), resolved.map { it.first })
        rule.onNodeWithTag("product_picker").assertDoesNotExist()
        assertEquals(emptyList<String>(), chronoTerms)
        assertEquals(emptyList<Pair<String, Long>>(), chosen)
    }

    @Test fun chrono_several_showsDialog_pickChoosesThenOpens() {
        showShopping(chronoActions(ChronodriveResolution.Several(listOf(bananeA, bananeB))))
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("product_picker").assertIsDisplayed()
        rule.onNodeWithTag("product_305254").performClick()
        rule.waitForIdle()
        assertEquals(listOf("food:9440" to 305254L), chosen)
        assertEquals(listOf(bananeB), openedProducts.map { it.first })
        assertEquals(emptyList<String>(), chronoTerms)
    }

    @Test fun chrono_several_none_callsOnChronodrive_notOnChooseProduct() {
        showShopping(chronoActions(ChronodriveResolution.Several(listOf(bananeA, bananeB))))
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("picker_none").performClick()
        rule.waitForIdle()
        assertEquals(listOf("farine"), chronoTerms)
        assertEquals(emptyList<Pair<String, Long>>(), chosen)
        assertEquals(emptyList<Pair<ChronodriveProduct, String>>(), openedProducts)
    }

    @Test fun chrono_several_cancel_doesNothing() {
        showShopping(chronoActions(ChronodriveResolution.Several(listOf(bananeA, bananeB))))
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("picker_cancel").performClick()
        rule.waitForIdle()
        assertEquals(emptyList<String>(), chronoTerms)
        assertEquals(emptyList<Pair<String, Long>>(), chosen)
        assertEquals(emptyList<Pair<ChronodriveProduct, String>>(), openedProducts)
    }

    @Test fun chrono_none_callsOnChronodriveOnly() {
        showShopping(chronoActions(ChronodriveResolution.None))
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        assertEquals(listOf("farine"), chronoTerms)
        assertEquals(emptyList<Pair<String, Long>>(), chosen)
        assertEquals(emptyList<Pair<ChronodriveProduct, String>>(), openedProducts)
    }

    @Test fun defaultActions_keepFeature002Behaviour() {
        showShopping(ShoppingActions(onChronodrive = { chronoTerms += it }))
        rule.onNodeWithTag("open_products").assertDoesNotExist()
        rule.onNodeWithTag("chrono_food:9440").performClick()
        rule.waitForIdle()
        assertEquals(listOf("farine"), chronoTerms)
    }

    @Test fun chrono_manualItem_usesTxtKey_withoutQuantity() {
        extras += ShoppingExtra(id = "x0", text = "2 papier toilette")
        showShopping(chronoActions(ChronodriveResolution.None))
        rule.onNodeWithTag("shopping_list").performScrollToNode(hasTestTag("chrono_extra:x0"))
        rule.onNodeWithTag("chrono_extra:x0").performClick()
        rule.waitForIdle()
        assertEquals(listOf("txt:papier toilette" to "papier toilette"), resolved)
        assertEquals(listOf("papier toilette"), chronoTerms)
    }

    @Test fun longPress_opensFavoritesDialog_pickChoosesWithoutOpeningProduct() {
        showShopping(chronoActions(ChronodriveResolution.None, favorites = listOf(bananeA, farineP)))
        rule.onNodeWithTag("label_food:9440").performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("favorites_picker").assertIsDisplayed()
        rule.onNodeWithTag("product_123456").performClick()
        rule.waitForIdle()
        assertEquals(listOf("food:9440" to 123456L), chosen)
        assertEquals(emptyList<Pair<ChronodriveProduct, String>>(), openedProducts)
        assertEquals(emptyList<String>(), chronoTerms)
        assertEquals(emptyList<Pair<String, String>>(), resolved)
    }

    @Test fun longPress_withoutFavorites_showsEmptyMessage() {
        showShopping(chronoActions(ChronodriveResolution.None))
        rule.onNodeWithTag("label_food:9440").performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("favorites_empty").assertIsDisplayed()
    }

    @Test fun shortTapOnLabel_stillShowsSourceRecipes() {
        showShopping(chronoActions(ChronodriveResolution.None, favorites = listOf(farineP)))
        rule.onNodeWithTag("label_food:22000").performClick()
        rule.onNodeWithTag("sources_food:22000").assertTextEquals("Pour : Crepes, Quiche")
        rule.onNodeWithTag("favorites_picker").assertDoesNotExist()
    }
}
