package com.example.recipelibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.MenuLogic
import com.example.recipelibrary.data.menu.ShoppingExtra
import com.example.recipelibrary.data.nutrition.Normalize
import com.example.recipelibrary.data.shopping.Aisles
import com.example.recipelibrary.data.shopping.ChronodriveSearch
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveResolution
import com.example.recipelibrary.data.shopping.QuantityFormat
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.data.shopping.ShoppingText
import kotlinx.coroutines.launch

private val Violet = Color(0xFF4A16C6)
private val VioletLight = Color(0xFFF3ECFF)
private val Grey = Color(0xFF625B70)
private val Pink = Color(0xFFEC4899)

/** Actions du menu, branchées sur [MenuViewModel] par l'activité et sur des faux dans les tests. */
class MenuActions(
    val onAdd: (Recipe) -> Unit = {},
    val onServings: (entryId: String, n: Int) -> Unit = { _, _ -> },
    val onTimes: (entryId: String, n: Int) -> Unit = { _, _ -> },
    /** « Indiquer les parts de la recette » : seule écriture d'une recette depuis le menu. */
    val onSetRecipeServings: (entry: MenuEntry, recipe: Recipe, n: Int) -> Unit = { _, _, _ -> },
    val onDay: (entryId: String, day: Int?) -> Unit = { _, _ -> },
    val onRemove: (entryId: String) -> Unit = {},
    val onClear: () -> Unit = {},
    val onOpenRecipe: (Recipe) -> Unit = {}
)

/** Actions de la liste de courses. */
class ShoppingActions(
    val onCheck: (key: String, checked: Boolean) -> Unit = { _, _ -> },
    val onAddExtra: (String) -> Unit = {},
    val onEditExtra: (id: String, text: String) -> Unit = { _, _ -> },
    val onExtraChecked: (id: String, checked: Boolean) -> Unit = { _, _ -> },
    val onRemoveExtra: (id: String) -> Unit = {},
    val onShare: (String) -> Unit = {},
    /** Copie le terme et ouvre Chronodrive (contracts/chronodrive-search.md). */
    val onChronodrive: (term: String) -> Unit = {},
    // ── Spec 003 : produits Chronodrive (valeurs par défaut = comportement 002) ──
    /** Ouvre « Mes produits Chronodrive ». */
    val onOpenProducts: (() -> Unit)? = null,
    /** Que faire pour cet article : produit retenu, un ou plusieurs candidats, aucun. */
    val resolveChrono: suspend (key: String, term: String) -> ChronodriveResolution = { _, _ -> ChronodriveResolution.None },
    /** Ouvre la fiche du produit et copie le nom de l'article. */
    val onOpenProduct: (product: ChronodriveProduct, itemName: String) -> Unit = { _, _ -> },
    /** Retient (ou remplace) le produit d'un article. */
    val onChooseProduct: (key: String, productId: Long) -> Unit = { _, _ -> },
    /** Favoris importés, pour l'appui long « Choisir mon produit Chronodrive ». */
    val favorites: List<ChronodriveProduct> = emptyList()
)

/**
 * Écran « Menu » avec deux onglets : le menu (recettes à cuisiner, par jour) et les courses.
 * @param shopping liste calculée ; null pendant le calcul ou le chargement de la table d'aliments.
 */
@Composable
fun MenuScreen(
    entries: List<MenuEntry>,
    recipesById: Map<String, Recipe>,
    allRecipes: List<Recipe>,
    shopping: List<ShoppingItem>?,
    actions: MenuActions,
    modifier: Modifier = Modifier,
    extras: List<ShoppingExtra> = emptyList(),
    shoppingActions: ShoppingActions = ShoppingActions()
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, backgroundColor = Color.White, contentColor = Violet) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.testTag("menu_tab"),
                text = { Text("Menu (${entries.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, modifier = Modifier.testTag("courses_tab"),
                text = { Text("Courses") })
        }
        if (tab == 0) MenuView(entries, recipesById, allRecipes, actions)
        else ShoppingListView(shopping, extras, menuEmpty = entries.isEmpty(), recipeCount = entries.size, actions = shoppingActions)
    }
}

@Composable
private fun MenuView(entries: List<MenuEntry>, recipesById: Map<String, Recipe>, allRecipes: List<Recipe>, actions: MenuActions) {
    var showAdd by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var servingsFor by remember { mutableStateOf<MenuEntry?>(null) }

    if (showAdd) AddRecipeDialog(allRecipes, onDismiss = { showAdd = false }, onPick = { actions.onAdd(it); showAdd = false })
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Vider le menu ?") },
            text = { Text("Toutes les recettes du menu et les coches de la liste seront retirées. Les recettes elles-mêmes et les articles ajoutés à la main sont gardés.") },
            confirmButton = {
                TextButton(onClick = { showClear = false; actions.onClear() }, modifier = Modifier.testTag("clear_confirm")) { Text("Vider") }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("Annuler") } }
        )
    }
    servingsFor?.let { entry ->
        val recipe = recipesById[entry.recipeId]
        if (recipe == null) servingsFor = null
        else NumberDialog(
            title = "Parts de la recette",
            text = "Pour combien de parts la recette « ${recipe.title} » est-elle écrite ?",
            range = Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS,
            onDismiss = { servingsFor = null },
            onOk = { n -> servingsFor = null; actions.onSetRecipeServings(entry, recipe, n) }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = Violet),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_recipe")
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Ajouter une recette", color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                TextButton(onClick = { showClear = true }, modifier = Modifier.testTag("clear_menu")) { Text("Vider le menu", color = Grey) }
            }
        }
        if (entries.isEmpty()) {
            Text(
                "Ton menu est vide. Ajoute les recettes que tu veux cuisiner, la liste de courses se fera toute seule.",
                color = Grey, modifier = Modifier.padding(24.dp).testTag("menu_empty")
            )
            return
        }
        val groups = MenuLogic.groupByDay(entries)
        LazyColumn(Modifier.fillMaxSize().testTag("menu_list"), contentPadding = PaddingValues(bottom = 24.dp)) {
            groups.forEach { group ->
                item(key = "day_${group.day}") {
                    Text(
                        MenuLogic.dayName(group.day), fontWeight = FontWeight.Bold, color = Violet,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp).testTag("group_${group.day ?: 0}")
                    )
                }
                items(group.entries, key = { it.id }) { entry ->
                    EntryCard(entry, recipesById[entry.recipeId], actions, onAskRecipeServings = { servingsFor = entry })
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: MenuEntry, recipe: Recipe?, actions: MenuActions, onAskRecipeServings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp), elevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("entry_${entry.id}")
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recipe?.title ?: "Recette supprimée",
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    modifier = Modifier.weight(1f).then(if (recipe != null) Modifier.clickable { actions.onOpenRecipe(recipe) } else Modifier)
                )
                IconButton(onClick = { actions.onRemove(entry.id) }, modifier = Modifier.testTag("remove_${entry.id}")) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer du menu", tint = Grey)
                }
            }
            if (recipe == null) return@Column
            Row(verticalAlignment = Alignment.CenterVertically) {
                val servings = entry.servings
                if (servings != null) {
                    Stepper(
                        value = servings, range = Recipe.MIN_SERVINGS..Recipe.MAX_SERVINGS,
                        label = if (servings > 1) "$servings parts" else "1 part",
                        tag = "servings_${entry.id}", onChange = { actions.onServings(entry.id, it) }
                    )
                } else {
                    Stepper(
                        value = entry.times, range = MenuEntry.MIN_TIMES..MenuEntry.MAX_TIMES,
                        label = "× ${entry.times} recette", tag = "times_${entry.id}",
                        onChange = { actions.onTimes(entry.id, it) }
                    )
                }
                Spacer(Modifier.weight(1f))
                DayPicker(entry, onDay = { actions.onDay(entry.id, it) })
            }
            if (entry.servings == null) {
                Text(
                    "Indiquer les parts de la recette", color = Violet, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onAskRecipeServings).testTag("set_recipe_servings_${entry.id}")
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, label: String, tag: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { if (value - 1 in range) onChange(value - 1) }, enabled = value - 1 in range,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp).testTag("${tag}_minus")
        ) { Text("−") }
        Text(label, modifier = Modifier.padding(horizontal = 10.dp).testTag("${tag}_value"))
        OutlinedButton(
            onClick = { if (value + 1 in range) onChange(value + 1) }, enabled = value + 1 in range,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp).testTag("${tag}_plus")
        ) { Text("+") }
    }
}

@Composable
private fun DayPicker(entry: MenuEntry, onDay: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.testTag("day_${entry.id}")) {
            Text(MenuLogic.dayName(entry.day), color = Violet)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (1..7).forEach { d ->
                DropdownMenuItem(onClick = { open = false; onDay(d) }, modifier = Modifier.testTag("day_option_$d")) { Text(MenuLogic.dayName(d)) }
            }
            DropdownMenuItem(onClick = { open = false; onDay(null) }, modifier = Modifier.testTag("day_option_0")) { Text("Sans jour") }
        }
    }
}

/** Recettes rangées par catégorie (ordre alphabétique, « Sans catégorie » à la fin), puis par titre. */
fun groupByCategory(recipes: List<Recipe>): List<Pair<String, List<Recipe>>> {
    val noCat = "Sans catégorie"
    return recipes
        .groupBy { it.category?.trim()?.takeIf { c -> c.isNotEmpty() } ?: noCat }
        .map { (cat, list) -> cat to list.sortedBy { Normalize.plain(it.title) } }
        .sortedWith(compareBy({ it.first == noCat }, { Normalize.plain(it.first) }))
}

@Composable
private fun AddRecipeDialog(recipes: List<Recipe>, onDismiss: () -> Unit, onPick: (Recipe) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = Normalize.plain(query.trim())
    val groups = remember(recipes, q) {
        groupByCategory(recipes.filter { q.isEmpty() || Normalize.plain(it.title).contains(q) })
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Column(Modifier.padding(16.dp)) {
                Text("Ajouter au menu", fontWeight = FontWeight.Bold, color = Violet, fontSize = 18.sp)
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Chercher une recette") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("add_search")
                )
                if (groups.isEmpty()) Text("Aucune recette trouvée", color = Grey, modifier = Modifier.padding(8.dp))
                LazyColumn(Modifier.weight(1f).testTag("add_list")) {
                    groups.forEach { (cat, list) ->
                        item(key = "cat_$cat") {
                            Text(
                                "${categoryEmoji(cat)} $cat (${list.size})",
                                fontWeight = FontWeight.Bold, color = Violet,
                                modifier = Modifier.fillMaxWidth().background(VioletLight, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp).testTag("add_cat_$cat")
                            )
                        }
                        items(list, key = { it.id }) { r ->
                            Text(
                                r.title,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(r) }
                                    .padding(vertical = 12.dp, horizontal = 12.dp).testTag("add_result_${r.id}")
                            )
                            Divider()
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Fermer") }
            }
        }
    }
}

/** Saisie d'un nombre entier dans [range]. */
@Composable
fun NumberDialog(title: String, text: String, range: IntRange, onDismiss: () -> Unit, onOk: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    val n = value.trim().toIntOrNull()?.takeIf { it in range }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text)
                OutlinedTextField(
                    value = value, onValueChange = { value = it.filter(Char::isDigit).take(3) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = value.isNotEmpty() && n == null,
                    modifier = Modifier.padding(top = 8.dp).testTag("number_field")
                )
                if (value.isNotEmpty() && n == null) Text("Entre un nombre de ${range.first} à ${range.last}", color = MaterialTheme.colors.error, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = { n?.let(onOk) }, enabled = n != null, modifier = Modifier.testTag("number_ok")) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// ── Liste de courses ─────────────────────────────────────────────────────────

/**
 * Vue « Courses » : cases à cocher (cochés en bas, barrés), bouton Chronodrive par article,
 * articles ajoutés à la main, partage. Au retour de Chronodrive, l'article envoyé est mis en évidence.
 */
@Composable
fun ShoppingListView(
    items: List<ShoppingItem>?,
    extras: List<ShoppingExtra>,
    menuEmpty: Boolean,
    recipeCount: Int,
    actions: ShoppingActions,
    modifier: Modifier = Modifier
) {
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    /** Dernier article envoyé à Chronodrive (clé `food:…`, `txt:…` ou `extra:<id>`). */
    var sentKey by rememberSaveable { mutableStateOf<String?>(null) }
    var editExtra by remember { mutableStateOf<ShoppingExtra?>(null) }
    /** Plusieurs favoris pour l'article : (clé de choix, nom, terme, candidats). */
    var picking by remember { mutableStateOf<ChronoPick?>(null) }
    /** Appui long : choisir à la main le produit de l'article (clé de choix, nom). */
    var choosing by remember { mutableStateOf<Pair<String, String>?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    /** « Chrono » (contracts/chrono-link.md) : fiche du produit retenu, choix, ou comportement 002. */
    fun chrono(choiceKey: String, name: String, term: String) {
        scope.launch {
            when (val r = actions.resolveChrono(choiceKey, term)) {
                is ChronodriveResolution.Retained -> actions.onOpenProduct(r.product, name)
                is ChronodriveResolution.Single -> actions.onOpenProduct(r.product, name)
                is ChronodriveResolution.Several -> picking = ChronoPick(choiceKey, name, term, r.candidates)
                ChronodriveResolution.None -> actions.onChronodrive(term)
            }
        }
    }

    picking?.let { p ->
        ProductPickerDialog(
            itemName = p.name, candidates = p.candidates,
            onPick = { product -> picking = null; actions.onChooseProduct(p.key, product.productId); actions.onOpenProduct(product, p.name) },
            onNone = { picking = null; actions.onChronodrive(p.term) },
            onCancel = { picking = null }
        )
    }
    choosing?.let { (key, name) ->
        FavoritesPickerDialog(
            title = "Mon produit Chronodrive pour « $name »",
            favorites = actions.favorites,
            onPick = { product -> choosing = null; actions.onChooseProduct(key, product.productId) },
            onCancel = { choosing = null }
        )
    }
    val calc = items.orEmpty()
    val shareText = ShoppingText.build(calc, extras, recipeCount)

    editExtra?.let { x ->
        TextDialog("Modifier l'article", x.text, onDismiss = { editExtra = null }, onOk = { actions.onEditExtra(x.id, it); editExtra = null })
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Coche ce que tu as déjà ou ce que tu as mis au panier.",
                color = Grey, fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { shareText?.let(actions.onShare) }, enabled = shareText != null,
                colors = ButtonDefaults.buttonColors(backgroundColor = Violet),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.testTag("share")
            ) {
                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Partager", color = Color.White)
            }
            if (actions.onOpenProducts != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("shopping_more")) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus d'options", tint = Violet)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            onClick = { menuOpen = false; actions.onOpenProducts.invoke() },
                            modifier = Modifier.testTag("open_products")
                        ) { Text("Mes produits Chronodrive") }
                    }
                }
            }
        }
        AddExtraField(onAdd = actions.onAddExtra)

        if (!menuEmpty && items == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet, modifier = Modifier.testTag("shopping_loading"))
            }
            return@Column
        }
        val openItems = calc.filter { !it.checked }
        val doneItems = calc.filter { it.checked }
        val openExtras = extras.filter { !it.checked }
        val doneExtras = extras.filter { it.checked }

        LazyColumn(Modifier.fillMaxSize().testTag("shopping_list"), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            if (menuEmpty) {
                item(key = "empty") {
                    Text("Ajoute d'abord des recettes au menu", color = Grey, modifier = Modifier.padding(16.dp).testTag("shopping_empty"))
                }
            }
            // Story 4 : articles groupés par rayon (titres seulement si au moins un rayon est connu)
            val showAisles = Aisles.showHeaders(openItems)
            val sections = Aisles.group(openItems).let { s ->
                if (showAisles && openExtras.isNotEmpty() && s.none { it.first == Aisles.OTHER }) s + (Aisles.OTHER to emptyList()) else s
            }
            for ((aisle, sectionItems) in sections) {
            if (showAisles) {
                item(key = "aisle_$aisle") {
                    Text(
                        aisle, color = Violet, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp).testTag("aisle_$aisle")
                    )
                }
            }
            items(sectionItems, key = { it.key }) { item ->
                ItemRow(
                    key = item.key, label = item.label, qty = itemQty(item), checked = false,
                    highlighted = sentKey == item.key,
                    sources = if (openKey == item.key) sourcesText(item) else null,
                    onToggleSources = { openKey = if (openKey == item.key) null else item.key },
                    onCheck = { c -> actions.onCheck(item.key, c); if (c && sentKey == item.key) sentKey = null },
                    onChronodrive = { sentKey = item.key; chrono(ChronodriveSearch.choiceKey(item), item.label, ChronodriveSearch.term(item)) },
                    onLongPress = { choosing = ChronodriveSearch.choiceKey(item) to item.label }
                )
            }
            }
            // Ajouts à la main : à la fin, donc dans « Autres » (dernier rayon) quand les rayons sont affichés
            items(openExtras, key = { "extra:${it.id}" }) { x ->
                ItemRow(
                    key = "extra:${x.id}", label = x.text, qty = "", checked = false,
                    highlighted = sentKey == "extra:${x.id}", sources = null,
                    onToggleSources = { editExtra = x },
                    onCheck = { c -> actions.onExtraChecked(x.id, c); if (c && sentKey == "extra:${x.id}") sentKey = null },
                    onChronodrive = {
                        sentKey = "extra:${x.id}"
                        chrono(ChronodriveSearch.choiceKeyForExtra(x.text), ChronodriveSearch.termForExtra(x.text), ChronodriveSearch.termForExtra(x.text))
                    },
                    onRemove = { actions.onRemoveExtra(x.id) },
                    onLongPress = { choosing = ChronodriveSearch.choiceKeyForExtra(x.text) to ChronodriveSearch.termForExtra(x.text) }
                )
            }
            if (doneItems.isNotEmpty() || doneExtras.isNotEmpty()) {
                item(key = "done_header") {
                    Text(
                        "Déjà à la maison ou dans le panier", color = Grey, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp).testTag("done_header")
                    )
                }
            }
            items(doneItems, key = { it.key }) { item ->
                ItemRow(
                    key = item.key, label = item.label, qty = itemQty(item), checked = true, highlighted = false,
                    sources = if (openKey == item.key) sourcesText(item) else null,
                    onToggleSources = { openKey = if (openKey == item.key) null else item.key },
                    onCheck = { c -> actions.onCheck(item.key, c) }, onChronodrive = null
                )
            }
            items(doneExtras, key = { "extra:${it.id}" }) { x ->
                ItemRow(
                    key = "extra:${x.id}", label = x.text, qty = "", checked = true, highlighted = false, sources = null,
                    onToggleSources = { editExtra = x },
                    onCheck = { c -> actions.onExtraChecked(x.id, c) }, onChronodrive = null,
                    onRemove = { actions.onRemoveExtra(x.id) }
                )
            }
        }
    }
}

private fun itemQty(item: ShoppingItem): String = if (item.seasoning) "à vérifier" else QuantityFormat.of(item)

private fun sourcesText(item: ShoppingItem): String =
    "Pour : " + item.sources.joinToString(", ") { if (it.count > 1) "${it.title} ×${it.count}" else it.title }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    key: String,
    label: String,
    qty: String,
    checked: Boolean,
    highlighted: Boolean,
    sources: String?,
    onToggleSources: () -> Unit,
    onCheck: (Boolean) -> Unit,
    onChronodrive: (() -> Unit)?,
    onRemove: (() -> Unit)? = null,
    /** Spec 003 : appui long = choisir le produit Chronodrive de l'article. */
    onLongPress: (() -> Unit)? = null
) {
    val textColor = if (checked) Grey else Color(0xFF1F2937)
    val deco = if (checked) TextDecoration.LineThrough else null
    Column(
        Modifier.fillMaxWidth()
            .background(if (highlighted) VioletLight else Color.Transparent)
            .testTag("item_$key")
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked, onCheckedChange = onCheck,
                colors = CheckboxDefaults.colors(checkedColor = Violet),
                modifier = Modifier.testTag("check_$key")
            )
            Column(
                Modifier.weight(1f)
                    .combinedClickable(onClick = onToggleSources, onLongClick = onLongPress)
                    .padding(vertical = 10.dp)
                    .semantics(mergeDescendants = true) {}.testTag("label_$key")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 16.sp, color = textColor, textDecoration = deco, modifier = Modifier.weight(1f, fill = false))
                    if (qty.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            qty, fontSize = if (qty == "à vérifier") 13.sp else 15.sp, textDecoration = deco,
                            fontWeight = if (qty == "à vérifier") FontWeight.Normal else FontWeight.SemiBold,
                            color = if (checked || qty == "à vérifier") Grey else Violet
                        )
                    }
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.testTag("remove_$key")) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer l'article", tint = Grey, modifier = Modifier.size(20.dp))
                }
            }
            if (onChronodrive != null) {
                TextButton(
                    onClick = onChronodrive, contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.testTag("chrono_$key")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Chercher sur Chronodrive", tint = Pink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Chrono", color = Pink, fontSize = 12.sp)
                }
            }
        }
        if (highlighted && !checked) {
            Row(Modifier.fillMaxWidth().padding(start = 52.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Mis au panier ?", color = Violet, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Button(
                    onClick = { onCheck(true) }, colors = ButtonDefaults.buttonColors(backgroundColor = Violet),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.testTag("mark_done_$key")
                ) { Text("Coché", color = Color.White) }
            }
        }
        if (sources != null) {
            Text(
                sources, color = Grey, fontSize = 13.sp,
                modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
                    .background(VioletLight, RoundedCornerShape(6.dp)).padding(6.dp).testTag("sources_$key")
            )
        }
        Divider()
    }
}

@Composable
private fun AddExtraField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = { if (text.isNotBlank()) { onAdd(text); text = "" } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(ShoppingExtra.MAX_LENGTH) }, singleLine = true,
            placeholder = { Text("Ajouter un article (ex. 2 papier toilette)") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f).testTag("extra_field")
        )
        IconButton(onClick = submit, enabled = text.isNotBlank(), modifier = Modifier.testTag("extra_add")) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter l'article", tint = if (text.isNotBlank()) Violet else Grey)
        }
    }
}

@Composable
private fun TextDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it.take(ShoppingExtra.MAX_LENGTH) }, singleLine = true,
                modifier = Modifier.testTag("extra_edit_field")
            )
        },
        confirmButton = { TextButton(onClick = { onOk(value) }, enabled = value.isNotBlank(), modifier = Modifier.testTag("extra_edit_ok")) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/** Article en attente de choix parmi plusieurs favoris. */
private data class ChronoPick(val key: String, val name: String, val term: String, val candidates: List<ChronodriveProduct>)
