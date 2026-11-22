package com.example.recipelibrary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.recipelibrary.data.DatabaseProvider
import com.example.recipelibrary.data.ImportQueue
import com.example.recipelibrary.data.PendingImport
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.RecipeCategories
import com.example.recipelibrary.data.RecipeRepository
import com.example.recipelibrary.ui.*
import com.google.gson.stream.JsonReader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.StringReader
import java.util.UUID

// ── Design tokens ────────────────────────────────────────────────────────────
private val ActiveLabel    = Color(0xFF4A16C6)
private val ActiveSurface  = Color(0xFFF3ECFF)
private val InactiveLabel  = Color(0xFF625B70)
private val NavBackground  = Color(0xFFFFFFFF)
private val NavDivider     = Color(0xFFEEEAF7)
private val ScreenBg       = Color(0xFFF8F6FC)
private val VioletPrimary  = Color(0xFF7A2CF6)

// ── Nav items ─────────────────────────────────────────────────────────────────
private data class NavItem(val route: String, val label: String, val icon: Int)
private val NAV_ITEMS = listOf(
    NavItem("list",       "Recettes",  R.drawable.ic_nav_home),
    NavItem("ajouter",    "Ajouter",   R.drawable.ic_nav_add),
    NavItem("search",     "Recherche", R.drawable.ic_nav_search),
    NavItem("menu",       "Menu",      R.drawable.ic_recipe_planning),
)
private val MAIN_ROUTES = setOf("list", "ajouter", "search", "menu")

// ── Shared header ─────────────────────────────────────────────────────────────
@Composable
private fun RecipeTopBar(title: String, showBack: Boolean = false, onBack: () -> Unit = {}) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Column {
                Spacer(Modifier.fillMaxWidth().height(statusBarTop))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (showBack) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_revert),
                                contentDescription = "Retour",
                                tint = ActiveLabel
                            )
                        }
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActiveLabel,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Divider(color = NavDivider, thickness = 1.dp)
    }
}

private val ActiveGradient = listOf(Color(0xFF9D6EFF), Color(0xFF7A2CF6))
private val ActiveSurfaceLight = Color(0xFFF3ECFF)
/** Creux de l'onglet sélectionné (effet d'appui) : fond violet plus soutenu et bord violet. */
private val NavPressedSurface = Color(0xFFE4D6FC)
private val NavPressedBorder = Color(0x664A16C6)
private val Accent = Color(0xFFEC4899)

// ── Bottom nav bar ─────────────────────────────────────────────────────────────
/** Hauteur de la barre du bas : grandes icônes + libellé dessous, sans rien couper. */
private fun navBarHeight(screenW: Int) = when {
    screenW < 360 -> 80.dp
    screenW < 600 -> 92.dp
    else          -> 108.dp
}
@Composable
private fun RecipeBottomNav(currentRoute: String?, onTabSelected: (String) -> Unit) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenW = LocalConfiguration.current.screenWidthDp

    val navHeight = navBarHeight(screenW)
    val pillSize = when {
        screenW < 360 -> 44.dp
        screenW < 600 -> 52.dp
        else          -> 64.dp
    }
    // Icônes détaillées : elles occupent presque tout le bouton pour rester lisibles
    val iconSize = (pillSize.value * 0.90f).dp
    val labelSp  = when {
        screenW < 360 -> 9.sp
        screenW < 600 -> 10.sp
        else          -> 12.sp
    }
    val pillRadius = (pillSize.value * 0.27f).dp

    Column(modifier = Modifier.background(NavBackground)) {
        Divider(color = NavDivider, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(navHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_ITEMS.forEach { item ->
                val selected = currentRoute == item.route
                val alpha by animateFloatAsState(if (selected) 1f else 0.78f, label = "alpha")
                // Effet « bouton enfoncé » sur l'onglet sélectionné : icône un peu plus petite et
                // plus basse, dans un creux violet bordé
                val press by animateFloatAsState(if (selected) 1f else 0f, label = "press")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(item.route) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(pillSize)
                            .then(
                                if (selected) Modifier
                                    .background(NavPressedSurface, RoundedCornerShape(pillRadius))
                                    .border(1.5.dp, NavPressedBorder, RoundedCornerShape(pillRadius))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(item.icon),
                            contentDescription = item.label,
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer {
                                    val k = 1f - 0.08f * press
                                    scaleX = k; scaleY = k
                                    translationY = 1.5.dp.toPx() * press
                                }
                                .alpha(alpha)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = labelSp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) ActiveLabel else InactiveLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(alpha)
                    )
                }
            }
        }
        Spacer(Modifier.fillMaxWidth().height(navBarBottom))
    }
}

// ── Navigation entre onglets ──────────────────────────────────────────────────
/**
 * Ouvre un onglet de la barre du bas. Toute ouverture d'onglet (barre, bandeau, notification)
 * doit passer par ici : si un onglet est ouvert autrement, la navigation mémorise son état
 * sous "list" et y renvoie à chaque fois qu'on veut revenir aux recettes.
 */
internal fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo("list") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// ── String helper ─────────────────────────────────────────────────────────────
private fun String.normalizeTitle(): String =
    if (length > 1 && this == this.uppercase()) lowercase().replaceFirstChar { it.uppercase() } else this

// ── Activity ───────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var pendingImport: Recipe? = null
    private var pendingOcrText: String? = null
    private var pendingImageUrl: String? = null
    /** Lien de la file d'attente en cours de validation : supprimé de la file une fois la recette enregistrée. */
    private var pendingQueueId: Long? = null
    /** Ouvrir l'écran de la file d'attente (depuis la notification). */
    private var openQueueRequested by mutableStateOf(false)
    /** Lien de page partagé vers l'app : ouvert dans l'import par URL et importé tout de suite. */
    private var sharedUrl by mutableStateOf<String?>(null)
    /** Image partagée vers l'app : l'utilisatrice choisit entre lire la recette ou en faire la photo d'une recette. */
    private var sharedImage by mutableStateOf<Uri?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Demandée la première fois qu'un lien est mis en file, pour pouvoir prévenir quand la recette est prête. */
    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Table des aliments chargée au démarrage ; null tant que le chargement n'est pas fini. */
    private var foodTable by androidx.compose.runtime.mutableStateOf<com.example.recipelibrary.data.nutrition.FoodTable?>(null)

    override fun onResume() {
        super.onResume()
        // De retour dans l'app (souvent à la maison) : on relance tout de suite les liens en attente
        lifecycleScope.launch { runCatching { ImportQueue.kickIfWaiting(applicationContext) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        com.example.recipelibrary.data.LanNetwork.init(applicationContext)
        // Table des aliments (calories) : chargée une fois, hors du fil principal.
        lifecycleScope.launch {
            val table = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { com.example.recipelibrary.data.nutrition.FoodTable.load(applicationContext) }.getOrNull()
            }
            foodTable = table
        }
        val db      = DatabaseProvider.getDatabase(applicationContext)
        val repo    = RecipeRepository(db.recipeDao(), db)
        val factory = RecipeViewModelFactory(repo)
        val listVm  = ViewModelProvider(this, factory).get(RecipeListViewModel::class.java)
        val menuVm  = ViewModelProvider(
            this, MenuViewModel.Factory(com.example.recipelibrary.data.menu.MenuRepository(db.menuDao()), listVm.recipes)
        ).get(MenuViewModel::class.java)
        val queueFlow = db.pendingImportDao().observeAll()
        // Aliments de remplacement (spec 001, story 6) : valables pour toutes les recettes.
        val substitutionDao = db.foodSubstitutionDao()
        val substitutionsFlow = substitutionDao.observeAll()

        handleIncomingIntent(intent)

        setContent {
            navController = rememberNavController()
            val recipes by listVm.recipes.collectAsState()
            val menuEntries by menuVm.entries.collectAsState()
            val substitutions by substitutionsFlow.collectAsState(initial = emptyList())
            // Table avec les aliments de remplacement : calories, parts estimées et liste de courses.
            val effectiveFoodTable = remember(foodTable, substitutions) {
                foodTable?.withSubstitutions(substitutions.associate { it.textKey to it.foodCode })
            }
            LaunchedEffect(effectiveFoodTable) { menuVm.setFoodTable(effectiveFoodTable) }
            val context = LocalContext.current
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route
            val queue by queueFlow.collectAsState(initial = emptyList())
            val readyCount = queue.count { it.status == PendingImport.READY }
            val openQueueScreen = { navController.navigate("queue") { launchSingleTop = true } }

            LaunchedEffect(Unit) {
                pendingImport?.let {
                    pendingImport = null
                    navController.navigate("add")
                }
            }
            // Onglet « Ajouter » (spec 005) : mode URL / Fichier / Photo, gardé à la rotation
            var addMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(AddMode.URL) }
            // Lien partagé : toujours sur le mode URL de l'onglet Ajouter
            LaunchedEffect(sharedUrl) {
                if (sharedUrl != null) {
                    addMode = AddMode.URL
                    if (currentRoute != "ajouter") navController.navigateToTab("ajouter")
                }
            }
            sharedImage?.let { uri ->
                AlertDialog(
                    onDismissRequest = { sharedImage = null },
                    title = { Text("Image reçue") },
                    text = { Text("Que veux-tu faire de cette image ?") },
                    confirmButton = {
                        TextButton(onClick = {
                            sharedImage = null
                            readRecipeFromImage(uri) { navController.navigate("ocr_review") }
                        }) { Text("Lire la recette") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            sharedImage = null
                            pendingImageUrl = uri.toString()
                            Toast.makeText(context, "Ouvre une recette pour lui mettre cette photo", Toast.LENGTH_LONG).show()
                        }) { Text("Photo d'une recette") }
                    }
                )
            }
            LaunchedEffect(openQueueRequested) {
                if (openQueueRequested) {
                    openQueueRequested = false
                    openQueueScreen()
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomNavHeight = if (currentRoute in MAIN_ROUTES) navBarHeight(LocalConfiguration.current.screenWidthDp) + 5.dp + navBarBottom else 0.dp

                NavHost(
                    navController = navController,
                    startDestination = "list",
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomNavHeight)
                ) {

                    // ── Liste principale ───────────────────────────────────────
                    composable("list") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Mes recettes") },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { navController.navigate("add") },
                                    backgroundColor = Accent,
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Nouvelle recette", tint = Color.White)
                                }
                            }
                        ) { innerPadding ->
                            Column(Modifier.padding(innerPadding)) {
                                if (queue.isNotEmpty()) {
                                    Card(
                                        backgroundColor = ActiveSurface,
                                        elevation = 0.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clickable { openQueueScreen() }
                                    ) {
                                        Text(
                                            (if (readyCount > 0) "📥 " else "⏳ ") +
                                                "File d'attente : ${queueSummary(queue)} — toucher pour voir",
                                            modifier = Modifier.padding(12.dp),
                                            color = ActiveLabel,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                HomeScreen(
                                    recipes = recipes,
                                    onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                    onNavigateToEdit = { id -> navController.navigate("edit/$id") },
                                    onDeleteRecipe = { id -> listVm.delete(id) },
                                    menuCountOf = { id -> menuEntries.count { it.recipeId == id } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ── Ajouter (spec 005) : URL / Fichier / Photo ───────────
                    composable("ajouter") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Ajouter") }
                        ) { innerPadding ->
                            AddTabScreen(
                                mode = addMode,
                                onModeChange = { addMode = it },
                                modifier = Modifier.padding(innerPadding),
                                url = {
                                    UrlImportScreen(
                                        onImport = { recipe ->
                                            pendingImport = RecipeCategories.keepKnown(recipe)
                                            navController.navigate("add") {
                                                popUpTo("ajouter") { inclusive = true }
                                            }
                                        },
                                        onBack = { navController.popBackStack() },
                                        parseJson = { text -> parseRecipeJson(text) },
                                        onQueue = { url ->
                                            askNotificationPermissionIfNeeded()
                                            lifecycleScope.launch { ImportQueue.add(applicationContext, url) }
                                        },
                                        pending = queue,
                                        onOpenQueue = openQueueScreen,
                                        sharedUrl = sharedUrl,
                                        onSharedUrlConsumed = { sharedUrl = null }
                                    )
                                },
                                file = {
                                    FileImportScreen(
                                        onFilePicked = { text ->
                                            pendingImport = parseTextRecipe(text)
                                            navController.navigate("add")
                                        }
                                    )
                                },
                                photo = {
                                    PhotoImportScreen(
                                        onGalleryPicked = { uri ->
                                            readRecipeFromImage(uri) { navController.navigate("ocr_review") }
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // ── File d'attente ────────────────────────────────────────
                    // ── Aliments de remplacement (spec 001, story 6) ─────────
                    composable("substitutions") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Aliments de remplacement", showBack = true) { navController.popBackStack() } }
                        ) { innerPadding ->
                            com.example.recipelibrary.ui.FoodSubstitutionsScreen(
                                substitutions = substitutions,
                                foodTable = foodTable,
                                onCancel = { key -> lifecycleScope.launch { substitutionDao.delete(key) } },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    composable("queue") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("File d'attente", showBack = true) { navController.popBackStack() } }
                        ) { innerPadding ->
                            ImportQueueScreen(
                                pending = queue,
                                parseJson = { text -> parseRecipeJson(text) },
                                modifier = Modifier.padding(innerPadding),
                                onOpen = { item ->
                                        val recipe = item.recipeJson?.let { parseRecipeJson(it) }
                                        if (recipe == null) {
                                            Toast.makeText(context, "Impossible de lire cette recette", Toast.LENGTH_SHORT).show()
                                        } else {
                                            pendingImport = RecipeCategories.keepKnown(recipe)
                                            pendingQueueId = item.id
                                            navController.navigate("add")
                                        }
                                    },
                                onRetry = { item ->
                                    lifecycleScope.launch { ImportQueue.retry(applicationContext, item) }
                                },
                                onDelete = { item ->
                                    lifecycleScope.launch { ImportQueue.remove(applicationContext, item.id) }
                                },
                                onRunNow = {
                                    lifecycleScope.launch { ImportQueue.kick(applicationContext) }
                                    Toast.makeText(context, "Import relancé (Wi-Fi de la maison nécessaire)", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // ── Search ────────────────────────────────────────────────
                    composable("search") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Recherche") }
                        ) { innerPadding ->
                            Box(Modifier.padding(innerPadding)) {
                                SearchScreen(
                                    recipes = recipes,
                                    onRecipeClick = { navController.navigate("detail/${it.id}") },
                                    onBack = { navController.navigate("list") }
                                )
                            }
                        }
                    }

                    // ── Menu et liste de courses (spec 002) ───────────────────
                    composable("menu") {
                        LaunchedEffect(Unit) { menuVm.expireChecks() }
                        val byId by menuVm.recipesById.collectAsState()
                        val shopping by menuVm.shopping.collectAsState()
                        val extras by menuVm.extras.collectAsState()
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Menu") }
                        ) { innerPadding ->
                            MenuScreen(
                                entries = menuEntries,
                                recipesById = byId,
                                allRecipes = recipes,
                                shopping = shopping,
                                modifier = Modifier.padding(innerPadding),
                                actions = MenuActions(
                                    onAdd = { menuVm.add(it) },
                                    onServings = { id, n -> menuVm.setServings(id, n) },
                                    onTimes = { id, n -> menuVm.setTimes(id, n) },
                                    onSetRecipeServings = { entry, r, n ->
                                        // Saisie volontaire : seule écriture d'une recette depuis le menu
                                        listVm.upsert(com.example.recipelibrary.data.RecipeEdits.withServings(r, n))
                                        menuVm.setServings(entry.id, n)
                                    },
                                    onDay = { id, d -> menuVm.setDay(id, d) },
                                    onRemove = { menuVm.remove(it) },
                                    onClear = { menuVm.clear() },
                                    onOpenRecipe = { navController.navigate("detail/${it.id}") }
                                ),
                                extras = extras,
                                shoppingActions = ShoppingActions(
                                    onCheck = { key, c -> menuVm.check(key, c) },
                                    onAddExtra = { menuVm.addExtra(it) },
                                    onEditExtra = { id, t -> menuVm.setExtraText(id, t) },
                                    onExtraChecked = { id, c -> menuVm.setExtraChecked(id, c) },
                                    onRemoveExtra = { menuVm.removeExtra(it) },
                                    onShare = { text ->
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Liste de courses")
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(send, "Partager la liste de courses"))
                                    },
                                    onChronodrive = { term -> ChronodriveLauncher.open(context, term) }
                                )
                            )
                        }
                    }

                    // ── Detail ────────────────────────────────────────────────
                    composable("detail/{id}") { back ->
                        val id = back.arguments?.getString("id") ?: return@composable
                        val recipe = recipes.find { it.id == id } ?: return@composable
                        val incomingImage = remember(pendingImageUrl) {
                            pendingImageUrl.also { pendingImageUrl = null }
                        }
                        RecipeDetailScreen(
                            recipe = recipe,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("edit/${recipe.id}") },
                            onDelete = {
                                listVm.delete(id) {
                                    navController.popBackStack()
                                }
                            },
                            onDuplicate = {
                                val copy = recipe.copy(
                                    id = UUID.randomUUID().toString(),
                                    title = "Copie de ${recipe.title}",
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                listVm.upsert(copy) {
                                    navController.navigate("detail/${copy.id}")
                                }
                                Toast.makeText(context, "Recette dupliquée ✓", Toast.LENGTH_SHORT).show()
                            },
                            onImageChange = { path ->
                                listVm.upsert(recipe.copy(imageUrl = path))
                            },
                            pendingImageUrl = incomingImage,
                            onOpenImageSearch = {
                                val query = Uri.encode(recipe.title)
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query&tbm=isch")))
                            },
                            foodTable = effectiveFoodTable,
                            onChooseFood = { key, code ->
                                lifecycleScope.launch {
                                    substitutionDao.upsert(com.example.recipelibrary.data.FoodSubstitution(key, code))
                                }
                                Toast.makeText(context, "Aliment retenu pour toutes les recettes", Toast.LENGTH_SHORT).show()
                            },
                            onOpenSubstitutions = { navController.navigate("substitutions") { launchSingleTop = true } },
                            onServingsChange = { n ->
                                listVm.upsert(com.example.recipelibrary.data.RecipeEdits.withServings(recipe, n))
                            },
                            onWeightChange = { line, w ->
                                // Correction à la main : prioritaire, enregistrée avec la recette (SC-005)
                                listVm.upsert(com.example.recipelibrary.data.RecipeEdits.withUserWeight(recipe, line, w))
                            },
                            onAddToMenu = {
                                menuVm.add(recipe)
                                Toast.makeText(context, "Ajoutée au menu", Toast.LENGTH_SHORT).show()
                            },
                            menuCount = menuEntries.count { it.recipeId == recipe.id }
                        )
                    }

                    // ── Add ───────────────────────────────────────────────────
                    composable("add") {
                        // Recette importée : les champs obligatoires vides deviennent « À compléter »
                        val preloaded = remember {
                            pendingImport.also { pendingImport = null }
                                ?.let(com.example.recipelibrary.data.ImportedRecipe::fillMissing)
                        }
                        val queueId = remember { pendingQueueId.also { pendingQueueId = null } }
                        AddEditRecipeScreen(
                            existing = preloaded,
                            onSave = { recipe ->
                                listVm.upsert(recipe) {
                                    navController.popBackStack()
                                }
                                // Recette validée depuis la file d'attente : on la retire de la file
                                if (queueId != null) {
                                    lifecycleScope.launch { ImportQueue.remove(applicationContext, queueId) }
                                }
                                Toast.makeText(context, "Recette enregistrée", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ── Edit ──────────────────────────────────────────────────
                    composable("edit/{id}") { back ->
                        val id = back.arguments?.getString("id") ?: return@composable
                        val recipe = recipes.find { it.id == id }
                        if (recipe == null) { navController.popBackStack(); return@composable }
                        AddEditRecipeScreen(
                            existing = recipe,
                            onSave = { updated ->
                                listVm.upsert(updated) {
                                    navController.popBackStack()
                                }
                                Toast.makeText(context, "Recette enregistrée", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ── OCR Review ────────────────────────────────────────────
                    composable("ocr_review") {
                        val ocrText = remember { pendingOcrText.also { pendingOcrText = null } ?: "" }
                        TextImportScreen(
                            initialText = ocrText,
                            onImport = { recipe ->
                                pendingImport = recipe
                                navController.navigate("add") {
                                    popUpTo("ocr_review") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (currentRoute in MAIN_ROUTES) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        RecipeBottomNav(currentRoute) { route ->
                            // « Ajouter » s'ouvre toujours sur l'URL, même s'il est déjà affiché (FR-002)
                            if (route == "ajouter") addMode = AddMode.URL
                            if (route != currentRoute) navController.navigateToTab(route)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(ImportQueue.EXTRA_OPEN_QUEUE, false)) {
            openQueueRequested = true
            return
        }
        if (intent.action != Intent.ACTION_SEND) {
            parseImportIntent(intent)?.let { openImport(it) }
            return
        }
        val type = intent.type ?: ""
        val stream: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        when {
            // Image (capture d'écran d'une recette, photo d'un plat) : on demande quoi en faire
            type.startsWith("image/") -> stream?.let { sharedImage = it }
            type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: stream?.let { uri -> runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull() }
                handleSharedText(text)
            }
            else -> parseImportIntent(intent)?.let { openImport(it) }
        }
    }

    /** Texte partagé : chaque contenu part vers la bonne extraction (contrat SharedText). */
    private fun handleSharedText(text: String?) {
        when (val shared = com.example.recipelibrary.data.share.SharedText.classify(text)) {
            is com.example.recipelibrary.data.share.SharedText.PageLink -> sharedUrl = shared.url
            is com.example.recipelibrary.data.share.SharedText.ImageLink -> {
                pendingImageUrl = shared.url
                Toast.makeText(this, "Image reçue — ouvre une recette pour l'appliquer", Toast.LENGTH_LONG).show()
            }
            is com.example.recipelibrary.data.share.SharedText.RecipeJson ->
                parseRecipeJson(shared.json)?.let { openImport(it) }
                    ?: Toast.makeText(this, "Recette JSON illisible", Toast.LENGTH_LONG).show()
            is com.example.recipelibrary.data.share.SharedText.RecipeText -> openImport(parseTextRecipe(shared.text))
            com.example.recipelibrary.data.share.SharedText.Unknown ->
                Toast.makeText(this, "Rien à importer dans ce partage", Toast.LENGTH_LONG).show()
        }
    }

    private fun openImport(recipe: Recipe) {
        pendingImport = recipe
        if (::navController.isInitialized) navController.navigate("add")
    }

    /** Lit le texte d'une image (photo, capture d'écran) puis ouvre la relecture. */
    private fun readRecipeFromImage(uri: Uri, openReview: () -> Unit) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener { result ->
                    if (result.text.isBlank()) {
                        Toast.makeText(this, "Aucun texte détecté dans la photo", Toast.LENGTH_LONG).show()
                    } else {
                        pendingOcrText = result.text
                        openReview()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de la lecture de la photo", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible de lire cette image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseImportIntent(intent: Intent?): Recipe? {
        if (intent == null) return null
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        uri ?: return null
        return try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
            parseRecipeJson(json)
        } catch (e: Exception) {
            Toast.makeText(this, "Fichier JSON invalide", Toast.LENGTH_LONG).show()
            null
        }
    }

}
