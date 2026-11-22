package com.example.recipelibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.clip
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.recipelibrary.R
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
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
private val Lavender = Color(0xFFEFE5FF)
private val VioletBorder = Color(0xFFC9B3F5)
private val Ink = Color(0xFF1F1A2E)

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
        TabRow(
            selectedTabIndex = tab, backgroundColor = Color.White, contentColor = Violet,
            indicator = { pos -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(pos[tab]).padding(horizontal = 24.dp), height = 3.dp, color = Violet) }
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.testTag("menu_tab"),
                selectedContentColor = Violet, unselectedContentColor = VioletBorder,
                text = { Text("Menu (${entries.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, modifier = Modifier.testTag("courses_tab"),
                selectedContentColor = Violet, unselectedContentColor = VioletBorder,
                text = { Text("Courses", fontSize = 18.sp, fontWeight = FontWeight.Bold) })
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
    /** Jours repliés (0 = « Sans jour ») : tout est déplié par défaut. */
    val folded = remember { mutableStateMapOf<Int, Boolean>() }

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
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = Violet),
                shape = RoundedCornerShape(26.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 3.dp),
                modifier = Modifier.weight(1f).height(52.dp).testTag("add_recipe")
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter une recette", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { showClear = true },
                    shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, VioletBorder),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(52.dp).testTag("clear_menu")
                ) { Icon(painterResource(R.drawable.ic_corbeille), contentDescription = "Vider le menu", tint = Color.Unspecified, modifier = Modifier.size(28.dp)) }
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
        LazyColumn(Modifier.fillMaxSize().testTag("menu_list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            groups.forEach { group ->
                val day = group.day ?: 0
                val open = folded[day] != true
                item(key = "day_${group.day}") {
                    SectionHeader(
                        open = open, onToggle = { folded[day] = open }, tag = "group_$day",
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_calendrier), null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            MenuLogic.dayName(group.day), fontWeight = FontWeight.Bold, color = Violet, fontSize = 19.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        val n = group.entries.size
                        Text(
                            if (n > 1) "$n recettes" else "1 recette", color = Grey, fontSize = 13.sp,
                            // Le badge est décoratif : le bandeau se lit « Mardi » (le nombre se voit dans la liste)
                            modifier = Modifier.border(1.dp, VioletBorder, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
                                .clearAndSetSemantics {}
                        )
                    }
                }
                if (open) {
                    itemsIndexed(group.entries, key = { _, e -> e.id }) { i, entry ->
                        EntryCard(
                            entry, recipesById[entry.recipeId], actions,
                            last = i == group.entries.lastIndex,
                            onAskRecipeServings = { servingsFor = entry }
                        )
                    }
                }
            }
        }
    }
}

/** Bandeau lavande repliable (jour du menu, rayon des courses) ; [content] = partie gauche. */
@Composable
private fun SectionHeader(
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    /** Étiquette de test du bandeau entier (il est cliquable, donc fusionné en un seul élément). */
    tag: String,
    content: @Composable RowScope.() -> Unit
) {
    val shape = if (open) RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp) else RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(Lavender).clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = Grey, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            painterResource(if (open) R.drawable.ic_chevron_haut else R.drawable.ic_chevron_bas),
            contentDescription = if (open) "Replier" else "Déplier", tint = Color.Unspecified, modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EntryCard(entry: MenuEntry, recipe: Recipe?, actions: MenuActions, last: Boolean, onAskRecipeServings: () -> Unit) {
    val shape = if (last) RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp) else RoundedCornerShape(0.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Color.White).testTag("entry_${entry.id}")
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = recipe?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_recipe_placeholder),
                error = painterResource(R.drawable.ic_recipe_placeholder),
                fallback = painterResource(R.drawable.ic_recipe_placeholder),
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp))
                    .then(if (recipe != null) Modifier.clickable { actions.onOpenRecipe(recipe) } else Modifier)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        recipe?.title ?: "Recette supprimée",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink,
                        modifier = Modifier.weight(1f).padding(top = 4.dp)
                            .then(if (recipe != null) Modifier.clickable { actions.onOpenRecipe(recipe) } else Modifier)
                    )
                    IconButton(onClick = { actions.onRemove(entry.id) }, modifier = Modifier.size(36.dp).testTag("remove_${entry.id}")) {
                        Icon(Icons.Default.Close, contentDescription = "Retirer du menu", tint = Grey)
                    }
                }
                if (recipe != null) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        OutlinedButton(
                            onClick = onAskRecipeServings,
                            shape = RoundedCornerShape(50), border = BorderStroke(1.5.dp, VioletBorder),
                            modifier = Modifier.align(Alignment.End).padding(top = 6.dp).testTag("set_recipe_servings_${entry.id}")
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Violet, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Définir les parts", color = Violet, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (!last) Divider(color = Lavender, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, label: String, tag: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { if (value - 1 in range) onChange(value - 1) }, enabled = value - 1 in range,
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, VioletBorder),
            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp).testTag("${tag}_minus")
        ) { Text("−", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink,
            modifier = Modifier.padding(horizontal = 10.dp).testTag("${tag}_value"))
        OutlinedButton(
            onClick = { if (value + 1 in range) onChange(value + 1) }, enabled = value + 1 in range,
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, VioletBorder),
            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp).testTag("${tag}_plus")
        ) { Text("+", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

/** Changer le jour d'une recette : icône calendrier, liste des jours. */
@Composable
private fun DayPicker(entry: MenuEntry, onDay: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(40.dp).testTag("day_${entry.id}")) {
            Icon(painterResource(R.drawable.ic_calendrier), contentDescription = "Jour : ${MenuLogic.dayName(entry.day)}", tint = Color.Unspecified, modifier = Modifier.size(26.dp))
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
    /** Rayons repliés : tout est déplié par défaut. */
    val folded = remember { mutableStateMapOf<String, Boolean>() }
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
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
                onClick = { shareText?.let(actions.onShare) }, enabled = shareText != null,
                shape = RoundedCornerShape(50), border = BorderStroke(1.5.dp, VioletBorder),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier.testTag("share")
            ) {
                Icon(painterResource(R.drawable.ic_partager), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Partager", color = Violet, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        AddExtraField(onAdd = actions.onAddExtra)
        if (actions.onOpenProducts != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp))
                    .background(Lavender).clickable { actions.onOpenProducts.invoke() }
                    .padding(horizontal = 16.dp, vertical = 12.dp).testTag("open_products"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_favori), null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text("Mes produits Chronodrive", color = Violet, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_fleche_droite), null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
            }
        }

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

        // Story 4 : articles groupés par rayon (titres seulement si au moins un rayon est connu).
        // Les ajouts à la main vont à la fin, donc dans « Autres » (dernier rayon) quand les rayons sont affichés.
        val showAisles = Aisles.showHeaders(openItems)
        val sections = Aisles.group(openItems).let { s ->
            if (showAisles && openExtras.isNotEmpty() && s.none { it.first == Aisles.OTHER }) s + (Aisles.OTHER to emptyList()) else s
        }
        val totals = Aisles.group(calc).associate { (a, l) -> a to l.size }
        val dones = Aisles.group(doneItems).associate { (a, l) -> a to l.size }

        LazyColumn(
            Modifier.fillMaxSize().testTag("shopping_list"), state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            if (menuEmpty) {
                item(key = "empty") {
                    Text("Ajoute d'abord des recettes au menu", color = Grey, modifier = Modifier.padding(vertical = 16.dp).testTag("shopping_empty"))
                }
            }
            sections.forEachIndexed { si, (aisle, sectionItems) ->
                val lastSection = si == sections.lastIndex
                val withExtras = if (showAisles) aisle == Aisles.OTHER else lastSection
                val rows: List<Any> = sectionItems + (if (withExtras) openExtras else emptyList())
                val open = !showAisles || folded[aisle] != true
                if (showAisles) {
                    item(key = "aisle_$aisle") {
                        val extrasCount = if (aisle == Aisles.OTHER) extras.size else 0
                        val doneCount = (dones[aisle] ?: 0) + (if (aisle == Aisles.OTHER) doneExtras.size else 0)
                        SectionHeader(
                            open = open && rows.isNotEmpty(), onToggle = { folded[aisle] = open },
                            trailing = "$doneCount / ${(totals[aisle] ?: 0) + extrasCount}", tag = "aisle_$aisle",
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Icon(painterResource(aisleIcon(aisle)), null, tint = Color.Unspecified, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(aisle, color = Violet, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                } else if (si == 0 && rows.isNotEmpty()) {
                    item(key = "list_top") { Spacer(Modifier.height(8.dp)) }
                }
                if (open) {
                    rows.forEachIndexed { i, row ->
                        val shape = rowShape(first = !showAisles && i == 0, last = i == rows.lastIndex)
                        when (row) {
                            is ShoppingItem -> item(key = row.key) {
                                ItemRow(
                                    key = row.key, label = row.label, qty = itemQty(row), checked = false, shape = shape,
                                    highlighted = sentKey == row.key,
                                    sources = if (openKey == row.key) sourcesText(row) else null,
                                    onToggleSources = { openKey = if (openKey == row.key) null else row.key },
                                    onCheck = { c -> actions.onCheck(row.key, c); if (c && sentKey == row.key) sentKey = null },
                                    onChronodrive = { sentKey = row.key; chrono(ChronodriveSearch.choiceKey(row), row.label, ChronodriveSearch.term(row)) },
                                    onLongPress = { choosing = ChronodriveSearch.choiceKey(row) to row.label }
                                )
                            }
                            is ShoppingExtra -> item(key = "extra:${row.id}") {
                                ItemRow(
                                    key = "extra:${row.id}", label = row.text, qty = "", checked = false, shape = shape,
                                    highlighted = sentKey == "extra:${row.id}", sources = null,
                                    onToggleSources = { editExtra = row },
                                    onCheck = { c -> actions.onExtraChecked(row.id, c); if (c && sentKey == "extra:${row.id}") sentKey = null },
                                    onChronodrive = {
                                        sentKey = "extra:${row.id}"
                                        chrono(ChronodriveSearch.choiceKeyForExtra(row.text), ChronodriveSearch.termForExtra(row.text), ChronodriveSearch.termForExtra(row.text))
                                    },
                                    onRemove = { actions.onRemoveExtra(row.id) },
                                    onLongPress = { choosing = ChronodriveSearch.choiceKeyForExtra(row.text) to ChronodriveSearch.termForExtra(row.text) }
                                )
                            }
                        }
                    }
                }
            }
            if (sections.isEmpty() && openExtras.isNotEmpty()) {
                item(key = "list_top") { Spacer(Modifier.height(8.dp)) }
                openExtras.forEachIndexed { i, x ->
                    item(key = "extra:${x.id}") {
                        ItemRow(
                            key = "extra:${x.id}", label = x.text, qty = "", checked = false,
                            shape = rowShape(first = i == 0, last = i == openExtras.lastIndex),
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
                }
            }
            if (doneItems.isNotEmpty() || doneExtras.isNotEmpty()) {
                item(key = "done_header") {
                    Text(
                        "Déjà à la maison ou dans le panier", color = Grey, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 6.dp).testTag("done_header")
                    )
                }
            }
            val doneCount = doneItems.size + doneExtras.size
            doneItems.forEachIndexed { i, it0 ->
                item(key = it0.key) {
                    ItemRow(
                        key = it0.key, label = it0.label, qty = itemQty(it0), checked = true, highlighted = false,
                        shape = rowShape(first = i == 0, last = i == doneCount - 1),
                        sources = if (openKey == it0.key) sourcesText(it0) else null,
                        onToggleSources = { openKey = if (openKey == it0.key) null else it0.key },
                        onCheck = { c -> actions.onCheck(it0.key, c) }, onChronodrive = null
                    )
                }
            }
            doneExtras.forEachIndexed { i, x ->
                val idx = doneItems.size + i
                item(key = "extra:${x.id}") {
                    ItemRow(
                        key = "extra:${x.id}", label = x.text, qty = "", checked = true, highlighted = false, sources = null,
                        shape = rowShape(first = idx == 0, last = idx == doneCount - 1),
                        onToggleSources = { editExtra = x },
                        onCheck = { c -> actions.onExtraChecked(x.id, c) }, onChronodrive = null,
                        onRemove = { actions.onRemoveExtra(x.id) }
                    )
                }
            }
        }
    }
}

/** Coins arrondis d'une ligne dans sa carte (haut si première sans bandeau, bas si dernière). */
private fun rowShape(first: Boolean, last: Boolean): RoundedCornerShape = RoundedCornerShape(
    topStart = if (first) 18.dp else 0.dp, topEnd = if (first) 18.dp else 0.dp,
    bottomStart = if (last) 18.dp else 0.dp, bottomEnd = if (last) 18.dp else 0.dp
)

/** Illustration du rayon (les rayons sucré et salé partagent l'épicerie ; « Autres » = le panier). */
private fun aisleIcon(aisle: String): Int = when (aisle) {
    "Fruits et légumes" -> R.drawable.ic_rayon_fruits_legumes
    "Crèmerie" -> R.drawable.ic_rayon_cremerie
    "Boucherie et poisson" -> R.drawable.ic_rayon_viandes_poissons
    "Épicerie salée", "Épicerie sucrée" -> R.drawable.ic_rayon_epicerie
    "Surgelés" -> R.drawable.ic_rayon_surgeles
    "Boissons" -> R.drawable.ic_rayon_boissons
    else -> R.drawable.ic_panier
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
    onLongPress: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp)
) {
    val textColor = if (checked) Grey else Ink
    val deco = if (checked) TextDecoration.LineThrough else null
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (highlighted) VioletLight else Color.White)
            .testTag("item_$key")
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked, onCheckedChange = onCheck,
                colors = CheckboxDefaults.colors(checkedColor = Violet, uncheckedColor = VioletBorder),
                modifier = Modifier.testTag("check_$key")
            )
            Column(
                Modifier.weight(1f)
                    .combinedClickable(onClick = onToggleSources, onLongClick = onLongPress)
                    .padding(vertical = 12.dp)
                    .semantics(mergeDescendants = true) {}.testTag("label_$key")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 17.sp, color = textColor, textDecoration = deco, modifier = Modifier.weight(1f, fill = false))
                    if (qty.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            qty, fontSize = if (qty == "à vérifier") 13.sp else 16.sp, textDecoration = deco,
                            fontWeight = if (qty == "à vérifier") FontWeight.Normal else FontWeight.Bold,
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
                IconButton(onClick = onChronodrive, modifier = Modifier.testTag("chrono_$key")) {
                    Icon(painterResource(R.drawable.ic_panier), contentDescription = "Chercher sur Chronodrive", tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                }
            }
        }
        if (highlighted && !checked) {
            Row(Modifier.fillMaxWidth().padding(start = 54.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier.padding(start = 54.dp, end = 16.dp, bottom = 8.dp)
                    .background(VioletLight, RoundedCornerShape(6.dp)).padding(6.dp).testTag("sources_$key")
            )
        }
        if (shape.bottomEndIsSquare()) Divider(color = Lavender, modifier = Modifier.padding(start = 54.dp, end = 12.dp))
    }
}

/** Vrai si la ligne n'est pas la dernière de sa carte (pas de coin arrondi en bas). */
private fun RoundedCornerShape.bottomEndIsSquare(): Boolean = this.bottomEnd == androidx.compose.foundation.shape.CornerSize(0.dp)

@Composable
private fun AddExtraField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = { if (text.isNotBlank()) { onAdd(text); text = "" } }
    OutlinedTextField(
        value = text, onValueChange = { text = it.take(ShoppingExtra.MAX_LENGTH) }, singleLine = true,
        placeholder = { Text("Ajouter un article (ex. 2 papier toilette)", color = Grey) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_plus), null, tint = Color.Unspecified, modifier = Modifier.size(26.dp)) },
        trailingIcon = {
            IconButton(onClick = submit, enabled = text.isNotBlank(), modifier = Modifier.testTag("extra_add")) {
                Icon(Icons.Default.Check, contentDescription = "Ajouter l'article", tint = if (text.isNotBlank()) Violet else VioletBorder)
            }
        },
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = Color.White, unfocusedBorderColor = VioletBorder, focusedBorderColor = Violet
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("extra_field")
    )
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
