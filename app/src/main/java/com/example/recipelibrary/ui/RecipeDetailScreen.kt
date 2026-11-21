package com.example.recipelibrary.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.recipelibrary.R
import com.example.recipelibrary.data.Recipe

private fun String.normalizeTitle(): String =
    if (length > 1 && this == this.uppercase()) lowercase().replaceFirstChar { it.uppercase() } else this

private fun dbgLog(context: Context, msg: String) {
    try {
        val f = java.io.File(context.filesDir, "clipboard_debug.txt")
        f.appendText("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date())}] $msg\n")
    } catch (_: Exception) {}
}

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit = {},
    onImageChange: (String) -> Unit = {},
    pendingImageUrl: String? = null,
    onOpenImageSearch: () -> Unit = {},
    /** Table des aliments ; null tant qu'elle n'est pas chargée (pas de bandeau). */
    foodTable: com.example.recipelibrary.data.nutrition.FoodTable? = null,
    /** Nombre de parts saisi depuis la fiche (story 5). */
    onServingsChange: ((Int) -> Unit)? = null,
    /** Poids corrigé d'une ligne d'ingrédients (story 2). */
    onWeightChange: ((String, com.example.recipelibrary.data.IngredientWeight) -> Unit)? = null
) {
    val context = LocalContext.current
    // Calcul mémorisé : refait seulement si la recette ou la table change (SC-004).
    val nutrition = remember(recipe, foodTable) {
        foodTable?.let { com.example.recipelibrary.data.nutrition.NutritionCalculator(it).estimate(recipe) }
    }
    var showNutritionDetail by remember { mutableStateOf(false) }
    if (showNutritionDetail && nutrition != null && onWeightChange != null) {
        NutritionDetailSheet(
            estimate = nutrition,
            onDismiss = { showNutritionDetail = false },
            onWeightChange = onWeightChange
        )
    }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var stepsExpanded by remember { mutableStateOf(false) }
    var clipboardPreviewFile by remember { mutableStateOf<java.io.File?>(null) }
    var showClipboardDialog by remember { mutableStateOf(false) }
    var waitingForClipboardImage by remember { mutableStateOf(false) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    // Auto-apply image URL received via share from Chrome
    LaunchedEffect(pendingImageUrl) {
        if (!pendingImageUrl.isNullOrBlank()) {
            onImageChange(pendingImageUrl)
            Toast.makeText(context, "Image appliquée ✓", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearClipboard() {
        try {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (_: Exception) {}
    }

    fun deletePreviewFile() {
        clipboardPreviewFile?.delete()
        clipboardPreviewFile = null
    }

    fun applyClipboardImage() {
        dbgLog(context, "applyClipboardImage called — previewFile=${clipboardPreviewFile?.name ?: "null"}")
        showClipboardDialog = false
        val dir = java.io.File(context.filesDir, "recipe_images").also { it.mkdirs() }
        val ts = System.currentTimeMillis()
        val dest = java.io.File(dir, "${recipe.id}_$ts.jpg")

        // Fast path: preview file was saved during detection — just copy it
        val tempFile = clipboardPreviewFile
        clipboardPreviewFile = null
        if (tempFile != null && tempFile.exists()) {
            try {
                dir.listFiles { f -> f.name.startsWith("${recipe.id}_") && f != dest }?.forEach { it.delete() }
                tempFile.copyTo(dest, overwrite = true)
                tempFile.delete()
                onImageChange(Uri.fromFile(dest).toString())
                clearClipboard()
                Toast.makeText(context, "Image appliquée ✓", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors de l'application", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Fallback: read clipboard directly (no preview file available)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            clearClipboard()
            Toast.makeText(context, "Presse-papier vide", Toast.LENGTH_SHORT).show()
            return
        }
        val item = clip.getItemAt(0)
        val uri = item?.uri
        if (uri != null) {
            // CRITICAL: open stream BEFORE clearing clipboard —
            // clearClipboard() revokes the URI permission granted by Chrome,
            // making any subsequent openInputStream call fail.
            val stream = try { context.contentResolver.openInputStream(uri) } catch (_: Exception) { null }
            clearClipboard()
            if (stream != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        dir.listFiles { f -> f.name.startsWith("${recipe.id}_") }?.forEach { it.delete() }
                        stream.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                        withContext(Dispatchers.Main) {
                            onImageChange(Uri.fromFile(dest).toString())
                            Toast.makeText(context, "Image appliquée ✓", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Erreur lors de la copie", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                return
            }
        } else {
            clearClipboard()
        }
        val textUrl = try { item?.coerceToText(context)?.toString()?.trim() } catch (_: Exception) { null }
            ?.takeIf { it.startsWith("http") }
        if (textUrl != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val conn = java.net.URL(textUrl).openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.connect()
                    dir.listFiles { f -> f.name.startsWith("${recipe.id}_") }?.forEach { it.delete() }
                    conn.inputStream.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                    withContext(Dispatchers.Main) {
                        onImageChange(Uri.fromFile(dest).toString())
                        Toast.makeText(context, "Image appliquée ✓", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Impossible de télécharger l'image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Impossible de récupérer l'image du presse-papier", Toast.LENGTH_SHORT).show()
        }
    }

    fun dismissClipboardDialog() {
        showClipboardDialog = false
        isPreviewLoading = false
        deletePreviewFile()
        clearClipboard()
    }

    // Detect clipboard image only when user explicitly went to Google Images from this screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingForClipboardImage) {
                // Guard flags updated synchronously to prevent double-trigger on rapid ON_RESUME events
                waitingForClipboardImage = false
                deletePreviewFile()  // Always discard any stale temp file
                // Android 12+ restricts clipboard until the window has focus; ON_RESUME fires too
                // early. Wait 500ms for onWindowFocusChanged(true) to fire before reading.
                scope.launch {
                    delay(500L)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip ?: run {
                        dbgLog(context, "ON_RESUME+delay — primaryClip still null after 500ms")
                        showClipboardDialog = true
                        return@launch
                    }
                    if (clip.itemCount == 0) {
                        dbgLog(context, "ON_RESUME+delay — clip empty after 500ms")
                        showClipboardDialog = true
                        return@launch
                    }
                    val item = clip.getItemAt(0)
                    val desc = clip.description
                    val mimes = (0 until desc.mimeTypeCount).map { desc.getMimeType(it) }
                    val uri = item?.uri
                    val rawText = try { item?.coerceToText(context)?.toString()?.take(120) } catch (_: Exception) { null }
                    dbgLog(context, "ON_RESUME+delay — MIME=$mimes | uri=${uri?.toString()?.take(80) ?: "null"} | text=${rawText ?: "null"}")
                    if (uri != null) {
                        // Open stream on Main (URI permission is active), copy bytes on IO
                        val stream = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) {
                            dbgLog(context, "openInputStream FAILED: ${e.javaClass.simpleName}: ${e.message?.take(100)}")
                            null
                        }
                        if (stream != null) {
                            dbgLog(context, "stream OK — copying to temp file")
                            try {
                                val tempFile = withContext(Dispatchers.IO) {
                                    val dir = java.io.File(context.filesDir, "recipe_images").also { it.mkdirs() }
                                    // Unique filename per detection — avoids Coil disk cache serving stale image
                                    val f = java.io.File(dir, "clipboard_${System.currentTimeMillis()}.jpg")
                                    stream.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                                    f
                                }
                                clipboardPreviewFile = tempFile
                                showClipboardDialog = true
                            } catch (e: Exception) {
                                dbgLog(context, "copy to temp FAILED: ${e.javaClass.simpleName}: ${e.message?.take(100)}")
                                showClipboardDialog = true
                            }
                            return@launch
                        }
                        dbgLog(context, "uri non null mais stream null — fallback textUrl")
                    }
                    // Fallback: Chrome may have put only a text URL (e.g. "Copy image link")
                    val textUrl = try { item?.coerceToText(context)?.toString()?.trim() } catch (_: Exception) { null }
                        ?.takeIf { it.startsWith("http") }
                    dbgLog(context, "textUrl fallback — url=${textUrl?.take(100) ?: "null"}")
                    if (textUrl != null) {
                        isPreviewLoading = true
                        showClipboardDialog = true
                        try {
                            val tempFile = withContext(Dispatchers.IO) {
                                val dir = java.io.File(context.filesDir, "recipe_images").also { it.mkdirs() }
                                val f = java.io.File(dir, "clipboard_${System.currentTimeMillis()}.jpg")
                                val conn = java.net.URL(textUrl).openConnection() as java.net.HttpURLConnection
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                                conn.connect()
                                conn.inputStream.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                                f
                            }
                            clipboardPreviewFile = tempFile
                            isPreviewLoading = false
                        } catch (e: Exception) {
                            dbgLog(context, "textUrl download FAILED: ${e.javaClass.simpleName}: ${e.message?.take(100)}")
                            isPreviewLoading = false
                        }
                    } else {
                        dbgLog(context, "aucun uri ni url — dialog sans preview")
                        showClipboardDialog = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val dir = java.io.File(context.filesDir, "recipe_images").also { it.mkdirs() }
                // Unique filename per replace → Coil never serves a cached old version
                dir.listFiles { f -> f.name.startsWith("${recipe.id}_") }?.forEach { it.delete() }
                val dest = java.io.File(dir, "${recipe.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                onImageChange(Uri.fromFile(dest).toString())
            } catch (e: Exception) {
                Toast.makeText(context, "Impossible de charger l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("Changer la photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ImagePickerCell(
                            emoji = "📷",
                            label = "Mes photos",
                            circleColor = MaterialTheme.colors.primary,
                            onClick = { showImageDialog = false; imageLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        )
                        ImagePickerCell(
                            emoji = "🔍",
                            label = "Google Images",
                            circleColor = MaterialTheme.colors.primary,
                            onClick = {
                                showImageDialog = false
                                waitingForClipboardImage = true
                                onOpenImageSearch()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!recipe.imageUrl.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ImagePickerCell(
                                emoji = "🗑️",
                                label = "Retirer",
                                circleColor = MaterialTheme.colors.error.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colors.error,
                                onClick = { showImageDialog = false; onImageChange("") },
                                modifier = Modifier.weight(1f)
                            )
                            ImagePickerCell(
                                emoji = "✕",
                                label = "Annuler",
                                circleColor = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                onClick = { showImageDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        ImagePickerCell(
                            emoji = "✕",
                            label = "Annuler",
                            circleColor = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            onClick = { showImageDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la recette ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cette action est définitive.")
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { showDeleteDialog = false; onDelete() },
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Supprimer", color = Color.White) }
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Annuler") }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        backgroundColor = ColorScreenBg,
        topBar = {
            AppTopBar(
                title = recipe.title.normalizeTitle(),
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = ColorTitleViolet)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(onClick = { menuExpanded = false; onEdit() }) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = ColorTitleViolet)
                                    Text("Modifier la recette")
                                }
                            }
                            DropdownMenuItem(onClick = {
                                menuExpanded = false
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, buildShareText(recipe))
                                }
                                context.startActivity(Intent.createChooser(intent, "Partager la recette"))
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp), tint = ColorTitleViolet)
                                    Text("Partager")
                                }
                            }
                            DropdownMenuItem(onClick = { menuExpanded = false; onDuplicate() }) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp), tint = ColorTitleViolet)
                                    Text("Dupliquer")
                                }
                            }
                            Divider()
                            DropdownMenuItem(onClick = { menuExpanded = false; showDeleteDialog = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = Color(0xFFCC0000))
                                    Text("Supprimer", color = Color(0xFFCC0000))
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).semantics { testTag = "detail_list" },
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero image
            item {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_recipe_placeholder),
                        error = painterResource(R.drawable.ic_recipe_placeholder),
                        fallback = painterResource(R.drawable.ic_recipe_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .background(ColorTitleViolet.copy(alpha = 0.85f), CircleShape)
                            .clickable { showImageDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Changer l'image",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Title + category chip
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        recipe.title.normalizeTitle(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    if (recipe.category != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF3ECFF)
                        ) {
                            Text(
                                "${categoryEmoji(recipe.category)} ${recipe.category}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                fontSize = 12.sp,
                                color = ColorTitleViolet,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Calories par part
            if (nutrition != null) {
                item {
                    NutritionBanner(
                        estimate = nutrition,
                        onOpenDetail = if (onWeightChange != null) ({ showNutritionDetail = true }) else null,
                        onServingsChange = onServingsChange
                    )
                }
            }

            // Description
            if (!recipe.description.isNullOrBlank()) {
                item {
                    Text(
                        recipe.description,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        style = MaterialTheme.typography.body1,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            // Ingredients card
            if (recipe.ingredients.isNotEmpty()) {
                item {
                    SectionCard(
                        emoji = "🧑‍🍳",
                        title = "Ingrédients",
                        action = {
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = recipe.ingredients.joinToString("\n") { "• $it" }
                                    cb.setPrimaryClip(ClipData.newPlainText("Ingrédients", text))
                                    Toast.makeText(context, "Ingrédients copiés ✓", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copier",
                                    modifier = Modifier.size(18.dp), tint = ColorTitleViolet)
                            }
                        }
                    ) {
                        recipe.ingredients.forEach { ingredient ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•  ", color = ColorTitleViolet, fontWeight = FontWeight.Bold)
                                Text(ingredient, style = MaterialTheme.typography.body1)
                            }
                        }
                    }
                }
            }

            // Steps card
            if (recipe.steps.isNotEmpty()) {
                item {
                    SectionCard(emoji = "🍳", title = "Préparation") {
                        val visibleSteps = if (stepsExpanded || recipe.steps.size <= 5)
                            recipe.steps else recipe.steps.take(5)
                        visibleSteps.forEachIndexed { i, step ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(ColorTitleViolet, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${i + 1}", color = Color.White, fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text(step, style = MaterialTheme.typography.body1,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        if (recipe.steps.size > 5) {
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { stepsExpanded = !stepsExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (stepsExpanded) "Voir moins ∧" else "Voir toutes les étapes ∨",
                                    color = ColorTitleViolet,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Notes card
            if (!recipe.notes.isNullOrBlank()) {
                item {
                    SectionCard(
                        emoji = "📋",
                        title = "Notes",
                        action = {
                            IconButton(
                                onClick = { onEdit() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier",
                                    modifier = Modifier.size(18.dp), tint = ColorTitleViolet)
                            }
                        }
                    ) {
                        Text(recipe.notes, style = MaterialTheme.typography.body1)
                    }
                }
            }

            // Tags
            if (recipe.tags.isNotEmpty()) {
                item {
                    Text(
                        "Tags : ${recipe.tags.joinToString(", ")}",
                        style = MaterialTheme.typography.body2,
                        color = ColorInactiveText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Source
            if (!recipe.sourceUrl.isNullOrBlank()) {
                item {
                    SectionCard(
                        emoji = "🔗",
                        title = "Source",
                        action = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("url", recipe.sourceUrl))
                                    Toast.makeText(context, "URL copiée 📋", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copier l'URL",
                                    tint = ColorInactiveText, modifier = Modifier.size(18.dp))
                            }
                        }
                    ) {
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recipe.sourceUrl)))
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                recipe.sourceUrl!!,
                                color = ColorTitleViolet,
                                style = MaterialTheme.typography.body2,
                                softWrap = true
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // Clipboard overlay — in Compose tree AFTER Scaffold = always on top of hero image
    if (showClipboardDialog) {
        BackHandler { dismissClipboardDialog() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { dismissClipboardDialog() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Utiliser l'image copiée ?", style = MaterialTheme.typography.h6)
                    if (isPreviewLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else if (clipboardPreviewFile != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(clipboardPreviewFile)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = "Aperçu",
                            contentScale = ContentScale.Crop,
                            fallback = painterResource(R.drawable.ic_recipe_placeholder),
                            error = painterResource(R.drawable.ic_recipe_placeholder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("Voulez-vous utiliser l'image que vous venez de copier depuis le navigateur ?")
                    }
                    Button(
                        onClick = { applyClipboardImage() },
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Oui, utiliser cette image") }
                    OutlinedButton(
                        onClick = { dismissClipboardDialog() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Annuler") }
                }
            }
        }
    }
    } // Box fillMaxSize
}

@Composable
private fun SectionCard(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 1.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "$emoji $title",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTitleViolet,
                    modifier = Modifier.weight(1f)
                )
                action?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ImagePickerCell(
    emoji: String,
    label: String,
    circleColor: Color,
    onClick: () -> Unit,
    contentColor: Color = Color.Unspecified,
    labelColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.h6, color = contentColor)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = labelColor,
            textAlign = TextAlign.Center
        )
    }
}

private fun buildShareText(recipe: Recipe): String = buildString {
    val emoji = categoryEmoji(recipe.category)
    appendLine("$emoji ${recipe.title}")
    recipe.category?.let { appendLine(it) }
    appendLine()
    recipe.description?.let { d -> if (d.isNotBlank()) { appendLine(d); appendLine() } }
    appendLine("📋 Ingrédients :")
    recipe.ingredients.forEach { appendLine("• $it") }
    appendLine()
    appendLine("👨‍🍳 Étapes de préparation :")
    recipe.steps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
    if (!recipe.notes.isNullOrBlank()) {
        appendLine()
        appendLine("💡 Notes :")
        appendLine(recipe.notes)
    }
    if (!recipe.sourceUrl.isNullOrBlank()) {
        appendLine()
        appendLine("🔗 Source : ${recipe.sourceUrl}")
    }
}
