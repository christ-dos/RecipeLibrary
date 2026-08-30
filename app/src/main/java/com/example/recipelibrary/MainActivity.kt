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
import com.example.recipelibrary.data.DatabaseProvider
import com.example.recipelibrary.data.Recipe
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
    NavItem("url_import", "URL",       R.drawable.ic_nav_url),
    NavItem("file",       "Fichier",   R.drawable.ic_nav_file),
    NavItem("photo",      "Photo",     R.drawable.ic_nav_photo),
    NavItem("search",     "Recherche", R.drawable.ic_nav_search),
)
private val MAIN_ROUTES = setOf("list", "url_import", "file", "photo", "search")

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
private val Accent = Color(0xFFEC4899)

// ── Bottom nav bar ─────────────────────────────────────────────────────────────
@Composable
private fun RecipeBottomNav(currentRoute: String?, onTabSelected: (String) -> Unit) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenW = LocalConfiguration.current.screenWidthDp

    val navHeight = when {
        screenW < 360 -> 64.dp
        screenW < 600 -> 76.dp
        else          -> 88.dp
    }
    val pillSize = when {
        screenW < 360 -> 40.dp
        screenW < 600 -> 48.dp
        else          -> 60.dp
    }
    val iconSize = (pillSize.value * 0.70f).dp
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
                                if (selected) Modifier.background(
                                    ActiveSurfaceLight,
                                    RoundedCornerShape(pillRadius)
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(item.icon),
                            contentDescription = item.label,
                            modifier = Modifier.size(iconSize).alpha(alpha)
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

// ── String helper ─────────────────────────────────────────────────────────────
private fun String.normalizeTitle(): String =
    if (length > 1 && this == this.uppercase()) lowercase().replaceFirstChar { it.uppercase() } else this

// ── Activity ───────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var pendingImport: Recipe? = null
    private var pendingOcrText: String? = null
    private var pendingImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val db      = DatabaseProvider.getDatabase(applicationContext)
        val repo    = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        val listVm  = ViewModelProvider(this, factory).get(RecipeListViewModel::class.java)

        handleIncomingIntent(intent)

        setContent {
            navController = rememberNavController()
            val recipes by listVm.recipes.collectAsState()
            val context = LocalContext.current
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route

            LaunchedEffect(Unit) {
                pendingImport?.let {
                    pendingImport = null
                    navController.navigate("add")
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomNavHeight = if (currentRoute in MAIN_ROUTES) 81.dp + navBarBottom else 0.dp

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
                            HomeScreen(
                                recipes = recipes,
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                onNavigateToEdit = { id -> navController.navigate("edit/$id") },
                                onDeleteRecipe = { id -> listVm.delete(id) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    // ── URL Import ────────────────────────────────────────────
                    composable("url_import") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Depuis URL") }
                        ) { innerPadding ->
                            Box(Modifier.padding(innerPadding)) {
                                UrlImportScreen(
                                    onImport = { recipe ->
                                        pendingImport = recipe
                                        navController.navigate("add") {
                                            popUpTo("url_import") { inclusive = true }
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    parseJson = { text -> parseRecipeJson(text) }
                                )
                            }
                        }
                    }

                    // ── File Import ───────────────────────────────────────────
                    composable("file") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Fichier") }
                        ) { innerPadding ->
                            Box(Modifier.padding(innerPadding)) {
                                FileImportScreen(
                                    onFilePicked = { text ->
                                        pendingImport = parseTextRecipe(text)
                                        navController.navigate("add")
                                    }
                                )
                            }
                        }
                    }

                    // ── Photo Import ──────────────────────────────────────────
                    composable("photo") {
                        Scaffold(
                            backgroundColor = ScreenBg,
                            topBar = { RecipeTopBar("Photo") }
                        ) { innerPadding ->
                            Box(Modifier.padding(innerPadding)) {
                                PhotoImportScreen(
                                    onGalleryPicked = { uri ->
                                        try {
                                            val image = InputImage.fromFilePath(context, uri)
                                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                            recognizer.process(image)
                                                .addOnSuccessListener { result ->
                                                    val text = result.text
                                                    if (text.isBlank()) {
                                                        Toast.makeText(context, "Aucun texte détecté dans la photo", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        pendingOcrText = text
                                                        navController.navigate("ocr_review")
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Erreur lors de la lecture de la photo", Toast.LENGTH_SHORT).show()
                                                }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Impossible de lire cette image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
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
                            }
                        )
                    }

                    // ── Add ───────────────────────────────────────────────────
                    composable("add") {
                        val preloaded = remember { pendingImport.also { pendingImport = null } }
                        AddEditRecipeScreen(
                            existing = preloaded,
                            onSave = { recipe ->
                                listVm.upsert(recipe) {
                                    navController.popBackStack()
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
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo("list") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
        val type = intent.type ?: ""
        when {
            type.startsWith("image/") && intent.action == Intent.ACTION_SEND -> {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri != null) {
                    pendingImageUrl = uri.toString()
                    Toast.makeText(this, "Image reçue — ouvre une recette pour l'appliquer", Toast.LENGTH_LONG).show()
                }
            }
            type == "text/plain" && intent.action == Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                val url = text.lines().firstOrNull { it.startsWith("http") }?.trim() ?: return
                if (url.matches(Regex(".*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?", RegexOption.IGNORE_CASE)) ||
                    url.contains("googleusercontent") || url.contains("wikimedia") ||
                    url.contains("unsplash") || url.contains("pexels")) {
                    pendingImageUrl = url
                    Toast.makeText(this, "Image reçue — ouvre une recette pour l'appliquer", Toast.LENGTH_LONG).show()
                } else {
                    parseRecipeJson(text)?.let { recipe ->
                        pendingImport = recipe
                        if (::navController.isInitialized) navController.navigate("add")
                    }
                }
            }
            else -> {
                parseImportIntent(intent)?.let { recipe ->
                    pendingImport = recipe
                    if (::navController.isInitialized) navController.navigate("add")
                }
            }
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
